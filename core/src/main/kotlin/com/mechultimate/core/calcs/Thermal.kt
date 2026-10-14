package com.mechultimate.core.calcs

import com.mechultimate.core.engine.Module
import com.mechultimate.core.engine.Status
import com.mechultimate.core.engine.calculator
import com.mechultimate.core.engine.visibleIf
import com.mechultimate.core.props.IF97
import com.mechultimate.core.props.Psychro
import com.mechultimate.core.props.SteamState
import com.mechultimate.core.engine.CalcScope
import com.mechultimate.core.numerics.Numerics
import com.mechultimate.core.units.Kind
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sqrt

object Thermo {
    private fun CalcScope.state(prefix: String, s: SteamState, primary: Boolean = false) {
        out("$prefix specific volume v", Kind.SPECIFIC_VOLUME, s.v)
        out("$prefix density ρ", Kind.DENSITY, s.rho)
        out("$prefix enthalpy h", Kind.SPECIFIC_ENERGY, s.h, primary = primary)
        out("$prefix entropy s", Kind.SPECIFIC_ENTROPY, s.s)
        out("$prefix isobaric heat capacity c_p", Kind.SPECIFIC_ENTROPY, s.cp)
    }

    val steam = calculator("THM-001", "Steam Tables — IAPWS-IF97", Module.THERMO) {
        description = "Thermodynamic properties of water and steam: saturation by temperature or pressure, or compressed liquid / superheated steam."
        keywords("steam", "water", "IF97", "IAPWS", "enthalpy", "entropy", "saturation", "superheated")
        ref("IAPWS-IF97", note = "Regions 1, 2 and 4")
        choice("mode", "Lookup", listOf("satT" to "Saturation — by temperature", "satP" to "Saturation — by pressure", "pt" to "Single phase — by pressure and temperature"))
        number("T", "Temperature", Kind.TEMPERATURE, 453.15, 273.15, 1073.15, visibleIf = visibleIf("mode", "satT", "pt"))
        positive("P", "Pressure (absolute)", Kind.PRESSURE_ABS, 10e5, max = 100e6, visibleIf = visibleIf("mode", "satP", "pt"))
        compute {
            try {
                when (c("mode")) {
                    "pt" -> {
                        val s = IF97.pt(n("P"), n("T"))
                        text("Phase", if (s.region == 1) "Compressed (sub-cooled) liquid — region 1" else "Superheated vapour — region 2", primary = true)
                        state("", s, primary = true)
                        if (n("T") <= IF97.T13) step("Saturation temperature at P", "T_sat(P)", Kind.TEMPERATURE, if (n("P") <= IF97.P_CRIT) IF97.tsat(n("P")) else Double.NaN, output = true)
                    }
                    else -> {
                        val t = if (c("mode") == "satT") n("T") else step("Saturation temperature", "T_sat(P) — region 4", Kind.TEMPERATURE, IF97.tsat(n("P")), primary = true)
                        if (c("mode") == "satT") step("Saturation pressure", "p_sat(T) — region 4", Kind.PRESSURE_ABS, IF97.psat(t), primary = true)
                        val (l, v) = IF97.saturationT(t)
                        state("Liquid", l)
                        state("Vapour", v)
                        out("Latent heat h_fg", Kind.SPECIFIC_ENERGY, v.h - l.h, primary = true)
                        out("Entropy of vaporization s_fg", Kind.SPECIFIC_ENTROPY, v.s - l.s)
                    }
                }
            } catch (e: IllegalArgumentException) {
                fail(e.message ?: "State out of range")
            }
        }
    }

