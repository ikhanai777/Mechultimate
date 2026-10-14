package com.mechultimate.core.calcs

import com.mechultimate.core.data.Api526
import com.mechultimate.core.engine.CalcScope
import com.mechultimate.core.engine.Module
import com.mechultimate.core.engine.calculator
import com.mechultimate.core.units.ATM_PA
import com.mechultimate.core.units.Kind
import com.mechultimate.core.units.formatNumber
import kotlin.math.pow
import kotlin.math.sqrt

/** Records the API 526 orifice selection for a required area in m². */
internal fun CalcScope.orificeSelection(aReq: Double) {
    val sel = Api526.select(aReq)
    if (sel == null) {
        text("API 526 orifice", "Larger than T — use multiple valves", primary = true)
        check(false, "", "Required area exceeds the largest API 526 orifice (T = 26.0 in²)")
    } else {
        text("API 526 orifice", "${sel.first}  (${formatNumber(sel.second)} in² / ${formatNumber(sel.second * 645.16)} mm²)", primary = true,
            formula = "Smallest API 526 orifice ≥ required area", ref = "API 526")
    }
}

object Relief {
    val gas = calculator("PRV-001", "Relief Valve Sizing — Gas / Vapour (API 520)", Module.RELIEF) {
        description = "Required effective discharge area for gas or vapour relief, critical or subcritical flow."
        keywords("PSV", "PRV", "safety valve", "gas", "vapour", "critical flow", "orifice", "API 520")
        ref("API 520 Part I", "5.6", "Eqs. (3) and (9), SI")
        ref("API 526", note = "Orifice designations")
        positive("W", "Required relieving rate W", Kind.MASS_FLOW, 10_000.0 / 3600)
        positive("Pset", "Set pressure", Kind.PRESSURE_GAUGE, 10e5)
        number("op", "Allowable overpressure", Kind.RATIO, 0.10, 0.0, 1.0, help = "10 % single valve, 16 % multiple valves, 21 % fire case")
        number("Pb", "Total back pressure", Kind.PRESSURE_GAUGE, 0.0, 0.0)
        positive("T", "Relieving temperature", Kind.TEMPERATURE, 373.15)
        positive("M", "Molecular weight M", Kind.DIMENSIONLESS, 28.96)
        positive("Z", "Compressibility factor Z", Kind.DIMENSIONLESS, 1.0)
        number("k", "Ratio of specific heats k", Kind.DIMENSIONLESS, 1.4, 1.0, 2.0, true)
        number("Kd", "Effective discharge coefficient K_d", Kind.DIMENSIONLESS, 0.975, 0.0, 1.0, true)
        number("Kb", "Back-pressure correction K_b", Kind.DIMENSIONLESS, 1.0, 0.0, 1.0, true, help = "1.0 for conventional valves at low back pressure; from manufacturer for balanced bellows")
        number("Kc", "Combination correction K_c", Kind.DIMENSIONLESS, 1.0, 0.0, 1.0, true, help = "0.9 with an upstream rupture disc")
        compute {
            val k = n("k")
            val p1 = step("Relieving pressure P₁", "P₁ = P_set·(1 + overpressure) + P_atm", Kind.PRESSURE_ABS, n("Pset") * (1 + n("op")) + ATM_PA, output = true)
            val p2 = step("Back pressure P₂", "P₂ = P_b + P_atm", Kind.PRESSURE_ABS, n("Pb") + ATM_PA)
            val rc = step("Critical pressure ratio", "(P₂/P₁)_c = [2/(k+1)]^(k/(k−1))", Kind.DIMENSIONLESS, (2 / (k + 1)).pow(k / (k - 1)))
            val wKgH = n("W") * 3600
            val p1k = p1 / 1000
            val tzm = n("T") * n("Z") / n("M")
            val aMm2 = if (p2 / p1 <= rc) {
                val cc = step("Coefficient C", "C = 0.03948·√{k·[2/(k+1)]^((k+1)/(k−1))}", Kind.DIMENSIONLESS, 0.03948 * sqrt(k * (2 / (k + 1)).pow((k + 1) / (k - 1))))
                text("Flow regime", "Critical (choked) flow", formula = "P₂/P₁ ≤ critical ratio")
                wKgH / (cc * n("Kd") * p1k * n("Kb") * n("Kc")) * sqrt(tzm)
            } else {
                val r = p2 / p1
                val f2 = step("Coefficient F₂", "F₂ = √{[k/(k−1)]·r^(2/k)·[1 − r^((k−1)/k)]/(1 − r)}", Kind.DIMENSIONLESS,
                    sqrt(k / (k - 1) * r.pow(2 / k) * (1 - r.pow((k - 1) / k)) / (1 - r)))
                text("Flow regime", "Subcritical flow", formula = "P₂/P₁ > critical ratio")
                17.9 * wKgH / (f2 * n("Kd") * n("Kc")) * sqrt(tzm / (p1k * (p1k - p2 / 1000)))
            }
            val a = step("Required effective area A", "A = W/(C·K_d·P₁·K_b·K_c)·√(T·Z/M)   [mm², kg/h, kPa(a), K]", Kind.AREA, aMm2 * 1e-6, "API 520-I", primary = true)
            orificeSelection(a)
        }
    }

