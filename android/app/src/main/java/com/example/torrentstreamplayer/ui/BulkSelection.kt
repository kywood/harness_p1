package com.example.torrentstreamplayer.ui

internal object BulkSelection {
    fun toggle(selected: Set<String>, key: String, checked: Boolean): Set<String> =
        if (checked) selected + key else selected - key
    fun all(keys: List<String>, selected: Set<String>): Set<String> =
        if (keys.isNotEmpty() && selected.containsAll(keys)) emptySet() else keys.toSet()
    fun delete(keys: Set<String>, action: (String) -> Boolean): Set<String> =
        keys.filter { key ->
            try { action(key) } catch (e: java.util.concurrent.CancellationException) { throw e }
            catch (_: Exception) { false }
        }.toSet()
}
