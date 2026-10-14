# 4. Design Tools

Design tools are interactive, canvas-based workspaces. A calculator is a form. A design tool is a model the user builds, edits and analyses. All design tools save into projects, feed results into calculators, and export to the report system.

| ID | Tool | Pri |
|----|------|-----|
| DT-01 | 2D Frame / Beam / Truss FEA | P1 |
| DT-02 | Piping Isometric Builder | P1 |
| DT-03 | Pipe Stress Lite | P2 |
| DT-04 | Parametric 2D Sketcher | P2 |
| DT-05 | CAD / Model Viewer | P1 |
| DT-06 | Mechanism Simulator | P2 |
| DT-07 | Tolerance Stack-up Analyzer | P0 |
| DT-08 | Pump & System Curve Workbench | P1 |
| DT-09 | P&ID / PFD Builder | P2 |
| DT-10 | Pressure Vessel Configurator | P1 |
| DT-11 | Mohr's Circle & Stress Visualizer | P0 |
| DT-12 | Psychrometric Chart Workbench | P0 |
| DT-13 | Thermodynamic Diagram Plotter (T-s, P-h, h-s) | P1 |
| DT-14 | GD&T Frame Builder & Checker | P1 |
| DT-15 | Bolt Pattern & Flange Designer | P2 |
| DT-16 | Heat Exchanger Layout (tube sheet) | P2 |

---

## DT-01 2D Frame / Beam / Truss FEA

- **Elements:** 2D truss (bar), Euler–Bernoulli beam, Timoshenko beam (optional), and frame (axial + bending), with pinned/rigid connections and member end releases.
- **Supports:** fixed, pinned, roller (any angle), spring (translational/rotational), and prescribed displacement.
- **Loads:** nodal forces/moments, uniform/trapezoidal distributed loads, self-weight, thermal (ΔT, gradient), and support settlement. Load cases and combinations (user-defined factors, ASCE 7 / Eurocode presets).
- **Sections and materials:** pick from STR-001 databases or define a custom section. Material from MAT-001.
- **Solver:** direct stiffness method, sparse symmetric solver (skyline/Cholesky). Up to 2,000 DOF on device in under 2 s.
- **Outputs:** deformed shape (scaled, animated), axial/shear/moment diagrams, reactions, member stresses, utilization vs AISC 360 (links to STR-012), buckling load factor (linear eigen), and natural frequencies (modal, lumped/consistent mass).
- **UX:** grid snap, node/member drawing with finger or stylus, undo/redo, numeric input table view, and DXF import of a line model.
- **Validation:** NAFEMS-style benchmark set plus textbook problems (see QA).

## DT-02 Piping Isometric Builder

- Draw pipe runs in isometric space using direction buttons (N/S/E/W/Up/Down) and lengths, or by tapping on an isometric grid.
- Component palette: elbows (LR/SR/5D), tees, reducers (concentric/eccentric), flanges, valves (by type), supports (rest, guide, anchor, spring, hanger), and instruments.
- Each component carries a spec (NPS, schedule, class, material) from a user-defined **Pipe Class / Piping Material Specification** (PMS) table.
- **Auto outputs:** cut lengths (deducting fitting take-offs per B16.9), **BOM / MTO** (material take-off with quantities, weights and descriptions), weld count (field/shop), and an isometric drawing sheet PDF (with north arrow, coordinates, BOM table and title block).
- Rolling-offset and slope helpers (from MFG-011).
- Export formats: PDF, DXF, PCF (Piping Component File, for interoperability with stress/iso tools), and CSV MTO.

## DT-03 Pipe Stress Lite

Linear-elastic 3D beam model generated from DT-02 (or drawn directly).

- Load cases per B31.3 / B31.1: **Sustained** (W + P), **Expansion** (T1 range), **Operating** (W + P + T), **Occasional** (wind, seismic g-load, relief thrust).
- Code stress checks: S_L vs S_h, S_E vs S_A (PIP-017/018), with SIFs from B31J (PIP-019).
- Support load summary, displacement at nodes, spring hanger selection (variable-load spring table, generic).
- Nozzle load check against API 610 Table 5 / NEMA SM-23 (simplified).
- **Scope disclaimer:** intended for screening and small systems (≤ 200 nodes). Nonlinear supports (gaps, friction) are handled iteratively in v2. The app does not replace dedicated stress software for critical lines.

## DT-04 Parametric 2D Sketcher

- Geometry: lines, arcs, circles, splines, slots, polygons, fillets/chamfers, offset, mirror, pattern.
- Constraint solver (geometric + dimensional): coincident, parallel, perpendicular, tangent, equal, concentric, horizontal/vertical, distance, angle, radius. Uses a Newton–Raphson solver with DOF analysis (based on the open SolveSpace approach, re-implemented or GPL-compatible).
- Output: section properties of the closed profile (feeds STR-002), area/perimeter/centroid, flat patterns (with MFG-003 bend data), and DXF/SVG export.
- Use cases: gaskets, plates, brackets, cutting templates, cam profiles.

## DT-05 CAD / Model Viewer

- 2D formats: DXF, DWG (read-only via a licensed library such as ODA Teigha, Enterprise build) and PDF drawings.
- 3D formats: STEP/IGES (OpenCASCADE, LGPL, dynamically linked), STL, OBJ, glTF, 3MF.
- Features: orbit/pan/zoom, section planes, measure (distance, angle, radius, area), exploded view, assembly tree, mass properties (volume, mass with assigned material, CoG, inertia), and markup/redline with photo export.
- AR mode (ARCore): place the model at real scale in the environment.
- Performance: assemblies up to 500k triangles at 30 fps on mid-range GPUs. LOD and frustum culling.

