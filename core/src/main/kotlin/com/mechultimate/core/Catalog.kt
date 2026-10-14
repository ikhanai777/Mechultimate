package com.mechultimate.core

import com.mechultimate.core.calcs.Fluids
import com.mechultimate.core.calcs.General
import com.mechultimate.core.calcs.Hvac
import com.mechultimate.core.calcs.Inspection
import com.mechultimate.core.calcs.Machine
import com.mechultimate.core.calcs.Manufacturing
import com.mechultimate.core.calcs.Piping
import com.mechultimate.core.calcs.Relief
import com.mechultimate.core.calcs.Structures
import com.mechultimate.core.calcs.Tanks
import com.mechultimate.core.calcs.Thermo
import com.mechultimate.core.calcs.Vessels
import com.mechultimate.core.calcs.Welding
import com.mechultimate.core.engine.Calculator
import com.mechultimate.core.engine.Module

/** Registry of every calculator in the app, with offline search. */
object Catalog {
    val all: List<Calculator> = (
        Piping.all + Relief.all + Vessels.all + Tanks.all + Inspection.all + Machine.all + Structures.all +
            Fluids.all + Thermo.all + Hvac.all + Welding.all + Manufacturing.all + General.all
        ).also { list ->
        val dupes = list.groupBy { it.id }.filterValues { it.size > 1 }.keys
        check(dupes.isEmpty()) { "Duplicate calculator ids: $dupes" }
    }

    private val byId = all.associateBy { it.id }

    fun get(id: String): Calculator? = byId[id]

    fun inModule(module: Module): List<Calculator> = all.filter { it.module == module }

    /** Codes and standards cited by any calculator, with the calculators that cite them. */
    val standards: Map<String, List<Calculator>> by lazy {
        all.flatMap { c -> c.references.map { it.code to c } }
            .groupBy({ it.first }, { it.second })
            .mapValues { (_, v) -> v.distinct() }
            .toSortedMap(String.CASE_INSENSITIVE_ORDER)
    }

    /**
     * Ranks calculators against a free-text query. Every query token must match somewhere
     * (id, title, keywords, references or description); title and id matches rank highest.
     */
    fun search(query: String): List<Calculator> {
        val tokens = query.lowercase().split(Regex("[\\s,]+")).filter { it.isNotBlank() }
        if (tokens.isEmpty()) return emptyList()
        return all.mapNotNull { calc ->
            val id = calc.id.lowercase()
            val title = calc.title.lowercase()
            val keys = calc.keywords.joinToString(" ").lowercase()
            val refs = calc.references.joinToString(" ") { "${it.code} ${it.clause} ${it.note}" }.lowercase()
            val desc = calc.description.lowercase()
            var score = 0
            for (t in tokens) {
                score += when {
                    id == t -> 100
                    title.contains(t) -> 10
                    keys.contains(t) -> 6
                    id.contains(t) || refs.contains(t) -> 4
                    desc.contains(t) -> 2
                    else -> return@mapNotNull null
                }
            }
            calc to score
        }.sortedWith(compareByDescending<Pair<Calculator, Int>> { it.second }.thenBy { it.first.id }).map { it.first }
    }
}
