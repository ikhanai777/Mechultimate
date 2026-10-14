package com.mechultimate.core.data

/**
 * Wrought steel pipe dimensions per ASME B36.10M (carbon/alloy) and B36.19M (stainless, "S" schedules).
 * Outside diameters and nominal walls are in inches, as tabulated; converted to SI on use.
 */
data class PipeSize(val nps: String, val dn: Int, val odIn: Double, val walls: List<Pair<String, Double>>) {
    val odM: Double get() = odIn * 0.0254

    /** Schedules ordered by wall thickness; identical walls are merged into one label (e.g. "40 / STD"). */
    val schedules: List<Schedule> by lazy {
        walls.groupBy { it.second }.toSortedMap().map { (wall, entries) ->
            Schedule(entries.joinToString(" / ") { it.first }, wall * 0.0254)
        }
    }
}

data class Schedule(val label: String, val wallM: Double)

object PipeData {
    private fun p(nps: String, dn: Int, od: Double, vararg walls: Pair<String, Double>) = PipeSize(nps, dn, od, walls.toList())

    val sizes: List<PipeSize> = listOf(
        p("1/8", 6, 0.405, "10S" to 0.049, "40" to 0.068, "STD" to 0.068, "80" to 0.095, "XS" to 0.095),
        p("1/4", 8, 0.540, "10S" to 0.065, "40" to 0.088, "STD" to 0.088, "80" to 0.119, "XS" to 0.119),
        p("3/8", 10, 0.675, "10S" to 0.065, "40" to 0.091, "STD" to 0.091, "80" to 0.126, "XS" to 0.126),
        p("1/2", 15, 0.840, "5S" to 0.065, "10S" to 0.083, "40" to 0.109, "STD" to 0.109, "80" to 0.147, "XS" to 0.147, "160" to 0.188, "XXS" to 0.294),
        p("3/4", 20, 1.050, "5S" to 0.065, "10S" to 0.083, "40" to 0.113, "STD" to 0.113, "80" to 0.154, "XS" to 0.154, "160" to 0.219, "XXS" to 0.308),
        p("1", 25, 1.315, "5S" to 0.065, "10S" to 0.109, "40" to 0.133, "STD" to 0.133, "80" to 0.179, "XS" to 0.179, "160" to 0.250, "XXS" to 0.358),
        p("1-1/4", 32, 1.660, "5S" to 0.065, "10S" to 0.109, "40" to 0.140, "STD" to 0.140, "80" to 0.191, "XS" to 0.191, "160" to 0.250, "XXS" to 0.382),
        p("1-1/2", 40, 1.900, "5S" to 0.065, "10S" to 0.109, "40" to 0.145, "STD" to 0.145, "80" to 0.200, "XS" to 0.200, "160" to 0.281, "XXS" to 0.400),
        p("2", 50, 2.375, "5S" to 0.065, "10S" to 0.109, "40" to 0.154, "STD" to 0.154, "80" to 0.218, "XS" to 0.218, "160" to 0.344, "XXS" to 0.436),
        p("2-1/2", 65, 2.875, "5S" to 0.083, "10S" to 0.120, "40" to 0.203, "STD" to 0.203, "80" to 0.276, "XS" to 0.276, "160" to 0.375, "XXS" to 0.552),
        p("3", 80, 3.500, "5S" to 0.083, "10S" to 0.120, "40" to 0.216, "STD" to 0.216, "80" to 0.300, "XS" to 0.300, "160" to 0.438, "XXS" to 0.600),
        p("3-1/2", 90, 4.000, "5S" to 0.083, "10S" to 0.120, "40" to 0.226, "STD" to 0.226, "80" to 0.318, "XS" to 0.318),
        p("4", 100, 4.500, "5S" to 0.083, "10S" to 0.120, "40" to 0.237, "STD" to 0.237, "80" to 0.337, "XS" to 0.337, "120" to 0.438, "160" to 0.531, "XXS" to 0.674),
        p("5", 125, 5.563, "5S" to 0.109, "10S" to 0.134, "40" to 0.258, "STD" to 0.258, "80" to 0.375, "XS" to 0.375, "120" to 0.500, "160" to 0.625, "XXS" to 0.750),
        p("6", 150, 6.625, "5S" to 0.109, "10S" to 0.134, "40" to 0.280, "STD" to 0.280, "80" to 0.432, "XS" to 0.432, "120" to 0.562, "160" to 0.719, "XXS" to 0.864),
        p("8", 200, 8.625, "5S" to 0.109, "10S" to 0.148, "20" to 0.250, "30" to 0.277, "40" to 0.322, "STD" to 0.322, "60" to 0.406, "80" to 0.500, "XS" to 0.500, "100" to 0.594, "120" to 0.719, "140" to 0.812, "XXS" to 0.875, "160" to 0.906),
        p("10", 250, 10.750, "5S" to 0.134, "10S" to 0.165, "20" to 0.250, "30" to 0.307, "40" to 0.365, "STD" to 0.365, "60" to 0.500, "XS" to 0.500, "80" to 0.594, "100" to 0.719, "120" to 0.844, "140" to 1.000, "XXS" to 1.000, "160" to 1.125),
        p("12", 300, 12.750, "5S" to 0.156, "10S" to 0.180, "20" to 0.250, "30" to 0.330, "STD" to 0.375, "40" to 0.406, "XS" to 0.500, "60" to 0.562, "80" to 0.688, "100" to 0.844, "120" to 1.000, "XXS" to 1.000, "140" to 1.125, "160" to 1.312),
        p("14", 350, 14.000, "5S" to 0.156, "10S" to 0.188, "10" to 0.250, "20" to 0.312, "30" to 0.375, "STD" to 0.375, "40" to 0.438, "XS" to 0.500, "60" to 0.594, "80" to 0.750, "100" to 0.938, "120" to 1.094, "140" to 1.250, "160" to 1.406),
        p("16", 400, 16.000, "5S" to 0.165, "10S" to 0.188, "10" to 0.250, "20" to 0.312, "30" to 0.375, "STD" to 0.375, "40" to 0.500, "XS" to 0.500, "60" to 0.656, "80" to 0.844, "100" to 1.031, "120" to 1.219, "140" to 1.438, "160" to 1.594),
        p("18", 450, 18.000, "5S" to 0.165, "10S" to 0.188, "10" to 0.250, "20" to 0.312, "STD" to 0.375, "30" to 0.438, "XS" to 0.500, "40" to 0.562, "60" to 0.750, "80" to 0.938, "100" to 1.156, "120" to 1.375, "140" to 1.562, "160" to 1.781),
        p("20", 500, 20.000, "5S" to 0.188, "10S" to 0.218, "10" to 0.250, "20" to 0.375, "STD" to 0.375, "30" to 0.500, "XS" to 0.500, "40" to 0.594, "60" to 0.812, "80" to 1.031, "100" to 1.281, "120" to 1.500, "140" to 1.750, "160" to 1.969),
        p("24", 600, 24.000, "5S" to 0.218, "10S" to 0.250, "10" to 0.250, "20" to 0.375, "STD" to 0.375, "XS" to 0.500, "30" to 0.562, "40" to 0.688, "60" to 0.969, "80" to 1.219, "100" to 1.531, "120" to 1.812, "140" to 2.062, "160" to 2.344),
    )

