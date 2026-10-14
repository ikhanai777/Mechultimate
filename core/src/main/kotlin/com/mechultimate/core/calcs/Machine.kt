package com.mechultimate.core.calcs

import com.mechultimate.core.engine.Module
import com.mechultimate.core.engine.Status
import com.mechultimate.core.engine.calculator
import com.mechultimate.core.engine.visibleIf
import com.mechultimate.core.units.G0
import com.mechultimate.core.units.Kind
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.sqrt

object Machine {
    val shaft = calculator("MCH-001", "Shaft Diameter — Fatigue (DE-Goodman)", Module.MACHINE) {
        description = "Minimum shaft diameter for combined fluctuating bending and torsion, with a first-cycle yield check."
        keywords("shaft", "fatigue", "Goodman", "endurance limit", "bending", "torsion", "ASME B106")
        ref("Shigley's Mechanical Engineering Design", "§7-4", "DE-Goodman criterion")
        positive("Ma", "Alternating bending moment M_a", Kind.TORQUE, 140.0)
        number("Mm", "Mean bending moment M_m", Kind.TORQUE, 0.0, 0.0)
        number("Ta", "Alternating torque T_a", Kind.TORQUE, 0.0, 0.0)
        number("Tm", "Mean torque T_m", Kind.TORQUE, 120.0, 0.0)
        number("Kf", "Fatigue stress-concentration factor K_f (bending)", Kind.DIMENSIONLESS, 1.7, 1.0)
        number("Kfs", "Fatigue stress-concentration factor K_fs (torsion)", Kind.DIMENSIONLESS, 1.5, 1.0)
        positive("Se", "Corrected endurance limit S_e", Kind.STRESS, 200e6, help = "S_e = k_a·k_b·k_c·k_d·k_e·S_e′; S_e′ ≈ 0.5·S_ut for steel (≤ 700 MPa)")
        positive("Sut", "Ultimate tensile strength S_ut", Kind.STRESS, 690e6)
        positive("Sy", "Yield strength S_y", Kind.STRESS, 580e6)
        number("nf", "Design factor n", Kind.DIMENSIONLESS, 1.5, 1.0)
        compute {
            val a = sqrt(4 * (n("Kf") * n("Ma")).pow(2) + 3 * (n("Kfs") * n("Ta")).pow(2))
            val b = sqrt(4 * (n("Kf") * n("Mm")).pow(2) + 3 * (n("Kfs") * n("Tm")).pow(2))
            val d = step("Minimum diameter d", "d = {16n/π·[√(4(K_f·M_a)² + 3(K_fs·T_a)²)/S_e + √(4(K_f·M_m)² + 3(K_fs·T_m)²)/S_ut]}^(1/3)",
                Kind.LENGTH, (16 * n("nf") / PI * (a / n("Se") + b / n("Sut"))).pow(1.0 / 3), "Shigley Eq. 7-8", primary = true)
            val d3 = PI * d.pow(3)
            val sMax = step("Max von Mises stress σ′_max", "σ′ = √[(32K_f(M_m+M_a)/πd³)² + 3(16K_fs(T_m+T_a)/πd³)²]", Kind.STRESS,
                sqrt((32 * n("Kf") * (n("Mm") + n("Ma")) / d3).pow(2) + 3 * (16 * n("Kfs") * (n("Tm") + n("Ta")) / d3).pow(2)))
            val ny = step("Yield safety factor n_y", "n_y = S_y / σ′_max", Kind.DIMENSIONLESS, n("Sy") / sMax, output = true)
            check(ny >= 1.0, "No first-cycle yielding", "First-cycle yielding: increase the diameter")
            info("Round up to the next standard size and recheck K_f with the final geometry.")
        }
    }

