package com.mechultimate.core.calcs

import com.mechultimate.core.data.PipeData
import com.mechultimate.core.engine.CalcScope
import com.mechultimate.core.engine.Module
import com.mechultimate.core.engine.Status
import com.mechultimate.core.engine.calculator
import com.mechultimate.core.engine.visibleIf
import com.mechultimate.core.numerics.Numerics
import com.mechultimate.core.units.ATM_PA
import com.mechultimate.core.units.G0
import com.mechultimate.core.units.Kind
import com.mechultimate.core.units.formatNumber
import kotlin.math.PI
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sqrt

internal const val STEEL_DENSITY = 7850.0

internal val scheduleOptions = listOf("5S", "10S", "10", "20", "30", "40", "STD", "60", "80", "XS", "100", "120", "140", "160", "XXS")
    .map { it to "Sch $it" }

/** Nominal wall (m) for an NPS and schedule label, or a calculation error if not listed. */
internal fun CalcScope.wallFor(nps: String, sch: String): Double =
    PipeData.size(nps).walls.firstOrNull { it.first == sch }?.second?.times(0.0254)
        ?: fail("Schedule $sch is not listed for NPS $nps in ASME B36.10M/B36.19M")

/** Shared body for B31.3 / B31.1 straight-pipe thickness; returns nothing, records results. */
private fun CalcScope.straightPipe(code: String, eqRef: String) {
    val nps = c("nps")
    val d = step("Outside diameter D", "B36.10M / B36.19M, NPS $nps", Kind.LENGTH, PipeData.size(nps).odM)
    val p = n("P"); val s = n("S"); val e = n("E"); val w = n("W"); val y = n("Y"); val ca = n("c"); val mt = n("MT")
    val t = step("Pressure design thickness t", "t = P·D / [2(S·E·W + P·Y)]", Kind.LENGTH, p * d / (2 * (s * e * w + p * y)), eqRef, output = true)
    val tm = step("Minimum required thickness t_m", "t_m = t + c", Kind.LENGTH, t + ca, "$code 304.1.1", primary = true)
    val tNom = step("Required nominal wall", "t_nom = t_m / (1 − mill tolerance)", Kind.LENGTH, tm / (1 - mt), output = true)
    check(t < d / 6, "t < D/6 — thin-wall equation applies", "t ≥ D/6: thin-wall equation not valid, special consideration required ($code 304.1.2(b))")
    check(p / (s * e) <= 0.385, "P/(S·E) ≤ 0.385", "P/(S·E) > 0.385: special consideration required ($code 304.1.2(b))")
    val sch = PipeData.selectSchedule(nps, tm, mt)
    if (sch == null) {
        text("Selected schedule", "None — required wall exceeds listed schedules", primary = true)
        check(false, "", "No listed schedule is thick enough for NPS $nps")
    } else {
        text("Selected schedule", "Sch ${sch.label} (${formatNumber(sch.wallM * 1000)} mm / ${formatNumber(sch.wallM / 0.0254)} in)", primary = true,
            formula = "Thinnest schedule with T·(1 − tol) ≥ t_m")
        val tAvail = sch.wallM * (1 - mt) - ca
        step("MAWP of selected schedule", "P = 2·S·E·W·T' / (D − 2·Y·T'),  T' = T(1 − tol) − c", Kind.PRESSURE_GAUGE,
            2 * s * e * w * tAvail / (d - 2 * y * tAvail), output = true)
    }
}

object Piping {
    private fun com.mechultimate.core.engine.CalculatorBuilder.straightPipeInputs(yHelp: String) {
        choice("nps", "Nominal pipe size", PipeData.npsOptions, "6")
        positive("P", "Internal design pressure", Kind.PRESSURE_GAUGE, 20e5)
        positive("S", "Allowable stress S at design temperature", Kind.STRESS, 137.9e6, help = "From the code's allowable stress table for the material (e.g. A106 Gr B ≈ 138 MPa / 20 ksi up to ~340 °C)")
        number("E", "Quality factor E", Kind.DIMENSIONLESS, 1.0, 0.0, 1.0, true, help = "Longitudinal weld joint / casting quality factor (seamless = 1.0, ERW = 0.85)")
        number("W", "Weld joint strength reduction factor W", Kind.DIMENSIONLESS, 1.0, 0.0, 1.0, true, help = "1.0 below the creep range")
        number("Y", "Coefficient Y", Kind.DIMENSIONLESS, 0.4, 0.0, 0.7, help = yHelp)
        number("c", "Corrosion + mechanical allowance", Kind.LENGTH, 1.5e-3, 0.0, help = "Include thread or groove depth where applicable")
        number("MT", "Mill under-tolerance", Kind.RATIO, 0.125, 0.0, 0.5, help = "12.5 % for most seamless and welded pipe")
    }

