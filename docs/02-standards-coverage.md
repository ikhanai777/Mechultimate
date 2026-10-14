# 2. Standards Coverage

This document lists every code and standard the app supports and the **depth of support** for each:

| Level | Meaning |
|-------|---------|
| **C** — Calculators | Equations implemented as calculators, with clause references |
| **D** — Data | Tabular data available in the databases (licensed or public-domain source) |
| **I** — Index | Clause index, in-house summaries, cross-links. No code text is reproduced. |
| **B** — BYO-license | Deep links into the user's own licensed PDF |

Default edition = latest published as of release. Earlier editions are kept wherever formulas or limits differ.

---

## 2.1 ASME

### 2.1.1 Boiler and Pressure Vessel Code (BPVC)

| Section | Title | Level | Key content in app |
|---------|-------|-------|--------------------|
| I | Power Boilers | C, I, B | PG-27 tube/pipe/drum thickness and MAWP, PG-29 heads, PG-32 openings, hydrotest PG-99 |
| II-A/B | Ferrous / Nonferrous Material Specs | D, I | Spec/grade index, chemistry, tensile/yield |
| II-C | Welding Consumables | I | SFA classification decoder |
| II-D | Properties | D (licensed pack), I | Allowable stress tables 1A/1B/2A/2B/3, external pressure charts (CS-1, CS-2, HA-1…), E, α, k |
| III | Nuclear Components | I | Index only (v1) |
| IV | Heating Boilers | C, I | HG-301 shells, HG-307 heads |
| V | Nondestructive Examination | I, C | Technique selector, IQI selection, film density/geometric unsharpness calc (Article 2) |
| VIII Div 1 | Pressure Vessels | C, D, I, B | **Full module** — UG-27, UG-28, UG-32, UG-33, UG-34, UG-37 to UG-45, UG-99/100, UCS-66/UCS-66.1 MDMT, UW-12 joint efficiency, UW-16 nozzle welds, Appendix 1 (conical, reinforcing rings), App. 2 flanges, App. 9 jackets, App. 13 non-circular |
| VIII Div 2 | Alternative Rules | C, I | Part 4 design-by-rule (shells, heads, nozzles), Part 5 fatigue screening (5.5.2) |
| VIII Div 3 | High Pressure | I | Index only |
| IX | Welding, Brazing & Fusing Qualifications | C, D, I | P-numbers/F-numbers/A-numbers, WPS/PQR essential-variable checker, qualified thickness ranges (QW-451), position ranges (QW-461) |
| X | FRP Vessels | I | Index |
| XII | Transport Tanks | I | Index |
| XIII | Overpressure Protection | C, I | Links to the relief-valve sizing module |

### 2.1.2 B31 Code for Pressure Piping

| Code | Title | Level | Key content |
|------|-------|-------|-------------|
| B31.1 | Power Piping | C, D, I | 104.1.2 thickness (Eq. 7/9), 104.3 branch reinforcement, Table 121.5 support spacing, Table 102.4.5 bend thinning, App. A allowables, 137 hydrotest |
| B31.3 | Process Piping | C, D, I | 304.1.2 (Eq. 3a/3b) straight pipe, 304.2 bends/miters, 304.3 branch reinforcement, 304.4 closures, 304.5 blanks (Eq. 15), 302.3.5 sustained/displacement stress range, 319.4.1 flexibility exemption, 345.4 hydro/pneumatic test, Table A-1 allowables, Table 302.3.4 quality factors, Table 304.1.1 Y coefficients, W weld strength reduction |
| B31.4 | Pipeline Transportation – Liquids | C, I | 403.2.1 wall thickness, hoop/longitudinal/combined stress, hydrotest |
| B31.5 | Refrigeration Piping | C, I | 504.1 wall thickness |
| B31.8 | Gas Transmission & Distribution | C, I | 841.1.1 design pressure (Barlow + F, E, T factors), location classes, 841.3 testing |
| B31.9 | Building Services Piping | C, I | 904.1 thickness, support spacing |
| B31.12 | Hydrogen Piping | C, I | IP-3.1 thickness with material performance factor Mf |
| B31G / Modified B31G | Remaining strength of corroded pipelines | C | Level 0/1 safe pressure, ERF |
| B31J | Stress intensification and flexibility factors | C, I | SIF/k-factors for fittings |
| B31T | Toughness requirements | I | Index |