    val insulatedPipe = calculator("THM-009", "Heat Loss — Insulated Pipe / Plane Wall", Module.THERMO) {
        description = "Steady conduction through a pipe wall and insulation with outside convection, or through a plane wall."
        keywords("heat loss", "insulation", "conduction", "thermal resistance", "surface temperature", "personnel protection")
        ref("Incropera, Fundamentals of Heat and Mass Transfer", "Ch. 3")
        choice("geo", "Geometry", listOf("pipe" to "Insulated pipe (per unit length)", "wall" to "Plane wall (per unit area)"))
        positive("r1", "Pipe inside radius", Kind.LENGTH, 0.0767, visibleIf = visibleIf("geo", "pipe"))
        positive("r2", "Pipe outside radius", Kind.LENGTH, 0.0841, visibleIf = visibleIf("geo", "pipe"))
        positive("k1", "Pipe wall conductivity", Kind.CONDUCTIVITY, 45.0, visibleIf = visibleIf("geo", "pipe"))
        positive("ti", "Insulation / wall thickness", Kind.LENGTH, 0.05)
        positive("k2", "Insulation / wall conductivity", Kind.CONDUCTIVITY, 0.045)
        positive("ho", "Outside film coefficient (convection + radiation)", Kind.HTC, 10.0)
        number("Ti", "Inside (fluid) temperature", Kind.TEMPERATURE, 453.15)
        number("To", "Ambient temperature", Kind.TEMPERATURE, 293.15)
        compute {
            val dt = n("Ti") - n("To")
            if (c("geo") == "pipe") {
                val r3 = n("r2") + n("ti")
                require(n("r2") > n("r1"), "Outside radius must exceed inside radius")
                val rw = step("Pipe wall resistance (m·K/W)", "R₁ = ln(r₂/r₁) / (2π·k₁)", null, ln(n("r2") / n("r1")) / (2 * PI * n("k1")))
                val ri = step("Insulation resistance (m·K/W)", "R₂ = ln(r₃/r₂) / (2π·k₂)", null, ln(r3 / n("r2")) / (2 * PI * n("k2")))
                val ro = step("Surface film resistance (m·K/W)", "R₃ = 1 / (2π·r₃·h_o)", null, 1 / (2 * PI * r3 * n("ho")))
                val q = step("Heat loss per length", "q′ = ΔT / ΣR", Kind.HEAT_PER_LENGTH, dt / (rw + ri + ro), primary = true)
                val ts = step("Insulation surface temperature", "T_s = T_o + q′·R₃", Kind.TEMPERATURE, n("To") + q * ro, primary = true)
                check(ts <= 333.15, "Surface ≤ 60 °C (personnel protection)", "Surface > 60 °C: personnel protection required", Status.WARN)
            } else {
                val r = step("Total resistance per m² (m²·K/W)", "R = t/k + 1/h_o", null, n("ti") / n("k2") + 1 / n("ho"))
                val q = step("Heat flux", "q″ = ΔT / R", Kind.HEAT_FLUX, dt / r, primary = true)
                step("Outer surface temperature", "T_s = T_o + q″/h_o", Kind.TEMPERATURE, n("To") + q / n("ho"), output = true)
            }
        }
    }

    val lmtd = calculator("THM-014", "Heat Exchanger — LMTD Method", Module.THERMO) {
        description = "Log-mean temperature difference and required area for a known duty."
        keywords("LMTD", "heat exchanger", "area", "duty", "counterflow", "shell and tube", "F factor")
        ref("TEMA", note = "LMTD with correction factor F")
        number("Thi", "Hot inlet", Kind.TEMPERATURE, 393.15)
        number("Tho", "Hot outlet", Kind.TEMPERATURE, 343.15)
        number("Tci", "Cold inlet", Kind.TEMPERATURE, 303.15)
        number("Tco", "Cold outlet", Kind.TEMPERATURE, 333.15)
        choice("arr", "Flow arrangement", listOf("counter" to "Counterflow", "parallel" to "Parallel flow"))
        number("F", "LMTD correction factor F", Kind.DIMENSIONLESS, 1.0, 0.5, 1.0, help = "1.0 for pure counterflow; from charts for multipass shell & tube")
        positive("Q", "Heat duty Q", Kind.POWER, 500_000.0)
        positive("U", "Overall heat transfer coefficient U", Kind.HTC, 500.0)
        compute {
            val counter = c("arr") == "counter"
            val d1 = step("ΔT₁", if (counter) "T_hi − T_co" else "T_hi − T_ci", Kind.TEMP_DIFF, if (counter) n("Thi") - n("Tco") else n("Thi") - n("Tci"))
            val d2 = step("ΔT₂", if (counter) "T_ho − T_ci" else "T_ho − T_co", Kind.TEMP_DIFF, if (counter) n("Tho") - n("Tci") else n("Tho") - n("Tco"))
            require(d1 > 0 && d2 > 0, "Temperature cross: terminal differences must be positive")
            val lm = step("LMTD", "LMTD = (ΔT₁ − ΔT₂) / ln(ΔT₁/ΔT₂)", Kind.TEMP_DIFF, if (abs(d1 - d2) < 1e-9) d1 else (d1 - d2) / ln(d1 / d2), output = true)
            step("Required area A", "A = Q / (U·F·LMTD)", Kind.AREA, n("Q") / (n("U") * n("F") * lm), primary = true, si = "m²", us = "ft²")
            if (n("F") < 0.75) warn("F < 0.75: consider more shell passes")
        }
    }

