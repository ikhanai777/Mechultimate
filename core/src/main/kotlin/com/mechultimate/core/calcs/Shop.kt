package com.mechultimate.core.calcs

import com.mechultimate.core.engine.Module
import com.mechultimate.core.engine.Status
import com.mechultimate.core.engine.calculator
import com.mechultimate.core.engine.visibleIf
import com.mechultimate.core.units.G0
import com.mechultimate.core.units.Kind
import com.mechultimate.core.units.formatNumber
import kotlin.math.PI
import kotlin.math.pow
import kotlin.math.sqrt
import kotlin.math.tan

object Welding {
    val heatInput = calculator("WLD-001", "Welding Heat Input", Module.WELDING) {
        description = "Arc heat input per unit length, with optional process thermal efficiency."
        keywords("heat input", "arc energy", "kJ/mm", "QW-409", "travel speed", "welding")
        ref("ASME BPVC IX", "QW-409.1")
        ref("EN 1011-1", note = "Thermal efficiency factor k")
        positive("V", "Arc voltage", Kind.VOLTAGE, 25.0)
        positive("I", "Welding current", Kind.CURRENT, 200.0)
        positive("s", "Travel speed", Kind.VELOCITY, 250.0 / 60_000, si = "mm/min", us = "in/min")
        choice("k", "Thermal efficiency", listOf("1.0" to "None (ASME IX arc energy, k = 1.0)", "1.0s" to "SAW (k = 1.0)", "0.8" to "SMAW / GMAW / FCAW (k = 0.8)", "0.6" to "GTAW / PAW (k = 0.6)"))
        compute {
            val k = c("k").removeSuffix("s").toDouble()
            step("Heat input", "HI = k·V·I / v", Kind.HEAT_INPUT, k * n("V") * n("I") / n("s"), "QW-409.1", primary = true)
        }
    }

    val carbonEquivalent = calculator("MAT-004", "Carbon Equivalent & Weldability", Module.WELDING) {
        description = "IIW carbon equivalent and Pcm from chemical composition, with preheat guidance."
        keywords("carbon equivalent", "CE", "CEV", "IIW", "Pcm", "weldability", "preheat", "hydrogen cracking")
        ref("AWS D1.1", "Annex H")
        ref("IIW", note = "CE formula")
        number("C", "C (wt %)", Kind.DIMENSIONLESS, 0.18, 0.0, 2.0)
        number("Mn", "Mn (wt %)", Kind.DIMENSIONLESS, 1.2, 0.0, 20.0)
        number("Si", "Si (wt %)", Kind.DIMENSIONLESS, 0.3, 0.0, 5.0)
        number("Cr", "Cr (wt %)", Kind.DIMENSIONLESS, 0.1, 0.0, 30.0)
        number("Mo", "Mo (wt %)", Kind.DIMENSIONLESS, 0.03, 0.0, 10.0)
        number("V", "V (wt %)", Kind.DIMENSIONLESS, 0.01, 0.0, 5.0)
        number("Ni", "Ni (wt %)", Kind.DIMENSIONLESS, 0.1, 0.0, 40.0)
        number("Cu", "Cu (wt %)", Kind.DIMENSIONLESS, 0.1, 0.0, 5.0)
        number("B", "B (wt %)", Kind.DIMENSIONLESS, 0.0, 0.0, 0.1)
        compute {
            val ce = step("CE (IIW)", "CE = C + Mn/6 + (Cr + Mo + V)/5 + (Ni + Cu)/15", Kind.DIMENSIONLESS,
                n("C") + n("Mn") / 6 + (n("Cr") + n("Mo") + n("V")) / 5 + (n("Ni") + n("Cu")) / 15, primary = true)
            step("Pcm (Ito–Bessyo)", "Pcm = C + Si/30 + (Mn + Cu + Cr)/20 + Ni/60 + Mo/15 + V/10 + 5B", Kind.DIMENSIONLESS,
                n("C") + n("Si") / 30 + (n("Mn") + n("Cu") + n("Cr")) / 20 + n("Ni") / 60 + n("Mo") / 15 + n("V") / 10 + 5 * n("B"), output = true)
            text("Weldability guidance", when {
                ce < 0.40 -> "Good weldability — preheat usually not required for thin sections"
                ce <= 0.45 -> "Fair — preheat may be required depending on thickness and hydrogen level"
                else -> "Preheat and low-hydrogen practice required; consider PWHT"
            }, primary = true)
            if (ce > 0.45) warn("CE > 0.45: hydrogen-assisted cracking risk — specify preheat")
        }
    }

    val all = listOf(heatInput, carbonEquivalent)
}

