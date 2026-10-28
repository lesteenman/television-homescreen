package me.steenman.tvlauncher.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "launcher")

/** Ordered list of favorite apps, stored as flattened component names. */
class FavoritesStore(private val context: Context) {

    val favorites: Flow<List<String>> = context.dataStore.data.map { prefs ->
        prefs[KEY].orEmpty().split(SEPARATOR).filter { it.isNotBlank() }
    }

    suspend fun add(key: String) = update { if (key in it) it else it + key }

    suspend fun remove(key: String) = update { it - key }

    suspend fun removePackage(packageName: String) =
        update { list -> list.filterNot { it.startsWith("$packageName/") } }

    /** Swaps [key] with its neighbour; [offset] is -1 (left) or +1 (right). */
    suspend fun move(key: String, offset: Int) = update { list ->
        val from = list.indexOf(key)
        val to = from + offset
        if (from < 0 || to !in list.indices) {
            list
        } else {
            list.toMutableList().apply { add(to, removeAt(from)) }
        }
    }

    private suspend fun update(transform: (List<String>) -> List<String>) {
        context.dataStore.edit { prefs ->
            val current = prefs[KEY].orEmpty().split(SEPARATOR).filter { it.isNotBlank() }
            prefs[KEY] = transform(current).joinToString(SEPARATOR)
        }
    }

    private companion object {
        val KEY = stringPreferencesKey("favorites")
        const val SEPARATOR = "\n"
    }
}