    val key = calculator("MCH-003", "Parallel Key — Shear & Crushing", Module.MACHINE) {
        description = "Shear and bearing stresses in a square or rectangular parallel key, and the required length."
        keywords("key", "keyway", "parallel key", "DIN 6885", "B17.1")
        ref("ASME B17.1 / DIN 6885", note = "Key dimensions")
        ref("Shigley's", "§7-7")
        positive("T", "Transmitted torque T", Kind.TORQUE, 500.0)
        positive("d", "Shaft diameter d", Kind.LENGTH, 0.05)
        positive("w", "Key width w", Kind.LENGTH, 0.014)
        positive("h", "Key height h", Kind.LENGTH, 0.009)
        positive("L", "Key length L", Kind.LENGTH, 0.05)
        positive("Sy", "Key yield strength S_y", Kind.STRESS, 350e6)
        number("n", "Required safety factor", Kind.DIMENSIONLESS, 2.0, 1.0)
        compute {
            val f = step("Force at shaft surface F", "F = 2T / d", Kind.FORCE, 2 * n("T") / n("d"))
            val tau = step("Shear stress τ", "τ = F / (w·L)", Kind.STRESS, f / (n("w") * n("L")), output = true)
            val sig = step("Bearing (crushing) stress σ", "σ = F / (h/2 · L)", Kind.STRESS, f / (n("h") / 2 * n("L")), output = true)
            val ns = step("Safety factor — shear", "n_s = 0.577·S_y / τ", Kind.DIMENSIONLESS, 0.577 * n("Sy") / tau, output = true)
            val nc = step("Safety factor — crushing", "n_c = S_y / σ", Kind.DIMENSIONLESS, n("Sy") / sig, output = true)
            step("Required key length", "L = max(n·F/(0.577·S_y·w), n·F/(S_y·h/2))", Kind.LENGTH,
                max(n("n") * f / (0.577 * n("Sy") * n("w")), n("n") * f / (n("Sy") * n("h") / 2)), primary = true)
            check(minOf(ns, nc) >= n("n"), "Key meets the required safety factor", "Key length is insufficient")
        }
    }

    val bearing = calculator("MCH-005", "Rolling Bearing Life — ISO 281", Module.MACHINE) {
        description = "Basic and modified rating life of a ball or roller bearing."
        keywords("bearing", "L10", "rating life", "dynamic load rating", "ISO 281", "ball bearing", "roller bearing")
        ref("ISO 281:2007")
        choice("type", "Bearing type", listOf("ball" to "Ball bearing (p = 3)", "roller" to "Roller bearing (p = 10/3)"))
        positive("C", "Basic dynamic load rating C", Kind.FORCE, 30_700.0)
        positive("Fr", "Radial load F_r", Kind.FORCE, 3000.0)
        number("Fa", "Axial load F_a", Kind.FORCE, 0.0, 0.0)
        number("X", "Radial factor X", Kind.DIMENSIONLESS, 1.0, 0.0)
        number("Y", "Axial factor Y", Kind.DIMENSIONLESS, 0.0, 0.0, help = "From the bearing catalogue for F_a/F_r > e")
        positive("n", "Speed", Kind.ROT_SPEED, 1500 * 2 * PI / 60)
        choice("rel", "Reliability", listOf("1.0" to "90 % (a₁ = 1.00)", "0.64" to "95 % (a₁ = 0.64)", "0.55" to "96 % (a₁ = 0.55)", "0.47" to "97 % (a₁ = 0.47)", "0.37" to "98 % (a₁ = 0.37)", "0.25" to "99 % (a₁ = 0.25)"))
        number("aiso", "Life modification factor a_ISO", Kind.DIMENSIONLESS, 1.0, 0.0, 50.0, true, help = "From ISO 281 for lubrication and contamination; 1.0 to ignore")
        compute {
            val p = step("Equivalent dynamic load P", "P = X·F_r + Y·F_a", Kind.FORCE, n("X") * n("Fr") + n("Y") * n("Fa"))
            require(p > 0, "Equivalent load must be positive")
            val exp = if (c("type") == "ball") 3.0 else 10.0 / 3.0
            val l10 = step("Basic rating life L₁₀", "L₁₀ = (C/P)^p  [10⁶ rev]", Kind.DIMENSIONLESS, (n("C") / p).pow(exp), output = true)
            val rpm = n("n") * 60 / (2 * PI)
            val l10h = step("L₁₀h", "L₁₀h = 10⁶·L₁₀ / (60·n)", Kind.TIME, l10 * 1e6 / (60 * rpm) * 3600, output = true, si = "h", us = "h")
            step("Modified rating life L_nm (hours)", "L_nm = a₁·a_ISO·L₁₀h", Kind.TIME, c("rel").toDouble() * n("aiso") * l10h, primary = true, si = "h", us = "h")
        }
    }