    val steam = calculator("PRV-003", "Relief Valve Sizing — Steam (API 520)", Module.RELIEF) {
        description = "Required effective discharge area for saturated or superheated steam (Napier-based)."
        keywords("steam", "PSV", "safety valve", "Napier", "KN", "KSH")
        ref("API 520 Part I", "5.7", "Eq. (11), SI")
        positive("W", "Required relieving rate W", Kind.MASS_FLOW, 5000.0 / 3600)
        positive("Pset", "Set pressure", Kind.PRESSURE_GAUGE, 10e5)
        number("op", "Allowable overpressure", Kind.RATIO, 0.10, 0.0, 1.0)
        number("Kd", "K_d", Kind.DIMENSIONLESS, 0.975, 0.0, 1.0, true)
        number("Kb", "K_b", Kind.DIMENSIONLESS, 1.0, 0.0, 1.0, true)
        number("Kc", "K_c", Kind.DIMENSIONLESS, 1.0, 0.0, 1.0, true)
        number("Ksh", "Superheat correction K_SH", Kind.DIMENSIONLESS, 1.0, 0.0, 1.0, true, help = "1.0 for saturated steam; from API 520 Table for superheated steam")
        compute {
            val p1 = step("Relieving pressure P₁", "P₁ = P_set·(1 + overpressure) + P_atm", Kind.PRESSURE_ABS, n("Pset") * (1 + n("op")) + ATM_PA, output = true)
            val p1k = p1 / 1000
            val kn = step("Napier correction K_N", if (p1k <= 10_339) "K_N = 1.0 (P₁ ≤ 10 339 kPa)" else "K_N = (0.02764·P₁ − 1000)/(0.03324·P₁ − 1061)", Kind.DIMENSIONLESS,
                if (p1k <= 10_339) 1.0 else (0.02764 * p1k - 1000) / (0.03324 * p1k - 1061))
            val aMm2 = 190.5 * n("W") * 3600 / (p1k * n("Kd") * n("Kb") * n("Kc") * kn * n("Ksh"))
            val a = step("Required effective area A", "A = 190.5·W/(P₁·K_d·K_b·K_c·K_N·K_SH)   [mm², kg/h, kPa(a)]", Kind.AREA, aMm2 * 1e-6, "API 520-I", primary = true)
            orificeSelection(a)
        }
    }