    val b313Thickness = calculator("PIP-001", "Pipe Wall Thickness — ASME B31.3", Module.PIPING) {
        description = "Minimum wall thickness of straight pipe under internal pressure for process piping, with schedule selection."
        keywords("process piping", "wall thickness", "schedule", "pressure design", "304.1.2", "3a")
        ref("ASME B31.3", "304.1.2", "Eq. (3a)")
        ref("ASME B36.10M / B36.19M", note = "Pipe dimensions")
        straightPipeInputs("Table 304.1.1: 0.4 for ferritic/austenitic steels below 482 °C; valid for t < D/6")
        assume("Internal pressure only; external loads and external pressure are checked separately.")
        compute { straightPipe("ASME B31.3", "B31.3 Eq. (3a)") }
    }

    val b311Thickness = calculator("PIP-002", "Pipe Wall Thickness — ASME B31.1", Module.PIPING) {
        description = "Minimum wall thickness of straight pipe for power piping, with schedule selection."
        keywords("power piping", "boiler external piping", "steam", "104.1.2")
        ref("ASME B31.1", "104.1.2", "Eq. (7)")
        straightPipeInputs("Table 104.1.2-1: 0.4 for ferritic steels ≤ 482 °C")
        assume("Allowance A includes corrosion, erosion and threading/grooving.")
        compute { straightPipe("ASME B31.1", "B31.1 104.1.2") }
    }

    val b314Thickness = calculator("PIP-003", "Pipeline Wall Thickness — ASME B31.4", Module.PIPING) {
        description = "Pressure design wall thickness for liquid transportation pipelines."
        keywords("pipeline", "liquid", "Barlow", "SMYS", "API 5L")
        ref("ASME B31.4", "403.2.1")
        positive("P", "Internal design gauge pressure", Kind.PRESSURE_GAUGE, 70e5)
        positive("D", "Outside diameter", Kind.LENGTH, 0.3239)
        positive("Sy", "Specified minimum yield strength (SMYS)", Kind.STRESS, 359e6, help = "API 5L X52 = 359 MPa (52 ksi)")
        number("E", "Weld joint factor E", Kind.DIMENSIONLESS, 1.0, 0.0, 1.0, true)
        number("F", "Design factor", Kind.DIMENSIONLESS, 0.72, 0.0, 0.72, true, help = "0.72 for most pipe; lower values may apply to specific locations")
        number("A", "Allowances (corrosion, threads, grooves)", Kind.LENGTH, 0.0, 0.0)
        compute {
            val s = step("Allowable stress S", "S = F·E·SMYS", Kind.STRESS, n("F") * n("E") * n("Sy"), "403.3.1", output = true)
            val t = step("Pressure design thickness t", "t = P·D / (2·S)", Kind.LENGTH, n("P") * n("D") / (2 * s), "403.2.1")
            step("Nominal wall required t_n", "t_n = t + A", Kind.LENGTH, t + n("A"), primary = true)
            check(t < n("D") / 6, "t < D/6", "t ≥ D/6: thick-wall design required", Status.WARN)
        }
    }

    val b318Pressure = calculator("PIP-004", "Gas Pipeline Design Pressure — ASME B31.8", Module.PIPING) {
        description = "Design pressure of steel gas pipelines from the Barlow formula with location class design factor."
        keywords("gas", "transmission", "location class", "Barlow", "MAOP")
        ref("ASME B31.8", "841.1.1")
        positive("D", "Outside diameter", Kind.LENGTH, 0.6096)
        positive("t", "Nominal wall thickness", Kind.LENGTH, 0.0127)
        positive("S", "SMYS", Kind.STRESS, 448e6, help = "API 5L X65 = 448 MPa")
        choice("cls", "Location class", listOf(
            "0.80" to "Class 1 Division 1 (F = 0.80)", "0.72" to "Class 1 Division 2 (F = 0.72)",
            "0.60" to "Class 2 (F = 0.60)", "0.50" to "Class 3 (F = 0.50)", "0.40" to "Class 4 (F = 0.40)",
        ), "0.72")
        number("E", "Longitudinal joint factor E", Kind.DIMENSIONLESS, 1.0, 0.0, 1.0, true)
        number("T", "Temperature derating factor T", Kind.DIMENSIONLESS, 1.0, 0.0, 1.0, true, help = "1.0 at or below 121 °C (250 °F)")
        compute {
            val f = c("cls").toDouble()
            step("Design factor F", "Table 841.1.6-1", Kind.DIMENSIONLESS, f)
            step("Design pressure P", "P = (2·S·t / D)·F·E·T", Kind.PRESSURE_GAUGE, 2 * n("S") * n("t") / n("D") * f * n("E") * n("T"), "841.1.1", primary = true)
            step("Hoop stress / SMYS at design pressure", "S_H / SMYS = F·E·T", Kind.RATIO, f * n("E") * n("T"))
        }
    }

