# 3. Calculator Catalog

Each calculator has a stable ID (`MODULE-NNN`), which is used in deep links, reports and test vectors. Equations are shown in the app's "Show work" view, with full symbol definitions and units. This catalog lists the governing relation and the reference for each.

**Priority:** P0 = MVP, P1 = v1.0, P2 = v1.x, P3 = v2+.

**Symbols (common):** P = design pressure, D = outside diameter, d = inside diameter, R = inside radius, S = allowable stress, E = joint/quality factor, W = weld strength reduction factor, Y = temperature coefficient, c/CA = corrosion + mechanical allowance, t = thickness.

---

## Module PIP — Piping (ASME B31 and related)

| ID | Calculator | Governing relation / method | Reference | Pri |
|----|-----------|-----------------------------|-----------|-----|
| PIP-001 | Pipe wall thickness — process piping | t = P·D / [2(S·E·W + P·Y)]; t_m = t + c; check t < D/6; select schedule with mill tolerance (12.5%) | B31.3 304.1.2 Eq. 3a | P0 |
| PIP-002 | Pipe wall thickness — power piping | t_m = P·D_o / [2(S·E·W + P·y)] + A | B31.1 104.1.2 | P0 |
| PIP-003 | Liquid pipeline wall thickness | t = P_i·D / (2·S), S = 0.72·E·S_y | B31.4 403.2.1 | P0 |
| PIP-004 | Gas pipeline design pressure | P = (2·S·t / D)·F·E·T, location class 1–4 factors | B31.8 841.1.1 | P0 |
| PIP-005 | Refrigeration / building services / hydrogen piping thickness | Code-specific variants (B31.5 504.1, B31.9 904.1, B31.12 with M_f) | B31.5/9/12 | P2 |
| PIP-006 | MAWP of existing pipe (inverse of 001–004) | Solve for P with measured t | B31.x | P0 |
| PIP-007 | Pipe bend / elbow thickness | t = P·D / [2(S·E·W/I + P·Y)], I_intrados = [4(R₁/D) − 1]/[4(R₁/D) − 2] | B31.3 304.2.1 | P1 |
| PIP-008 | Mitered bend | Allowable pressure Eq. 4a/4b/4c, θ ≤ 22.5° vs > 22.5° | B31.3 304.2.3 | P1 |
| PIP-009 | Branch connection reinforcement | Area replacement: A₁ ≤ A₂ + A₃ + A₄, d₂ half-width, L₄ = min(2.5(T_h − c), 2.5(T_b − c) + T_r) | B31.3 304.3.3 / B31.1 104.3.1 | P0 |
| PIP-010 | Blind flange / permanent blank thickness | t_m = d_g·√(3P / (16·S·E·W)) + c | B31.3 304.5.3 Eq. 15 | P0 |
| PIP-011 | Flange P-T rating lookup & class selection | Interpolate rating table by material group and temperature; pick minimum class ≥ design P | B16.5 / B16.47 / B16.34 | P0 |
| PIP-012 | Flange equivalent pressure (external moment & force) | P_eq = 16M/(πG³) + 4F/(πG²) + P (Kellogg method), compare to rating | Kellogg / B16.5 practice | P1 |
| PIP-013 | Pipe support span | L = min(L_s, L_d); stress L_s = √(10·Z·S_h/w) (M = wL²/10), deflection L_d = (384·E·I·δ/(5·w))^¼ (conservative); also tabulated spans | B31.1 Table 121.5, MSS SP-58 | P0 |
| PIP-014 | Thermal expansion | ΔL = α·L·ΔT (or tabulated mean coefficients) | B31.3 Table C-1 | P0 |
| PIP-015 | Expansion loop sizing | Guided-cantilever: L = √(3·E·D·ΔL / S_A) | Kellogg, Design of Piping Systems | P0 |
| PIP-016 | Flexibility exemption check | D·y / (L − U)² ≤ K₁ | B31.3 319.4.1(c) | P1 |
| PIP-017 | Allowable displacement stress range | S_A = f(1.25·S_c + 0.25·S_h), with liberal option f·[1.25(S_c + S_h) − S_L]; f = 6.0·N^−0.2 ≤ 1.2 | B31.3 302.3.5 | P1 |
| PIP-018 | Sustained stress | S_L = √[(|I_a·F_a/A_p| + S_b)² + (2·S_t)²] (2016+ eqs.) | B31.3 320.2 | P1 |
| PIP-019 | SIF & flexibility factor | h = T·R₁ / r₂², i = 0.9/h^(2/3) (elbow), plus tees, reducers | B31J | P1 |
| PIP-020 | Hydrostatic test pressure | P_T = 1.5·P·(S_T / S), S_T/S ≤ 6.5; check yield at test | B31.3 345.4.2 | P0 |
| PIP-021 | Pneumatic test pressure | P_T = 1.1·P; stored energy E = P·V/(γ−1)·[1 − (P_atm/P)^((γ−1)/γ)]; safe distance | B31.3 345.5, ASME PCC-2 Art. 501 | P0 |
| PIP-022 | Line sizing by velocity / ΔP per 100 m | Criteria tables by service (liquid, gas, steam, slurry, two-phase) | Company practice, API RP 14E | P0 |
| PIP-023 | Erosional velocity | V_e = C / √ρ_m | API RP 14E | P0 |
| PIP-024 | Pressure drop — single-phase incompressible | Darcy–Weisbach h_f = f·(L/D)·v²/(2g); f by Colebrook, Churchill, Swamee–Jain, Haaland; minor losses (K, L/D, 3-K Darby) | Crane TP-410, Darby | P0 |
| PIP-025 | Hazen–Williams | h_f = 10.67·L·Q^1.852 / (C^1.852·d^4.87) (SI) | NFPA 13 | P0 |
| PIP-026 | Compressible gas flow | Isothermal, adiabatic (Fanno), Weymouth, Panhandle A/B, AGA fully-turbulent | Crane, GPSA | P1 |
| PIP-027 | Two-phase pressure drop | Lockhart–Martinelli, Beggs–Brill, Friedel; flow regime map (Baker/Taitel–Dukler) | Literature | P2 |
| PIP-028 | Water hammer / surge | Joukowsky ΔP = ρ·a·Δv; wave speed a = √[(K/ρ)/(1 + K·D·C₁/(E·t))]; critical time 2L/a | Wylie & Streeter | P1 |
| PIP-029 | Steam pipe sizing | Velocity & ΔP using IAPWS-IF97 properties; condensate load at start-up/running | — | P0 |
| PIP-030 | Condensate & flash steam | Flash fraction = (h_f1 − h_f2)/h_fg2; two-phase return line sizing | — | P1 |
| PIP-031 | Gas piping sizing (fuel gas) | NFPA 54 / IFGC longest-length method, Spitzglass/Mueller | NFPA 54 | P2 |
| PIP-032 | Pipe weight & content | Steel + fluid + insulation + cladding weight per m; buoyancy (submerged) | B36.10 data | P0 |
| PIP-033 | Insulation thickness & heat loss | Cylindrical conduction + surface convection/radiation; economic thickness; personnel protection (≤ 60 °C surface) | ASTM C680, 3E Plus method | P1 |
| PIP-034 | Heat tracing | Required W/m = heat loss × safety factor; tracer count | IEEE 515 | P2 |
| PIP-035 | Thrust block / restraint load | F = P·A + ρ·Q·v at bends and dead ends | AWWA M9/M11 | P1 |
| PIP-036 | Buried pipe | Soil load (Marston), live load (Boussinesq), ring deflection (Spangler/Iowa formula), buckling | AWWA M11, ALA | P2 |
| PIP-037 | Corroded pipeline remaining strength | B31G / Modified B31G (0.85dL), RSTRENG-lite, ERF | ASME B31G | P1 |
| PIP-038 | Thermowell wake-frequency | f_s = N_s·V/b; resonance and stress checks | ASME PTC 19.3 TW-2016 | P1 |
| PIP-039 | Slug force at elbows | F = ρ·v²·A·√[2(1 − cos θ)]·DLF | Literature | P2 |
| PIP-040 | Pipe stress — 2D/3D lite | Links to Design Tool DT-03 | B31.3 | P2 |
| PIP-041 | Orifice / flow element | ISO 5167 orifice, nozzle, venturi (Reader-Harris/Gallagher C, expansibility ε) | ISO 5167-2/3/4 | P1 |
| PIP-042 | Restriction orifice | Single/multistage RO sizing, choked flow check | Crane | P1 |
| PIP-043 | Gasket & bolt load | Wm1 = H + H_p, Wm2 = π·b·G·y; bolt area check | VIII-1 App. 2 | P1 |
| PIP-044 | Bolt torque for flanges | T = K·D·F; target bolt stress (PCC-1 App. O); pass sequence | ASME PCC-1 | P0 |
| PIP-045 | Spectacle blind selection | B16.48 dimension lookup by class & NPS | B16.48 | P1 |
| PIP-046 | Valve selection helper | Type selector by service; B16.34 rating; API 600/602/608/609 | B16.34 | P2 |

