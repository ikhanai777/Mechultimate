# 1. Product Specification — MechUltimate (Android)

| Item | Value |
|------|-------|
| Product | MechUltimate — Mechanical Engineering Suite |
| Platform | Android phone and tablet (Chromebook and foldables supported) |
| Min / Target SDK | API 26 (Android 8.0) / latest stable API |
| Distribution | Google Play (AAB with Play Asset Delivery) and a universal signed APK for direct/enterprise install |
| Document status | Draft v1.0 |

---

## 1.1 Vision

One offline app that replaces the binder of code books, the spreadsheet collection and the dozen single-purpose calculator apps that a mechanical engineer carries today. Every number the app produces is **traceable** (equation, clause, edition, inputs and intermediate values), **unit-safe** (dimensional analysis in the engine) and **reportable** (a PDF calc sheet ready for a design file).

## 1.2 Target users

| Persona | Typical needs |
|---------|---------------|
| **Piping engineer** (EPC, oil & gas, power) | Wall thickness (B31.1/B31.3/B31.4/B31.8), flange ratings (B16.5), pipe data (B36.10/19), spans, expansion, hydrotest, pressure drop, line sizing |
| **Pressure vessel / static equipment engineer** | ASME VIII-1/VIII-2 shells, heads, nozzles, MAWP, MDMT, external pressure, API 650/653 tanks, TEMA exchangers, API 579 FFS |
| **Rotating equipment engineer** | Pumps (API 610, HI), NPSH, affinity laws, compressors, seals (API 682), bearings, couplings, vibration (ISO 10816/20816) |
| **Machine designer / product engineer** | Shafts, gears (AGMA/ISO 6336), bearings (ISO 281), bolts (VDI 2230), springs, fits (ISO 286), GD&T (Y14.5), stack-ups, fatigue, beams |
| **HVAC / MEP engineer** | Psychrometrics, cooling/heating loads, duct sizing (SMACNA/ASHRAE), chilled-water piping, fans and pumps |
| **Maintenance / reliability / inspector** | Remaining life, corrosion rate, retirement thickness, bolt torque (PCC-1), hardness conversion, vibration severity, weld inspection |
| **Manufacturing / welding engineer** | Speeds and feeds, sheet-metal bending, weld size and heat input, preheat, WPS/PQR checks (ASME IX / AWS D1.1) |
| **Students and PE/FE candidates** | Learning mode, worked examples, formula sheets, practice problems |

## 1.3 Scope

### In scope (v1.x)
1. **Calculation suite** — 250+ calculators across 12 modules (see [Calculator Catalog](03-calculator-catalog.md)).
2. **Standards library** — structured index, clause navigator, edition tracking, and licensed or public tabular data (see [Standards Coverage](02-standards-coverage.md)).
3. **Engineering databases** — pipe, flange, fitting, valve, gasket, bolt, steel section, bearing, material, fluid and refrigerant data.
4. **Design tools** — 2D frame/truss FEA, piping isometric with stress-lite, parametric sketcher, CAD viewer, P&ID builder, tolerance stack-up and Mohr's circle (see [Design Tools](04-design-tools.md)).
5. **Projects and reports** — group calcs into projects, revision control, PDF/XLSX/JSON export, and a QR-linked calc ID.
6. **Unit system** — SI, US Customary or mixed, with conversion of 1,000+ units.

### Out of scope (v1.x)
- Full 3D solid CAD authoring (viewer only).
- Full 3D nonlinear FEA or CFD. Linear 2D/beam FEA only on device; heavy solvers are a v3 cloud option.
- Reproducing copyrighted code text verbatim (see licensing in §2.4 of the Standards document).
- Code stamping or certification workflows (e.g., the ASME U-stamp itself).

## 1.4 Functional requirements

