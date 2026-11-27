package com.dswork.list.data

import android.content.Context
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate

/**
 * 极简本地存储：SharedPreferences + JSON。
 * items 是 Compose 可观察列表，任何增删改都会立即刷新界面并落盘。
 */
class TodoStore(context: Context) {

    private val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    private var idSeed: Long = prefs.getLong(KEY_SEED, System.currentTimeMillis())

    val items: SnapshotStateList<Todo> = mutableStateListOf<Todo>().also { list ->
        list.addAll(load())
    }

    /** 数据变化回调（通知调度等需要跟随事项增删改同步）。 */
    var onChange: (() -> Unit)? = null

    /** null 表示用户从未手动切换过，交给系统深色模式决定。 */
    val darkPref: Boolean?
        get() = if (prefs.contains(KEY_DARK)) prefs.getBoolean(KEY_DARK, false) else null

    fun setDark(dark: Boolean) {
        prefs.edit().putBoolean(KEY_DARK, dark).apply()
    }

    fun itemsFor(date: LocalDate): List<Todo> =
        items.filter { it.date == date }.sortedWith(compareBy({ it.minutes }, { it.id }))

    fun add(date: LocalDate, minutes: Int, title: String) {
        items.add(Todo(nextId(), date, minutes.coerceIn(0, 1439), title.trim()))
        persist()
    }

    fun update(todo: Todo) {
        val index = items.indexOfFirst { it.id == todo.id }
        if (index >= 0) {
            items[index] = todo.copy(title = todo.title.trim())
            persist()
        }
    }

    fun toggle(id: Long) {
        val index = items.indexOfFirst { it.id == id }
        if (index >= 0) {
            items[index] = items[index].copy(done = !items[index].done)
            persist()
        }
    }

    fun delete(id: Long) {
        items.removeAll { it.id == id }
        persist()
    }

    private fun nextId(): Long {
        idSeed += 1
        prefs.edit().putLong(KEY_SEED, idSeed).apply()
        return idSeed
    }

    private fun persist() {
        val array = JSONArray()
        items.forEach { todo ->
            array.put(
                JSONObject()
                    .put("id", todo.id)
                    .put("date", todo.date.toEpochDay())
                    .put("minutes", todo.minutes)
                    .put("title", todo.title)
                    .put("done", todo.done)
            )
        }
        prefs.edit().putString(KEY_ITEMS, array.toString()).apply()
        onChange?.invoke()
    }

    private fun load(): List<Todo> {
        val raw = prefs.getString(KEY_ITEMS, null) ?: return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            buildList {
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    add(
                        Todo(
                            id = obj.optLong("id", i.toLong()),
                            date = LocalDate.ofEpochDay(obj.getLong("date")),
                            minutes = obj.getInt("minutes"),
                            title = obj.getString("title"),
                            done = obj.optBoolean("done", false),
                        )
                    )
                }
            }
        }.getOrDefault(emptyList())
    }

    private companion object {
        const val PREFS = "list_store"
        const val KEY_ITEMS = "items"
        const val KEY_DARK = "dark_theme"
        const val KEY_SEED = "id_seed"
    }
}