## Module PRV — Pressure relief & overpressure protection

| ID | Calculator | Relation | Reference | Pri |
|----|-----------|----------|-----------|-----|
| PRV-001 | Gas/vapor, critical flow | A = W/(C·K_d·P₁·K_b·K_c)·√(T·Z/M), C = f(k) | API 520-I 5.6.3 | P0 |
| PRV-002 | Gas/vapor, subcritical flow | A = W/(735·F₂·K_d·K_c)·√(Z·T/(M·P₁·(P₁ − P₂))) (US) | API 520-I 5.6.4 | P0 |
| PRV-003 | Steam | A = W/(190.5·P₁·K_d·K_b·K_c·K_N·K_SH) (US) or by Napier with IF97 | API 520-I 5.7 | P0 |
| PRV-004 | Liquid (certified) | A = 11.78·Q/(K_d·K_w·K_c·K_v)·√(G/(P₁ − P₂)) (SI) | API 520-I 5.8 | P0 |
| PRV-005 | Two-phase (omega method) | Leung ω-method, critical pressure ratio η_c | API 520-I Annex C | P2 |
| PRV-006 | Orifice letter selection | Next API 526 orifice ≥ required area (D…T) | API 526 | P0 |
| PRV-007 | Fire case (wetted area) | Q = 43,200·F·A_ws^0.82 (W, A in m²), environmental factor F; unwetted vessel case | API 521 4.4.13 | P0 |
| PRV-008 | Blocked outlet / thermal relief / tube rupture | 10/13 rule (low-pressure side test pressure ≥ 10/13 of HP design) | API 521 | P1 |
| PRV-009 | Inlet pressure loss & backpressure | 3% inlet rule; built-up vs superimposed backpressure; K_b/K_w curves | API 520-II | P1 |
| PRV-010 | Reaction force (open discharge) | F = (W/366)·√(k·T/((k+1)·M)) + A_o·P₂ (US units) | API 520-II 4.4 | P1 |
| PRV-011 | Rupture disc sizing | K_r method, combination capacity factor 0.9 | ASME VIII / XIII | P1 |
| PRV-012 | Flare radiation & stack height | API 521 simple method: D = √(τ·F·Q/(4·π·K)) | API 521 | P2 |
| PRV-013 | Tank venting | Normal in/out-breathing and emergency (fire) venting | API 2000 | P1 |

