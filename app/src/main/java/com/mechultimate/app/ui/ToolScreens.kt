package com.mechultimate.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mechultimate.app.BuildConfig
import com.mechultimate.app.data.Settings
import com.mechultimate.core.Catalog
import com.mechultimate.core.units.Kind
import com.mechultimate.core.units.UnitSystem
import com.mechultimate.core.units.Units
import com.mechultimate.core.units.formatNumber
import java.text.DateFormat
import java.util.Date

@Composable
fun ConverterScreen(system: UnitSystem, sig: Int) {
    var kindName by rememberSaveable { mutableStateOf(Kind.PRESSURE_GAUGE.name) }
    val kind = Kind.valueOf(kindName)
    var text by rememberSaveable { mutableStateOf("1") }
    var from by rememberSaveable { mutableStateOf(kind.defaultUnit(system).symbol) }
    LaunchedEffect(kindName) { if (kind.units.none { it.symbol == from }) from = kind.defaultUnit(system).symbol }
    val fromUnit = kind.units.firstOrNull { it.symbol == from } ?: kind.defaultUnit(system)
    val si = parseNumber(text)?.let { fromUnit.toSi(it) }

    LazyColumn(Modifier.fillMaxSize(), contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item { Text("Unit converter", style = MaterialTheme.typography.headlineSmall) }
        item { Dropdown("Quantity", Units.converterKinds, kind, { it.label }, { kindName = it.name; from = it.defaultUnit(system).symbol }) }
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    text, { text = it }, label = { Text("Value") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    textStyle = MaterialTheme.typography.bodyLarge.copy(fontFamily = FontFamily.Monospace),
                    isError = si == null, modifier = Modifier.weight(1f),
                )
                UnitPicker(kind.units, fromUnit.symbol, { from = it })
            }
        }
        if (kind == Kind.PRESSURE_GAUGE || kind == Kind.PRESSURE_ABS) {
            item { Text("Gauge ↔ absolute conversion uses a standard atmosphere of 101.325 kPa.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
        items(kind.units) { u ->
            ListItem(
                headlineContent = { Text(si?.let { formatNumber(u.fromSi(it), sig + 2) } ?: "—", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Medium) },
                supportingContent = { Text(u.name) },
                trailingContent = { Text(u.symbol, fontFamily = FontFamily.Monospace) },
            )
        }
    }
}

@Composable
fun SavedScreen(app: AppViewModel, onOpen: (String, String) -> Unit) {
    val items by app.saved.items.collectAsStateWithLifecycle()
    val df = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)
    LazyColumn(Modifier.fillMaxSize()) {
        item { Text("Saved calculations", Modifier.padding(16.dp), style = MaterialTheme.typography.headlineSmall) }
        if (items.isEmpty()) {
            item {
                Text("Nothing saved yet. Use the save button on any calculator to keep its inputs in a project.",
                    Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        items.groupBy { it.project }.toSortedMap().forEach { (project, list) ->
            item(key = "p-$project") { Column(Modifier.padding(horizontal = 16.dp)) { SectionTitle(project) } }
            items(list, key = { it.uid }) { s ->
                val calc = Catalog.get(s.calcId)
                ListItem(
                    modifier = Modifier.clickable { onOpen(s.calcId, s.uid) },
                    leadingContent = calc?.let { { Icon(it.module.icon(), null, tint = MaterialTheme.colorScheme.primary) } },
                    headlineContent = { Text(s.title) },
                    supportingContent = { Text("${s.calcId} · Rev ${s.revision} · ${df.format(Date(s.savedAt))}") },
                    trailingContent = { IconButton(onClick = { app.deleteSaved(s.uid) }) { Icon(Icons.Outlined.Delete, "Delete") } },
                )
            }
        }
    }
}

@Composable
fun SettingsScreen(app: AppViewModel, s: Settings) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Settings", style = MaterialTheme.typography.headlineSmall)
        Text("Default unit system", style = MaterialTheme.typography.titleSmall)
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            UnitSystem.entries.forEachIndexed { i, u ->
                SegmentedButton(
                    selected = s.unitSystem == u, onClick = { app.setUnitSystem(u) },
                    shape = SegmentedButtonDefaults.itemShape(i, UnitSystem.entries.size),
                ) { Text(u.label) }
            }
        }
        Text("Every field and result also has its own unit picker.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text("Significant figures: ${s.sigFigs}", style = MaterialTheme.typography.titleSmall)
        Slider(value = s.sigFigs.toFloat(), onValueChange = { app.setSigFigs(it.toInt()) }, valueRange = 3f..8f, steps = 4)
        HorizontalDivider()
        Text("Report header", style = MaterialTheme.typography.titleSmall)
        var company by rememberSaveable { mutableStateOf(s.company) }
        var engineer by rememberSaveable { mutableStateOf(s.engineer) }
        OutlinedTextField(company, { company = it; app.setCompany(it) }, label = { Text("Company") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(engineer, { engineer = it; app.setEngineer(it) }, label = { Text("Prepared by") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        HorizontalDivider()
        Text("About", style = MaterialTheme.typography.titleSmall)
        Text("MechUltimate ${BuildConfig.VERSION_NAME} · ${Catalog.all.size} calculators")
        Text(DISCLAIMER, style = MaterialTheme.typography.bodySmall)
        Text(
            "Standards data: pipe dimensions (ASME B36.10M/B36.19M) and API 526 orifice areas are included. " +
                "Allowable stresses and flange ratings are entered by the user from their licensed code copy. " +
                "Steam properties: IAPWS-IF97. Psychrometrics: ASHRAE Fundamentals.",
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
