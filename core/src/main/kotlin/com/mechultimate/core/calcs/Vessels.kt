package com.mechultimate.core.calcs

import com.mechultimate.core.engine.CalcScope
import com.mechultimate.core.engine.CalculatorBuilder
import com.mechultimate.core.engine.Module
import com.mechultimate.core.engine.Status
import com.mechultimate.core.engine.calculator
import com.mechultimate.core.engine.visibleIf
import com.mechultimate.core.units.G0
import com.mechultimate.core.units.Kind
import kotlin.math.PI
import kotlin.math.acos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sqrt

object Vessels {
    private fun CalculatorBuilder.common(diaLabel: String, diaDefault: Double) {
        positive("P", "Internal design pressure P", Kind.PRESSURE_GAUGE, 10e5, help = "Include static liquid head at the component")
        positive("D", diaLabel, Kind.LENGTH, diaDefault)
        positive("S", "Allowable stress S", Kind.STRESS, 138e6, help = "From ASME II-D Table 1A at design temperature (e.g. SA-516-70 = 138 MPa / 20 ksi up to 343 °C)")
        number("E", "Joint efficiency E", Kind.DIMENSIONLESS, 1.0, 0.0, 1.0, true, help = "Table UW-12: Type 1 full RT 1.0, spot RT 0.85, none 0.70")
        number("ca", "Corrosion allowance", Kind.LENGTH, 3e-3, 0.0)
        number("tn", "Nominal thickness provided (for MAWP)", Kind.LENGTH, 0.012, 0.0, help = "Leave 0 to skip the MAWP calculation")
    }

    /** Records required thickness, MAWP and pass/fail for a component. */
    private fun CalcScope.finish(tReq: Double, ca: Double, mawp: (Double) -> Double) {
        val tMin = step("Required thickness incl. corrosion", "t_r = t + c", Kind.LENGTH, tReq + ca, primary = true)
        val tn = n("tn")
        if (tn > 0) {
            val tc = tn - ca
            require(tc > 0, "Nominal thickness must exceed corrosion allowance")
            step("MAWP (corroded)", "MAWP from rearranged equation with t = t_n − c", Kind.PRESSURE_GAUGE, mawp(tc), output = true)
            check(tn >= tMin, "Provided thickness ≥ required", "Provided thickness is less than required")
        }
    }

    val cylinder = calculator("VES-001", "Cylindrical Shell — Internal Pressure (UG-27)", Module.VESSELS) {
        description = "Required thickness and MAWP of a cylindrical shell under internal pressure (circumferential and longitudinal stress)."
        keywords("shell", "cylinder", "UG-27", "pressure vessel", "MAWP", "Section VIII")
        ref("ASME BPVC VIII-1", "UG-27(c)", "Eqs. (1) and (2)")
        common("Inside diameter D (uncorroded)", 1.5)
        compute {
            val ca = n("ca"); val p = n("P"); val s = n("S"); val e = n("E")
            val r = step("Corroded inside radius R", "R = D/2 + c", Kind.LENGTH, n("D") / 2 + ca)
            val tc = step("Circumferential stress thickness", "t = P·R / (S·E − 0.6·P)", Kind.LENGTH, p * r / (s * e - 0.6 * p), "UG-27(c)(1)", output = true)
            val tl = step("Longitudinal stress thickness", "t = P·R / (2·S·E + 0.4·P)", Kind.LENGTH, p * r / (2 * s * e + 0.4 * p), "UG-27(c)(2)", output = true)
            check(p <= 0.385 * s * e, "P ≤ 0.385·S·E — thin-shell rules apply", "P > 0.385·S·E: use Appendix 1-2 thick-shell formulas")
            check(max(tc, tl) <= r / 2, "t ≤ R/2", "t > R/2: use Appendix 1-2")
            finish(max(tc, tl), ca) { t -> min(s * e * t / (r + 0.6 * t), 2 * s * e * t / (r - 0.4 * t)) }
        }
    }