    val blindThickness = calculator("PIP-010", "Blind Flange / Blank Thickness — ASME B31.3", Module.PIPING) {
        description = "Minimum thickness of a permanent blank."
        keywords("blank", "blind", "spade", "spectacle", "Eq 15")
        ref("ASME B31.3", "304.5.3", "Eq. (15)")
        positive("P", "Design gauge pressure", Kind.PRESSURE_GAUGE, 20e5)
        positive("dg", "Gasket inside diameter d_g", Kind.LENGTH, 0.170, help = "Gasket ID for raised/flat face; gasket pitch diameter for ring joints")
        positive("S", "Allowable stress S", Kind.STRESS, 137.9e6)
        number("E", "Quality factor E", Kind.DIMENSIONLESS, 1.0, 0.0, 1.0, true)
        number("W", "Weld strength reduction factor W", Kind.DIMENSIONLESS, 1.0, 0.0, 1.0, true)
        number("c", "Corrosion allowance c", Kind.LENGTH, 1.5e-3, 0.0)
        compute {
            val t = step("Pressure design thickness", "t = d_g·√(3P / (16·S·E·W))", Kind.LENGTH, n("dg") * sqrt(3 * n("P") / (16 * n("S") * n("E") * n("W"))), "Eq. (15)")
            step("Minimum required thickness t_m", "t_m = t + c", Kind.LENGTH, t + n("c"), primary = true)
        }
    }

    val thermalExpansion = calculator("PIP-014", "Thermal Expansion of Pipe", Module.PIPING) {
        description = "Free thermal growth of a pipe run between installation and operating temperature."
        keywords("expansion", "growth", "thermal", "ΔL")
        ref("ASME B31.3", "Appendix C", "Thermal expansion data")
        positive("L", "Pipe length", Kind.LENGTH, 30.0, si = "m", us = "ft")
        number("T1", "Installation temperature", Kind.TEMPERATURE, 294.15)
        number("T2", "Operating temperature", Kind.TEMPERATURE, 473.15)
        positive("a", "Mean coefficient of thermal expansion α", Kind.EXPANSION, 12.0e-6, help = "Carbon steel ≈ 12 µm/(m·K); austenitic stainless ≈ 17 µm/(m·K) (mean, 20–200 °C)")
        compute {
            val dt = step("Temperature change ΔT", "ΔT = T₂ − T₁", Kind.TEMP_DIFF, n("T2") - n("T1"))
            step("Thermal growth ΔL", "ΔL = α·L·ΔT", Kind.LENGTH, n("a") * n("L") * dt, primary = true)
            step("Unit expansion", "e = α·ΔT", Kind.RATIO, n("a") * dt, output = true)
        }
    }

    val expansionLoop = calculator("PIP-015", "Expansion Loop / Leg Length (Guided Cantilever)", Module.PIPING) {
        description = "Minimum perpendicular leg length to absorb a thermal displacement within the allowable displacement stress range."
        keywords("loop", "offset", "flexibility", "guided cantilever", "Kellogg")
        ref("M.W. Kellogg, Design of Piping Systems", note = "Guided-cantilever method")
        ref("ASME B31.3", "302.3.5", "Allowable displacement stress range S_A")
        positive("D", "Pipe outside diameter", Kind.LENGTH, 0.1683)
        positive("dL", "Displacement to absorb Δ", Kind.LENGTH, 0.05)
        positive("E", "Elastic modulus E", Kind.MODULUS, 203e9)
        positive("SA", "Allowable displacement stress range S_A", Kind.STRESS, 200e6)
        compute {
            val l = step("Required leg length L", "L = √(3·E·D·Δ / S_A)", Kind.LENGTH, sqrt(3 * n("E") * n("D") * n("dL") / n("SA")), primary = true, si = "m", us = "ft")
            step("Symmetric loop: leg height for Δ/2 each side", "H = √(3·E·D·(Δ/2) / S_A)", Kind.LENGTH, l / sqrt(2.0), output = true, si = "m", us = "ft")
            info("Stress intensification at elbows is not included; apply a margin or run a flexibility analysis for critical lines.")
        }
    }