### FR-1 Home and navigation
- FR-1.1 The home dashboard shows recent calcs, favorites, active project, and a "Quick tools" strip (unit converter, pipe data, flange rating, steam tables).
- FR-1.2 Navigation is by **Module → Category → Calculator**, plus **Browse by Standard** (e.g., ASME B31.3 → all calcs that cite it).
- FR-1.3 Global search covers calculators, standards, clauses, materials, pipe sizes and formulas. It is fuzzy and offline (SQLite FTS5) and accepts synonyms (e.g., "Barlow" finds pipe wall thickness, "UG-27" finds the cylindrical shell calc).
- FR-1.4 Voice search and a command palette for tablets and Chromebooks (`Ctrl+K`).

### FR-2 Calculator behaviour (applies to every calculator)
- FR-2.1 **Input panel**: typed fields with a unit picker per field, default values, range validation, and inline help (definition, typical values, the clause the input comes from).
- FR-2.2 **Lookup-assisted inputs**: picking "NPS 6, Sch 40" fills OD and wall; picking "SA-106 Gr B @ 300 °C" fills allowable stress; picking "Butt-weld, 100% RT" fills joint efficiency E.
- FR-2.3 **Live computation**: results update as inputs change (debounced at 150 ms). Heavy calcs show a progress indicator and can be cancelled.
- FR-2.4 **Result panel**: primary results highlighted with PASS/FAIL/WARN status against code limits, a utilization ratio bar, secondary results, and intermediate values.
- FR-2.5 **"Show work" view**: renders the equations (LaTeX via KaTeX/MathJax-native) with substituted values, step by step, with clause references and the edition used.
- FR-2.6 **Edition selector**: where the formula or limit changes between code editions (e.g., B31.3 2016 vs 2022, BPVC 2021 vs 2023), the user picks the edition and the default is the latest.
- FR-2.7 **Warnings and applicability checks**: e.g., B31.3 Eq. 3a is valid only for t < D/6, and the app warns and points to 304.1.2(b) when that is exceeded; UG-27 is valid only for P ≤ 0.385SE.
- FR-2.8 **Charts**: parametric sweep of any input against any output (e.g., wall thickness vs. design temperature), with export as PNG/CSV.
- FR-2.9 **Solve-for**: any calculator with an invertible relation lets the user choose the unknown (e.g., solve for MAWP given thickness). Root-finding is used when no closed form exists.
- FR-2.10 **Save / duplicate / compare**: save to project, duplicate as a "case", and compare up to 4 cases side by side.
- FR-2.11 **Batch mode**: paste or import a CSV of inputs (e.g., a line list) and compute every row. Results export to XLSX.

### FR-3 Standards library
- FR-3.1 Hierarchical index of every supported standard: organization → document → part/section → clause.
- FR-3.2 Each clause entry shows the title, a plain-language **summary written in-house**, the related calculators, the related tables, the edition history and the "what changed" notes.
- FR-3.3 Tabular data (pipe dimensions, P-T ratings, allowable stresses) is shown in sortable, filterable tables with unit switching and interpolation.
- FR-3.4 **Bring-your-own-license**: users or organizations holding a code license can attach their own PDF copies, and the app deep-links clause references to the page in those PDFs (on-device PDF viewer with bookmarks).
- FR-3.5 Edition update notifications when a new edition of a followed standard is released, with a change summary.