    private val metricBolts = listOf(6 to 1.0, 8 to 1.25, 10 to 1.5, 12 to 1.75, 14 to 2.0, 16 to 2.0, 20 to 2.5, 24 to 3.0, 30 to 3.5, 36 to 4.0)

    val bolt = calculator("MCH-013", "Bolted Joint — Preload, Torque & Safety", Module.MACHINE) {
        description = "Preload, tightening torque, load sharing and safety factors of a tension-loaded metric bolt."
        keywords("bolt", "preload", "torque", "joint separation", "ISO 898", "property class", "VDI 2230")
        ref("ISO 898-1", note = "Proof strength by property class")
        ref("Shigley's", "§8-7 to 8-9")
        choice("size", "Thread (ISO coarse)", metricBolts.map { "${it.first}:${it.second}" to "M${it.first} × ${it.second}" }, "12:1.75")
        choice("cls", "Property class", listOf("8.8" to "8.8", "10.9" to "10.9", "12.9" to "12.9"), "8.8")
        choice("use", "Preload level", listOf("0.75" to "Reusable joint (0.75 × proof load)", "0.90" to "Permanent joint (0.90 × proof load)"))
        number("K", "Torque (nut) factor K", Kind.DIMENSIONLESS, 0.2, 0.05, 0.4)
        number("P", "External tensile load per bolt", Kind.FORCE, 10_000.0, 0.0)
        number("Cj", "Joint stiffness constant C = k_b/(k_b + k_m)", Kind.DIMENSIONLESS, 0.25, 0.0, 1.0)
        compute {
            val (dStr, pStr) = c("size").split(":")
            val d = dStr.toDouble() / 1000; val pitch = pStr.toDouble() / 1000
            val sp = step("Proof strength S_p", "ISO 898-1", Kind.STRESS, when (c("cls")) { "8.8" -> if (d <= 0.016) 580e6 else 600e6; "10.9" -> 830e6; else -> 970e6 })
            val at = step("Tensile stress area A_t", "A_t = π/4·(d − 0.9382·P)²", Kind.AREA, PI / 4 * (d - 0.9382 * pitch).pow(2))
            val fi = step("Preload F_i", "F_i = k·A_t·S_p", Kind.FORCE, c("use").toDouble() * at * sp, output = true)
            step("Tightening torque T", "T = K·d·F_i", Kind.TORQUE, n("K") * d * fi, primary = true)
            val pl = n("P"); val cj = n("Cj")
            if (pl > 0) {
                val fb = step("Bolt load F_b", "F_b = F_i + C·P", Kind.FORCE, fi + cj * pl, output = true)
                val np = step("Yield (proof) factor n_p", "n_p = S_p·A_t / F_b", Kind.DIMENSIONLESS, sp * at / fb, output = true)
                val n0 = step("Separation factor n₀", "n₀ = F_i / [P·(1 − C)]", Kind.DIMENSIONLESS, fi / (pl * (1 - cj)), output = true)
                check(np >= 1.0, "Bolt stays below proof load", "Bolt load exceeds proof load")
                check(n0 >= 1.2, "Joint will not separate (n₀ ≥ 1.2)", "Joint separation risk (n₀ < 1.2)", Status.WARN)
            }
        }
    }

