package com.mechultimate.core.props

import com.mechultimate.core.numerics.Numerics
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.pow

/** Moist-air state. Temperatures °C, pressures Pa, W kg/kg, h J/kg dry air, v m³/kg dry air. */
data class MoistAir(
    val tdb: Double, val twb: Double, val tdp: Double, val rh: Double, val w: Double,
    val h: Double, val v: Double, val pw: Double, val pws: Double, val p: Double,
)

/** Psychrometric relations of ASHRAE Handbook — Fundamentals (2017), Chapter 1. */
object Psychro {
    /** Standard atmospheric pressure (Pa) at altitude [z] (m). */
    fun pressureAtAltitude(z: Double) = 101_325.0 * (1 - 2.25577e-5 * z).pow(5.2559)

    /** Saturation vapour pressure (Pa) over ice (t < 0 °C) or liquid water (t ≥ 0 °C), Hyland–Wexler. */
    fun pws(tC: Double): Double {
        val t = tC + 273.15
        val lnP = if (tC < 0) {
            -5.6745359e3 / t + 6.3925247 - 9.6778430e-3 * t + 6.2215701e-7 * t * t +
                2.0747825e-9 * t.pow(3) - 9.4840240e-13 * t.pow(4) + 4.1635019 * ln(t)
        } else {
            -5.8002206e3 / t + 1.3914993 - 4.8640239e-2 * t + 4.1764768e-5 * t * t -
                1.4452093e-8 * t.pow(3) + 6.5459673 * ln(t)
        }
        return exp(lnP)
    }

    fun humidityRatio(pw: Double, p: Double) = 0.621945 * pw / (p - pw)
    fun enthalpy(tC: Double, w: Double) = (1.006 * tC + w * (2501 + 1.86 * tC)) * 1000.0
    fun specificVolume(tC: Double, w: Double, p: Double) = 0.287042 * (tC + 273.15) * (1 + 1.607858 * w) / (p / 1000.0)

    /** Humidity ratio from dry bulb and thermodynamic wet bulb (ASHRAE eqs. 33/35). */
    fun wFromWetBulb(tdb: Double, twb: Double, p: Double): Double {
        val wsStar = humidityRatio(pws(twb), p)
        return if (twb >= 0) {
            ((2501 - 2.326 * twb) * wsStar - 1.006 * (tdb - twb)) / (2501 + 1.86 * tdb - 4.186 * twb)
        } else {
            ((2830 - 0.24 * twb) * wsStar - 1.006 * (tdb - twb)) / (2830 + 1.86 * tdb - 2.1 * twb)
        }
    }

    fun dewPoint(pw: Double): Double = Numerics.brent(-100.0, 200.0, 1e-9) { pws(it) - pw }

    fun wetBulb(tdb: Double, w: Double, p: Double, tdp: Double): Double =
        if (tdb - tdp < 1e-9) tdb else Numerics.brent(tdp - 1e-6, tdb, 1e-9) { wFromWetBulb(tdb, it, p) - w }

    /** Builds the full state from dry bulb and humidity ratio. */
    fun fromTdbW(tdb: Double, w: Double, p: Double): MoistAir {
        require(w >= 0) { "Humidity ratio must be non-negative" }
        val pw = w * p / (0.621945 + w)
        val pws = pws(tdb)
        require(pw <= pws * 1.0001) { "Air would be supersaturated at this dry-bulb temperature" }
        val tdp = if (pw > 0) dewPoint(pw) else Double.NEGATIVE_INFINITY
        val twb = wetBulb(tdb, w, p, if (pw > 0) tdp else -100.0)
        return MoistAir(tdb, twb, tdp, pw / pws, w, enthalpy(tdb, w), specificVolume(tdb, w, p), pw, pws, p)
    }

    fun fromTdbRh(tdb: Double, rh: Double, p: Double): MoistAir {
        require(rh in 0.0..1.0) { "Relative humidity must be between 0 and 100 %" }
        return fromTdbW(tdb, humidityRatio(rh * pws(tdb), p), p)
    }

    fun fromTdbTwb(tdb: Double, twb: Double, p: Double): MoistAir {
        require(twb <= tdb + 1e-9) { "Wet bulb cannot exceed dry bulb" }
        return fromTdbW(tdb, wFromWetBulb(tdb, twb, p), p)
    }

    fun fromTdbTdp(tdb: Double, tdp: Double, p: Double): MoistAir {
        require(tdp <= tdb + 1e-9) { "Dew point cannot exceed dry bulb" }
        return fromTdbW(tdb, humidityRatio(pws(tdp), p), p)
    }
}