    val flexibilityCheck = calculator("PIP-016", "Flexibility Exemption Check — B31.3 319.4.1", Module.PIPING) {
        description = "Simplified test of whether a two-anchor piping system of uniform size needs formal flexibility analysis."
        keywords("flexibility", "exemption", "Dy/(L-U)²", "K1")
        ref("ASME B31.3", "319.4.1(c)", "Eq. (16)")
        positive("D", "Outside diameter D", Kind.LENGTH, 0.1683, si = "mm")
        positive("y", "Resultant displacement to be absorbed y", Kind.LENGTH, 0.05, si = "mm")
        positive("L", "Developed length between anchors L", Kind.LENGTH, 20.0, si = "m", us = "ft")
        positive("U", "Straight-line distance between anchors U", Kind.LENGTH, 15.0, si = "m", us = "ft")
        positive("SA", "Allowable displacement stress range S_A", Kind.STRESS, 200e6)
        positive("Ea", "Reference modulus E_a", Kind.MODULUS, 203e9)
        compute {
            require(n("L") > n("U"), "Developed length L must exceed anchor distance U")
            val lhs = step("D·y / (L − U)²", "D[mm]·y[mm] / (L − U)[m]²", Kind.DIMENSIONLESS, (n("D") * 1000) * (n("y") * 1000) / (n("L") - n("U")).pow(2))
            val k1 = step("K₁", "K₁ = 208 000·S_A / E_a  (mm/m)²", Kind.DIMENSIONLESS, 208_000 * n("SA") / n("Ea"))
            step("Ratio", "D·y/(L−U)² ÷ K₁", Kind.DIMENSIONLESS, lhs / k1, primary = true)
            check(lhs <= k1, "Exempt from formal analysis (≤ K₁)", "Formal flexibility analysis required (> K₁)")
            info("Not valid for systems with more than two anchors, non-uniform size, or severe cyclic conditions.")
        }
    }

    val hydrotest = calculator("PIP-020", "Hydrostatic Test Pressure — ASME B31.3", Module.PIPING) {
        description = "Minimum hydrostatic leak test pressure."
        keywords("hydrotest", "leak test", "test pressure", "345.4.2")
        ref("ASME B31.3", "345.4.2", "Eq. (24)")
        positive("P", "Internal design gauge pressure", Kind.PRESSURE_GAUGE, 20e5)
        positive("ST", "Allowable stress at test temperature S_T", Kind.STRESS, 137.9e6)
        positive("S", "Allowable stress at design temperature S", Kind.STRESS, 120e6)
        compute {
            val rr = step("Stress ratio R_r", "R_r = S_T / S  (≤ 6.5)", Kind.DIMENSIONLESS, min(n("ST") / n("S"), 6.5))
            step("Test pressure P_T", "P_T = 1.5·P·R_r", Kind.PRESSURE_GAUGE, 1.5 * n("P") * rr, "Eq. (24)", primary = true)
            info("Reduce P_T if it would produce a stress above yield at test temperature (345.4.2(b)). Add static head for elevation differences.")
        }
    }

