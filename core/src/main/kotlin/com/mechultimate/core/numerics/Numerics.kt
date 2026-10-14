package com.mechultimate.core.numerics

import kotlin.math.abs
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.sqrt

object Numerics {
    /**
     * Brent's method root finder on [a, b]. Requires f(a) and f(b) to have opposite signs.
     */
    fun brent(a0: Double, b0: Double, tol: Double = 1e-12, maxIter: Int = 200, f: (Double) -> Double): Double {
        var a = a0
        var b = b0
        var fa = f(a)
        var fb = f(b)
        require(fa * fb <= 0) { "Root is not bracketed in [$a0, $b0]" }
        if (fa == 0.0) return a
        if (fb == 0.0) return b
        var c = a
        var fc = fa
        var d = b - a
        var e = d
        repeat(maxIter) {
            if (fb * fc > 0) { c = a; fc = fa; d = b - a; e = d }
            if (abs(fc) < abs(fb)) { a = b; b = c; c = a; fa = fb; fb = fc; fc = fa }
            val tol1 = 2 * Math.ulp(1.0) * abs(b) + 0.5 * tol
            val xm = 0.5 * (c - b)
            if (abs(xm) <= tol1 || fb == 0.0) return b
            if (abs(e) >= tol1 && abs(fa) > abs(fb)) {
                val s = fb / fa
                var p: Double
                var q: Double
                if (a == c) {
                    p = 2 * xm * s
                    q = 1 - s
                } else {
                    val qq = fa / fc
                    val r = fb / fc
                    p = s * (2 * xm * qq * (qq - r) - (b - a) * (r - 1))
                    q = (qq - 1) * (r - 1) * (s - 1)
                }
                if (p > 0) q = -q
                p = abs(p)
                if (2 * p < minOf(3 * xm * q - abs(tol1 * q), abs(e * q))) {
                    e = d
                    d = p / q
                } else {
                    d = xm; e = d
                }
            } else {
                d = xm; e = d
            }
            a = b
            fa = fb
            b += if (abs(d) > tol1) d else if (xm > 0) tol1 else -tol1
            fb = f(b)
        }
        return b
    }

    /** Linear interpolation in a table sorted by x. Throws outside the range unless [extrapolate]. */
    fun interp(xs: DoubleArray, ys: DoubleArray, x: Double, extrapolate: Boolean = false): Double {
        require(xs.size == ys.size && xs.size >= 2)
        if (!extrapolate) require(x >= xs.first() - 1e-12 && x <= xs.last() + 1e-12) { "x=$x outside table range" }
        var i = xs.indexOfFirst { it >= x }
        if (i == -1) i = xs.size - 1
        if (i == 0) i = 1
        val t = (x - xs[i - 1]) / (xs[i] - xs[i - 1])
        return ys[i - 1] + t * (ys[i] - ys[i - 1])
    }

    /**
     * Darcy friction factor. Laminar (Re < 2300): 64/Re. Otherwise the Colebrook–White equation
     * solved iteratively, starting from Swamee–Jain.
     */
    fun darcyFriction(re: Double, relRoughness: Double): Double {
        require(re > 0)
        if (re < 2300) return 64.0 / re
        var f = 0.25 / log10(relRoughness / 3.7 + 5.74 / re.pow(0.9)).pow(2)
        repeat(100) {
            val x = -2.0 * log10(relRoughness / 3.7 + 2.51 / (re * sqrt(f)))
            val fNew = 1.0 / (x * x)
            if (abs(fNew - f) < 1e-14) return fNew
            f = fNew
        }
        return f
    }
}