    val spring = calculator("MCH-016", "Helical Compression Spring", Module.MACHINE) {
        description = "Rate, stress, deflection and solid height of a round-wire helical compression spring."
        keywords("spring", "compression spring", "Wahl", "spring rate", "spring index")
        ref("EN 13906-1 / SMI Handbook", note = "Wahl correction factor")
        positive("d", "Wire diameter d", Kind.LENGTH, 0.003)
        positive("D", "Mean coil diameter D", Kind.LENGTH, 0.024)
        positive("Na", "Active coils N_a", Kind.DIMENSIONLESS, 8.0)
        choice("ends", "End type", listOf("sg" to "Squared and ground (N_t = N_a + 2)", "s" to "Squared (N_t = N_a + 2)", "p" to "Plain (N_t = N_a)"))
        positive("G", "Shear modulus G", Kind.MODULUS, 79.3e9)
        positive("F", "Working load F", Kind.FORCE, 100.0)
        positive("tauAllow", "Allowable shear stress", Kind.STRESS, 700e6, help = "Typically 0.40–0.50 × S_ut for static service")
        compute {
            val cIdx = step("Spring index C", "C = D / d", Kind.DIMENSIONLESS, n("D") / n("d"), output = true)
            val kw = step("Wahl factor K_W", "K_W = (4C − 1)/(4C − 4) + 0.615/C", Kind.DIMENSIONLESS, (4 * cIdx - 1) / (4 * cIdx - 4) + 0.615 / cIdx)
            val k = step("Spring rate k", "k = G·d⁴ / (8·D³·N_a)", Kind.FORCE_PER_LENGTH, n("G") * n("d").pow(4) / (8 * n("D").pow(3) * n("Na")), primary = true)
            val tau = step("Corrected shear stress τ", "τ = K_W·8·F·D / (π·d³)", Kind.STRESS, kw * 8 * n("F") * n("D") / (PI * n("d").pow(3)), output = true)
            step("Deflection at F", "y = F / k", Kind.LENGTH, n("F") / k, output = true)
            val nt = if (c("ends") == "p") n("Na") else n("Na") + 2
            val ground = c("ends") == "sg"
            step("Solid height", if (ground) "L_s = d·N_t" else "L_s = d·(N_t + 1)", Kind.LENGTH, n("d") * (if (ground) nt else nt + 1), output = true)
            check(cIdx in 4.0..12.0, "Spring index within 4–12", "Spring index outside 4–12 (hard to manufacture or prone to tangling)", Status.WARN)
            check(tau <= n("tauAllow"), "Shear stress within allowable", "Shear stress exceeds allowable")
        }
    }

    val pressFit = calculator("MCH-018", "Press / Shrink Fit (Lamé)", Module.MACHINE) {
        description = "Interface pressure, stresses, torque capacity and assembly temperature of an interference fit."
        keywords("press fit", "shrink fit", "interference", "Lamé", "hub", "DIN 7190")
        ref("Shigley's", "§3-16", "Press and shrink fits")
        positive("d", "Nominal interface diameter", Kind.LENGTH, 0.05)
        positive("delta", "Diametral interference δ", Kind.LENGTH, 40e-6)
        positive("do", "Hub outside diameter", Kind.LENGTH, 0.10)
        number("di", "Shaft bore diameter (0 = solid)", Kind.LENGTH, 0.0, 0.0)
        positive("L", "Engagement length", Kind.LENGTH, 0.05)
        positive("Eo", "Hub modulus", Kind.MODULUS, 207e9)
        number("nuo", "Hub Poisson's ratio", Kind.DIMENSIONLESS, 0.29, 0.0, 0.5)
        positive("Ei", "Shaft modulus", Kind.MODULUS, 207e9)
        number("nui", "Shaft Poisson's ratio", Kind.DIMENSIONLESS, 0.29, 0.0, 0.5)
        positive("mu", "Friction coefficient μ", Kind.DIMENSIONLESS, 0.12)
        positive("alpha", "Hub expansion coefficient", Kind.EXPANSION, 12e-6)
        compute {
            val d = n("d"); val dout = n("do"); val di = n("di")
            require(dout > d && di < d, "Require bore < interface < hub OD")
            val p = step("Interface pressure p", "p = δ / {d·[(1/E_o)((d_o²+d²)/(d_o²−d²) + ν_o) + (1/E_i)((d²+d_i²)/(d²−d_i²) − ν_i)]}", Kind.STRESS,
                n("delta") / (d * ((1 / n("Eo")) * ((dout * dout + d * d) / (dout * dout - d * d) + n("nuo")) + (1 / n("Ei")) * ((d * d + di * di) / (d * d - di * di) - n("nui")))), primary = true)
            step("Hub hoop stress at bore", "σ_t = p·(d_o² + d²)/(d_o² − d²)", Kind.STRESS, p * (dout * dout + d * d) / (dout * dout - d * d), output = true)
            step("Shaft hoop stress at surface", "σ_t = −p·(d² + d_i²)/(d² − d_i²)", Kind.STRESS, -p * (d * d + di * di) / (d * d - di * di), output = true)
            step("Torque capacity", "T = π·μ·p·d²·L / 2", Kind.TORQUE, PI * n("mu") * p * d * d * n("L") / 2, output = true)
            step("Axial holding force", "F = π·μ·p·d·L", Kind.FORCE, PI * n("mu") * p * d * n("L"), output = true)
            step("Hub heating for zero interference", "ΔT = δ / (α·d)", Kind.TEMP_DIFF, n("delta") / (n("alpha") * d), output = true)
        }
    }