    val pneumaticTest = calculator("PIP-021", "Pneumatic Test Pressure & Stored Energy", Module.PIPING) {
        description = "Pneumatic leak test pressure and the stored energy (TNT equivalent) of the compressed gas."
        keywords("pneumatic", "stored energy", "TNT", "safe distance", "PCC-2")
        ref("ASME B31.3", "345.5.4")
        ref("ASME PCC-2", "Article 501", "Stored energy")
        positive("P", "Internal design gauge pressure", Kind.PRESSURE_GAUGE, 10e5)
        positive("V", "Test volume", Kind.VOLUME, 2.0)
        number("k", "Ratio of specific heats γ", Kind.DIMENSIONLESS, 1.4, 1.0, 2.0, true, help = "Air / nitrogen = 1.4")
        compute {
            val pt = step("Test pressure P_T", "P_T = 1.1·P", Kind.PRESSURE_GAUGE, 1.1 * n("P"), "345.5.4", primary = true)
            val pa = step("Absolute test pressure", "P_abs = P_T + P_atm", Kind.PRESSURE_ABS, pt + ATM_PA)
            val k = n("k")
            val e = step("Stored energy E", "E = P_abs·V/(γ−1)·[1 − (P_atm/P_abs)^((γ−1)/γ)]", Kind.ENERGY,
                pa * n("V") / (k - 1) * (1 - (ATM_PA / pa).pow((k - 1) / k)), "PCC-2 Art. 501", output = true)
            step("TNT equivalent", "m_TNT = E / 4.184 MJ/kg", Kind.MASS, e / 4.184e6, output = true)
            warn("Pneumatic testing is hazardous. Establish exclusion distances per ASME PCC-2 Article 501 and site procedures.")
        }
    }

    val erosionalVelocity = calculator("PIP-023", "Erosional Velocity — API RP 14E", Module.PIPING) {
        description = "Maximum velocity to limit erosion in two-phase and gas/liquid lines."
        keywords("erosion", "velocity", "two-phase", "C factor", "14E")
        ref("API RP 14E", "2.5")
        positive("rho", "Mixture density ρ_m", Kind.DENSITY, 100.0)
        choice("C", "Empirical constant C", listOf(
            "100" to "100 — continuous service", "125" to "125 — intermittent service",
            "150" to "150 — continuous, solids-free, corrosion controlled", "200" to "200 — non-corrosive, solids-free",
        ))
        number("v", "Actual mixture velocity (optional)", Kind.VELOCITY, 0.0, 0.0)
        compute {
            val rhoUs = n("rho") / 16.018463374
            val ve = step("Erosional velocity V_e", "V_e = C / √ρ_m   (ft/s, ρ in lb/ft³)", Kind.VELOCITY, c("C").toDouble() / sqrt(rhoUs) * 0.3048, primary = true)
            if (n("v") > 0) check(n("v") <= ve, "Velocity below erosional limit", "Velocity exceeds erosional limit")
        }
    }

    val pressureDrop = calculator("PIP-024", "Pressure Drop — Darcy–Weisbach", Module.PIPING) {
        description = "Single-phase incompressible pressure drop in straight pipe plus fittings, Colebrook friction factor."
        keywords("pressure drop", "friction", "Colebrook", "Moody", "head loss", "line sizing", "K factor")
        ref("Crane TP-410", note = "Flow of Fluids")
        positive("Q", "Volume flow rate", Kind.VOLUME_FLOW, 50.0 / 3600)
        positive("d", "Pipe inside diameter", Kind.LENGTH, 0.1023)
        positive("L", "Pipe length", Kind.LENGTH, 100.0, si = "m", us = "ft")
        number("eps", "Absolute roughness ε", Kind.LENGTH, 4.5e-5, 0.0, help = "Commercial steel 0.045 mm; stainless 0.015 mm; PVC 0.0015 mm")
        positive("rho", "Density ρ", Kind.DENSITY, 998.0)
        positive("mu", "Dynamic viscosity μ", Kind.DYN_VISCOSITY, 1.0e-3)
        number("K", "Sum of fitting resistance coefficients ΣK", Kind.DIMENSIONLESS, 0.0, 0.0)
        number("dz", "Elevation rise (outlet − inlet)", Kind.LENGTH, 0.0, si = "m", us = "ft")
        compute {
            val d = n("d"); val rho = n("rho")
            val a = step("Flow area A", "A = π·d²/4", Kind.AREA, PI * d * d / 4)
            val v = step("Velocity v", "v = Q / A", Kind.VELOCITY, n("Q") / a, output = true)
            val re = step("Reynolds number Re", "Re = ρ·v·d / μ", Kind.DIMENSIONLESS, rho * v * d / n("mu"), output = true)
            val f = step("Darcy friction factor f", if (re < 2300) "f = 64 / Re (laminar)" else "1/√f = −2·log₁₀(ε/3.7d + 2.51/(Re·√f))", Kind.DIMENSIONLESS,
                Numerics.darcyFriction(re, n("eps") / d), output = true)
            if (re in 2300.0..4000.0) warn("Transitional flow (2300 < Re < 4000): friction factor is uncertain")
            val q = 0.5 * rho * v * v
            val dpf = step("Friction loss ΔP_f", "ΔP_f = f·(L/d)·ρv²/2", Kind.PRESSURE_DIFF, f * n("L") / d * q)
            val dpk = step("Fitting loss ΔP_K", "ΔP_K = ΣK·ρv²/2", Kind.PRESSURE_DIFF, n("K") * q)
            val dpz = step("Static head ΔP_z", "ΔP_z = ρ·g·Δz", Kind.PRESSURE_DIFF, rho * G0 * n("dz"))
            val dp = step("Total pressure drop ΔP", "ΔP = ΔP_f + ΔP_K + ΔP_z", Kind.PRESSURE_DIFF, dpf + dpk + dpz, primary = true)
            step("Head loss (friction + fittings)", "h = (ΔP_f + ΔP_K) / (ρ·g)", Kind.LENGTH, (dpf + dpk) / (rho * G0), output = true, si = "m", us = "ft")
            step("Friction gradient per 100 m", "ΔP_f·100 / L", Kind.PRESSURE_DIFF, dpf * 100 / n("L"), output = true)
            if (dp < 0) info("Negative total: elevation gain exceeds losses (flow downhill).")
        }
    }