    fun size(nps: String): PipeSize = sizes.first { it.nps == nps }

    /** Choice options for an NPS picker. */
    val npsOptions: List<Pair<String, String>> = sizes.map { it.nps to "NPS ${it.nps}  (DN ${it.dn}, OD ${it.odIn} in)" }

    /**
     * Thinnest listed schedule whose wall, after applying the fractional mill under-tolerance,
     * is at least [requiredM]. Returns null if no listed schedule is thick enough.
     */
    fun selectSchedule(nps: String, requiredM: Double, millTolerance: Double): Schedule? =
        size(nps).schedules.firstOrNull { it.wallM * (1 - millTolerance) >= requiredM - 1e-12 }
}

/** API 526 standard effective orifice areas (in²) by letter designation. */
object Api526 {
    val orifices: List<Pair<String, Double>> = listOf(
        "D" to 0.110, "E" to 0.196, "F" to 0.307, "G" to 0.503, "H" to 0.785, "J" to 1.287, "K" to 1.838,
        "L" to 2.853, "M" to 3.60, "N" to 4.34, "P" to 6.38, "Q" to 11.05, "R" to 16.0, "T" to 26.0,
    )

    /** Smallest orifice with area ≥ [requiredM2], or null if larger than T. */
    fun select(requiredM2: Double): Pair<String, Double>? =
        orifices.firstOrNull { it.second * 6.4516e-4 >= requiredM2 }
}