    val flywheel = calculator("MCH-024", "Flywheel Sizing", Module.MACHINE) {
        description = "Required mass moment of inertia to limit speed fluctuation, and an equivalent solid-disc size."
        keywords("flywheel", "speed fluctuation", "inertia", "energy fluctuation")
        ref("Shigley's", "§16-12")
        positive("dE", "Energy fluctuation per cycle ΔE", Kind.ENERGY, 2000.0)
        positive("n", "Mean speed", Kind.ROT_SPEED, 600 * 2 * PI / 60)
        positive("Cs", "Coefficient of speed fluctuation C_s", Kind.DIMENSIONLESS, 0.02, help = "(ω_max − ω_min)/ω; 0.002 generators … 0.2 crushers")
        positive("rho", "Disc material density", Kind.DENSITY, 7200.0)
        positive("t", "Disc thickness", Kind.LENGTH, 0.05)
        compute {
            val i = step("Required inertia I", "I = ΔE / (C_s·ω²)", Kind.MASS_INERTIA, n("dE") / (n("Cs") * n("n").pow(2)), primary = true)
            val r = step("Equivalent solid-disc radius", "r = (2I / (ρ·π·t))^¼", Kind.LENGTH, (2 * i / (n("rho") * PI * n("t"))).pow(0.25), output = true)
            step("Disc mass", "m = ρ·π·r²·t", Kind.MASS, n("rho") * PI * r * r * n("t"), output = true)
            step("Rim speed", "v = ω·r", Kind.VELOCITY, n("n") * r, output = true)
        }
    }

    val filletWeld = calculator("MCH-027", "Fillet Weld — Direct Load", Module.MACHINE) {
        description = "Throat shear stress of fillet welds under a direct load, against the AWS D1.1 allowable stress."
        keywords("fillet weld", "weld size", "throat", "AWS D1.1", "E70")
        ref("AWS D1.1", "Table 2.3", "Allowable stress 0.30·F_EXX on throat")
        positive("F", "Load on weld group", Kind.FORCE, 50_000.0)
        positive("h", "Fillet leg size h", Kind.LENGTH, 0.006)
        positive("L", "Total effective weld length", Kind.LENGTH, 0.2)
        choice("fexx", "Filler metal", listOf("413e6" to "E60XX (F_EXX = 413 MPa)", "482e6" to "E70XX (F_EXX = 482 MPa)", "551e6" to "E80XX (F_EXX = 551 MPa)"), "482e6")
        compute {
            val tau = step("Throat shear stress τ", "τ = F / (0.707·h·L)", Kind.STRESS, n("F") / (0.707 * n("h") * n("L")), output = true)
            val allow = step("Allowable stress", "τ_allow = 0.30·F_EXX", Kind.STRESS, 0.30 * c("fexx").toDouble())
            step("Utilization", "τ / τ_allow", Kind.RATIO, tau / allow, primary = true)
            step("Required leg size", "h = F / (0.707·L·τ_allow)", Kind.LENGTH, n("F") / (0.707 * n("L") * allow), output = true)
            check(tau <= allow, "Weld stress within allowable", "Weld overstressed")
        }
    }

