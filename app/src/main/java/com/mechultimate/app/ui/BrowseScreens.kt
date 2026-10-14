package com.mechultimate.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mechultimate.app.data.Settings
import com.mechultimate.core.Catalog
import com.mechultimate.core.engine.Module

@Composable
fun HomeScreen(settings: Settings, onModule: (Module) -> Unit, onCalc: (String) -> Unit, onSearch: () -> Unit, onStandards: () -> Unit) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(160.dp),
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("MechUltimate", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.SemiBold)
                Text("${Catalog.all.size} calculators · ${Catalog.standards.size} codes & references · offline",
                    style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Card(
                    onClick = onSearch,
                    shape = RoundedCornerShape(28.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                ) {
                    Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Icon(Icons.Outlined.Search, contentDescription = null)
                        Text("Search calculators, codes, clauses…", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AssistChip(onClick = onStandards, label = { Text("Browse by standard") }, leadingIcon = { Icon(Icons.AutoMirrored.Outlined.MenuBook, null, Modifier.size(18.dp)) })
                    AssistChip(onClick = { onCalc("THM-001") }, label = { Text("Steam tables") })
                    AssistChip(onClick = { onCalc("PIP-001") }, label = { Text("B31.3 wall") })
                }
            }
        }
        val favs = settings.favorites.mapNotNull { Catalog.get(it) }.sortedBy { it.id }
        if (favs.isNotEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) { SectionTitle("Favourites") }
            items(favs, span = { GridItemSpan(maxLineSpan) }) { CalcListItem(it, true) { onCalc(it.id) } }
        }
        val recents = settings.recents.mapNotNull { Catalog.get(it) }.take(4)
        if (recents.isNotEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) { SectionTitle("Recent") }
            items(recents, span = { GridItemSpan(maxLineSpan) }) { CalcListItem(it, it.id in settings.favorites) { onCalc(it.id) } }
        }
        item(span = { GridItemSpan(maxLineSpan) }) { SectionTitle("Modules") }
        items(Module.entries.filter { Catalog.inModule(it).isNotEmpty() }) { m ->
            Card(onClick = { onModule(m) }, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
                Column(Modifier.padding(16.dp).fillMaxWidth()) {
                    Icon(m.icon(), contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
                    Spacer(Modifier.height(8.dp))
                    Text(m.title, style = MaterialTheme.typography.titleMedium)
                    Text(m.subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2)
                    Spacer(Modifier.height(4.dp))
                    Text("${Catalog.inModule(m).size} tools", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.secondary)
                }
            }
        }
    }
}

@Composable
fun SectionTitle(text: String) =
    Text(text, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 8.dp))

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModuleScreen(module: Module, favorites: Set<String>, onBack: () -> Unit, onCalc: (String) -> Unit) {
    Scaffold(topBar = {
        TopAppBar(
            title = { Text(module.title) },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
        )
    }) { pad ->
        LazyColumn(Modifier.padding(pad)) {
            item { Text(module.subtitle, Modifier.padding(horizontal = 16.dp, vertical = 4.dp), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            items(Catalog.inModule(module)) { CalcListItem(it, it.id in favorites) { onCalc(it.id) } }
        }
    }
}

@Composable
fun SearchScreen(onCalc: (String) -> Unit) {
    var q by rememberSaveable { mutableStateOf("") }
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }
    val results = remember(q) { Catalog.search(q) }
    Column(Modifier.fillMaxSize()) {
        OutlinedTextField(
            value = q, onValueChange = { q = it },
            modifier = Modifier.fillMaxWidth().padding(16.dp).focusRequester(focus),
            placeholder = { Text("e.g. UG-27, nozzle, NPSH, B31.3, steam") },
            leadingIcon = { Icon(Icons.Outlined.Search, null) },
            trailingIcon = { if (q.isNotEmpty()) IconButton(onClick = { q = "" }) { Icon(Icons.Outlined.Close, "Clear") } },
            singleLine = true,
            shape = RoundedCornerShape(28.dp),
        )
        LazyColumn {
            if (q.isBlank()) {
                item { Text("Search by name, code, clause, equation or keyword.", Modifier.padding(16.dp), color = MaterialTheme.colorScheme.onSurfaceVariant) }
                items(Catalog.all) { CalcListItem(it, false) { onCalc(it.id) } }
            } else if (results.isEmpty()) {
                item { Text("No calculators match “$q”.", Modifier.padding(16.dp)) }
            } else {
                items(results) { CalcListItem(it, false) { onCalc(it.id) } }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StandardsScreen(onBack: () -> Unit, onCalc: (String) -> Unit) {
    val expanded = remember { mutableStateMapOf<String, Boolean>() }
    Scaffold(topBar = {
        TopAppBar(
            title = { Text("Codes & Standards") },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
        )
    }) { pad ->
        LazyColumn(Modifier.padding(pad)) {
            item {
                Text(
                    "Calculators implement code equations and cite the clause. Code text is not reproduced — consult your licensed copy.",
                    Modifier.padding(16.dp), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Catalog.standards.forEach { (code, calcs) ->
                item(key = code) {
                    val open = expanded[code] == true
                    ListItem(
                        modifier = Modifier.clickable { expanded[code] = !open },
                        headlineContent = { Text(code) },
                        supportingContent = { Text("${calcs.size} calculator" + if (calcs.size > 1) "s" else "") },
                        trailingContent = { Icon(if (open) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore, null) },
                    )
                    if (open) {
                        Box(Modifier.padding(start = 16.dp)) {
                            Column { calcs.forEach { c -> CalcListItem(c, false) { onCalc(c.id) } } }
                        }
                    }
                    HorizontalDivider()
                }
            }
        }
    }
}
