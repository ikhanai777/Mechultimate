package com.mechultimate.app.ui

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.PictureAsPdf
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mechultimate.app.data.Settings
import com.mechultimate.app.report.PdfReport
import com.mechultimate.core.Catalog
import com.mechultimate.core.engine.CalcResult
import com.mechultimate.core.engine.ChoiceInput
import com.mechultimate.core.engine.NumberInput
import com.mechultimate.core.engine.Output
import com.mechultimate.core.engine.Status
import com.mechultimate.core.engine.Step
import com.mechultimate.core.units.UnitSystem
import com.mechultimate.core.units.formatNumber

/** Formats an SI value in the unit chosen for a result (by label) or the default for the system. */
class ResultFormatter(private val system: UnitSystem, val sig: Int, private val overrides: Map<String, String>) {
    fun unitFor(label: String, kind: com.mechultimate.core.units.Kind, si: String?, us: String?) =
        overrides[label]?.let { s -> kind.units.firstOrNull { it.symbol == s } } ?: displayUnit(kind, system, si, us)

    fun output(o: Output): String = when {
        o.text != null -> o.text!!
        o.kind == null || o.value == null -> formatNumber(o.value ?: Double.NaN, sig)
        else -> unitFor(o.label, o.kind!!, o.siUnit, o.usUnit).let { u -> formatNumber(u.fromSi(o.value!!), sig) + " " + u.symbol }
    }

    fun step(s: Step): String = when {
        s.text != null -> s.text!!
        s.value == null -> ""
        s.kind == null -> formatNumber(s.value!!, sig)
        else -> unitFor(s.label, s.kind!!, s.siUnit, s.usUnit).let { u -> formatNumber(u.fromSi(s.value!!), sig) + " " + u.symbol }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalculatorScreen(calcId: String, uid: String?, app: AppViewModel, settings: Settings, onBack: () -> Unit) {
    val calc = Catalog.get(calcId) ?: run { Text("Unknown calculator $calcId"); return }
    val saved = uid?.let { app.saved.get(it) }
    val vm: CalculatorViewModel = viewModel(key = "calc-$calcId-$uid-${settings.unitSystem}") {
        CalculatorViewModel(
            calc, settings.unitSystem,
            saved?.let { com.mechultimate.core.engine.InputValues(it.numbers, it.choices) },
            saved?.uid, saved?.title, saved?.project,
        )
    }
    val context = LocalContext.current
    var showSave by remember { mutableStateOf(false) }
    val fmt = ResultFormatter(settings.unitSystem, settings.sigFigs, vm.outputUnits)
    val favorite = calc.id in settings.favorites

    Scaffold(topBar = {
        TopAppBar(
            title = { Column { Text(calc.title, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.titleMedium); Text(calc.id, style = MaterialTheme.typography.labelSmall) } },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
            actions = {
                IconButton(onClick = { app.toggleFavorite(calc.id) }) {
                    Icon(if (favorite) Icons.Outlined.Star else Icons.Outlined.StarBorder, if (favorite) "Remove favourite" else "Add favourite")
                }
                IconButton(onClick = { vm.reset() }) { Icon(Icons.Outlined.Refresh, "Reset inputs") }
                IconButton(onClick = { showSave = true }) { Icon(Icons.Outlined.Save, "Save") }
                IconButton(onClick = {
                    runCatching {
                        val file = PdfReport.write(context, calc, vm.title, vm.project, vm, fmt, settings)
                        val uri = FileProvider.getUriForFile(context, context.packageName + ".files", file)
                        val send = Intent(Intent.ACTION_SEND).setType("application/pdf").putExtra(Intent.EXTRA_STREAM, uri)
                            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        context.startActivity(Intent.createChooser(send, "Share calculation report"))
                    }.onFailure { Toast.makeText(context, "Report failed: ${it.message}", Toast.LENGTH_LONG).show() }
                }) { Icon(Icons.Outlined.PictureAsPdf, "Export PDF report") }
            },
        )
    }) { pad ->
        BoxWithConstraints(Modifier.padding(pad).fillMaxSize().imePadding()) {
            if (maxWidth >= 840.dp) {
                Row(Modifier.fillMaxSize()) {
                    Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Header(calc.description)
                        InputsCard(vm, settings.unitSystem)
                    }
                    Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        ResultsCard(vm.result, fmt, vm)
                        WorkCard(vm.result, fmt)
                        BasisCard(calc)
                    }
                }
            } else {
                Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Header(calc.description)
                    InputsCard(vm, settings.unitSystem)
                    ResultsCard(vm.result, fmt, vm)
                    WorkCard(vm.result, fmt)
                    BasisCard(calc)
                    Spacer(Modifier.height(24.dp))
                }
            }
        }
    }

    if (showSave) {
        var title by rememberSaveable { mutableStateOf(vm.title) }
        var project by rememberSaveable { mutableStateOf(vm.project) }
        AlertDialog(
            onDismissRequest = { showSave = false },
            title = { Text(if (vm.savedUid != null) "Save new revision" else "Save calculation") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(title, { title = it }, label = { Text("Title / tag") }, singleLine = true)
                    OutlinedTextField(project, { project = it }, label = { Text("Project") }, singleLine = true)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    vm.title = title; vm.project = project.ifBlank { "General" }
                    app.save(calc.id, vm.title, vm.project, vm.values(), vm.savedUid)
                    showSave = false
                    Toast.makeText(context, "Saved", Toast.LENGTH_SHORT).show()
                }) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { showSave = false }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun Header(description: String) =
    Text(description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)

@Composable
private fun SectionCard(title: String, content: @Composable () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
        Column(Modifier.padding(16.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
            content()
        }
    }
}

@Composable
private fun InputsCard(vm: CalculatorViewModel, system: UnitSystem) {
    val errors = vm.result.fieldErrors
    SectionCard("Inputs") {
        for (spec in vm.calc.inputs) {
            if (!vm.isVisible(spec.id)) continue
            when (spec) {
                is NumberInput -> {
                    val f = vm.fields.getValue(spec.id)
                    Row(verticalAlignment = Alignment.Top) {
                        OutlinedTextField(
                            value = f.text,
                            onValueChange = { vm.setText(spec.id, it) },
                            label = { Text(spec.label) },
                            isError = errors.containsKey(spec.id),
                            supportingText = (errors[spec.id] ?: spec.help.takeIf { it.isNotBlank() })?.let { { Text(it) } },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodyLarge.copy(fontFamily = FontFamily.Monospace),
                            modifier = Modifier.weight(1f),
                        )
                        UnitPicker(spec.kind.units, f.unit, { vm.setUnit(spec, it) }, Modifier.width(96.dp).padding(top = 12.dp))
                    }
                }
                is ChoiceInput -> {
                    val sel = spec.options.first { it.key == vm.choices[spec.id] }
                    Dropdown(spec.label, spec.options, sel, { it.label }, { vm.setChoice(spec.id, it.key) },
                        supporting = spec.help.takeIf { it.isNotBlank() })
                }
            }
        }
    }
}

@Composable
private fun ResultsCard(result: CalcResult, fmt: ResultFormatter, vm: CalculatorViewModel) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
        Column(Modifier.padding(16.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Results", style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                StatusBadge(result.overall)
            }
            result.error?.let { Text(it, color = statusColor(Status.FAIL), style = MaterialTheme.typography.bodyMedium) }
            for (o in result.outputs.filter { it.primary }) OutputRow(o, fmt, vm, big = true)
            val secondary = result.outputs.filter { !it.primary }
            if (secondary.isNotEmpty()) {
                HorizontalDivider()
                for (o in secondary) OutputRow(o, fmt, vm, big = false)
            }
            if (result.checks.isNotEmpty()) {
                HorizontalDivider()
                for (c in result.checks) {
                    Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(c.status.icon(), null, tint = statusColor(c.status), modifier = Modifier.size(18.dp).padding(top = 2.dp))
                        Text(c.message, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }
}

@Composable
private fun OutputRow(o: Output, fmt: ResultFormatter, vm: CalculatorViewModel, big: Boolean) {
    Column {
        Text(o.label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f))
        if (o.kind != null && o.value != null) {
            val unit = fmt.unitFor(o.label, o.kind!!, o.siUnit, o.usUnit)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    formatNumber(unit.fromSi(o.value!!), fmt.sig),
                    style = if (big) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.titleMedium,
                    fontFamily = FontFamily.Monospace, fontWeight = if (big) FontWeight.SemiBold else FontWeight.Normal,
                )
                UnitPicker(o.kind!!.units, unit.symbol, { vm.outputUnits[o.label] = it })
            }
        } else {
            Text(fmt.output(o), style = if (big) MaterialTheme.typography.titleLarge else MaterialTheme.typography.bodyLarge,
                fontWeight = if (big) FontWeight.SemiBold else FontWeight.Normal)
        }
    }
}