    val all = listOf(shaft, key, bearing, bolt, spring, pressFit, flywheel, filletWeld)
}

object Structures {
    private val cases = listOf(
        "ss_udl" to "Simply supported — uniform load",
        "ss_pc" to "Simply supported — central point load",
        "ss_pa" to "Simply supported — point load at a",
        "cant_p" to "Cantilever — end point load",
        "cant_udl" to "Cantilever — uniform load",
        "ff_udl" to "Fixed–fixed — uniform load",
        "ff_pc" to "Fixed–fixed — central point load",
    )

    val beam = calculator("STR-003", "Beam Formulas — Reactions, Moment, Deflection", Module.STRUCTURES) {
        description = "Maximum reaction, bending moment, bending stress and deflection for standard beam cases."
        keywords("beam", "deflection", "bending moment", "cantilever", "simply supported", "Roark")
        ref("Roark's Formulas for Stress and Strain", "Table 8.1")
        choice("case", "Load case", cases)
        positive("L", "Span L", Kind.LENGTH, 4.0, si = "m", us = "ft")
        positive("w", "Uniform load w", Kind.FORCE_PER_LENGTH, 5000.0, si = "kN/m", us = "lbf/ft", visibleIf = visibleIf("case", "ss_udl", "cant_udl", "ff_udl"))
        positive("P", "Point load P", Kind.FORCE, 10_000.0, visibleIf = visibleIf("case", "ss_pc", "ss_pa", "cant_p", "ff_pc"))
        positive("a", "Load position a from left support", Kind.LENGTH, 1.0, si = "m", us = "ft", visibleIf = visibleIf("case", "ss_pa"))
        positive("E", "Elastic modulus E", Kind.MODULUS, 200e9)
        positive("I", "Second moment of area I", Kind.AREA_MOMENT, 8.356e-6)
        positive("Z", "Elastic section modulus Z", Kind.SECTION_MODULUS, 5.57e-4)
        positive("Sallow", "Allowable bending stress", Kind.STRESS, 165e6)
        positive("lim", "Deflection limit L / …", Kind.DIMENSIONLESS, 360.0)
        compute {
            val l = n("L"); val ei = n("E") * n("I")
            val (r, m, d, formula) = when (c("case")) {
                "ss_udl" -> n("w").let { w -> Quad(w * l / 2, w * l * l / 8, 5 * w * l.pow(4) / (384 * ei), "R = wL/2,  M = wL²/8,  δ = 5wL⁴/384EI") }
                "ss_pc" -> n("P").let { p -> Quad(p / 2, p * l / 4, p * l.pow(3) / (48 * ei), "R = P/2,  M = PL/4,  δ = PL³/48EI") }
                "ss_pa" -> {
                    val p = n("P"); val a = n("a")
                    require(a < l, "a must be less than L")
                    val b = l - a; val bs = minOf(a, b)
                    Quad(p * maxOf(a, b) / l, p * a * b / l, p * bs * (l * l - bs * bs).pow(1.5) / (9 * sqrt(3.0) * ei * l), "R = P·b/L,  M = P·a·b/L,  δ = P·b(L² − b²)^1.5/(9√3·EI·L)")
                }
                "cant_p" -> n("P").let { p -> Quad(p, p * l, p * l.pow(3) / (3 * ei), "R = P,  M = PL,  δ = PL³/3EI") }
                "cant_udl" -> n("w").let { w -> Quad(w * l, w * l * l / 2, w * l.pow(4) / (8 * ei), "R = wL,  M = wL²/2,  δ = wL⁴/8EI") }
                "ff_udl" -> n("w").let { w -> Quad(w * l / 2, w * l * l / 12, w * l.pow(4) / (384 * ei), "R = wL/2,  M = wL²/12 (ends),  δ = wL⁴/384EI") }
                else -> n("P").let { p -> Quad(p / 2, p * l / 8, p * l.pow(3) / (192 * ei), "R = P/2,  M = PL/8,  δ = PL³/192EI") }
            }
            text("Case formulas", formula, formula = formula)
            step("Maximum reaction", "see case", Kind.FORCE, r, output = true)
            step("Maximum moment", "see case", Kind.TORQUE, m, output = true, si = "kN·m", us = "lbf·ft")
            val sigma = step("Maximum bending stress", "σ = M / Z", Kind.STRESS, m / n("Z"), primary = true)
            val defl = step("Maximum deflection", "see case", Kind.LENGTH, d, primary = true)
            step("Span / deflection", "L / δ", Kind.DIMENSIONLESS, l / d, output = true)
            check(sigma <= n("Sallow"), "Bending stress within allowable", "Bending stress exceeds allowable")
            check(defl <= l / n("lim"), "Deflection within L/${n("lim").toInt()}", "Deflection exceeds L/${n("lim").toInt()}")
        }
    }