## Module VES — Pressure vessels (ASME VIII Div 1 unless noted)

| ID | Calculator | Relation | Reference | Pri |
|----|-----------|----------|-----------|-----|
| VES-001 | Cylindrical shell — internal pressure | Circumferential t = P·R/(S·E − 0.6·P); longitudinal t = P·R/(2·S·E + 0.4·P); limits P ≤ 0.385SE, t ≤ R/2; thick-wall App. 1-2 | UG-27, App 1-2 | P0 |
| VES-002 | Spherical shell / hemispherical head | t = P·L/(2·S·E − 0.2·P) | UG-27(d), UG-32 | P0 |
| VES-003 | 2:1 ellipsoidal head | t = P·D/(2·S·E − 0.2·P); general ellipsoidal with K factor App. 1-4 | UG-32 / App 1-4 | P0 |
| VES-004 | Torispherical (F&D) head | t = 0.885·P·L/(S·E − 0.1·P) (L = D, r = 0.06L); general with M factor | UG-32 / App 1-4 | P0 |
| VES-005 | Conical section & toriconical | t = P·D/[2·cos α·(S·E − 0.6·P)]; reinforcement at junctions App 1-5 | UG-32(g), App 1-5/1-8 | P1 |
| VES-006 | Flat heads & covers | t = d·√(C·P/(S·E)) (+ bolted: 1.9·W·h_G/(S·E·d³) term) | UG-34 | P0 |
| VES-007 | External pressure — cylinder | Factor A from L/D_o & D_o/t (geometric chart computed analytically), factor B from material chart; P_a = 4B/[3(D_o/t)] | UG-28, II-D Subpart 3 | P1 |
| VES-008 | External pressure — heads, stiffening rings | UG-33; UG-29 required moment of inertia I_s = [D_o²·L_s·(t + A_s/L_s)·A]/14 | UG-29, UG-33 | P1 |
| VES-009 | Nozzle reinforcement | Area replacement A ≤ A₁ + A₂ + A₃ + A₄₁ + A₄₂ + A₅; limits, F correction factor, strength reduction f_r | UG-37 to UG-42 | P0 |
| VES-010 | Nozzle neck thickness | t = max(t_a, t_b) per UG-45 | UG-45 | P0 |
| VES-011 | Opening without reinforcement check | UG-36(c)(3) size limits | UG-36 | P0 |
| VES-012 | Nozzle weld strength | Load paths 1-1, 2-2, 3-3 | UG-41, UW-16 | P1 |
| VES-013 | Joint efficiency selector | Table UW-12 by joint type & RT extent | UW-11/UW-12 | P0 |
| VES-014 | MAWP / MAP roll-up | Minimum of all component MAWPs, corrected for static head | UG-98 | P0 |
| VES-015 | Hydrostatic / pneumatic test | P_T = 1.3·MAWP·LSR (UG-99(b)); pneumatic 1.1·MAWP·LSR (UG-100); Div 2: 1.25·MAWP·(S_T/S) | UG-99, UG-100, Div 2 8.2 | P0 |
| VES-016 | MDMT & impact test exemption | UCS-66 curves A/B/C/D, governing thickness, UCS-66.1 coincident ratio reduction, UG-20(f) | UCS-66, UCS-66.1 | P0 |
| VES-017 | Bolted flange design | App. 2 (integral/loose): W_m1, W_m2, moments, S_H, S_R, S_T, rigidity J ≤ 1 | App. 2, App. S | P1 |
| VES-018 | Jacketed vessel | Jacket closure types and thickness | App. 9 | P2 |
| VES-019 | Non-circular vessels | Rectangular sections membrane + bending | App. 13 | P2 |
| VES-020 | Saddle supports | Zick analysis: longitudinal bending at midspan & saddle, tangential shear, circumferential stress at horn | Zick / PD 5500 App. G | P1 |
| VES-021 | Skirt & base ring | Skirt thickness under weight + wind/seismic; base-ring & anchor bolt design | Jawad & Farr, ASCE 7 | P1 |
| VES-022 | Leg / lug supports | Bijlaard local stresses (WRC 107/537 lite) | WRC 107/537 | P2 |
| VES-023 | Wind & seismic loads on vertical vessels | ASCE 7 velocity pressure, shear & moment along height; seismic base shear | ASCE 7 | P1 |
| VES-024 | Lifting lug | Tension, shear, bearing, weld; impact factor | ASME BTH-1 | P1 |
| VES-025 | Vessel volume & weight | Shells + heads (exact head volumes), partial fill level vs volume curve, empty/operating/test weights | — | P0 |
| VES-026 | Div 2 design-by-rule | Part 4 shells, heads, nozzles | VIII-2 Part 4 | P2 |
| VES-027 | Fatigue screening | Method A/B per 5.5.2 | VIII-2 5.5 | P2 |
| VES-028 | Boiler tube / drum | t = P·D/(2·S·E + 2·y·P) + C (PG-27.2.1/2) | Section I PG-27 | P1 |
| VES-029 | EN 13445 shell & head | e = P·D_i/(2f·z − P); torispherical (β-factor) | EN 13445-3 | P2 |