@Composable
private fun WorkCard(result: CalcResult, fmt: ResultFormatter) {
    var open by rememberSaveable { mutableStateOf(false) }
    SectionCard("Show work") {
        Row(Modifier.fillMaxWidth().clickable { open = !open }, verticalAlignment = Alignment.CenterVertically) {
            Text(if (open) "Hide calculation steps" else "${result.steps.size} calculation steps", modifier = Modifier.weight(1f))
            Icon(if (open) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore, null)
        }
        if (open) {
            result.steps.forEachIndexed { i, s ->
                Column(Modifier.padding(vertical = 4.dp)) {
                    Text("${i + 1}. ${s.label}", style = MaterialTheme.typography.labelLarge)
                    Text(s.formula, fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodySmall)
                    val v = fmt.step(s)
                    if (v.isNotBlank()) Text("= $v", fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                    s.reference?.let { Text(it, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary) }
                }
            }
        }
    }
}

@Composable
private fun BasisCard(calc: com.mechultimate.core.engine.Calculator) {
    SectionCard("Basis") {
        Text("References", style = MaterialTheme.typography.labelLarge)
        calc.references.forEach { Text("• $it", style = MaterialTheme.typography.bodySmall) }
        if (calc.assumptions.isNotEmpty()) {
            Spacer(Modifier.height(4.dp))
            Text("Assumptions", style = MaterialTheme.typography.labelLarge)
            calc.assumptions.forEach { Text("• $it", style = MaterialTheme.typography.bodySmall) }
        }
        Spacer(Modifier.height(4.dp))
        Text(DISCLAIMER, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