    private data class Quad(val r: Double, val m: Double, val d: Double, val f: String)

    val column = calculator("STR-005", "Column Buckling — Euler / Johnson", Module.STRUCTURES) {
        description = "Critical buckling load of a straight column, selecting Euler or Johnson parabola by slenderness."
        keywords("column", "buckling", "Euler", "Johnson", "slenderness", "strut")
        ref("Shigley's", "§4-11 to 4-13")
        choice("K", "End conditions (theoretical K)", listOf("1.0" to "Pinned–pinned (K = 1.0)", "2.0" to "Fixed–free (K = 2.0)", "0.7" to "Fixed–pinned (K = 0.7)", "0.5" to "Fixed–fixed (K = 0.5)"))
        positive("L", "Unbraced length L", Kind.LENGTH, 3.0, si = "m", us = "ft")
        positive("A", "Cross-section area A", Kind.AREA, 2.85e-3)
        positive("I", "Least second moment of area I", Kind.AREA_MOMENT, 4.5e-6)
        positive("E", "Elastic modulus E", Kind.MODULUS, 200e9)
        positive("Sy", "Yield strength S_y", Kind.STRESS, 250e6)
        number("P", "Applied axial load", Kind.FORCE, 100_000.0, 0.0)
        compute {
            val r = step("Radius of gyration r", "r = √(I/A)", Kind.LENGTH, sqrt(n("I") / n("A")))
            val sl = step("Slenderness ratio KL/r", "KL / r", Kind.DIMENSIONLESS, c("K").toDouble() * n("L") / r, output = true)
            val slT = step("Transition slenderness", "(KL/r)_t = √(2π²E / S_y)", Kind.DIMENSIONLESS, sqrt(2 * PI * PI * n("E") / n("Sy")), output = true)
            val pcr = if (sl >= slT) {
                text("Method", "Euler (long column)")
                step("Critical load P_cr", "P_cr = π²·E·A / (KL/r)²", Kind.FORCE, PI * PI * n("E") * n("A") / (sl * sl), primary = true)
            } else {
                text("Method", "Johnson parabola (intermediate column)")
                step("Critical load P_cr", "P_cr = A·[S_y − (S_y·KL/(2π·r))²/E]", Kind.FORCE, n("A") * (n("Sy") - (n("Sy") * sl / (2 * PI)).pow(2) / n("E")), primary = true)
            }
            if (n("P") > 0) {
                val fs = step("Safety factor", "n = P_cr / P", Kind.DIMENSIONLESS, pcr / n("P"), output = true)
                check(fs >= 2.0, "Safety factor ≥ 2", "Safety factor below 2", Status.WARN)
            }
        }
    }