## Module TNK — Storage tanks

| ID | Calculator | Relation | Reference | Pri |
|----|-----------|----------|-----------|-----|
| TNK-001 | Shell thickness — 1-foot method | t_d = 4.9·D·(H − 0.3)·G / S_d + CA; t_t = 4.9·D·(H − 0.3) / S_t (SI, mm) | API 650 5.6.3 | P0 |
| TNK-002 | Variable-design-point method | Iterative per-course | API 650 5.6.4 | P2 |
| TNK-003 | Bottom & annular plate | Annular plate thickness table by stress & product | API 650 5.5 | P1 |
| TNK-004 | Roof (cone/dome) | Self-supporting cone t = D/(4.8 sin θ)·√(T/2.2) (SI) | API 650 5.10 | P1 |
| TNK-005 | Wind girder / stiffening ring | Section modulus and transformed shell height | API 650 5.9 | P1 |
| TNK-006 | Wind overturning & anchorage | Uplift cases, anchor bolt load | API 650 5.11–5.12 | P1 |
| TNK-007 | Seismic (Annex E) | Impulsive/convective, sloshing height | API 650 Annex E | P2 |
| TNK-008 | Min. shell thickness in service | t_min = 2.6·(H − 1)·D·G/(S·E) (US) | API 653 4.3 | P0 |
| TNK-009 | Settlement evaluation | Cosine curve fit, out-of-plane deflection | API 653 Annex B | P2 |
| TNK-010 | Strapping / volume tables | Tank capacity table vs height incl. heads | API MPMS Ch. 2 | P1 |

## Module INS — Inspection, integrity & FFS

| ID | Calculator | Relation | Reference | Pri |
|----|-----------|----------|-----------|-----|
| INS-001 | Corrosion rate & remaining life | CR = (t_prev − t_act)/Δyears (ST & LT); RL = (t_act − t_req)/CR; next inspection = min(RL/2, max interval) | API 510 / 570 | P0 |
| INS-002 | General metal loss | Level 1/2 average thickness vs t_min, MAWP_r | API 579 Part 4 | P1 |
| INS-003 | Local metal loss | RSF = R_t / [1 − (1/M_t)·(1 − R_t)], Folias factor M_t(λ) | API 579 Part 5 | P1 |
| INS-004 | Pitting | Level 1 pit charts, RSF | API 579 Part 6 | P2 |
| INS-005 | Weld misalignment & out-of-roundness | Level 1 | API 579 Part 8 | P2 |
| INS-006 | Composite repair thickness | t_min from composite allowable strain | PCC-2 Art. 401 | P2 |
| INS-007 | Hardness conversion | HRC ↔ HB ↔ HV ↔ HRB ↔ UTS | ASTM E140 | P0 |
| INS-008 | NDE: radiography geometric unsharpness | U_g = F·d/D | ASME V Art. 2 | P1 |
| INS-009 | Vibration severity | Velocity RMS → zone A/B/C/D per machine group | ISO 20816-3 | P0 |
| INS-010 | Rotor balancing | e_per = G·1000/ω (g·mm/kg); U_per = e_per·m | ISO 21940-11 | P1 |
| INS-011 | Weibull / MTBF | Weibull fit, B10 life, availability | — | P2 |

## Module MCH — Machine design

