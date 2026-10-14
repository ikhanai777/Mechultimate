package com.mechultimate.app.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.mechultimate.core.units.UnitSystem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

private val Context.dataStore by preferencesDataStore(name = "settings")

data class Settings(
    val unitSystem: UnitSystem = UnitSystem.SI,
    val sigFigs: Int = 4,
    val disclaimerAccepted: Boolean = false,
    val favorites: Set<String> = emptySet(),
    val recents: List<String> = emptyList(),
    val company: String = "",
    val engineer: String = "",
)

class SettingsRepository(private val context: Context) {
    private object Keys {
        val units = stringPreferencesKey("unit_system")
        val sig = intPreferencesKey("sig_figs")
        val disclaimer = booleanPreferencesKey("disclaimer_v1")
        val favorites = stringSetPreferencesKey("favorites")
        val recents = stringPreferencesKey("recents")
        val company = stringPreferencesKey("company")
        val engineer = stringPreferencesKey("engineer")
    }

    val settings: Flow<Settings> = context.dataStore.data.map { p ->
        Settings(
            unitSystem = p[Keys.units]?.let { runCatching { UnitSystem.valueOf(it) }.getOrNull() } ?: UnitSystem.SI,
            sigFigs = p[Keys.sig] ?: 4,
            disclaimerAccepted = p[Keys.disclaimer] ?: false,
            favorites = p[Keys.favorites] ?: emptySet(),
            recents = p[Keys.recents]?.split(',')?.filter { it.isNotBlank() } ?: emptyList(),
            company = p[Keys.company] ?: "",
            engineer = p[Keys.engineer] ?: "",
        )
    }

    suspend fun setUnitSystem(s: UnitSystem) = context.dataStore.edit { it[Keys.units] = s.name }
    suspend fun setSigFigs(n: Int) = context.dataStore.edit { it[Keys.sig] = n.coerceIn(3, 8) }
    suspend fun acceptDisclaimer() = context.dataStore.edit { it[Keys.disclaimer] = true }
    suspend fun setCompany(v: String) = context.dataStore.edit { it[Keys.company] = v }
    suspend fun setEngineer(v: String) = context.dataStore.edit { it[Keys.engineer] = v }

    suspend fun toggleFavorite(id: String) = context.dataStore.edit {
        val cur = it[Keys.favorites] ?: emptySet()
        it[Keys.favorites] = if (id in cur) cur - id else cur + id
    }

    suspend fun pushRecent(id: String) = context.dataStore.edit {
        val cur = it[Keys.recents]?.split(',')?.filter { s -> s.isNotBlank() } ?: emptyList()
        it[Keys.recents] = (listOf(id) + cur.filter { s -> s != id }).take(8).joinToString(",")
    }
}

/** A saved calculation: calculator id plus its inputs in SI. */
@Serializable
data class SavedCalc(
    val uid: String,
    val calcId: String,
    val title: String,
    val project: String,
    val numbers: Map<String, Double>,
    val choices: Map<String, String>,
    val savedAt: Long,
    val revision: Int = 0,
)

/** Stores saved calculations as a single JSON file in app-private storage. */
class SavedCalcRepository(context: Context) {
    private val file = File(context.filesDir, "saved_calcs.json")
    private val json = Json { ignoreUnknownKeys = true; prettyPrint = false }
    private val mutex = Mutex()
    private val _items = MutableStateFlow<List<SavedCalc>>(emptyList())
    val items: StateFlow<List<SavedCalc>> = _items.asStateFlow()

    suspend fun load() = withContext(Dispatchers.IO) {
        mutex.withLock {
            _items.value = if (file.exists()) {
                runCatching { json.decodeFromString<List<SavedCalc>>(file.readText()) }.getOrDefault(emptyList())
            } else emptyList()
        }
    }

    suspend fun upsert(item: SavedCalc) = update { list ->
        val existing = list.firstOrNull { it.uid == item.uid }
        val next = if (existing != null) item.copy(revision = existing.revision + 1) else item
        listOf(next) + list.filter { it.uid != item.uid }
    }

    suspend fun delete(uid: String) = update { list -> list.filter { it.uid != uid } }

    fun get(uid: String): SavedCalc? = _items.value.firstOrNull { it.uid == uid }

    private suspend fun update(transform: (List<SavedCalc>) -> List<SavedCalc>) = withContext(Dispatchers.IO) {
        mutex.withLock {
            val next = transform(_items.value)
            val tmp = File(file.parentFile, file.name + ".tmp")
            tmp.writeText(json.encodeToString(next))
            tmp.renameTo(file)
            _items.value = next
        }
    }
}