### 2.1.3 B16 Valves, Flanges, Fittings, Gaskets

| Standard | Level | Content |
|----------|-------|---------|
| B16.5 | C, D | Flange P-T ratings by material group (Tables 2-1.1 to 2-3.19), dimensions, bolting, class selection tool |
| B16.47 | D | Series A (MSS SP-44) and Series B (API 605) large-diameter flanges |
| B16.9 | D | Butt-weld fittings (elbows, tees, reducers, caps) |
| B16.11 | D | Forged socket-weld and threaded fittings |
| B16.10 | D | Valve face-to-face / end-to-end |
| B16.20 / B16.21 | D | Spiral-wound, ring-joint, and non-metallic gaskets |
| B16.25 | D | Butt-weld end preparation (bevel) |
| B16.34 | C, D | Valve P-T ratings (Standard/Special class), minimum wall |
| B16.36 | D | Orifice flanges |
| B16.48 | D | Line blanks (spectacle, paddle, spacer) |
| B16.1 / B16.24 / B16.42 | D | Cast iron, bronze, ductile iron flanges |

### 2.1.4 Other ASME

| Standard | Level | Content |
|----------|-------|---------|
| B36.10M / B36.19M | D | Welded/seamless steel pipe and stainless pipe: NPS, OD, schedules, wall, weight, internal area |
| B1.1 / B1.13M / B1.20.1 | D | Unified inch threads, metric threads, NPT pipe threads |
| B18 series | D | Bolts, nuts, screws, washers, pins, keys (B18.2.1, B18.2.2, B18.3, B17.1 keys) |
| B4.1 / B4.2 | C, D | Preferred limits and fits (inch / metric) |
| B46.1 | C, I | Surface texture parameters and conversion (Ra, Rz, Rq, RMS, N-grades) |
| B106.1M | C | Design of transmission shafting |
| B30 series | C, I | Cranes, slings, below-the-hook lifting (B30.20; BTH-1 design categories) |
| BTH-1 | C | Below-the-hook lifting device design (lifting lugs, spreader beams) |
| Y14.5 | I, C | GD&T symbol reference, tolerance zone calculator, bonus tolerance, virtual condition, position conversion |
| Y14.100 / Y14.41 | I | Drawing practices, MBD |
| PCC-1 | C | Bolted flange joint assembly: target bolt stress, torque tables, tightening patterns (legacy cross pattern & alternative patterns) |
| PCC-2 | C, I | Repair: weld overlays, composite wraps (Article 401 thickness calc) |
| PTC series | I, C | Performance test codes: PTC 4 (boiler efficiency), PTC 6 (steam turbine), PTC 8.2 (pumps), PTC 10 (compressors), PTC 19.3 TW (thermowell wake-frequency calc) |
| FFS-1 / API 579-1 | C | See API section |
| A13.1 | D | Pipe identification color/label sizing |
| A17.1 | I | Elevators (index) |

## 2.2 API (American Petroleum Institute)