| ID | Calculator | Relation | Reference | Pri |
|----|-----------|----------|-----------|-----|
| MCH-001 | Shaft diameter (static & fatigue) | DE-Goodman: d = {16n/π·[2(K_f·M_a)/S_e + √3·(K_fs·T_m)/S_ut]}^(1/3); also DE-Gerber, DE-ASME elliptic, Soderberg; Marin factors for S_e | Shigley, ASME B106.1M, DIN 743 | P0 |
| MCH-002 | Shaft critical speed | Rayleigh–Ritz / Dunkerley | — | P1 |
| MCH-003 | Keys & keyways | Shear & crushing: τ = 2T/(d·w·L), σ = 4T/(d·h·L); standard key sizes | ASME B17.1, DIN 6885 | P0 |
| MCH-004 | Splines | Involute spline torque capacity | ANSI B92.1, ISO 4156 | P2 |
| MCH-005 | Rolling bearing life | L₁₀ = (C/P)^p, p = 3 (ball) or 10/3 (roller); P = X·F_r + Y·F_a; modified L_nm = a₁·a_ISO·L₁₀ | ISO 281 | P0 |
| MCH-006 | Hydrodynamic journal bearing | Sommerfeld number, Raimondi–Boyd charts (digitized) | Shigley | P2 |
| MCH-007 | Spur & helical gear geometry | Module/DP, pitch dia., addendum, contact ratio, center distance, profile shift | ISO 21771, AGMA 908 | P0 |
| MCH-008 | Gear strength — bending & pitting | AGMA 2001: σ = W_t·K_o·K_v·K_s·(P_d/F)·(K_m·K_B/J); σ_c = C_p·√(W_t·K_o·K_v·K_s·(K_m/(d_P·F))·(C_f/I)); ISO 6336 method B | AGMA 2001-D04, ISO 6336 | P1 |
| MCH-009 | Bevel, worm gears | Geometry & rating | AGMA 2003, AGMA 6034 | P2 |
| MCH-010 | Gear train / planetary ratios | Willis equation, tooth-count solver | — | P0 |
| MCH-011 | Belt drives (V, timing, flat) | Length, wrap angle, tension ratio T₁/T₂ = e^(μθ/sin(β/2)), power rating | Gates/RMA | P1 |
| MCH-012 | Chain drives | Pitch selection, chain length, power rating | ANSI B29.1, ISO 606 | P1 |
| MCH-013 | Bolted joint | VDI 2230 (stiffness, joint diagram, preload loss, embedding); simple: F_i = 0.75·A_t·S_p; torque T = K·d·F_i; separation & fatigue checks | VDI 2230, Shigley | P0 |
| MCH-014 | Bolt pattern / eccentric load | Elastic method for group shear & moment | AISC, Shigley | P1 |
| MCH-015 | Thread stripping & engagement | Required engagement length | FED-STD-H28, ISO 898 | P1 |
| MCH-016 | Helical compression spring | k = G·d⁴/(8·D³·N_a); τ = K_w·8·F·D/(π·d³), K_w = (4C − 1)/(4C − 4) + 0.615/C; buckling, surge frequency | SMI, EN 13906-1 | P0 |
| MCH-017 | Extension, torsion, disc (Belleville) springs | Almen–László for Belleville | EN 16983 | P1 |
| MCH-018 | Press / shrink fit | Lamé: p = δ / {d·[(1/E_o)·((d_o² + d²)/(d_o² − d²) + ν_o) + (1/E_i)·((d² + d_i²)/(d² − d_i²) − ν_i)]}; torque capacity T = π·μ·p·d²·L/2; assembly temperature | Shigley, DIN 7190 | P0 |
| MCH-019 | Fits & tolerances | ISO 286 IT grades & deviations, hole/shaft basis, clearance/interference limits; ANSI B4.1/B4.2 | ISO 286, ASME B4 | P0 |
| MCH-020 | Power screw / lead screw | T_raise = F·d_m/2·(l + π·μ·d_m·sec α)/(π·d_m − μ·l·sec α); efficiency, self-locking | Shigley | P1 |
| MCH-021 | Ball screw | Life, critical speed, buckling | ISO 3408 | P2 |
| MCH-022 | Couplings | Service factor sizing, misalignment | AGMA 9002 | P2 |
| MCH-023 | Clutches & brakes | Uniform wear/pressure disc; band brake T₁/T₂ = e^(μθ); drum brakes | Shigley | P1 |
| MCH-024 | Flywheel | I = E_s/(C_s·ω²) | — | P1 |
| MCH-025 | Cams | Displacement/velocity/acceleration (SHM, cycloidal, polynomial 3-4-5) | Norton | P2 |
| MCH-026 | Four-bar & slider-crank | Grashof check, position/velocity/acceleration analysis (also in DT-06) | — | P1 |
| MCH-027 | Welded joints | Weld-group method (throat, polar moment), fillet capacity; AWS D1.1 allowable; fatigue category | AWS D1.1, Shigley, Blodgett | P0 |
| MCH-028 | Fatigue life | S-N (Basquin), Goodman/Gerber/Soderberg/ASME-elliptic, Miner's rule, rainflow counting from imported load history | Shigley, ASTM E1049 | P1 |
| MCH-029 | Stress concentration | K_t from Peterson charts (digitized curve fits) for common geometries; notch sensitivity q (Neuber) | Peterson | P1 |
| MCH-030 | Fracture mechanics (basic) | K_I = Y·σ·√(πa); critical crack size; Paris law life | BS 7910 (index) | P2 |
| MCH-031 | Rivets & pins | Shear, bearing, clevis pin bending | — | P1 |
| MCH-032 | O-rings | Groove dimensions, squeeze, gland fill | AS568, ISO 3601 | P1 |
| MCH-033 | Wire rope & slings | Rope SWL, D/d ratio, sling angle factor | ASME B30.9, ISO 2408 | P1 |
| MCH-034 | Conveyor (belt) | CEMA capacity, effective tension T_e, power | CEMA | P2 |
| MCH-035 | Hydraulic cylinder & circuit | Force = P·A, speed = Q/A, rod buckling, pump/motor sizing | ISO 6020/6022 | P1 |
| MCH-036 | Pneumatic cylinder & air consumption | Free-air consumption in Nl/min | — | P1 |

## Module STR — Strength of materials & structures

| ID | Calculator | Relation | Reference | Pri |
|----|-----------|----------|-----------|-----|
| STR-001 | Section properties — library | A, I, Z, S, r, J, C_w for 40+ shapes incl. AISC/EN databases | AISC Manual | P0 |
| STR-002 | Section properties — arbitrary polygon | Green's theorem on drawn/imported polygon (holes supported) | — | P1 |
| STR-003 | Beam formulas | 60+ load/support cases (Roark): reactions, shear, moment, slope, deflection; superposition | Roark's Formulas | P0 |
| STR-004 | Continuous beam | Matrix stiffness solver (also DT-01) | — | P0 |
| STR-005 | Columns | Euler P_cr = π²·E·I/(K·L)²; Johnson parabola below transition; AISC 360 E3 | AISC 360 | P0 |
| STR-006 | Combined stress & Mohr's circle | Principal stresses, max shear, von Mises, Tresca; 2D & 3D | — | P0 |
| STR-007 | Thick-walled cylinders | Lamé σ_r, σ_θ distribution | — | P1 |
| STR-008 | Plates | Rectangular & circular plates, various edge conditions (Roark tables) | Roark | P1 |
| STR-009 | Torsion | Circular, thin-walled closed (Bredt), open sections, rectangular | — | P0 |
| STR-010 | Curved beams | Winkler–Bach: σ = M·(r_n − r)/(A·e·r) | — | P2 |
| STR-011 | Thermal stress | σ = E·α·ΔT (restrained), bimetal | — | P0 |
| STR-012 | Steel member design (LRFD/ASD) | AISC 360 tension (D), compression (E), flexure (F), shear (G), combined (H1-1) | AISC 360 | P1 |
| STR-013 | Base plate & anchor bolts | AISC Design Guide 1 method | AISC DG1 | P2 |
| STR-014 | Pipe rack / platform beam check | Simplified using STR-012 | — | P2 |
| STR-015 | Buckling of plates & shells | Classical formulas with knock-down | — | P2 |