### FR-4 Databases
- FR-4.1 Materials: ASME/ASTM specifications (SA-/A-), grades, UNS numbers, P-numbers and group numbers (ASME IX), chemical composition, mechanical properties, allowable stress vs. temperature (Section II-D, B31.1 App. A, B31.3 Table A-1), thermal expansion, modulus, and MDMT curve assignment (UCS-66).
- FR-4.2 Piping components: pipe (B36.10M/B36.19M, EN 10220/10216, JIS G3452/3454/3459), flanges (B16.5, B16.47 A/B, EN 1092-1, JIS B2220), fittings (B16.9, B16.11, B16.28), valves face-to-face (B16.10), gaskets (B16.20, B16.21), stud bolts, and line blanks/spectacle blinds (B16.48).
- FR-4.3 Fasteners: unified threads (B1.1), metric threads (ISO 261/965), bolt grades (SAE J429, ISO 898-1, ASTM A193/A194/A320), and nut and washer dimensions (B18).
- FR-4.4 Structural: AISC shapes (W, S, HP, C, MC, L, WT, HSS, pipe), EN/BS sections (IPE, HEA/HEB/HEM, UPN, UC/UB), and IS sections.
- FR-4.5 Bearings: generic ISO dimension series (60xx, 62xx, 63xx, 222xx…) with C, C0 and limiting speeds. Manufacturer catalogs are optional downloadable packs.
- FR-4.6 Fluids: IAPWS-IF97 water/steam, 120+ pure fluids and refrigerants (CoolProp), humid air (ASHRAE RP-1485), natural gas (AGA-8 / GERG-2008), fuels and combustion data, and liquid viscosity/density tables.
- FR-4.7 Users can add custom materials, components and fluids. These sync with the project and can be shared within a team.

### FR-5 Projects, revisions and reports
- FR-5.1 A project holds metadata (client, job no., location, design basis, unit system, code editions) and calcs organized in folders (e.g., by line number or equipment tag).
- FR-5.2 Each calc has a revision history (Rev A, B, 0, 1…), with originator / checker / approver fields and timestamps.
- FR-5.3 **PDF calc sheet**: company header/logo, project block, inputs table, step-by-step solution, results with status, references, assumptions, revision block, and a QR code linking to the calc's unique ID. A4 and Letter.
- FR-5.4 Export formats are PDF, XLSX (with live formulas where possible), CSV, JSON (machine-readable, re-importable) and DOCX (Pro).
- FR-5.5 Share via the Android share sheet. Optional encrypted team sync (Pro/Enterprise).
- FR-5.6 A full-project "Design Basis Report" combines all calcs with a table of contents.

### FR-6 Units
- FR-6.1 Global unit systems are SI, US Customary, and Custom (per-quantity preferences, e.g., pressure in barg but length in mm).
- FR-6.2 Every input field may override its unit, and conversion happens automatically.
- FR-6.3 Gauge vs. absolute pressure is handled explicitly (barg/bara, psig/psia) with a user-set atmospheric reference.
- FR-6.4 Standalone converter covering 80+ quantities and 1,000+ units, including engineering-specific ones (SCFM, Nm³/h, °API, SSU, BTU/hr·ft²·°F, kgf/cm²g).

### FR-7 Learning mode
- FR-7.1 Every calculator has a "Learn" tab with theory, derivation, assumptions, typical pitfalls and a worked example. The worked examples double as test vectors.
- FR-7.2 A formula handbook is browsable by subject (statics, strength, dynamics, fluids, thermo, heat transfer, machine design).
- FR-7.3 FE/PE Mechanical practice question bank (Phase 3) with explanations linked to calculators.

### FR-8 AI engineering assistant (optional, online, Pro)
- FR-8.1 A natural-language request ("size a 2-inch steam line for 500 kg/h at 10 barg") opens the right calculator with inputs pre-filled for the user to confirm. **The AI never produces the numerical result itself.** All numbers come from the deterministic engine.
- FR-8.2 Explains results and clauses in plain language, citing the app's own clause summaries.
- FR-8.3 Clearly labelled, opt-in, and can be switched off entirely by Enterprise admins. No project data is sent without explicit consent.

### FR-9 Field tools (use the phone's sensors)
- Inclinometer / slope meter (pipe slope, for drain lines).
- Vibration meter (accelerometer → velocity RMS, FFT spectrum, ISO 20816 severity zone). Labelled as indicative only.
- Sound level (indicative), stroboscope (screen/flash), tachometer (camera-based optical RPM, experimental).
- Photo markup for inspection notes, with geotag and an attach-to-calc option.
- Measurement camera (AR ruler via ARCore) for rough dimensions.

## 1.5 UX requirements

