package com.mechultimate.core.props

import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.sqrt

/** Water/steam state in SI: p Pa, T K, v m³/kg, h J/kg, s J/(kg·K), cp J/(kg·K). */
data class SteamState(val p: Double, val t: Double, val v: Double, val h: Double, val s: Double, val cp: Double, val region: Int) {
    val rho: Double get() = 1.0 / v
}

/**
 * IAPWS Industrial Formulation 1997 (IAPWS-IF97): region 1 (compressed liquid), region 2
 * (superheated vapour) and region 4 (saturation line). Region 3 (near-critical) and region 5
 * (above 1073.15 K) are not implemented in the MVP and are reported as out of range.
 */
object IF97 {
    private const val R = 461.526 // J/(kg·K)
    const val T_CRIT = 647.096
    const val P_CRIT = 22.064e6
    const val T_MIN = 273.15
    const val T_MAX = 1073.15
    const val P_MAX = 100e6
    /** Upper temperature of region 1 and the region 1/3 boundary. */
    const val T13 = 623.15

    private val n4 = doubleArrayOf(
        0.11670521452767e4, -0.72421316703206e6, -0.17073846940092e2, 0.12020824702470e5,
        -0.32325550322333e7, 0.14915108613530e2, -0.48232657361591e4, 0.40511340542057e6,
        -0.23855557567849, 0.65017534844798e3,
    )

    /** Saturation pressure (Pa) at temperature [t] (K), 273.15 K ≤ T ≤ 647.096 K. */
    fun psat(t: Double): Double {
        require(t in T_MIN..T_CRIT) { "Saturation temperature out of range" }
        val th = t + n4[8] / (t - n4[9])
        val a = th * th + n4[0] * th + n4[1]
        val b = n4[2] * th * th + n4[3] * th + n4[4]
        val c = n4[5] * th * th + n4[6] * th + n4[7]
        return (2 * c / (-b + sqrt(b * b - 4 * a * c))).pow(4) * 1e6
    }

    /** Saturation temperature (K) at pressure [p] (Pa), 611.213 Pa ≤ p ≤ 22.064 MPa. */
    fun tsat(p: Double): Double {
        require(p in 611.212..P_CRIT) { "Saturation pressure out of range" }
        val beta = (p / 1e6).pow(0.25)
        val e = beta * beta + n4[2] * beta + n4[5]
        val f = n4[0] * beta * beta + n4[3] * beta + n4[6]
        val g = n4[1] * beta * beta + n4[4] * beta + n4[7]
        val d = 2 * g / (-f - sqrt(f * f - 4 * e * g))
        return (n4[9] + d - sqrt((n4[9] + d).pow(2) - 4 * (n4[8] + n4[9] * d))) / 2
    }

    /** Boundary between regions 2 and 3: pressure (Pa) as a function of T (K). */
    fun pB23(t: Double): Double = (0.34805185628969e3 - 0.11671859879975e1 * t + 0.10192970039326e-2 * t * t) * 1e6

    private val i1 = intArrayOf(0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 1, 1, 2, 2, 2, 2, 2, 3, 3, 3, 4, 4, 4, 5, 8, 8, 21, 23, 29, 30, 31, 32)
    private val j1 = intArrayOf(-2, -1, 0, 1, 2, 3, 4, 5, -9, -7, -1, 0, 1, 3, -3, 0, 1, 3, 17, -4, 0, 6, -5, -2, 10, -8, -11, -6, -29, -31, -38, -39, -40, -41)
    private val n1 = doubleArrayOf(
        0.14632971213167, -0.84548187169114, -0.37563603672040e1, 0.33855169168385e1,
        -0.95791963387872, 0.15772038513228, -0.16616417199501e-1, 0.81214629983568e-3,
        0.28319080123804e-3, -0.60706301565874e-3, -0.18990068218419e-1, -0.32529748770505e-1,
        -0.21841717175414e-1, -0.52838357969930e-4, -0.47184321073267e-3, -0.30001780793026e-3,
        0.47661393906987e-4, -0.44141845330846e-5, -0.72694996297594e-15, -0.31679644845054e-4,
        -0.28270797985312e-5, -0.85205128120103e-9, -0.22425281908000e-5, -0.65171222895601e-6,
        -0.14341729937924e-12, -0.40516996860117e-6, -0.12734301741641e-8, -0.17424871230634e-9,
        -0.68762131295531e-18, 0.14478307828521e-19, 0.26335781662795e-22, -0.11947622640071e-22,
        0.18228094581404e-23, -0.93537087292458e-25,
    )

    /** Region 1 (compressed / saturated liquid). */
    fun region1(p: Double, t: Double): SteamState {
        val pi = p / 16.53e6
        val tau = 1386.0 / t
        val a = 7.1 - pi
        val b = tau - 1.222
        var g = 0.0; var gp = 0.0; var gt = 0.0; var gtt = 0.0
        for (k in n1.indices) {
            val i = i1[k]; val j = j1[k]; val n = n1[k]
            g += n * a.pow(i) * b.pow(j)
            gp += -n * i * a.pow(i - 1) * b.pow(j)
            gt += n * a.pow(i) * j * b.pow(j - 1)
            gtt += n * a.pow(i) * j * (j - 1) * b.pow(j - 2)
        }
        return SteamState(
            p, t,
            v = pi * gp * R * t / p,
            h = tau * gt * R * t,
            s = (tau * gt - g) * R,
            cp = -tau * tau * gtt * R,
            region = 1,
        )
    }