    val entu = calculator("THM-015", "Heat Exchanger — ε-NTU Method", Module.THERMO) {
        description = "Duty and outlet temperatures of an exchanger with known area (rating)."
        keywords("NTU", "effectiveness", "rating", "heat exchanger", "outlet temperature", "crossflow")
        ref("Kays & London / Incropera", "Table 11.3")
        positive("mh", "Hot-side mass flow", Kind.MASS_FLOW, 2.0)
        positive("cph", "Hot-side c_p", Kind.SPECIFIC_ENTROPY, 4190.0)
        positive("mc", "Cold-side mass flow", Kind.MASS_FLOW, 3.0)
        positive("cpc", "Cold-side c_p", Kind.SPECIFIC_ENTROPY, 4180.0)
        number("Thi", "Hot inlet", Kind.TEMPERATURE, 363.15)
        number("Tci", "Cold inlet", Kind.TEMPERATURE, 293.15)
        positive("U", "Overall U", Kind.HTC, 800.0)
        positive("A", "Area", Kind.AREA, 20.0, si = "m²", us = "ft²")
        choice("cfg", "Configuration", listOf("counter" to "Counterflow", "parallel" to "Parallel flow", "shell1" to "Shell & tube, 1 shell pass (2, 4… tube passes)", "cross" to "Crossflow, both fluids unmixed"))
        compute {
            val ch = n("mh") * n("cph"); val cc = n("mc") * n("cpc")
            val cmin = step("C_min", "min(ṁc_p)", Kind.DIMENSIONLESS, minOf(ch, cc))
            val cr = step("Capacity ratio C_r", "C_min / C_max", Kind.DIMENSIONLESS, cmin / maxOf(ch, cc))
            val ntu = step("NTU", "NTU = U·A / C_min", Kind.DIMENSIONLESS, n("U") * n("A") / cmin, output = true)
            val eps = step("Effectiveness ε", "per configuration", Kind.RATIO, when (c("cfg")) {
                "counter" -> if (abs(1 - cr) < 1e-9) ntu / (1 + ntu) else (1 - exp(-ntu * (1 - cr))) / (1 - cr * exp(-ntu * (1 - cr)))
                "parallel" -> (1 - exp(-ntu * (1 + cr))) / (1 + cr)
                "shell1" -> { val s = sqrt(1 + cr * cr); 2 / (1 + cr + s * (1 + exp(-ntu * s)) / (1 - exp(-ntu * s))) }
                else -> 1 - exp((1 / cr) * ntu.pow(0.22) * (exp(-cr * ntu.pow(0.78)) - 1))
            }, output = true)
            val q = step("Heat duty Q", "Q = ε·C_min·(T_hi − T_ci)", Kind.POWER, eps * cmin * (n("Thi") - n("Tci")), primary = true)
            step("Hot outlet", "T_ho = T_hi − Q/C_h", Kind.TEMPERATURE, n("Thi") - q / ch, output = true)
            step("Cold outlet", "T_co = T_ci + Q/C_c", Kind.TEMPERATURE, n("Tci") + q / cc, output = true)
        }
    }

