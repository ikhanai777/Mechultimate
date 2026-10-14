package com.mechultimate.core.calcs

import com.mechultimate.core.engine.Module
import com.mechultimate.core.engine.Status
import com.mechultimate.core.engine.calculator
import com.mechultimate.core.engine.visibleIf
import com.mechultimate.core.props.IF97
import com.mechultimate.core.units.G0
import com.mechultimate.core.units.Kind
import kotlin.math.PI
import kotlin.math.pow
import kotlin.math.sqrt

object Fluids {
    val reynolds = calculator("FLD-001", "Reynolds Number & Flow Regime", Module.FLUIDS) {
        description = "Reynolds number for pipe flow and the corresponding flow regime."
        keywords("Reynolds", "laminar", "turbulent", "flow regime")
        ref("Fluid mechanics", note = "Re = ρvD/μ")
        positive("v", "Mean velocity", Kind.VELOCITY, 2.0)
        positive("D", "Characteristic length (pipe ID)", Kind.LENGTH, 0.1)
        positive("rho", "Density", Kind.DENSITY, 998.0)
        positive("mu", "Dynamic viscosity", Kind.DYN_VISCOSITY, 1e-3)
        compute {
            val re = step("Reynolds number", "Re = ρ·v·D / μ", Kind.DIMENSIONLESS, n("rho") * n("v") * n("D") / n("mu"), primary = true)
            text("Regime (pipe flow)", when { re < 2300 -> "Laminar (Re < 2300)"; re < 4000 -> "Transitional (2300–4000)"; else -> "Turbulent (Re > 4000)" }, primary = true)
            step("Kinematic viscosity", "ν = μ / ρ", Kind.KIN_VISCOSITY, n("mu") / n("rho"), output = true)
        }
    }

    private val iecMotorsKw = listOf(0.37, 0.55, 0.75, 1.1, 1.5, 2.2, 3.0, 4.0, 5.5, 7.5, 11.0, 15.0, 18.5, 22.0, 30.0, 37.0, 45.0, 55.0, 75.0, 90.0, 110.0, 132.0, 160.0, 200.0, 250.0, 315.0, 355.0, 400.0, 450.0, 500.0)

    val pumpPower = calculator("FLD-003", "Pump Power & Motor Sizing", Module.FLUIDS) {
        description = "Hydraulic, shaft and electrical power of a pump, and the next standard IEC motor rating."
        keywords("pump", "power", "motor", "efficiency", "brake horsepower", "BHP", "head")
        ref("Hydraulic Institute", note = "P = ρgQH/η")
        positive("Q", "Flow rate", Kind.VOLUME_FLOW, 100.0 / 3600)
        positive("H", "Total dynamic head", Kind.LENGTH, 50.0, si = "m", us = "ft")
        positive("rho", "Fluid density", Kind.DENSITY, 998.0)
        number("eta", "Pump efficiency", Kind.RATIO, 0.75, 0.0, 1.0, true)
        number("etaM", "Motor efficiency", Kind.RATIO, 0.94, 0.0, 1.0, true)
        number("margin", "Motor sizing margin", Kind.RATIO, 0.10, 0.0, 1.0)
        compute {
            val ph = step("Hydraulic power", "P_h = ρ·g·Q·H", Kind.POWER, n("rho") * G0 * n("Q") * n("H"), output = true)
            val ps = step("Shaft power", "P_s = P_h / η_p", Kind.POWER, ph / n("eta"), primary = true)
            step("Electrical input", "P_e = P_s / η_m", Kind.POWER, ps / n("etaM"), output = true)
            val need = ps * (1 + n("margin")) / 1000
            val motor = iecMotorsKw.firstOrNull { it >= need }
            text("Standard motor (IEC)", motor?.let { "$it kW" } ?: "> 500 kW — special", primary = true, formula = "Next IEC rating ≥ P_s·(1 + margin)")
        }
    }

