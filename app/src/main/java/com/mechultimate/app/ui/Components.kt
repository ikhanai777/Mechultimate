package com.mechultimate.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Air
import androidx.compose.material.icons.outlined.Architecture
import androidx.compose.material.icons.outlined.Calculate
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Construction
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material.icons.outlined.OilBarrel
import androidx.compose.material.icons.outlined.Plumbing
import androidx.compose.material.icons.outlined.PrecisionManufacturing
import androidx.compose.material.icons.outlined.PropaneTank
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.Thermostat
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.mechultimate.core.engine.Calculator
import com.mechultimate.core.engine.Module
import com.mechultimate.core.engine.Status
import com.mechultimate.core.units.EngUnit

fun Module.icon(): ImageVector = when (this) {
    Module.PIPING -> Icons.Outlined.Plumbing
    Module.RELIEF -> Icons.Outlined.Speed
    Module.VESSELS -> Icons.Outlined.PropaneTank
    Module.TANKS -> Icons.Outlined.OilBarrel
    Module.INSPECTION -> Icons.Outlined.Visibility
    Module.MACHINE -> Icons.Outlined.PrecisionManufacturing
    Module.STRUCTURES -> Icons.Outlined.Architecture
    Module.FLUIDS -> Icons.Outlined.WaterDrop
    Module.THERMO -> Icons.Outlined.Thermostat
    Module.HVAC -> Icons.Outlined.Air
    Module.WELDING -> Icons.Outlined.LocalFireDepartment
    Module.MANUFACTURING -> Icons.Outlined.Construction
    Module.GENERAL -> Icons.Outlined.Calculate
}

@Composable
fun statusColor(status: Status): Color {
    val dark = isSystemInDarkTheme()
    return when (status) {
        Status.PASS -> if (dark) StatusColors.passDark else StatusColors.pass
        Status.WARN -> if (dark) StatusColors.warnDark else StatusColors.warn
        Status.FAIL -> if (dark) StatusColors.failDark else StatusColors.fail
        Status.INFO -> MaterialTheme.colorScheme.onSurfaceVariant
    }
}

fun Status.icon(): ImageVector = when (this) {
    Status.PASS -> Icons.Outlined.CheckCircle
    Status.WARN -> Icons.Outlined.WarningAmber
    Status.FAIL -> Icons.Outlined.ErrorOutline
    Status.INFO -> Icons.Outlined.Info
}

@Composable
fun StatusBadge(status: Status, modifier: Modifier = Modifier) {
    val label = when (status) { Status.PASS -> "PASS"; Status.WARN -> "CHECK"; Status.FAIL -> "FAIL"; Status.INFO -> "RESULT" }
    val color = statusColor(status)
    Surface(color = color.copy(alpha = 0.14f), contentColor = color, shape = RoundedCornerShape(50), modifier = modifier) {
        Row(Modifier.padding(horizontal = 10.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Icon(status.icon(), contentDescription = null, modifier = Modifier.size(16.dp))
            Text(label, style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
fun CalcListItem(calc: Calculator, favorite: Boolean, onClick: () -> Unit) {
    ListItem(
        modifier = Modifier.clickable(onClick = onClick),
        leadingContent = { Icon(calc.module.icon(), contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
        headlineContent = { Text(calc.title) },
        supportingContent = {
            Text(
                calc.id + "  ·  " + calc.references.firstOrNull()?.let { "${it.code} ${it.clause}".trim() }.orEmpty(),
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
            )
        },
        trailingContent = if (favorite) { { Icon(Icons.Outlined.Star, contentDescription = "Favourite", tint = MaterialTheme.colorScheme.tertiary) } } else null,
    )
}

/** Compact unit selector shown next to numeric fields and results. */
@Composable
fun UnitPicker(units: List<EngUnit>, selected: String, onSelect: (String) -> Unit, modifier: Modifier = Modifier) {
    var open by remember { mutableStateOf(false) }
    if (units.size <= 1) {
        Text(selected, modifier = modifier.padding(horizontal = 8.dp), fontFamily = FontFamily.Monospace)
        return
    }
    TextButton(onClick = { open = true }, modifier = modifier) {
        Text(selected, fontFamily = FontFamily.Monospace)
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            units.forEach { u ->
                DropdownMenuItem(text = { Text("${u.symbol}  —  ${u.name}") }, onClick = { onSelect(u.symbol); open = false })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> Dropdown(label: String, options: List<T>, selected: T, text: (T) -> String, onSelect: (T) -> Unit, modifier: Modifier = Modifier, supporting: String? = null) {
    var open by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = open, onExpandedChange = { open = it }, modifier = modifier) {
        OutlinedTextField(
            value = text(selected),
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            supportingText = supporting?.let { { Text(it) } },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = open) },
            modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable, true),
        )
        ExposedDropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            options.forEach { o ->
                DropdownMenuItem(text = { Text(text(o)) }, onClick = { onSelect(o); open = false })
            }
        }
    }
}