object Manufacturing {
    val speedsFeeds = calculator("MFG-001", "Speeds, Feeds & Cutting Power", Module.MANUFACTURING) {
        description = "Spindle speed, feed rate, metal removal rate and cutting power for turning, milling and drilling."
        keywords("speeds and feeds", "rpm", "cutting speed", "feed rate", "MRR", "machining", "milling", "turning", "drilling")
        ref("Machining handbook relations", note = "Sandvik-style formulas")
        choice("op", "Operation", listOf("turn" to "Turning", "mill" to "Milling", "drill" to "Drilling"))
        positive("vc", "Cutting speed v_c", Kind.VELOCITY, 200.0 / 60, si = "m/min", us = "ft/min")
        positive("D", "Diameter (workpiece / cutter / drill)", Kind.LENGTH, 0.05)
        positive("f", "Feed per revolution f_n", Kind.LENGTH, 0.2e-3, visibleIf = visibleIf("op", "turn", "drill"))
        positive("fz", "Feed per tooth f_z", Kind.LENGTH, 0.1e-3, visibleIf = visibleIf("op", "mill"))
        positive("z", "Number of teeth z", Kind.DIMENSIONLESS, 4.0, visibleIf = visibleIf("op", "mill"))
        positive("ap", "Depth of cut a_p", Kind.LENGTH, 2e-3, visibleIf = visibleIf("op", "turn", "mill"))
        positive("ae", "Width of cut a_e", Kind.LENGTH, 0.025, visibleIf = visibleIf("op", "mill"))
        positive("kc", "Specific cutting force k_c", Kind.STRESS, 2000e6, help = "Steel ≈ 1500–2500 N/mm²; aluminium ≈ 700; cast iron ≈ 1100")
        number("eta", "Machine efficiency", Kind.RATIO, 0.8, 0.0, 1.0, true)
        compute {
            val vcMin = n("vc") * 60
            val dmm = n("D") * 1000
            val rpm = step("Spindle speed n", "n = 1000·v_c / (π·D)", Kind.ROT_SPEED, 1000 * vcMin / (PI * dmm) * 2 * PI / 60, primary = true)
            val rpmVal = rpm * 60 / (2 * PI)
            val mrr = when (c("op")) {
                "turn" -> {
                    step("Feed rate v_f", "v_f = f_n·n", Kind.VELOCITY, n("f") * rpmVal / 60, output = true, si = "mm/min", us = "in/min")
                    step("Metal removal rate Q", "Q = v_c·a_p·f_n", Kind.VOLUME_FLOW, n("vc") * n("ap") * n("f"), output = true, si = "L/min", us = "cfm")
                }
                "mill" -> {
                    val vf = step("Table feed v_f", "v_f = f_z·z·n", Kind.VELOCITY, n("fz") * n("z") * rpmVal / 60, output = true, si = "mm/min", us = "in/min")
                    step("Metal removal rate Q", "Q = a_p·a_e·v_f", Kind.VOLUME_FLOW, n("ap") * n("ae") * vf, output = true, si = "L/min", us = "cfm")
                }
                else -> {
                    step("Penetration rate v_f", "v_f = f_n·n", Kind.VELOCITY, n("f") * rpmVal / 60, output = true, si = "mm/min", us = "in/min")
                    step("Metal removal rate Q", "Q = π·D²/4·v_f", Kind.VOLUME_FLOW, PI * n("D").pow(2) / 4 * n("f") * rpmVal / 60, output = true, si = "L/min", us = "cfm")
                }
            }
            val pc = step("Cutting power P_c", "P_c = Q·k_c", Kind.POWER, mrr * n("kc"), output = true)
            step("Spindle power required", "P = P_c / η", Kind.POWER, pc / n("eta"), primary = true)
            info("MRR is shown as a volume flow: 1 L/min = 1000 cm³/min.")
        }
    }

    private val threads = listOf(
        "M3:3:0.5", "M4:4:0.7", "M5:5:0.8", "M6:6:1.0", "M8:8:1.25", "M10:10:1.5", "M12:12:1.75", "M14:14:2.0", "M16:16:2.0",
        "M20:20:2.5", "M24:24:3.0", "1/4-20 UNC:6.35:1.27", "5/16-18 UNC:7.9375:1.4111", "3/8-16 UNC:9.525:1.5875",
        "1/2-13 UNC:12.7:1.9538", "5/8-11 UNC:15.875:2.3091", "3/4-10 UNC:19.05:2.54", "1-8 UNC:25.4:3.175",
    )

    val tapDrill = calculator("MFG-002", "Tap Drill Size & Thread Engagement", Module.MANUFACTURING) {
        description = "Tap drill diameter for a target percentage of thread, or the thread percentage of a chosen drill."
        keywords("tap drill", "thread", "percent thread", "tapping", "UNC", "metric thread")
        ref("ISO 261 / ASME B1.1", note = "%thread = 76.98·(D − drill)/P")
        choice("thr", "Thread", threads.map { it to it.substringBefore(':') }, "M10:10:1.5")
        number("pct", "Target thread engagement", Kind.RATIO, 0.75, 0.5, 1.0, help = "65–75 % is typical; higher percentages add little strength but raise tap torque")
        number("drill", "Check a drill size (optional)", Kind.LENGTH, 0.0, 0.0)
        compute {
            val parts = c("thr").split(":")
            val d = parts[1].toDouble() / 1000; val p = parts[2].toDouble() / 1000
            step("Tap drill diameter", "d_drill = D − %thread·P / 0.7698", Kind.LENGTH, d - n("pct") * p / 0.7698, primary = true)
            step("Simple rule (≈ 77 %)", "d_drill ≈ D − P", Kind.LENGTH, d - p, output = true)
            if (n("drill") > 0) step("Thread % with chosen drill", "%thread = 0.7698·(D − d_drill)/P", Kind.RATIO, 0.7698 * (d - n("drill")) / p, output = true)
        }
    }