| Standard | Level | Content |
|----------|-------|---------|
| API 5L | D | Line pipe grades, PSL1/PSL2 properties |
| API 6A / 6D | D, I | Wellhead equipment, pipeline valves |
| API 520 Part I / II | C | Relief device sizing (gas/vapor critical and subcritical, steam with Kn/Ksh, liquid, two-phase per Annex C/Leung omega), inlet loss (3% rule), backpressure effects, reaction forces |
| API 521 | C | Relieving scenarios: fire case (wetted area, Q = 21,000·F·A^0.82 / 43,200·F·A^0.82), blocked outlet, tube rupture (10/13 rule), flare radiation (simple API method), flare header sizing |
| API 526 | D | Flanged steel PRV dimensions and orifice letters (D–T) |
| API 527 | I | Seat tightness |
| API 560 | C, I | Fired heaters: tube thickness (refers to API 530) |
| API 530 | C | Heater tube thickness (elastic/rupture design) |
| API 579-1 / ASME FFS-1 | C | Part 4 general metal loss (Level 1/2), Part 5 local metal loss (RSF), Part 6 pitting (Level 1 charts), Part 8 weld misalignment (Level 1), remaining life |
| API 594 / 599 / 600 / 602 / 608 / 609 | D, I | Valve types, min. wall, design references |
| API 610 | C, I | Centrifugal pump: nozzle loads table, allowable operating region, NPSH margin, specific speed, suction-specific speed |
| API 617 / 618 / 619 | I, C | Compressors: polytropic/isentropic head, power, discharge temperature |
| API 650 | C | Tank shell (1-foot method, variable-design-point), bottom/annular plate, roof, wind girders, overturning/anchorage, venting reference |
| API 653 | C | Tank inspection: min. shell thickness, remaining life, settlement evaluation (cosine fit, Annex B) |
| API 661 | I | Air-cooled exchangers |
| API 670 / 671 / 672 / 674 / 675 / 676 / 682 / 686 | I, C | Machinery protection, couplings, PD pumps (pulsation estimate), seals (flush plan selector), installation |
| API 2000 | C | Tank venting: normal (in/out-breathing) and emergency venting |
| API RP 14E | C | Erosional velocity Ve = C/√ρm, two-phase line sizing |
| API RP 505 / 500 | I | Hazardous area classification (index) |
| API 570 / 510 | C, I | Piping/vessel inspection: corrosion rate (ST/LT), remaining life, next inspection date |
| API 571 / 580 / 581 | I, C | Damage mechanisms reference, RBI (qualitative matrix) |

## 2.3 Other standards bodies

| Body | Standards | Level / Content |
|------|-----------|-----------------|
| **ISO** | ISO 281 (bearing life), ISO 286 (fits), ISO 6336 (gears), ISO 4287/21920 (surface), ISO 5167 (orifice, nozzle, venturi), ISO 10816/20816 (vibration severity), ISO 1940-1/21940 (balancing grades), ISO 898-1 (fastener properties), ISO 261/965 (threads), ISO 4014/4017/4032 (bolts/nuts), ISO 1101 (GPS/GD&T), ISO 2768 (general tolerances), ISO 5199 (pumps), ISO 13709 (=API 610), ISO 4126 (safety valves), ISO 9906 (pump acceptance), ISO 12100 (risk), ISO 13849 (safety PL calculator), ISO 7730 (thermal comfort PMV/PPD) — C, D, I |
| **EN** | EN 13445 (unfired pressure vessels: shells/heads), EN 13480 (metallic industrial piping), EN 1092-1 (flanges PN), EN 10216/10217/10220/10253 (tubes & fittings), EN 1993 (Eurocode 3 steel members), EN 1591 (flange calc, index), EN 12952/12953 (boilers), EN ISO 9606/15614 (welder/WPS qual.) — C, D, I |
| **DIN / JIS / BS / GB / IS / GOST** | DIN 2448/2458, DIN 743 (shaft strength), DIN 3990 (gears), JIS B2220 flanges, JIS G pipe, BS 1387, BS 5500 (legacy PD 5500), GB/T 150 (vessels), IS 2825, IS 1239, GOST 32569 — D, I (C where formulas are common) |
| **AWS** | D1.1 (structural steel welding: prequalified joints, preheat Table 5.8 / Annex H methods, weld size limits), D1.2, D1.6, A2.4 (symbols reference), A5.x (filler classifications) — C, D, I |
| **TEMA** | 10th ed. classes R/C/B, shell/head types (AES, BEM…), tube pitch, tube count estimation, baffle spacing limits, vibration screening — C, D, I |
| **HEI / HI** | Heat Exchange Institute (surface condensers), Hydraulic Institute (ANSI/HI 9.6.1 NPSH margin, 9.6.3 POR/AOR, 9.6.4 vibration) — C, I |
| **ASHRAE** | Fundamentals (psychrometrics, load calc CLTD/RTS), Std 15 (refrigerant safety), 34 (designations), 55 (comfort), 62.1 (ventilation rate procedure), 90.1 (energy envelope/equipment tables) — C, D, I |
| **SMACNA** | HVAC Duct Construction (gauges, reinforcement), seismic restraint index — D, I |
| **NFPA** | 13 (sprinkler hydraulic calc: Hazen-Williams, K-factor), 14 (standpipes), 20 (fire pumps 150%/65% rule), 30 (flammable liquids index), 54/58 (gas piping sizing tables) — C, I |
| **AISC** | 360 (steel members: tension, compression, flexure LRFD/ASD), Steel Manual shapes database, Design Guide index — C, D |
| **AGMA** | 2001-D04/2101 (gear bending/pitting), 908 (geometry factors), 6011 (high-speed), 9005 (lubrication) — C, I |
| **MSS** | SP-25, SP-44, SP-58 (pipe hangers; replaces SP-69), SP-75 (high-strength fittings), SP-97 (branch outlets) — D, I |
| **ASTM** | Material specs (A105, A106, A182, A216, A234, A312, A333, A350, A516, A193/A194, B-series), E140 (hardness conversion), E8 (tensile), E23 (Charpy) — D, I |
| **SAE** | J429 (bolt grades), J403/J404 (steel chemistry), J1199 — D |
| **VDI** | VDI 2230 (bolted joints, systematic calc), VDI 2700 (load securing) — C |
| **IEC / NEMA** | IEC 60534 (control valve sizing, = ISA-75.01), IEC 60034 (motor efficiency classes), NEMA MG-1 (motor frames) — C, D |
| **ISA** | 5.1 (P&ID symbols), 75.01 (control valve Cv), 84 (SIL index) — C, D |
| **Crane / Darby** | Crane TP-410 K-factors, Darby 3-K method (published literature, not a standard) — C |
| **CEMA** | Belt conveyor design (capacity, tension, power) — C |
| **NEMA SM 23 / API 612** | Steam turbine nozzle loads — C |
| **EJMA** | Expansion joint design (bellows stress, spring rate, cycle life) — C |
| **NORSOK** | L-001/L-002 (piping), M-001 (materials selection) — I |
| **DNV** | DNV-ST-F101 (submarine pipelines, index), DNV-RP-C203 (fatigue S-N curves) — C, I |