    val sphere = calculator("VES-002", "Spherical Shell / Hemispherical Head (UG-27 / UG-32)", Module.VESSELS) {
        description = "Required thickness and MAWP of a spherical shell or hemispherical head."
        keywords("sphere", "hemispherical", "hemi head", "UG-32")
        ref("ASME BPVC VIII-1", "UG-27(d), UG-32(f)")
        common("Inside diameter D (uncorroded)", 1.5)
        compute {
            val ca = n("ca"); val p = n("P"); val s = n("S"); val e = n("E")
            val l = step("Corroded inside radius L", "L = D/2 + c", Kind.LENGTH, n("D") / 2 + ca)
            val t = step("Pressure thickness t", "t = P·L / (2·S·E − 0.2·P)", Kind.LENGTH, p * l / (2 * s * e - 0.2 * p), "UG-27(d)", output = true)
            check(p <= 0.665 * s * e, "P ≤ 0.665·S·E", "P > 0.665·S·E: use Appendix 1-3")
            finish(t, ca) { tc -> 2 * s * e * tc / (l + 0.2 * tc) }
        }
    }

    val ellipsoidal = calculator("VES-003", "2:1 Ellipsoidal Head (UG-32)", Module.VESSELS) {
        description = "Required thickness and MAWP of a 2:1 semi-ellipsoidal head under pressure on the concave side."
        keywords("ellipsoidal", "2:1 head", "elliptical", "dished head", "UG-32")
        ref("ASME BPVC VIII-1", "UG-32(d)")
        common("Inside diameter D (uncorroded)", 1.5)
        compute {
            val ca = n("ca"); val p = n("P"); val s = n("S"); val e = n("E")
            val d = step("Corroded inside diameter", "D_c = D + 2c", Kind.LENGTH, n("D") + 2 * ca)
            val t = step("Pressure thickness t", "t = P·D / (2·S·E − 0.2·P)", Kind.LENGTH, p * d / (2 * s * e - 0.2 * p), "UG-32(d)", output = true)
            finish(t, ca) { tc -> 2 * s * e * tc / (d + 0.2 * tc) }
            info("Minimum thickness after forming must be specified to account for thinning in the knuckle.")
        }
    }

    val torispherical = calculator("VES-004", "Torispherical (F&D) Head (UG-32 / App. 1-4)", Module.VESSELS) {
        description = "Required thickness and MAWP of a torispherical head using the general M factor."
        keywords("torispherical", "flanged and dished", "F&D", "ASME head", "knuckle", "crown radius")
        ref("ASME BPVC VIII-1", "UG-32(e), Appendix 1-4(d)")
        positive("P", "Internal design pressure P", Kind.PRESSURE_GAUGE, 10e5)
        positive("L", "Inside crown radius L (uncorroded)", Kind.LENGTH, 1.5)
        positive("r", "Inside knuckle radius r (uncorroded)", Kind.LENGTH, 0.09)
        positive("S", "Allowable stress S", Kind.STRESS, 138e6)
        number("E", "Joint efficiency E", Kind.DIMENSIONLESS, 1.0, 0.0, 1.0, true)
        number("ca", "Corrosion allowance", Kind.LENGTH, 3e-3, 0.0)
        number("tn", "Nominal thickness provided (for MAWP)", Kind.LENGTH, 0.012, 0.0)
        compute {
            val ca = n("ca"); val p = n("P"); val s = n("S"); val e = n("E")
            val l = n("L") + ca
            val rr = n("r") - ca
            require(rr > 0, "Knuckle radius must exceed corrosion allowance")
            val m = step("Factor M", "M = ¼·(3 + √(L/r))", Kind.DIMENSIONLESS, 0.25 * (3 + sqrt(l / rr)), "App. 1-4(d)", output = true)
            val t = step("Pressure thickness t", "t = P·L·M / (2·S·E − 0.2·P)", Kind.LENGTH, p * l * m / (2 * s * e - 0.2 * p), "App. 1-4(d)", output = true)
            check(n("r") >= 0.06 * n("L"), "Knuckle radius ≥ 6 % of crown radius", "Knuckle radius < 6 % of crown radius (UG-32(j))")
            check(l / rr <= 16.67 + 1e-9, "L/r ≤ 16⅔", "L/r > 16⅔ is not permitted (App. 1-4(d))")
            finish(t, ca) { tc -> 2 * s * e * tc / (l * m + 0.2 * tc) }
        }
    }