    val npsh = calculator("FLD-004", "NPSH Available", Module.FLUIDS) {
        description = "Net positive suction head available and margin against NPSH required."
        keywords("NPSH", "cavitation", "suction", "NPSHa", "NPSHr", "vapour pressure")
        ref("ANSI/HI 9.6.1", note = "NPSH margin")
        choice("fluid", "Vapour pressure", listOf("water" to "Water — from IAPWS-IF97 at temperature", "other" to "Other liquid — enter vapour pressure"))
        number("T", "Liquid temperature", Kind.TEMPERATURE, 313.15, 273.16, 623.15, visibleIf = visibleIf("fluid", "water"))
        positive("Pv", "Vapour pressure (absolute)", Kind.PRESSURE_ABS, 7_384.0, visibleIf = visibleIf("fluid", "other"))
        positive("rho", "Liquid density", Kind.DENSITY, 992.0)
        positive("Ps", "Pressure on liquid surface (absolute)", Kind.PRESSURE_ABS, 101_325.0)
        number("z", "Liquid level above pump centreline (negative for lift)", Kind.LENGTH, 2.0, si = "m", us = "ft")
        number("hf", "Suction line losses", Kind.LENGTH, 0.5, 0.0, si = "m", us = "ft")
        number("npshr", "NPSH required (from pump curve)", Kind.LENGTH, 3.0, 0.0, si = "m", us = "ft")
        number("ratio", "Required margin ratio NPSHa/NPSHr", Kind.DIMENSIONLESS, 1.1, 1.0, help = "HI 9.6.1 suggests 1.1–2.5 depending on application")
        compute {
            val pv = if (c("fluid") == "water") step("Vapour pressure", "p_sat(T) — IAPWS-IF97", Kind.PRESSURE_ABS, IF97.psat(n("T"))) else n("Pv")
            val rg = n("rho") * G0
            val a = step("NPSH available", "NPSHa = (P_s − P_v)/(ρ·g) + z − h_f", Kind.LENGTH, (n("Ps") - pv) / rg + n("z") - n("hf"), primary = true, si = "m", us = "ft")
            if (n("npshr") > 0) {
                val r = step("Margin ratio", "NPSHa / NPSHr", Kind.DIMENSIONLESS, a / n("npshr"), output = true)
                step("Margin", "NPSHa − NPSHr", Kind.LENGTH, a - n("npshr"), output = true, si = "m", us = "ft")
                check(r >= n("ratio"), "Margin ratio meets requirement", "Insufficient NPSH margin — cavitation risk")
            }
        }
    }

    val affinity = calculator("FLD-005", "Pump Affinity Laws", Module.FLUIDS) {
        description = "New flow, head and power after a speed change or impeller trim."
        keywords("affinity", "speed change", "impeller trim", "VFD", "fan laws")
        ref("Hydraulic Institute", note = "Affinity laws")
        positive("Q1", "Flow at duty 1", Kind.VOLUME_FLOW, 100.0 / 3600)
        positive("H1", "Head at duty 1", Kind.LENGTH, 50.0, si = "m", us = "ft")
        positive("P1", "Power at duty 1", Kind.POWER, 18_000.0)
        positive("N1", "Speed 1", Kind.ROT_SPEED, 2950 * 2 * PI / 60)
        positive("N2", "Speed 2", Kind.ROT_SPEED, 2450 * 2 * PI / 60)
        positive("D1", "Impeller diameter 1", Kind.LENGTH, 0.25)
        positive("D2", "Impeller diameter 2", Kind.LENGTH, 0.25)
        compute {
            val rn = n("N2") / n("N1"); val rd = n("D2") / n("D1")
            step("Flow Q₂", "Q₂ = Q₁·(N₂/N₁)·(D₂/D₁)", Kind.VOLUME_FLOW, n("Q1") * rn * rd, primary = true)
            step("Head H₂", "H₂ = H₁·(N₂/N₁)²·(D₂/D₁)²", Kind.LENGTH, n("H1") * (rn * rd).pow(2), primary = true, si = "m", us = "ft")
            step("Power P₂", "P₂ = P₁·(N₂/N₁)³·(D₂/D₁)³", Kind.POWER, n("P1") * (rn * rd).pow(3), primary = true)
            if (rd < 0.85) warn("Impeller trims below ~85 % reduce accuracy of the affinity laws and pump efficiency.")
        }
    }

    val specificSpeed = calculator("FLD-007", "Pump Specific Speed & Suction Specific Speed", Module.FLUIDS) {
        description = "Specific speed (impeller type) and suction specific speed at best efficiency point."
        keywords("specific speed", "Ns", "Nss", "suction specific speed", "impeller type", "API 610")
        ref("API 610 / ISO 13709", note = "Suction specific speed limits")
        positive("N", "Speed", Kind.ROT_SPEED, 2950 * 2 * PI / 60)
        positive("Q", "Flow at BEP", Kind.VOLUME_FLOW, 100.0 / 3600)
        positive("H", "Head per stage at BEP", Kind.LENGTH, 50.0, si = "m", us = "ft")
        positive("npshr", "NPSHr at BEP", Kind.LENGTH, 3.0, si = "m", us = "ft")
        choice("eye", "Impeller suction", listOf("1" to "Single suction", "2" to "Double suction"))
        compute {
            val rpm = n("N") * 60 / (2 * PI)
            val qEye = n("Q") / c("eye").toDouble()
            val gpm = n("Q") / 6.30901964e-5; val gpmEye = qEye / 6.30901964e-5
            val hFt = n("H") / 0.3048; val nFt = n("npshr") / 0.3048
            val ns = step("Specific speed N_s (US)", "N_s = N·√Q / H^0.75   [rpm, gpm, ft]", Kind.DIMENSIONLESS, rpm * sqrt(gpm) / hFt.pow(0.75), primary = true)
            step("Specific speed n_q (metric)", "n_q = N·√Q / H^0.75   [rpm, m³/s, m]", Kind.DIMENSIONLESS, rpm * sqrt(n("Q")) / n("H").pow(0.75), output = true)
            val nss = step("Suction specific speed N_ss (US)", "N_ss = N·√Q_eye / NPSHr^0.75", Kind.DIMENSIONLESS, rpm * sqrt(gpmEye) / nFt.pow(0.75), output = true)
            text("Impeller type", when { ns < 1500 -> "Radial (low N_s)"; ns < 4000 -> "Radial / Francis"; ns < 9000 -> "Mixed flow"; else -> "Axial flow" })
            check(nss <= 11_000, "N_ss ≤ 11 000 (US)", "N_ss > 11 000: higher risk of suction recirculation", Status.WARN)
        }
    }