    val bendAllowance = calculator("MFG-003", "Sheet Metal Bend Allowance & Flat Length", Module.MANUFACTURING) {
        description = "Bend allowance, bend deduction and flat-pattern length of a single bend from outside flange dimensions."
        keywords("sheet metal", "bend allowance", "bend deduction", "K-factor", "flat pattern", "press brake")
        ref("Sheet metal practice", note = "Neutral axis at K·t from the inside surface")
        positive("t", "Material thickness t", Kind.LENGTH, 2e-3)
        positive("R", "Inside bend radius R", Kind.LENGTH, 2e-3)
        positive("ang", "Bend angle (from flat)", Kind.ANGLE, PI / 2)
        number("K", "K-factor", Kind.DIMENSIONLESS, 0.44, 0.0, 0.5, help = "≈ 0.33 soft/sharp bends … 0.5 large radii; 0.44 is a common default")
        positive("L1", "Flange 1 outside length", Kind.LENGTH, 0.05)
        positive("L2", "Flange 2 outside length", Kind.LENGTH, 0.03)
        compute {
            val th = n("ang")
            val ba = step("Bend allowance BA", "BA = θ·(R + K·t)", Kind.LENGTH, th * (n("R") + n("K") * n("t")), output = true)
            val ossb = step("Outside setback OSSB", "OSSB = tan(θ/2)·(R + t)", Kind.LENGTH, tan(th / 2) * (n("R") + n("t")))
            val bd = step("Bend deduction BD", "BD = 2·OSSB − BA", Kind.LENGTH, 2 * ossb - ba, output = true)
            step("Flat length", "L = L₁ + L₂ − BD", Kind.LENGTH, n("L1") + n("L2") - bd, primary = true)
            check(n("R") >= n("t") * 0.5, "Bend radius ≥ 0.5 t", "Very tight bend radius: check material minimum bend radius", Status.WARN)
        }
    }

    val all = listOf(speedsFeeds, tapDrill, bendAllowance)
}

object General {
    val sdof = calculator("GEN-008", "Single-DOF Vibration & Isolation", Module.GENERAL) {
        description = "Natural frequency, damped frequency, dynamic magnification and transmissibility of a mass–spring–damper."
        keywords("vibration", "natural frequency", "resonance", "isolation", "transmissibility", "damping", "SDOF")
        ref("Rao, Mechanical Vibrations", "Ch. 3")
        positive("m", "Mass m", Kind.MASS, 100.0)
        positive("k", "Stiffness k", Kind.FORCE_PER_LENGTH, 1.0e6)
        number("zeta", "Damping ratio ζ", Kind.RATIO, 0.05, 0.0, 0.99)
        number("f", "Forcing frequency", Kind.FREQUENCY, 25.0, 0.0)
        compute {
            val wn = sqrt(n("k") / n("m"))
            val fn = step("Natural frequency f_n", "f_n = (1/2π)·√(k/m)", Kind.FREQUENCY, wn / (2 * PI), primary = true)
            step("Damped natural frequency f_d", "f_d = f_n·√(1 − ζ²)", Kind.FREQUENCY, fn * sqrt(1 - n("zeta").pow(2)), output = true)
            step("Static deflection", "δ_st = m·g / k", Kind.LENGTH, n("m") * G0 / n("k"), output = true)
            if (n("f") > 0) {
                val r = step("Frequency ratio r", "r = f / f_n", Kind.DIMENSIONLESS, n("f") / fn, output = true)
                val z = n("zeta")
                val den = (1 - r * r).pow(2) + (2 * z * r).pow(2)
                step("Dynamic magnification", "M = 1/√[(1 − r²)² + (2ζr)²]", Kind.DIMENSIONLESS, 1 / sqrt(den), output = true)
                val tr = step("Transmissibility TR", "TR = √[(1 + (2ζr)²) / ((1 − r²)² + (2ζr)²)]", Kind.DIMENSIONLESS, sqrt((1 + (2 * z * r).pow(2)) / den), primary = true)
                step("Isolation efficiency", "1 − TR", Kind.RATIO, 1 - tr, output = true)
                when {
                    r in 0.8..1.2 -> check(false, "", "Near resonance (0.8 < r < 1.2)")
                    r > sqrt(2.0) -> check(true, "Isolation region (r > √2) — " + formatNumber((1 - tr) * 100, 3) + " % isolation", "")
                    else -> warn("Amplification region (r < √2): force is transmitted with gain ≥ 1")
                }
            }
        }
    }

    val all = listOf(sdof)
}