    private val j0 = intArrayOf(0, 1, -5, -4, -3, -2, -1, 2, 3)
    private val n0 = doubleArrayOf(
        -0.96927686500217e1, 0.10086655968018e2, -0.56087911283020e-2, 0.71452738081455e-1,
        -0.40710498223928, 0.14240819171444e1, -0.43839511319450e1, -0.28408632460772, 0.21268463753307e-1,
    )
    private val ir = intArrayOf(1, 1, 1, 1, 1, 2, 2, 2, 2, 2, 3, 3, 3, 3, 3, 4, 4, 4, 5, 6, 6, 6, 7, 7, 7, 8, 8, 9, 10, 10, 10, 16, 16, 18, 20, 20, 20, 21, 22, 23, 24, 24, 24)
    private val jr = intArrayOf(0, 1, 2, 3, 6, 1, 2, 4, 7, 36, 0, 1, 3, 6, 35, 1, 2, 3, 7, 3, 16, 35, 0, 11, 25, 8, 36, 13, 4, 10, 14, 29, 50, 57, 20, 35, 48, 21, 53, 39, 26, 40, 58)
    private val nr = doubleArrayOf(
        -0.17731742473213e-2, -0.17834862292358e-1, -0.45996013696365e-1, -0.57581259083432e-1,
        -0.50325278727930e-1, -0.33032641670203e-4, -0.18948987516315e-3, -0.39392777243355e-2,
        -0.43797295650573e-1, -0.26674547914087e-4, 0.20481737692309e-7, 0.43870667284435e-6,
        -0.32277677238570e-4, -0.15033924542148e-2, -0.40668253562649e-1, -0.78847309559367e-9,
        0.12790717852285e-7, 0.48225372718507e-6, 0.22922076337661e-5, -0.16714766451061e-10,
        -0.21171472321355e-2, -0.23895741934104e2, -0.59059564324270e-17, -0.12621808899101e-5,
        -0.38946842435739e-1, 0.11256211360459e-10, -0.82311340897998e1, 0.19809712802088e-7,
        0.10406965210174e-18, -0.10234747095929e-12, -0.10018179379511e-8, -0.80882908646985e-10,
        0.10693031879409, -0.33662250574171, 0.89185845355421e-24, 0.30629316876232e-12,
        -0.42002467698208e-5, -0.59056029685639e-25, 0.37826947613457e-5, -0.12768608934681e-14,
        0.73087610595061e-28, 0.55414715350778e-16, -0.94369707241210e-6,
    )

    /** Region 2 (superheated / saturated vapour). */
    fun region2(p: Double, t: Double): SteamState {
        val pi = p / 1e6
        val tau = 540.0 / t
        var g0 = ln(pi); var g0t = 0.0; var g0tt = 0.0
        for (k in n0.indices) {
            g0 += n0[k] * tau.pow(j0[k])
            g0t += n0[k] * j0[k] * tau.pow(j0[k] - 1)
            g0tt += n0[k] * j0[k] * (j0[k] - 1) * tau.pow(j0[k] - 2)
        }
        val b = tau - 0.5
        var gr = 0.0; var grp = 0.0; var grt = 0.0; var grtt = 0.0
        for (k in nr.indices) {
            val i = ir[k]; val j = jr[k]; val n = nr[k]
            gr += n * pi.pow(i) * b.pow(j)
            grp += n * i * pi.pow(i - 1) * b.pow(j)
            grt += n * pi.pow(i) * j * b.pow(j - 1)
            grtt += n * pi.pow(i) * j * (j - 1) * b.pow(j - 2)
        }
        return SteamState(
            p, t,
            v = pi * (1 / pi + grp) * R * t / p,
            h = tau * (g0t + grt) * R * t,
            s = (tau * (g0t + grt) - (g0 + gr)) * R,
            cp = -tau * tau * (g0tt + grtt) * R,
            region = 2,
        )
    }

    /** Single-phase state at (p, T), choosing region 1 or 2. Throws for unsupported regions. */
    fun pt(p: Double, t: Double): SteamState {
        require(p > 0 && p <= P_MAX) { "Pressure must be between 0 and 100 MPa" }
        require(t in T_MIN..T_MAX) { "Temperature must be between 0 °C and 800 °C" }
        if (t <= T13) {
            val ps = psat(t)
            return if (p >= ps) region1(p, t) else region2(p, t)
        }
        require(p <= pB23(t)) { "State is in IF97 region 3 (near-critical), not supported in this version" }
        return region2(p, t)
    }

    /** Saturated liquid and vapour at temperature [t] (K), limited to T ≤ 623.15 K. */
    fun saturationT(t: Double): Pair<SteamState, SteamState> {
        require(t in T_MIN..T13) { "Saturation temperature must be between 0 °C and 350 °C in this version" }
        val p = psat(t)
        return region1(p, t) to region2(p, t)
    }
}