## DT-06 Mechanism Simulator

- Planar linkages: four-bar, slider-crank, quick-return, and user-defined (joints: revolute, prismatic).
- Kinematic analysis: position, velocity and acceleration over a crank cycle. Coupler curve tracing. Transmission angle. Grashof classification.
- Kinetostatics: joint forces and input torque for given masses/inertias and external loads.
- Cam designer: follower types (knife, roller, flat), motion programs (MCH-025), pressure angle, and profile export (DXF).
- Gear train visualizer: animated planetary trains with ratio verification (MCH-010).

## DT-07 Tolerance Stack-up Analyzer

- Build a 1D dimension loop (vector chain) from a list of dimensions with direction, nominal, ± tolerance or limits, and distribution (normal, uniform, triangular, custom Cp/Cpk).
- Methods: worst-case, RSS, modified RSS (Bender factor), and Monte Carlo (10⁶ samples on device, with a histogram).
- Results: gap nominal/min/max, percent contribution (Pareto), predicted yield/PPM against the spec, and sensitivity.
- Import of fits from MCH-019 (ISO 286) and GD&T contributions from DT-14 (position, profile as ± equivalents).
- 2D stack-up (v2): vector loops with angular contributors.

## DT-08 Pump & System Curve Workbench

- Enter or digitize pump curves (H-Q, η-Q, NPSHr-Q, P-Q) from points or a photo of a datasheet (curve digitizer with manual calibration).
- System curve from static head + friction (piping network from PIP-024 or a simple K-sum).
- Operating point; parallel/series pumps; VFD speed curves (affinity laws); impeller trim; minimum continuous flow; POR/AOR shading per HI 9.6.3.
- Energy: annual energy and cost at a duty-cycle profile, with VFD savings (feeds GEN-009).

## DT-09 P&ID / PFD Builder

- ISA 5.1 symbol library (instruments, valves, equipment, lines) and ISO 10628 alternatives.
- Smart connectors, line numbering (e.g., `6"-P-1001-A1A-H`), tag management, and an equipment/instrument/line list auto-generated as XLSX.
- Line list entries open directly into PIP calculators (pre-filled size, class, design conditions) and batch-run them (FR-2.11).
- Export: PDF and SVG. DEXPI XML in v2.

## DT-10 Pressure Vessel Configurator

- Assemble a vessel from components: heads (2:1 SE, F&D, hemi, flat, cone), shell courses, nozzles (with orientation and projection), saddles/skirt/legs, internals (weight only), and insulation.
- Global design data: P, T, CA, MDMT, joint efficiency, material per component, liquid level and SG.
- Runs **all** relevant VES calculators automatically: shell/head thickness, static head per component, MAWP roll-up (VES-014), nozzle reinforcement (VES-009), MDMT (VES-016), external pressure (VES-007), wind/seismic (VES-023), saddle/skirt (VES-020/021), weights and volumes (VES-025), and hydrotest (VES-015).
- Outputs: 2D GA drawing (elevation + nozzle orientation), nozzle schedule, vessel datasheet, and a full ASME VIII-1 calc report.

## DT-11 Mohr's Circle & Stress Visualizer

- 2D/3D stress state input. Interactive Mohr circle(s) with a draggable element rotation, principal planes, and max shear.
- Failure envelopes: Tresca, von Mises, max normal (Rankine), Coulomb–Mohr, and modified Mohr, with a safety factor on the plot.
- Strain rosette reduction (0/45/90, 0/60/120) to principal strains/stresses.

## DT-12 Psychrometric Chart Workbench

- Full interactive ASHRAE-style chart at any altitude in SI or IP units. Tap to add state points.
- Draw processes: sensible heating/cooling, cooling with dehumidification (ADP, bypass factor), adiabatic mixing, steam/evaporative humidification, and sensible heat ratio lines.
- Comfort zone overlay (ASHRAE 55) and a design-day weather point library (ASHRAE climatic data summary, licensed or public subset).
- Export of chart PNG/PDF and state table.

## DT-13 Thermodynamic Diagram Plotter

- T-s, P-h, h-s (Mollier) and P-v diagrams for any CoolProp fluid or IF97 steam.
- Overlay the cycles from THM-004/005/006 with state-point tables. Isobars, isotherms and quality lines.

## DT-14 GD&T Frame Builder & Checker

- Build feature control frames per ASME Y14.5-2018 / ISO 1101: all 12/14 characteristics, modifiers (MMC, LMC, RFS, projected zone, free state, tangent plane, unequal profile).
- Validity checker (e.g., flags modifiers that are invalid for the characteristic and missing datum references).
- Position calculator: actual position from X/Y deviations (2·√(Δx² + Δy²)), bonus tolerance, virtual condition, and conversion between ± coordinate and position.
- Fixed/floating fastener formulas: T = H − F (floating); T₁ + T₂ = H − F (fixed).

## DT-15 Bolt Pattern & Flange Designer

- Bolt circle layout (equal/unequal spacing) with DXF export.
- Combines PIP-043/044, VES-017 and MCH-013 for a custom flange: gasket selection, bolt load, flange stresses and torque sheet per PCC-1 (pass-by-pass torque table and pattern diagram, printable).

## DT-16 Heat Exchanger Layout

- Tube sheet layout: tube OD, pitch, pattern (30°/45°/60°/90°), passes, tie rods, impingement zone, and outer tube limit. Tube count and drawing.
- Links to THM-016 thermal design and to VES calculators for shell/channel/tubesheet thickness (tubesheet per UHX is v2).