    val flatHead = calculator("VES-006", "Unstayed Flat Head / Cover (UG-34)", Module.VESSELS) {
        description = "Minimum thickness of an unstayed circular flat head without bolting moment."
        keywords("flat head", "cover", "blind", "UG-34", "C factor")
        ref("ASME BPVC VIII-1", "UG-34(c)(2)", "Eq. (1)")
        positive("P", "Design pressure P", Kind.PRESSURE_GAUGE, 10e5)
        positive("d", "Diameter d (per Figure UG-34)", Kind.LENGTH, 0.5)
        number("C", "Attachment factor C", Kind.DIMENSIONLESS, 0.33, 0.1, 0.8, help = "From Figure UG-34 for the attachment detail (e.g. 0.17, 0.20, 0.30, 0.33)")
        positive("S", "Allowable stress S", Kind.STRESS, 138e6)
        number("E", "Joint efficiency E", Kind.DIMENSIONLESS, 1.0, 0.0, 1.0, true)
        number("ca", "Corrosion allowance", Kind.LENGTH, 3e-3, 0.0)
        compute {
            val t = step("Pressure thickness t", "t = d·√(C·P / (S·E))", Kind.LENGTH, n("d") * sqrt(n("C") * n("P") / (n("S") * n("E"))), "UG-34 Eq. (1)", output = true)
            step("Required thickness incl. corrosion", "t_r = t + c", Kind.LENGTH, t + n("ca"), primary = true)
            info("For bolted covers (sketches j, k) add the bolting-moment term 1.9·W·h_G/(S·E·d³).")
        }
    }

    val hydrotest = calculator("VES-015", "Vessel Test Pressure (UG-99 / UG-100)", Module.VESSELS) {
        description = "Minimum hydrostatic or pneumatic test pressure for a Division 1 vessel."
        keywords("hydrotest", "pneumatic", "test pressure", "LSR", "UG-99", "UG-100")
        ref("ASME BPVC VIII-1", "UG-99(b), UG-100(b)")
        choice("type", "Test type", listOf("hydro" to "Hydrostatic (1.3 × MAWP × LSR)", "pneu" to "Pneumatic (1.1 × MAWP × LSR)"))
        positive("MAWP", "MAWP", Kind.PRESSURE_GAUGE, 10e5)
        positive("ST", "Allowable stress at test temperature", Kind.STRESS, 138e6)
        positive("S", "Allowable stress at design temperature", Kind.STRESS, 138e6)
        number("h", "Liquid height above the lowest point (hydro)", Kind.LENGTH, 0.0, 0.0, si = "m", us = "ft")
        compute {
            val lsr = step("Lowest stress ratio LSR", "LSR = S_test / S_design", Kind.DIMENSIONLESS, n("ST") / n("S"))
            val f = if (c("type") == "hydro") 1.3 else 1.1
            val pt = step("Test pressure at top", "P_T = ${f}·MAWP·LSR", Kind.PRESSURE_GAUGE, f * n("MAWP") * lsr, primary = true)
            if (c("type") == "hydro" && n("h") > 0) step("Test pressure at lowest point", "P_T + ρ_w·g·h", Kind.PRESSURE_GAUGE, pt + 1000 * G0 * n("h"), output = true)
            if (c("type") == "pneu") warn("Pneumatic tests require UG-100 restrictions and a documented safety assessment.")
        }
    }

    /** Equivalent head depth ratio a/D for the partial-volume ellipsoid model. */
    private fun headDepthRatio(type: String) = when (type) { "ell" -> 0.25; "hemi" -> 0.5; "tori" -> 0.1545; else -> 0.0 }