    val all = listOf(steam, insulatedPipe, lmtd, entu)
}

object Hvac {
    val psychro = calculator("HVC-001", "Psychrometrics — Moist Air Properties", Module.HVAC) {
        description = "Full moist-air state from dry bulb plus relative humidity, wet bulb or dew point, at any altitude."
        keywords("psychrometric", "humidity", "wet bulb", "dew point", "enthalpy", "relative humidity", "moist air")
        ref("ASHRAE Handbook — Fundamentals (2017)", "Ch. 1", "Hyland–Wexler")
        choice("mode", "Second property", listOf("rh" to "Relative humidity", "wb" to "Wet-bulb temperature", "dp" to "Dew-point temperature"))
        number("tdb", "Dry-bulb temperature", Kind.TEMPERATURE, 298.15, 173.15, 473.15)
        number("rh", "Relative humidity", Kind.RATIO, 0.5, 0.0, 1.0, visibleIf = visibleIf("mode", "rh"))
        number("twb", "Wet-bulb temperature", Kind.TEMPERATURE, 291.15, 173.15, 473.15, visibleIf = visibleIf("mode", "wb"))
        number("tdp", "Dew-point temperature", Kind.TEMPERATURE, 287.15, 173.15, 473.15, visibleIf = visibleIf("mode", "dp"))
        number("z", "Altitude", Kind.LENGTH, 0.0, -500.0, 9000.0, si = "m", us = "ft")
        compute {
            val p = step("Atmospheric pressure", "p = 101.325·(1 − 2.25577×10⁻⁵·Z)^5.2559", Kind.PRESSURE_ABS, Psychro.pressureAtAltitude(n("z")), "Eq. (3)", output = true)
            val tdb = n("tdb") - 273.15
            val st = try {
                when (c("mode")) {
                    "rh" -> Psychro.fromTdbRh(tdb, n("rh"), p)
                    "wb" -> Psychro.fromTdbTwb(tdb, n("twb") - 273.15, p)
                    else -> Psychro.fromTdbTdp(tdb, n("tdp") - 273.15, p)
                }
            } catch (e: IllegalArgumentException) { fail(e.message ?: "Invalid state") }
            step("Saturation pressure p_ws", "Hyland–Wexler", Kind.PRESSURE_ABS, st.pws, "Eq. (5)/(6)")
            step("Vapour partial pressure p_w", "p_w = W·p/(0.621945 + W)", Kind.PRESSURE_ABS, st.pw)
            step("Humidity ratio W", "W = 0.621945·p_w/(p − p_w)", Kind.HUMIDITY_RATIO, st.w, "Eq. (20)", primary = true)
            step("Relative humidity", "φ = p_w / p_ws", Kind.RATIO, st.rh, output = true)
            step("Dew point", "p_ws(t_d) = p_w", Kind.TEMPERATURE, st.tdp + 273.15, output = true)
            step("Wet bulb", "solve Eq. (33)/(35) for t*", Kind.TEMPERATURE, st.twb + 273.15, output = true)
            step("Enthalpy h", "h = 1.006·t + W·(2501 + 1.86·t)", Kind.SPECIFIC_ENERGY, st.h, "Eq. (32)", primary = true)
            step("Specific volume v", "v = 0.287042·(t + 273.15)·(1 + 1.607858·W)/p", Kind.SPECIFIC_VOLUME, st.v, "Eq. (28)", output = true)
            out("Moist air density", Kind.DENSITY, (1 + st.w) / st.v)
        }
    }

