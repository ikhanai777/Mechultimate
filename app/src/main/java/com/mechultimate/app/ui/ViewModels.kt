package com.mechultimate.app.ui

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mechultimate.app.data.SavedCalc
import com.mechultimate.app.data.SavedCalcRepository
import com.mechultimate.app.data.Settings
import com.mechultimate.app.data.SettingsRepository
import com.mechultimate.core.engine.CalcResult
import com.mechultimate.core.engine.Calculator
import com.mechultimate.core.engine.ChoiceInput
import com.mechultimate.core.engine.InputValues
import com.mechultimate.core.engine.NumberInput
import com.mechultimate.core.units.EngUnit
import com.mechultimate.core.units.Kind
import com.mechultimate.core.units.UnitSystem
import com.mechultimate.core.units.formatNumber
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

class AppViewModel(app: Application) : AndroidViewModel(app) {
    private val settingsRepo = SettingsRepository(app)
    val saved = SavedCalcRepository(app)

    val settings: StateFlow<Settings?> = settingsRepo.settings
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    init { viewModelScope.launch { saved.load() } }

    fun setUnitSystem(s: UnitSystem) = viewModelScope.launch { settingsRepo.setUnitSystem(s) }
    fun setSigFigs(n: Int) = viewModelScope.launch { settingsRepo.setSigFigs(n) }
    fun acceptDisclaimer() = viewModelScope.launch { settingsRepo.acceptDisclaimer() }
    fun toggleFavorite(id: String) = viewModelScope.launch { settingsRepo.toggleFavorite(id) }
    fun pushRecent(id: String) = viewModelScope.launch { settingsRepo.pushRecent(id) }
    fun setCompany(v: String) = viewModelScope.launch { settingsRepo.setCompany(v) }
    fun setEngineer(v: String) = viewModelScope.launch { settingsRepo.setEngineer(v) }

    fun save(calcId: String, title: String, project: String, values: InputValues, uid: String?) = viewModelScope.launch {
        saved.upsert(SavedCalc(uid ?: UUID.randomUUID().toString(), calcId, title, project, values.numbers, values.choices, System.currentTimeMillis()))
    }

    fun deleteSaved(uid: String) = viewModelScope.launch { saved.delete(uid) }
}

/** Unit used to display a quantity, honouring an optional per-field preference. */
fun displayUnit(kind: Kind, system: UnitSystem, si: String?, us: String?): EngUnit {
    val preferred = if (system == UnitSystem.SI) si else us
    return preferred?.let { p -> kind.units.firstOrNull { it.symbol == p } } ?: kind.defaultUnit(system)
}

/** Parses user input, accepting either '.' or ',' as decimal separator. */
fun parseNumber(text: String): Double? = text.trim().replace(',', '.').toDoubleOrNull()

class FieldState(text: String, unit: String) {
    var text by mutableStateOf(text)
    var unit by mutableStateOf(unit)
}

/** Holds the editable state of one calculator screen and recomputes on every change. */
class CalculatorViewModel(
    val calc: Calculator,
    private val system: UnitSystem,
    initial: InputValues?,
    val savedUid: String?,
    savedTitle: String?,
    savedProject: String?,
) : ViewModel() {
    val fields = mutableStateMapOf<String, FieldState>()
    val choices = mutableStateMapOf<String, String>()
    /** Display unit chosen per output label. */
    val outputUnits = mutableStateMapOf<String, String>()
    var result by mutableStateOf(CalcResult())
        private set
    var title by mutableStateOf(savedTitle ?: calc.title)
    var project by mutableStateOf(savedProject ?: "General")

    init { load(initial ?: calc.defaults()) }

    private fun load(values: InputValues) {
        val defaults = calc.defaults()
        for (spec in calc.inputs) {
            when (spec) {
                is NumberInput -> {
                    val unit = displayUnit(spec.kind, system, spec.siUnit, spec.usUnit)
                    val given = values.numbers[spec.id]
                    val si = given ?: defaults.numbers.getValue(spec.id)
                    // Example defaults are rounded for readability after unit conversion; saved values keep full precision.
                    val sig = if (given != null && given != defaults.numbers[spec.id]) 8 else 4
                    fields[spec.id] = FieldState(formatNumber(unit.fromSi(si), sig), unit.symbol)
                }
                is ChoiceInput -> choices[spec.id] = values.choices[spec.id]?.takeIf { v -> spec.options.any { it.key == v } } ?: spec.default
            }
        }
        recompute()
    }

    fun reset() = load(calc.defaults())

    fun values(): InputValues {
        val numbers = calc.inputs.filterIsInstance<NumberInput>().associate { spec ->
            val f = fields.getValue(spec.id)
            spec.id to (parseNumber(f.text)?.let { spec.kind.unit(f.unit).toSi(it) } ?: Double.NaN)
        }
        return InputValues(numbers, choices.toMap())
    }

    fun setText(id: String, text: String) { fields.getValue(id).text = text; recompute() }

    fun setUnit(spec: NumberInput, symbol: String) {
        val f = fields.getValue(spec.id)
        val v = parseNumber(f.text)
        if (v != null) f.text = formatNumber(spec.kind.unit(symbol).fromSi(spec.kind.unit(f.unit).toSi(v)), 6)
        f.unit = symbol
        recompute()
    }

    fun setChoice(id: String, key: String) { choices[id] = key; recompute() }

    fun isVisible(id: String): Boolean = calc.isVisible(calc.inputs.first { it.id == id }, InputValues(emptyMap(), choices.toMap()))

    private fun recompute() { result = calc.compute(values()) }
}