## Module FLD — Fluid mechanics & turbomachinery

| ID | Calculator | Relation | Reference | Pri |
|----|-----------|----------|-----------|-----|
| FLD-001 | Reynolds number & flow regime | Re = ρ·v·D/μ | — | P0 |
| FLD-002 | Friction factor solver | Colebrook 1/√f = −2·log₁₀(ε/(3.7D) + 2.51/(Re·√f)); Moody chart plot | — | P0 |
| FLD-003 | Pump head & power | TDH from system; P_hyd = ρ·g·Q·H; P_shaft = P_hyd/η; motor sizing with service factor | HI | P0 |
| FLD-004 | NPSH available | NPSH_a = (P_s − P_v)/(ρ·g) + z_s − h_fs (+ v²/2g); margin ratio per HI 9.6.1 | ANSI/HI 9.6.1 | P0 |
| FLD-005 | Affinity laws & impeller trim | Q ∝ N·D, H ∝ N²·D², P ∝ N³·D³ | HI | P0 |
| FLD-006 | System curve vs pump curve | Operating point, parallel/series pumps, VFD curves (also DT-08) | — | P1 |
| FLD-007 | Specific speed & suction-specific speed | N_s = N·√Q/H^0.75; N_ss limits | API 610 | P0 |
| FLD-008 | Viscosity correction | HI 9.6.7 correction factors C_Q, C_H, C_η | ANSI/HI 9.6.7 | P1 |
| FLD-009 | Compressor head & power | Polytropic H_p = Z·R·T₁·(n/(n − 1))·[(P₂/P₁)^((n−1)/n) − 1]; discharge temperature; staging | API 617, GPSA | P1 |
| FLD-010 | Fan laws & fan power | P = Q·Δp/η; system effect | AMCA 201/203 | P1 |
| FLD-011 | Control valve sizing | Liquid C_v = Q·√(G/ΔP) with F_P, F_L, choked flow; gas with Y, x_T; noise estimation (IEC 60534-8, lite) | ISA 75.01 / IEC 60534-2-1 | P1 |
| FLD-012 | Orifice / nozzle discharge | Q = C_d·A·√(2Δp/ρ); choked gas flow | — | P0 |
| FLD-013 | Open channel flow | Manning v = (1/n)·R^(2/3)·S^(1/2) | — | P2 |
| FLD-014 | Hydrostatics & buoyancy | Forces on submerged surfaces, center of pressure | — | P1 |
| FLD-015 | Tank draining time | Torricelli with C_d, various geometries | — | P1 |
| FLD-016 | Ejector / eductor | Entrainment ratio (empirical) | — | P3 |
| FLD-017 | Hydraulic turbine & Pelton | Power, specific speed | — | P3 |
| FLD-018 | Compressible flow (isentropic, normal shock, Fanno, Rayleigh) | Gas-dynamics tables | — | P2 |
| FLD-019 | Drag & lift | F = ½·ρ·v²·C_d·A | — | P2 |

## Module THM — Thermodynamics & heat transfer