    val liquid = calculator("PRV-004", "Relief Valve Sizing — Liquid (API 520)", Module.RELIEF) {
        description = "Required effective discharge area for liquid relief with capacity-certified valves, including viscosity correction."
        keywords("liquid", "PSV", "thermal relief", "viscosity", "Kv", "Kw")
        ref("API 520 Part I", "5.8", "Eq. (17), SI")
        positive("Q", "Required flow Q", Kind.VOLUME_FLOW, 300.0 / 60_000)
        positive("Pset", "Set pressure", Kind.PRESSURE_GAUGE, 10e5)
        number("op", "Allowable overpressure", Kind.RATIO, 0.10, 0.0, 1.0)
        number("Pb", "Total back pressure", Kind.PRESSURE_GAUGE, 0.0, 0.0)
        positive("G", "Specific gravity G (water = 1)", Kind.DIMENSIONLESS, 1.0)
        positive("mu", "Viscosity at flowing temperature", Kind.DYN_VISCOSITY, 1e-3)
        number("Kd", "K_d", Kind.DIMENSIONLESS, 0.65, 0.0, 1.0, true)
        number("Kw", "Back-pressure correction K_w", Kind.DIMENSIONLESS, 1.0, 0.0, 1.0, true)
        number("Kc", "K_c", Kind.DIMENSIONLESS, 1.0, 0.0, 1.0, true)
        compute {
            val qLmin = n("Q") * 60_000
            val p1 = step("Upstream relieving pressure P₁", "P₁ = P_set·(1 + overpressure)", Kind.PRESSURE_GAUGE, n("Pset") * (1 + n("op")))
            val dp = (p1 - n("Pb")) / 1000
            require(dp > 0, "Back pressure must be below relieving pressure")
            val base = 11.78 * qLmin / (n("Kd") * n("Kw") * n("Kc")) * sqrt(n("G") / dp)
            val a0 = step("Area with K_v = 1", "A = 11.78·Q/(K_d·K_w·K_c·K_v)·√(G/(P₁−P₂))   [mm², L/min, kPa]", Kind.AREA, base * 1e-6)
            val re = step("Reynolds number", "Re = Q·18 800·G / (μ·√A)   [L/min, cP, mm²]", Kind.DIMENSIONLESS, qLmin * 18_800 * n("G") / (n("mu") * 1000 * sqrt(base)))
            val kv = step("Viscosity correction K_v", "K_v = (0.9935 + 2.878/Re^0.5 + 342.75/Re^1.5)^−1  (≤ 1)", Kind.DIMENSIONLESS,
                minOf(1.0, 1 / (0.9935 + 2.878 / re.pow(0.5) + 342.75 / re.pow(1.5))))
            val a = step("Required effective area A", "A = A(K_v=1) / K_v", Kind.AREA, a0 / kv, "API 520-I", primary = true)
            orificeSelection(a)
        }
    }

    val fire = calculator("PRV-007", "Fire Case Heat Input & Relief Load (API 521)", Module.RELIEF) {
        description = "Heat absorbed by a liquid-filled vessel exposed to an open pool fire, and the resulting vapour relief load."
        keywords("fire", "wetted area", "pool fire", "environmental factor", "API 521")
        ref("API 521", "4.4.13.2", "Eqs. for heat absorption, SI")
        positive("A", "Wetted area A_ws", Kind.AREA, 50.0, si = "m²", us = "ft²", help = "Wetted surface within 7.6 m (25 ft) above grade")
        number("F", "Environmental factor F", Kind.DIMENSIONLESS, 1.0, 0.0, 1.0, true, help = "1.0 bare vessel; lower values for approved insulation, water spray, depressuring")
        choice("drain", "Drainage & fire-fighting", listOf("yes" to "Adequate drainage and fire-fighting (C₁ = 43 200)", "no" to "Inadequate (C₁ = 70 900)"))
        positive("lambda", "Latent heat of vaporization", Kind.SPECIFIC_ENERGY, 300e3)
        compute {
            val c1 = if (c("drain") == "yes") 43_200.0 else 70_900.0
            val q = step("Heat absorption Q", "Q = C₁·F·A_ws^0.82   [W, m²]", Kind.POWER, c1 * n("F") * n("A").pow(0.82), "API 521", output = true)
            step("Relief load W", "W = Q / λ", Kind.MASS_FLOW, q / n("lambda"), primary = true)
            info("Use 21 % overpressure for the fire case when sizing the valve with PRV-001.")
        }
    }

    val all = listOf(gas, steam, liquid, fire)
}