    val hazenWilliams = calculator("PIP-025", "Pressure Drop — Hazen–Williams (Water)", Module.PIPING) {
        description = "Friction loss for water flow, as used in fire protection and water distribution."
        keywords("Hazen Williams", "fire", "sprinkler", "water", "C factor", "NFPA 13")
        ref("NFPA 13", note = "Hydraulic calculation")
        positive("Q", "Flow rate", Kind.VOLUME_FLOW, 0.02)
        positive("d", "Inside diameter", Kind.LENGTH, 0.1023)
        positive("L", "Equivalent length", Kind.LENGTH, 100.0, si = "m", us = "ft")
        number("C", "Hazen–Williams C", Kind.DIMENSIONLESS, 120.0, 40.0, 160.0, help = "Black steel (wet) 120; galvanized 120; copper/stainless 150; plastic 150")
        compute {
            val h = step("Head loss h_f", "h_f = 10.67·L·Q^1.852 / (C^1.852·d^4.87)   (SI)", Kind.LENGTH,
                10.67 * n("L") * n("Q").pow(1.852) / (n("C").pow(1.852) * n("d").pow(4.87)), output = true, si = "m", us = "ft")
            step("Pressure loss ΔP", "ΔP = ρ·g·h_f  (ρ = 999 kg/m³)", Kind.PRESSURE_DIFF, 999.0 * G0 * h, primary = true)
            step("Velocity", "v = 4Q/(πd²)", Kind.VELOCITY, 4 * n("Q") / (PI * n("d").pow(2)), output = true)
        }
    }

    val waterHammer = calculator("PIP-028", "Water Hammer — Joukowsky Surge", Module.PIPING) {
        description = "Pressure rise from rapid valve closure and the pressure wave speed in an elastic pipe."
        keywords("surge", "water hammer", "Joukowsky", "wave speed", "transient")
        ref("Wylie & Streeter, Fluid Transients")
        positive("rho", "Fluid density ρ", Kind.DENSITY, 998.0)
        positive("K", "Fluid bulk modulus K", Kind.MODULUS, 2.19e9)
        positive("D", "Pipe inside diameter", Kind.LENGTH, 0.2)
        positive("t", "Pipe wall thickness", Kind.LENGTH, 0.008)
        positive("E", "Pipe elastic modulus", Kind.MODULUS, 200e9)
        choice("anch", "Pipe restraint", listOf("joints" to "Expansion joints throughout (c₁ = 1)", "anch" to "Anchored against axial movement (c₁ = 1 − ν²)", "up" to "Anchored at upstream end only (c₁ = 1 − ν/2)"), "anch")
        number("nu", "Poisson's ratio ν", Kind.DIMENSIONLESS, 0.3, 0.0, 0.5)
        positive("dv", "Velocity change Δv", Kind.VELOCITY, 2.0)
        positive("L", "Pipe length to reflection point", Kind.LENGTH, 500.0, si = "m", us = "ft")
        compute {
            val nu = n("nu")
            val c1 = step("Restraint factor c₁", "per restraint condition", Kind.DIMENSIONLESS, when (c("anch")) { "joints" -> 1.0; "up" -> 1 - nu / 2; else -> 1 - nu * nu })
            val a = step("Wave speed a", "a = √[(K/ρ) / (1 + c₁·K·D/(E·t))]", Kind.VELOCITY,
                sqrt((n("K") / n("rho")) / (1 + c1 * n("K") * n("D") / (n("E") * n("t")))), output = true)
            step("Surge pressure ΔP", "ΔP = ρ·a·Δv", Kind.PRESSURE_DIFF, n("rho") * a * n("dv"), primary = true)
            step("Critical closure time", "t_c = 2L / a", Kind.TIME, 2 * n("L") / a, output = true)
            info("Joukowsky applies to closures faster than 2L/a; slower closures give proportionally less surge.")
        }
    }