| ID | Calculator | Relation | Reference | Pri |
|----|-----------|----------|-----------|-----|
| THM-001 | Steam tables | IAPWS-IF97 regions 1–5 (all property pairs: P-T, P-h, P-s, h-s, T-x) | IAPWS-IF97 | P0 |
| THM-002 | Fluid & refrigerant properties | CoolProp (HEOS) for 120+ fluids incl. R-134a, R-410A, R-32, R-1234yf, R-744, NH₃ | CoolProp | P0 |
| THM-003 | Ideal-gas & real-gas mixtures | Z factor (Peng–Robinson, SRK), natural gas (AGA-8 / GERG-2008) | AGA-8 | P1 |
| THM-004 | Rankine cycle | Simple, reheat, regenerative (open/closed FWH); thermal efficiency, heat rate | — | P1 |
| THM-005 | Brayton, Otto, Diesel, dual, Stirling | Air-standard + real component efficiencies | — | P1 |
| THM-006 | Vapor-compression refrigeration | COP, cycle on P-h diagram (plot), subcooling/superheat | — | P1 |
| THM-007 | Combustion | Stoichiometric air, excess air from O₂, flue gas composition, adiabatic flame temperature, HHV/LHV | — | P1 |
| THM-008 | Boiler efficiency | Direct & indirect (heat-loss) method | ASME PTC 4 (simplified) | P1 |
| THM-009 | Conduction | Plane wall/cylinder/sphere composite, critical insulation radius | — | P0 |
| THM-010 | Convection correlations | Dittus–Boelter, Gnielinski, Sieder–Tate, Churchill–Bernstein, Churchill–Chu, flat plate | Incropera | P0 |
| THM-011 | Radiation | Stefan–Boltzmann, view factors (common geometries), radiation shields | — | P1 |
| THM-012 | Fins | Efficiency for straight/annular/pin fins | — | P1 |
| THM-013 | Transient conduction | Lumped capacitance (Bi < 0.1), one-term Heisler series | — | P1 |
| THM-014 | Heat exchanger — LMTD | Q = U·A·F·LMTD, F-factor for 1-2, 2-4 shell passes, crossflow | TEMA | P0 |
| THM-015 | Heat exchanger — ε-NTU | Effectiveness relations for all common configurations | Kays & London | P0 |
| THM-016 | Shell & tube thermal design | Kern & Bell–Delaware methods; tube count, baffle spacing, ΔP | TEMA, Bell–Delaware | P2 |
| THM-017 | Overall U & fouling | 1/U = 1/h_o + R_fo + r_o·ln(r_o/r_i)/k + R_fi·(r_o/r_i) + (r_o/r_i)/h_i | TEMA fouling table | P0 |
| THM-018 | Plate heat exchanger | Area & ΔP (generic correlations) | — | P2 |
| THM-019 | Air-cooled exchanger | Briggs–Young finned tube | API 661 | P3 |
| THM-020 | Cooling tower | Merkel number, approach/range | CTI | P2 |
| THM-021 | Condensation & boiling | Nusselt film condensation, Rohsenow, critical heat flux | — | P2 |

## Module HVC — HVAC & refrigeration

| ID | Calculator | Relation | Reference | Pri |
|----|-----------|----------|-----------|-----|
| HVC-001 | Psychrometric calculator & chart | Interactive chart; any 2 of (DB, WB, DP, RH, W, h) at any altitude; processes (mixing, heating, cooling-coil, humidification) | ASHRAE Fundamentals | P0 |
| HVC-002 | Cooling load | RTS method (simplified) & CLTD/CLF quick method; room-by-room | ASHRAE | P1 |
| HVC-003 | Heating load | UA·ΔT + infiltration + ventilation | ASHRAE | P1 |
| HVC-004 | Air quantity | Q = q_s/(1.08·ΔT) (US) / 1.2·c_p form (SI) | — | P0 |
| HVC-005 | Duct sizing | Equal friction, velocity reduction, static regain; round↔rectangular equivalent D_e = 1.30·(ab)^0.625/(a+b)^0.25 | ASHRAE, SMACNA | P0 |
| HVC-006 | Duct fitting losses | ASHRAE duct fitting database-style coefficients (in-house) | ASHRAE | P1 |
| HVC-007 | Ventilation rate | VRP: V_bz = R_p·P_z + R_a·A_z | ASHRAE 62.1 | P1 |
| HVC-008 | Chilled / hot water flow | Q = load/(ρ·c_p·ΔT) | — | P0 |
| HVC-009 | Refrigerant line sizing | Suction/discharge/liquid lines, oil return velocity | ASHRAE Refrigeration | P2 |
| HVC-010 | Thermal comfort | PMV/PPD | ISO 7730 / ASHRAE 55 | P2 |
| HVC-011 | Expansion tank | V_t = V_s·[(v₂/v₁ − 1) − 3αΔT]/(1 − P₁/P₂) | ASHRAE | P1 |
| HVC-012 | Sprinkler hydraulics | Q = K·√P, Hazen–Williams, demand curve | NFPA 13 | P2 |
| HVC-013 | Fire pump check | 150% flow at ≥ 65% rated head | NFPA 20 | P2 |
| HVC-014 | Smoke exhaust / stairwell pressurization | Simplified | NFPA 92 | P3 |

## Module MAT — Materials

| ID | Calculator | Relation / content | Reference | Pri |
|----|-----------|--------------------|-----------|-----|
| MAT-001 | Material browser | Specs, grades, UNS, P-No., allowable S(T), E(T), α(T), k(T) | ASME II, ASTM | P0 |
| MAT-002 | Allowable stress lookup & interpolation | Linear interpolation per code; no extrapolation beyond tabulated range | II-D, B31.3 A-1 | P0 |
| MAT-003 | Material equivalents | ASTM ↔ EN ↔ JIS ↔ DIN ↔ GB ↔ IS cross-reference | — | P1 |
| MAT-004 | Carbon equivalent & preheat | CE_IIW = C + Mn/6 + (Cr + Mo + V)/5 + (Ni + Cu)/15; Pcm; AWS D1.1 Annex H | AWS D1.1 | P0 |
| MAT-005 | Galvanic compatibility | Galvanic series in seawater, anodic index difference | MIL-STD-889 | P1 |
| MAT-006 | Corrosion allowance & material selection guide | Service-based selection matrix (CO₂, H₂S/NACE MR0175 sour service hardness limits) | NACE MR0175/ISO 15156 | P2 |
| MAT-007 | Hardness ↔ tensile | See INS-007 | ASTM E140/A370 | P0 |
| MAT-008 | Plastics & elastomers database | Temperature limits, chemical compatibility | — | P2 |

## Module WLD — Welding