    val mohr = calculator("STR-006", "Principal Stresses & Mohr's Circle (2D)", Module.STRUCTURES) {
        description = "Principal stresses, maximum shear, principal angle and von Mises / Tresca equivalent stresses for plane stress."
        keywords("Mohr", "principal stress", "von Mises", "Tresca", "plane stress", "combined stress")
        ref("Shigley's", "§3-6, §5-4/5-5")
        number("sx", "Normal stress σx", Kind.STRESS, 80e6)
        number("sy", "Normal stress σy", Kind.STRESS, -40e6)
        number("txy", "Shear stress τxy", Kind.STRESS, 50e6)
        positive("Sy", "Yield strength S_y", Kind.STRESS, 250e6)
        compute {
            val sx = n("sx"); val sy = n("sy"); val txy = n("txy")
            val c0 = step("Circle centre", "C = (σx + σy)/2", Kind.STRESS, (sx + sy) / 2)
            val rad = step("Circle radius", "R = √[((σx − σy)/2)² + τxy²]", Kind.STRESS, sqrt(((sx - sy) / 2).pow(2) + txy * txy))
            val s1 = step("σ₁", "σ₁ = C + R", Kind.STRESS, c0 + rad, output = true)
            val s2 = step("σ₂", "σ₂ = C − R", Kind.STRESS, c0 - rad, output = true)
            step("Max in-plane shear", "τ_max = R", Kind.STRESS, rad, output = true)
            step("Principal angle θp", "θp = ½·atan2(2τxy, σx − σy)", Kind.ANGLE, 0.5 * atan2(2 * txy, sx - sy), output = true)
            val vm = step("von Mises stress", "σ′ = √(σ₁² − σ₁σ₂ + σ₂²)", Kind.STRESS, sqrt(s1 * s1 - s1 * s2 + s2 * s2), primary = true)
            val tr = step("Tresca equivalent stress", "σ_T = max(|σ₁ − σ₂|, |σ₁|, |σ₂|)", Kind.STRESS, maxOf(abs(s1 - s2), abs(s1), abs(s2)), output = true)
            step("Safety factor (von Mises)", "n = S_y / σ′", Kind.DIMENSIONLESS, n("Sy") / vm, output = true)
            step("Safety factor (Tresca)", "n = S_y / σ_T", Kind.DIMENSIONLESS, n("Sy") / tr, output = true)
            check(vm < n("Sy"), "Below yield (von Mises)", "Yield predicted (von Mises)")
        }
    }

    val torsion = calculator("STR-009", "Torsion of Circular Shafts", Module.STRUCTURES) {
        description = "Shear stress and angle of twist of a solid or hollow circular shaft, and the power it transmits."
        keywords("torsion", "twist", "shear stress", "polar moment", "hollow shaft")
        ref("Shigley's", "§3-12")
        positive("T", "Torque T", Kind.TORQUE, 1000.0)
        positive("do", "Outside diameter", Kind.LENGTH, 0.05)
        number("di", "Inside diameter (0 = solid)", Kind.LENGTH, 0.0, 0.0)
        positive("L", "Length", Kind.LENGTH, 1.0, si = "m", us = "ft")
        positive("G", "Shear modulus G", Kind.MODULUS, 79.3e9)
        number("n", "Speed (for power)", Kind.ROT_SPEED, 0.0, 0.0)
        compute {
            require(n("di") < n("do"), "Inside diameter must be less than outside diameter")
            val j = step("Polar moment J", "J = π(d_o⁴ − d_i⁴)/32", Kind.AREA_MOMENT, PI * (n("do").pow(4) - n("di").pow(4)) / 32)
            step("Max shear stress τ", "τ = T·(d_o/2) / J", Kind.STRESS, n("T") * n("do") / 2 / j, primary = true)
            step("Angle of twist θ", "θ = T·L / (G·J)", Kind.ANGLE, n("T") * n("L") / (n("G") * j), output = true)
            if (n("n") > 0) step("Transmitted power", "P = T·ω", Kind.POWER, n("T") * n("n"), output = true)
        }
    }

    val all = listOf(beam, column, mohr, torsion)
}