    val volume = calculator("VES-025", "Vessel Volume & Liquid Level", Module.VESSELS) {
        description = "Total and partial volume of a horizontal or vertical vessel with dished heads, plus liquid weight."
        keywords("volume", "capacity", "liquid level", "partial volume", "strapping", "weight")
        ref("Geometric relations", note = "F&D head treated as an equivalent ellipsoid (≈ 0.0809·D³ per head)")
        choice("orient", "Orientation", listOf("h" to "Horizontal", "v" to "Vertical"))
        choice("head", "Head type", listOf("ell" to "2:1 ellipsoidal", "tori" to "Torispherical (ASME F&D)", "hemi" to "Hemispherical", "flat" to "Flat"))
        positive("D", "Inside diameter D", Kind.LENGTH, 2.0, si = "m", us = "ft")
        number("L", "Shell length tangent-to-tangent", Kind.LENGTH, 6.0, 0.0, si = "m", us = "ft")
        number("h", "Liquid level (from bottom of vessel)", Kind.LENGTH, 1.0, 0.0, si = "m", us = "ft")
        positive("rho", "Liquid density", Kind.DENSITY, 1000.0)
        compute {
            val d = n("D"); val r = d / 2; val l = n("L"); val h = n("h")
            val a = step("Equivalent head depth a", "a = k·D", Kind.LENGTH, headDepthRatio(c("head")) * d, si = "m", us = "ft")
            val vHeads = 4.0 / 3.0 * PI * r * r * a
            val total = step("Total volume", "V = π·D²·L/4 + 2 heads", Kind.VOLUME, PI * r * r * l + vHeads, output = true)
            val vLiq = if (c("orient") == "h") {
                require(h <= d, "Level cannot exceed the diameter")
                val seg = r * r * acos((r - h) / r) - (r - h) * sqrt(2 * r * h - h * h)
                seg * l + (if (r > 0) a / r else 0.0) * PI * h * h * (3 * r - h) / 3
            } else {
                val height = l + 2 * a
                require(h <= height + 1e-9, "Level cannot exceed the overall height")
                fun capVol(y: Double) = if (a == 0.0) 0.0 else PI * r * r * (y - (a.pow(3) - (a - y).pow(3)) / (3 * a * a))
                when {
                    h <= a -> capVol(h)
                    h <= a + l -> capVol(a) + PI * r * r * (h - a)
                    else -> vHeads - capVol(height - h) + PI * r * r * l
                }
            }
            step("Liquid volume", "partial shell + partial heads", Kind.VOLUME, vLiq, primary = true)
            step("Fill fraction", "V_liq / V_total", Kind.RATIO, vLiq / total, output = true)
            step("Liquid mass", "m = ρ·V_liq", Kind.MASS, n("rho") * vLiq, output = true)
            step("Mass when full", "m = ρ·V_total", Kind.MASS, n("rho") * total, output = true)
        }
    }

    val all = listOf(cylinder, sphere, ellipsoidal, torispherical, flatHead, hydrotest, volume)
}

object Tanks {
    val shell650 = calculator("TNK-001", "Tank Shell Thickness — API 650 One-Foot Method", Module.TANKS) {
        description = "Required design and hydrostatic-test thickness of each shell course."
        keywords("tank", "shell course", "one-foot", "1-foot method", "atmospheric storage", "API 650")
        ref("API 650", "5.6.3", "1-foot method (SI)")
        ref("API 650", "5.6.1.1", "Minimum nominal thickness")
        positive("D", "Nominal tank diameter D", Kind.LENGTH, 30.0, max = 61.0, si = "m", us = "ft")
        positive("H", "Design liquid level above bottom of course H", Kind.LENGTH, 15.0, si = "m", us = "ft")
        positive("G", "Design specific gravity G", Kind.DIMENSIONLESS, 1.0)
        positive("Sd", "Allowable product design stress S_d", Kind.STRESS, 160e6, help = "Table 5-2: e.g. A283 C 137 MPa, A36 160 MPa, A516-70 173 MPa")
        positive("St", "Allowable hydrostatic test stress S_t", Kind.STRESS, 171e6)
        number("CA", "Corrosion allowance", Kind.LENGTH, 1.5e-3, 0.0)
        compute {
            val d = n("D"); val h = n("H")
            require(h > 0.3, "H must exceed 0.3 m")
            val td = step("Design shell thickness t_d", "t_d = 4.9·D·(H − 0.3)·G / S_d + CA   [mm, m, MPa]", Kind.LENGTH,
                (4.9 * d * (h - 0.3) * n("G") / (n("Sd") / 1e6) + n("CA") * 1000) / 1000, "5.6.3.2", output = true)
            val tt = step("Hydrostatic test thickness t_t", "t_t = 4.9·D·(H − 0.3) / S_t", Kind.LENGTH,
                4.9 * d * (h - 0.3) / (n("St") / 1e6) / 1000, "5.6.3.2", output = true)
            val tmin = step("Minimum nominal thickness", "5 / 6 / 8 / 10 mm by diameter", Kind.LENGTH,
                when { d < 15 -> 5e-3; d <= 36 -> 6e-3; d <= 60 -> 8e-3; else -> 10e-3 }, "5.6.1.1")
            step("Required shell thickness", "t = max(t_d, t_t, t_min)", Kind.LENGTH, maxOf(td, tt, tmin), primary = true)
        }
    }