| ID | Calculator | Relation | Reference | Pri |
|----|-----------|----------|-----------|-----|
| WLD-001 | Heat input | HI = 60·V·I·η/(1000·S) kJ/mm (S in mm/min) | ASME IX QW-409.1 | P0 |
| WLD-002 | Preheat | AWS D1.1 Table 5.8 (prequalified) and Annex H (hardness/hydrogen control); EN 1011-2 CET method | AWS D1.1, EN 1011-2 | P0 |
| WLD-003 | WPS/PQR qualification range | Thickness, diameter, P-No. groupings, position — from PQR values | ASME IX QW-451, QW-452 | P1 |
| WLD-004 | Weld metal volume & consumables | Groove geometry cross-section × length / deposition efficiency; time & cost | — | P0 |
| WLD-005 | Fillet weld sizing | Required leg from load; min/max size | AWS D1.1 Table 5.7 | P0 |
| WLD-006 | PWHT requirement | Holding temperature & time by P-No. and thickness | VIII-1 UCS-56, B31.3 Table 331.1.1 | P1 |
| WLD-007 | Welding symbol builder | Interactive AWS A2.4 / ISO 2553 symbol creator | AWS A2.4 | P1 |

## Module MFG — Manufacturing & machining

| ID | Calculator | Relation | Reference | Pri |
|----|-----------|----------|-----------|-----|
| MFG-001 | Speeds & feeds — turning, milling, drilling | n = 1000·V_c/(π·D); v_f = f_z·z·n; MRR; cutting power P_c = k_c·MRR; tool material/workpiece database | Sandvik-style generic data | P0 |
| MFG-002 | Tap drill & thread data | Drill = D − P (metric) and % thread | ISO 261, B1.1 | P0 |
| MFG-003 | Sheet metal bend allowance | BA = θ·(π/180)·(R + K·t); bend deduction; flat-pattern; min. flange | — | P0 |
| MFG-004 | Press tonnage | Blanking F = L·t·τ_s; bending (V-die) F = k·L·t²·UTS/W | — | P1 |
| MFG-005 | Surface finish conversion | Ra ↔ Rz ↔ RMS ↔ N-grade; achievable by process | ASME B46.1, ISO 1302 | P0 |
| MFG-006 | Tolerance stack-up | Worst-case, RSS, Monte Carlo (also DT-07) | — | P0 |
| MFG-007 | Casting | Shrinkage allowance, Chvorinov solidification time t = B·(V/A)ⁿ, riser sizing | — | P2 |
| MFG-008 | Injection molding | Clamp force = projected area × cavity pressure, cooling time | — | P2 |
| MFG-009 | 3D printing | Print time & material estimate from STL (DT-05) | — | P3 |
| MFG-010 | Machining cost & time | Setup + cycle + tool life (Taylor V·T^n = C) | — | P1 |
| MFG-011 | Pipe fabrication | Pipe cutting templates (saddle/fish-mouth, miter, lateral), rolling offsets: travel = √(set² + roll²)/sin θ | — | P0 |
| MFG-012 | Weight & cost estimator | Shapes × density × price | — | P0 |

## Module GEN — General utilities

| ID | Tool | Content | Pri |
|----|------|---------|-----|
| GEN-001 | Unit converter | 80+ quantities, 1,000+ units, gauge/absolute, temperature differences | P0 |
| GEN-002 | Engineering calculator | Scientific + unit-aware arithmetic (e.g., `3 ft + 200 mm`), history, constants | P0 |
| GEN-003 | Equation solver | User-defined equations, solve for any variable (Newton/Brent), unit-aware | P1 |
| GEN-004 | Interpolation | Linear, double (bilinear), log-log, spline; paste table | P0 |
| GEN-005 | Curve fit / regression | Polynomial, exponential, power, custom; R² | P1 |
| GEN-006 | Matrix & linear algebra | Determinant, inverse, eigen, LU | P2 |
| GEN-007 | ODE solver | RK4/RK45 for user-entered systems (e.g., vibrating systems) | P2 |
| GEN-008 | Vibration (SDOF/2DOF) | ω_n = √(k/m), damping, transmissibility, isolator selection | P1 |
| GEN-009 | Engineering economics | NPV, IRR, payback, LCC (pumps/motors), energy savings (VFD) | P1 |
| GEN-010 | Geometry & area/volume | 2D/3D shapes, tank partial volumes | P0 |
| GEN-011 | Motor data | Full-load current, efficiency class (IEC 60034-30-1), NEMA frames, slip, starting torque | P1 |
| GEN-012 | Formula handbook | Searchable, linked to calculators | P0 |
| GEN-013 | Constants & data tables | Physical constants (CODATA), gravity by latitude, atmosphere (ISA/US Std 1976) | P0 |

---

### Totals

| Module | Count |
|--------|------:|
| PIP Piping | 46 |
| PRV Relief | 13 |
| VES Vessels | 29 |
| TNK Tanks | 10 |
| INS Inspection/FFS | 11 |
| MCH Machine design | 36 |
| STR Structures | 15 |
| FLD Fluids | 19 |
| THM Thermo/heat transfer | 21 |
| HVC HVAC | 14 |
| MAT Materials | 8 |
| WLD Welding | 7 |
| MFG Manufacturing | 12 |
| GEN General | 13 |
| **Total named calculators** | **254** |

Many calculators have sub-modes (e.g., STR-003 has 60+ beam cases, FLD-018 has 4 flow models), so there are well over 400 distinct computations.

> **Accuracy note:** The equations in this catalog are written in summary form. The authoritative implementation for each calculator is its definition file plus the code edition it cites. Each formula is verified against the code text and the worked examples during implementation (see QA §6.3).