## 2.4 Licensing and copyright strategy

Codes and standards are copyrighted by their publishers. The app follows these rules:

1. **Equations are implemented, not copied.** Mathematical relations are not copyrightable. The app implements them in code and cites the clause and equation number. Clause text is **never** reproduced verbatim.
2. **Summaries are original.** All clause summaries are written in-house in plain language and reviewed by a licensed PE. They are short and paraphrased, and they point to the source.
3. **Tabular data has three tiers:**
   - **Public / factual / widely published data** (pipe OD and schedule walls, thread geometry, unit conversions, physical constants, steam tables from IAPWS, CoolProp fluids) ships in the base app.
   - **Licensed data** (e.g., ASME II-D allowable stress tables, B16.5 P-T rating tables, API 579 charts) ships as **licensed data packs** under a commercial data license from the publisher (ASME, API) or an authorized reseller (e.g., IHS/S&P Global, Techstreet). Packs are encrypted and tied to the account entitlement.
   - **User-supplied data**: Enterprise users can import their own licensed tables (CSV/XLSX template) into a private library.
4. **BYO-license PDF linking**: users who own a code PDF can register it, and the app maps clause IDs to page numbers. The PDF stays on device.
5. **Trademarks** are used only to identify the standard. An "Not affiliated with or endorsed by ASME, API, ISO…" statement appears in About and on every report.
6. **Edition integrity**: every data record carries `source`, `edition`, `table/figure id`, `license_tier` and `verified_by` fields (see Architecture §5.6).

## 2.5 Edition management

- Each standard has an edition list (e.g., B31.3: 2012, 2014, 2016, 2018, 2020, 2022, 2024).
- Calculators declare which editions they implement and any per-edition differences (formula, factor tables, limits).
- A project locks its editions at creation. Changing them later triggers a "re-validate all calcs" prompt with a diff of results.
- Release notes include a "Code changes affecting results" section.