    val airflow = calculator("HVC-004", "Supply Airflow from Sensible Load", Module.HVAC) {
        description = "Supply air volume needed to remove a sensible heat load at a given supply temperature difference."
        keywords("airflow", "cfm", "sensible load", "supply air", "1.08", "air quantity")
        ref("ASHRAE Handbook — Fundamentals", note = "q_s = ρ·c_p·Q·ΔT")
        positive("qs", "Sensible load", Kind.POWER, 10_000.0)
        positive("dt", "Room − supply temperature difference", Kind.TEMP_DIFF, 10.0)
        positive("rho", "Air density", Kind.DENSITY, 1.2)
        positive("cp", "Air specific heat c_p (moist)", Kind.SPECIFIC_ENTROPY, 1020.0)
        compute {
            val q = step("Supply airflow", "Q = q_s / (ρ·c_p·ΔT)", Kind.VOLUME_FLOW, n("qs") / (n("rho") * n("cp") * n("dt")), primary = true, si = "m³/h", us = "cfm")
            step("Mass flow", "ṁ = ρ·Q", Kind.MASS_FLOW, n("rho") * q, output = true)
        }
    }

    val duct = calculator("HVC-005", "Duct Sizing & Friction Loss", Module.HVAC) {
        description = "Velocity and friction loss in round or rectangular ducts; equivalent round diameter of rectangular ducts."
        keywords("duct", "friction", "equivalent diameter", "rectangular duct", "velocity", "SMACNA", "Pa/m")
        ref("ASHRAE Handbook — Fundamentals", "Ch. 21", "Equivalent diameter and Darcy friction")
        choice("shape", "Duct shape", listOf("round" to "Round", "rect" to "Rectangular"))
        positive("Q", "Airflow", Kind.VOLUME_FLOW, 1.0)
        positive("D", "Diameter", Kind.LENGTH, 0.4, visibleIf = visibleIf("shape", "round"))
        positive("a", "Width a", Kind.LENGTH, 0.5, visibleIf = visibleIf("shape", "rect"))
        positive("b", "Height b", Kind.LENGTH, 0.3, visibleIf = visibleIf("shape", "rect"))
        positive("L", "Length", Kind.LENGTH, 10.0, si = "m", us = "ft")
        number("eps", "Roughness ε", Kind.LENGTH, 9e-5, 0.0, help = "Galvanized steel 0.09 mm")
        positive("rho", "Air density", Kind.DENSITY, 1.2)
        positive("mu", "Air viscosity", Kind.DYN_VISCOSITY, 1.81e-5)
        compute {
            val (area, dh) = if (c("shape") == "round") {
                PI * n("D").pow(2) / 4 to n("D")
            } else {
                val a = n("a"); val b = n("b")
                val de = step("Equivalent round diameter", "D_e = 1.30·(a·b)^0.625 / (a + b)^0.25", Kind.LENGTH, 1.30 * (a * b).pow(0.625) / (a + b).pow(0.25), "Eq. (25)", output = true)
                a * b to de
            }
            val v = step("Velocity", "v = Q / A", Kind.VELOCITY, n("Q") / area, primary = true, si = "m/s", us = "ft/min")
            val vEq = n("Q") / (PI * dh * dh / 4)
            val re = step("Reynolds number", "Re = ρ·v·D / μ", Kind.DIMENSIONLESS, n("rho") * vEq * dh / n("mu"))
            val f = step("Friction factor", "Colebrook", Kind.DIMENSIONLESS, Numerics.darcyFriction(re, n("eps") / dh))
            val grad = step("Friction gradient", "ΔP/L = f/D·ρv²/2", Kind.PRESSURE_DIFF, f / dh * n("rho") * vEq * vEq / 2, output = true, si = "Pa", us = "inH₂O")
            step("Friction loss", "ΔP = (ΔP/L)·L", Kind.PRESSURE_DIFF, grad * n("L"), primary = true, si = "Pa", us = "inH₂O")
            check(grad <= 1.0 * 1.0, "Friction rate ≤ 1 Pa/m (low-velocity design)", "Friction rate > 1 Pa/m: higher than typical low-velocity design", Status.WARN)
        }
    }

    val all = listOf(psychro, airflow, duct)
}