    val shell653 = calculator("TNK-008", "Tank Shell Minimum Thickness — API 653", Module.TANKS) {
        description = "Minimum acceptable shell thickness of an in-service tank for continued operation."
        keywords("tank inspection", "retirement", "minimum thickness", "API 653", "fitness")
        ref("API 653", "4.3.3.1", "Eq. for t_min (US customary)")
        positive("D", "Nominal tank diameter D", Kind.LENGTH, 30.48, si = "m", us = "ft")
        positive("H", "Height from bottom of course to max liquid level H", Kind.LENGTH, 12.0, si = "m", us = "ft")
        positive("G", "Specific gravity G", Kind.DIMENSIONLESS, 1.0)
        positive("S", "Maximum allowable stress S", Kind.STRESS, 160e6, help = "Table 4-1: lesser of 0.80Y or 0.429T (bottom and second course), 0.88Y or 0.472T (others)")
        number("E", "Joint efficiency E", Kind.DIMENSIONLESS, 1.0, 0.0, 1.0, true)
        compute {
            val dFt = n("D") / 0.3048; val hFt = n("H") / 0.3048; val sPsi = n("S") / 6894.757293168
            require(hFt > 1, "H must exceed 1 ft")
            val tIn = 2.6 * (hFt - 1) * dFt * n("G") / (sPsi * n("E"))
            val t = step("Minimum thickness t_min", "t_min = 2.6·(H − 1)·D·G / (S·E)   [in, ft, psi]", Kind.LENGTH, tIn * 0.0254, "4.3.3.1", output = true)
            step("Minimum acceptable thickness", "max(t_min, 0.1 in)", Kind.LENGTH, max(t, 0.1 * 0.0254), primary = true)
        }
    }

    val all = listOf(shell650, shell653)
}

object Inspection {
    val remainingLife = calculator("INS-001", "Corrosion Rate & Remaining Life (API 510 / 570)", Module.INSPECTION) {
        description = "Corrosion rate between two thickness readings, remaining life and next inspection date."
        keywords("corrosion rate", "remaining life", "inspection interval", "thickness monitoring", "API 570", "API 510")
        ref("API 570", "7.1")
        ref("API 510", "7.1")
        positive("t0", "Previous / initial thickness", Kind.LENGTH, 0.0127)
        positive("t1", "Actual (latest) thickness", Kind.LENGTH, 0.0110)
        positive("treq", "Required thickness", Kind.LENGTH, 0.0075)
        positive("yrs", "Time between readings", Kind.TIME, 10 * 31_557_600.0, si = "yr", us = "yr")
        choice("maxInt", "Maximum interval", listOf("5" to "API 570 Class 1 piping (5 yr)", "10" to "API 570 Class 2/3 piping (10 yr)", "10v" to "API 510 vessel internal (10 yr)"), "10")
        compute {
            val yr = 31_557_600.0
            val loss = n("t0") - n("t1")
            require(loss >= 0, "Actual thickness exceeds previous thickness — check readings")
            val crYear = step("Corrosion rate (per year)", "CR = (t_prev − t_actual) / Δt", Kind.LENGTH, loss / (n("yrs") / yr), output = true, si = "mm", us = "mil")
            val cr = crYear / yr
            check(n("t1") >= n("treq"), "Actual thickness ≥ required", "Actual thickness is below required: evaluate fitness for service (API 579)")
            if (cr <= 0) {
                text("Remaining life", "No measurable corrosion", primary = true)
                text("Next inspection", "At maximum interval (${c("maxInt").trimEnd('v')} yr)")
            } else {
                val rl = step("Remaining life", "RL = (t_actual − t_required) / CR", Kind.TIME, (n("t1") - n("treq")) / cr, primary = true, si = "yr", us = "yr")
                step("Next inspection interval", "min(RL/2, maximum interval)", Kind.TIME, min(rl / 2, c("maxInt").trimEnd('v').toDouble() * yr), output = true, si = "yr", us = "yr")
            }
        }
    }