    /** Weight per metre of steel, content and insulation for a schedule pipe. */
    internal fun CalcScope.pipeWeights(): Double {
        val nps = c("nps"); val sch = c("sch")
        val od = step("Outside diameter", "B36.10M", Kind.LENGTH, PipeData.size(nps).odM)
        val wt = step("Nominal wall", "Sch $sch", Kind.LENGTH, wallFor(nps, sch))
        val id = step("Inside diameter", "ID = OD − 2t", Kind.LENGTH, od - 2 * wt, output = true)
        val ws = step("Steel weight", "w_s = ρ_s·π(OD² − ID²)/4", Kind.LINEAR_DENSITY, STEEL_DENSITY * PI * (od * od - id * id) / 4, output = true)
        val wc = step("Content weight", "w_c = ρ_c·π·ID²/4", Kind.LINEAR_DENSITY, n("rhoC") * PI * id * id / 4, output = true)
        val ti = n("ti")
        val wi = step("Insulation weight", "w_i = ρ_i·π[(OD + 2t_i)² − OD²]/4", Kind.LINEAR_DENSITY, n("rhoI") * PI * ((od + 2 * ti).pow(2) - od * od) / 4, output = true)
        return ws + wc + wi
    }

    private fun com.mechultimate.core.engine.CalculatorBuilder.weightInputs() {
        choice("nps", "Nominal pipe size", PipeData.npsOptions, "6")
        choice("sch", "Schedule", scheduleOptions, "40")
        number("rhoC", "Content density", Kind.DENSITY, 1000.0, 0.0)
        number("ti", "Insulation thickness", Kind.LENGTH, 0.05, 0.0)
        number("rhoI", "Insulation density (incl. jacket)", Kind.DENSITY, 150.0, 0.0)
    }

    val pipeWeight = calculator("PIP-032", "Pipe Weight — Steel, Contents & Insulation", Module.PIPING) {
        description = "Weight per unit length of schedule pipe, contents and insulation."
        keywords("weight", "mass", "kg/m", "lb/ft", "dead load")
        ref("ASME B36.10M / B36.19M")
        weightInputs()
        compute {
            val w = pipeWeights()
            out("Total weight per length", Kind.LINEAR_DENSITY, w, primary = true)
            out("Total load per length", Kind.FORCE_PER_LENGTH, w * G0, si = "N/m", us = "lbf/ft")
        }
    }

    val supportSpan = calculator("PIP-013", "Pipe Support Span", Module.PIPING) {
        description = "Maximum span between supports, limited by bending stress and sag."
        keywords("span", "support spacing", "sag", "deflection", "MSS SP-58", "121.5")
        ref("ASME B31.1", "Table 121.5", "Suggested spacing (comparison)")
        ref("MSS SP-58")
        weightInputs()
        choice("model", "Beam model", listOf("cont" to "Continuous over supports (M = wL²/10, δ = wL⁴/185EI)", "ss" to "Simply supported (M = wL²/8, δ = 5wL⁴/384EI)"))
        positive("Sb", "Allowable sustained bending stress", Kind.STRESS, 20e6, help = "Typical practice 15–25 MPa to leave room for pressure stress")
        positive("dmax", "Allowable sag", Kind.LENGTH, 2.5e-3)
        positive("E", "Elastic modulus", Kind.MODULUS, 203e9)
        compute {
            val w = pipeWeights() * G0
            val od = PipeData.size(c("nps")).odM
            val id = od - 2 * wallFor(c("nps"), c("sch"))
            val i = step("Moment of inertia I", "I = π(OD⁴ − ID⁴)/64", Kind.AREA_MOMENT, PI * (od.pow(4) - id.pow(4)) / 64)
            val z = step("Section modulus Z", "Z = 2I / OD", Kind.SECTION_MODULUS, 2 * i / od)
            val cont = c("model") == "cont"
            val ls = step("Stress-limited span", if (cont) "L = √(10·Z·S_b / w)" else "L = √(8·Z·S_b / w)", Kind.LENGTH,
                sqrt((if (cont) 10.0 else 8.0) * z * n("Sb") / w), output = true, si = "m", us = "ft")
            val ld = step("Sag-limited span", if (cont) "L = (185·E·I·δ / w)^¼" else "L = (384·E·I·δ / 5w)^¼", Kind.LENGTH,
                (if (cont) 185 * n("E") * i * n("dmax") / w else 384 * n("E") * i * n("dmax") / (5 * w)).pow(0.25), output = true, si = "m", us = "ft")
            step("Maximum support span", "L = min(stress, sag)", Kind.LENGTH, min(ls, ld), primary = true, si = "m", us = "ft")
            info("Reduce spans at concentrated loads (valves, flanges) and at changes of direction.")
        }
    }