- **Design system**: Material 3 with dynamic color, light/dark/high-contrast themes, and an engineering-grade monospace font for numbers.
- **Layouts**: phone (single-pane), foldable/tablet (list–detail with inputs on the left and results/work on the right), and Chromebook keyboard shortcuts.
- **One-hand use**: primary actions in the thumb zone; numeric keypad with engineering keys (±, E, π, unit cycle).
- **Glove / field mode**: large touch targets (≥ 56 dp), high-contrast, screen-awake toggle.
- **Accessibility**: TalkBack labels on all fields, results read with units, scalable text up to 200%, WCAG 2.2 AA contrast.
- **Localization**: English at launch, then Spanish, Portuguese, Arabic (RTL), Hindi, Chinese (Simplified), Russian, French, German, Indonesian and Turkish. Number and decimal formats are locale-aware, with an override.
- **Onboarding**: choose discipline(s), default unit system and default code editions. The app then personalizes the home screen.

## 1.6 Monetization and tiers

| Tier | Price model | Contents |
|------|-------------|----------|
| **Free** | Free, no ads in calculators | Unit converter, pipe data, ~40 core calculators, basic PDF (watermarked), learning mode |
| **Pro** | Subscription (monthly/annual) or one-time lifetime | All calculators and design tools, full databases, unwatermarked branded reports, batch mode, projects, AI assistant |
| **Discipline packs** | One-time IAP | E.g., "Pressure Vessel Pack", "Piping Pack", "HVAC Pack", for users who don't want a subscription |
| **Enterprise** | Per-seat annual | Team sync, admin policy (lock code editions, disable AI), custom materials/components library, SSO (SAML/OIDC), MDM support (Android Enterprise managed config), audit log, licensed-standards data packs |
| **Education** | Free/discounted with verification | Pro features with "educational use" watermark on reports |

## 1.7 Non-functional requirements

| ID | Category | Requirement |
|----|----------|-------------|
| NFR-1 | Offline | 100% of calculators, databases and tools work offline. Network is needed only for sync, AI, pack downloads and purchases. |
| NFR-2 | Performance | Cold start < 1.5 s on a mid-range device (e.g., Snapdragon 6-series, 4 GB RAM). Calculator open < 300 ms. Closed-form result < 50 ms. Iterative result < 500 ms. 2D FEA with 500 elements < 2 s. |
| NFR-3 | Size | Base install < 40 MB. Data packs are delivered on demand (Play Asset Delivery / in-app download for the APK build). Full install < 250 MB. |
| NFR-4 | Accuracy | Results match the verification suite to ≤ 0.1% relative (or the documented tolerance of the reference). Displayed precision is configurable, and rounding follows code conventions (e.g., thickness rounded up). |
| NFR-5 | Reliability | Crash-free sessions ≥ 99.8%. Autosave of inputs, so no data is lost on process death. |
| NFR-6 | Security | Projects are encrypted at rest (SQLCipher, key in Android Keystore). TLS 1.3 for sync. No analytics on calc content. Play Integrity check on licensed data packs. |
| NFR-7 | Privacy | GDPR/CCPA compliant. Telemetry is opt-in and anonymized. Data export/delete on request. |
| NFR-8 | Battery | No background work except scheduled sync (WorkManager, unmetered + charging by default). |
| NFR-9 | Maintainability | Calculators are defined declaratively and are independently testable. Adding a calculator needs no UI code (see Architecture §5.4). |
| NFR-10 | Compatibility | ARM64, ARMv7 and x86_64. Android 8–latest. 16 KB page-size compliant. |

## 1.8 Legal and safety

- A first-run acceptance and a per-report footer disclaimer state that the app is an aid and results must be verified by a qualified engineer.
- Each calc's "basis" section lists assumptions and limitations explicitly.
- Copyrighted standards are not reproduced. See the licensing strategy in [Standards Coverage §2.4](02-standards-coverage.md#24-licensing-and-copyright-strategy).
- Trademarks (ASME®, API®, etc.) are used nominatively only, with no implied endorsement.