    val vibration = calculator("INS-009", "Vibration Severity — ISO 20816-3", Module.INSPECTION) {
        description = "Evaluation zone (A–D) of broadband vibration velocity measured on bearing housings of industrial machines."
        keywords("vibration", "severity", "ISO 10816", "ISO 20816", "velocity RMS", "zone")
        ref("ISO 20816-3", note = "Zone boundaries for groups 1 and 2")
        positive("v", "Vibration velocity (RMS, 10–1000 Hz)", Kind.VELOCITY, 3.0e-3, si = "mm/s", us = "mm/s")
        choice("grp", "Machine group", listOf("1" to "Group 1 — large machines, 300 kW – 50 MW", "2" to "Group 2 — medium machines, 15 – 300 kW"), "2")
        choice("fdn", "Support", listOf("rigid" to "Rigid foundation", "flex" to "Flexible foundation"))
        compute {
            val b = when (c("grp") to c("fdn")) {
                "1" to "rigid" -> doubleArrayOf(2.3, 4.5, 7.1)
                "1" to "flex" -> doubleArrayOf(3.5, 7.1, 11.0)
                "2" to "rigid" -> doubleArrayOf(1.4, 2.8, 4.5)
                else -> doubleArrayOf(2.3, 4.5, 7.1)
            }
            val v = n("v") * 1000
            text("Zone boundaries", "A/B ${b[0]}  ·  B/C ${b[1]}  ·  C/D ${b[2]} mm/s", formula = "ISO 20816-3 Table A.1/A.2")
            val zone = when { v <= b[0] -> "A — newly commissioned"; v <= b[1] -> "B — unrestricted long-term operation"; v <= b[2] -> "C — restricted operation"; else -> "D — damage may occur" }
            text("Evaluation zone", zone, primary = true)
            when {
                v <= b[1] -> checks(Status.PASS, "Acceptable for long-term operation")
                v <= b[2] -> checks(Status.WARN, "Zone C: plan corrective action")
                else -> checks(Status.FAIL, "Zone D: vibration severe enough to cause damage")
            }
        }
    }

    val balancing = calculator("INS-010", "Rotor Balance Tolerance — ISO 21940-11", Module.INSPECTION) {
        description = "Permissible residual unbalance for a rigid rotor from its balance quality grade."
        keywords("balancing", "balance grade", "G2.5", "G6.3", "residual unbalance", "ISO 1940")
        ref("ISO 21940-11", note = "formerly ISO 1940-1")
        choice("G", "Balance quality grade", listOf("0.4" to "G 0.4", "1" to "G 1", "2.5" to "G 2.5 — turbines, compressors, machine-tool drives", "6.3" to "G 6.3 — pumps, fans, motors, general", "16" to "G 16", "40" to "G 40"), "6.3")
        positive("m", "Rotor mass", Kind.MASS, 100.0)
        positive("n", "Maximum service speed", Kind.ROT_SPEED, 3000 * 2 * PI / 60)
        positive("r", "Correction radius", Kind.LENGTH, 0.15)
        choice("planes", "Correction planes", listOf("1" to "Single plane", "2" to "Two planes (symmetric, half each)"), "2")
        compute {
            val g = c("G").toDouble()
            val e = step("Permissible specific unbalance e_per", "e_per = G / ω", Kind.LENGTH, g * 1e-3 / n("n"), si = "µm", us = "mil")
            val u = e * n("m")
            text("Permissible residual unbalance U_per", "${com.mechultimate.core.units.formatNumber(u * 1e6)} g·mm (total)", primary = true, formula = "U_per = e_per·m")
            step("Residual mass per plane at correction radius", "m_c = U_per / (planes·r)", Kind.MASS, u / (c("planes").toDouble() * n("r")), output = true, si = "g", us = "g")
        }
    }

    val all = listOf(remainingLife, vibration, balancing)
}

private fun CalcScope.checks(status: Status, msg: String) = when (status) {
    Status.PASS -> check(true, msg, msg)
    Status.FAIL -> check(false, msg, msg)
    Status.WARN -> warn(msg)
    Status.INFO -> info(msg)
}