    val controlValve = calculator("FLD-011", "Control Valve Sizing — Liquid (IEC 60534)", Module.FLUIDS) {
        description = "Required flow coefficient C_v / K_v for liquid service, including choked-flow limitation."
        keywords("control valve", "Cv", "Kv", "choked flow", "ISA 75.01", "cavitation", "FL")
        ref("IEC 60534-2-1 / ISA-75.01.01", note = "Incompressible fluid, turbulent, no fittings")
        positive("Q", "Flow rate", Kind.VOLUME_FLOW, 50.0 / 3600)
        positive("P1", "Inlet pressure (absolute)", Kind.PRESSURE_ABS, 10e5)
        positive("P2", "Outlet pressure (absolute)", Kind.PRESSURE_ABS, 6e5)
        positive("Pv", "Vapour pressure (absolute)", Kind.PRESSURE_ABS, 3_170.0)
        positive("Pc", "Critical pressure (absolute)", Kind.PRESSURE_ABS, 220.64e5, help = "Water 220.64 bar")
        positive("G", "Specific gravity", Kind.DIMENSIONLESS, 1.0)
        number("FL", "Liquid pressure recovery factor F_L", Kind.DIMENSIONLESS, 0.9, 0.3, 1.0, help = "Globe ≈ 0.9; ball ≈ 0.6; butterfly ≈ 0.55–0.7")
        compute {
            val dp = n("P1") - n("P2")
            require(dp > 0, "Outlet pressure must be below inlet pressure")
            val ff = step("Critical pressure ratio factor F_F", "F_F = 0.96 − 0.28·√(P_v/P_c)", Kind.DIMENSIONLESS, 0.96 - 0.28 * sqrt(n("Pv") / n("Pc")))
            val dpChoked = step("Choked ΔP", "ΔP_choked = F_L²·(P₁ − F_F·P_v)", Kind.PRESSURE_DIFF, n("FL").pow(2) * (n("P1") - ff * n("Pv")), output = true)
            val dpSize = minOf(dp, dpChoked)
            if (dp > dpChoked) warn("Flow is choked (cavitating or flashing): sizing uses ΔP_choked; consider anti-cavitation trim.")
            val kv = step("K_v", "K_v = Q[m³/h]·√(G / ΔP[bar])", Kind.DIMENSIONLESS, n("Q") * 3600 * sqrt(n("G") / (dpSize / 1e5)), output = true)
            step("C_v", "C_v = 1.156·K_v", Kind.DIMENSIONLESS, 1.156 * kv, primary = true)
            text("Valve selection", "Choose a valve whose rated C_v puts this duty at about 20–80 % of travel")
        }
    }

    val orifice = calculator("FLD-012", "Orifice Flow (Liquid)", Module.FLUIDS) {
        description = "Flow through a sharp-edged orifice in a pipe for a measured differential pressure."
        keywords("orifice", "discharge coefficient", "flow measurement", "restriction", "beta ratio")
        ref("ISO 5167-2", note = "Simplified with constant C_d")
        positive("d", "Orifice bore d", Kind.LENGTH, 0.05)
        positive("D", "Pipe inside diameter D", Kind.LENGTH, 0.1023)
        number("Cd", "Discharge coefficient C_d", Kind.DIMENSIONLESS, 0.61, 0.3, 1.0)
        positive("dp", "Differential pressure ΔP", Kind.PRESSURE_DIFF, 25_000.0)
        positive("rho", "Density", Kind.DENSITY, 998.0)
        compute {
            val beta = step("Beta ratio β", "β = d / D", Kind.DIMENSIONLESS, n("d") / n("D"))
            require(beta < 1, "Orifice must be smaller than the pipe")
            val a = PI * n("d").pow(2) / 4
            val q = step("Volume flow Q", "Q = C_d/√(1 − β⁴)·A·√(2ΔP/ρ)", Kind.VOLUME_FLOW, n("Cd") / sqrt(1 - beta.pow(4)) * a * sqrt(2 * n("dp") / n("rho")), primary = true)
            step("Mass flow", "ṁ = ρ·Q", Kind.MASS_FLOW, n("rho") * q, output = true)
            check(beta in 0.1..0.75, "β within 0.1–0.75", "β outside the usual 0.1–0.75 range", Status.WARN)
        }
    }

    val all = listOf(reynolds, pumpPower, npsh, affinity, specificSpeed, controlValve, orifice)
}