    private val uncBolts = listOf(
        "0.5:13" to "1/2\"-13 UNC", "0.625:11" to "5/8\"-11 UNC", "0.75:10" to "3/4\"-10 UNC", "0.875:9" to "7/8\"-9 UNC",
        "1.0:8" to "1\"-8 UNC", "1.125:8" to "1-1/8\"-8 UN", "1.25:8" to "1-1/4\"-8 UN", "1.375:8" to "1-3/8\"-8 UN",
        "1.5:8" to "1-1/2\"-8 UN", "1.625:8" to "1-5/8\"-8 UN", "1.75:8" to "1-3/4\"-8 UN", "1.875:8" to "1-7/8\"-8 UN",
        "2.0:8" to "2\"-8 UN", "2.25:8" to "2-1/4\"-8 UN", "2.5:8" to "2-1/2\"-8 UN",
    )

    val flangeBoltTorque = calculator("PIP-044", "Flange Bolt Torque — ASME PCC-1", Module.PIPING) {
        description = "Bolt load and tightening torque for a target bolt stress, with a legacy-pattern pass schedule."
        keywords("torque", "flange", "bolting", "stud", "B7", "nut factor", "PCC-1")
        ref("ASME PCC-1", "Appendix O", "Target bolt prestress")
        ref("ASME B1.1", note = "Tensile stress area")
        choice("bolt", "Stud bolt size", uncBolts, "0.75:10")
        number("nb", "Number of bolts", Kind.DIMENSIONLESS, 8.0, 1.0, 200.0)
        positive("Sb", "Target bolt stress (on tensile stress area)", Kind.STRESS, 345e6, help = "PCC-1 typical 40–70 % of yield; e.g. 345 MPa (50 ksi) for A193 B7")
        number("K", "Nut factor K", Kind.DIMENSIONLESS, 0.20, 0.05, 0.4, help = "≈ 0.16–0.2 with lubricant; 0.2 is a common default")
        compute {
            val (dStr, nStr) = c("bolt").split(":")
            val dIn = dStr.toDouble(); val tpi = nStr.toDouble()
            val asIn = 0.7854 * (dIn - 0.9743 / tpi).pow(2)
            val a = step("Tensile stress area A_s", "A_s = 0.7854·(d − 0.9743/n)²", Kind.AREA, asIn * 6.4516e-4, "ASME B1.1")
            val f = step("Bolt load per bolt F", "F = S_b·A_s", Kind.FORCE, n("Sb") * a, output = true)
            step("Total bolt load", "F_total = N·F", Kind.FORCE, f * n("nb"), output = true)
            val t = step("Target torque T", "T = K·d·F", Kind.TORQUE, n("K") * dIn * 0.0254 * f, primary = true)
            text("Pass schedule (legacy cross pattern)",
                listOf(0.3 to "Pass 1 (20–30 %)", 0.6 to "Pass 2 (50–70 %)", 1.0 to "Pass 3 (100 %)").joinToString("\n") { (k, name) ->
                    "$name: ${formatNumber(k * t)} N·m / ${formatNumber(k * t / 1.3558179483)} lbf·ft"
                } + "\nThen circular passes at 100 % until nuts no longer turn")
        }
    }

    val all = listOf(
        b313Thickness, b311Thickness, b314Thickness, b318Pressure, blindThickness, supportSpan, thermalExpansion,
        expansionLoop, flexibilityCheck, hydrotest, pneumaticTest, erosionalVelocity, pressureDrop, hazenWilliams,
        waterHammer, pipeWeight, flangeBoltTorque,
    )
}
