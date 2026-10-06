package com.liam.kaptalismusaufhalter.security

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/**
 * Local, size-capped record of security events (newest first). Lives next to [SecurityPrefs] so it
 * never touches the Room schema, and never leaves the device.
 */
class SecurityLog(context: Context) {

    private val prefs = SecurityPrefs(context).prefs

    // SecurityLog instances are created freely (service, worker, UI) - the lock must be shared
    // between them, otherwise two threads can read the same list and one of the events is lost.
    fun nextId(now: Long = System.currentTimeMillis()): Long = synchronized(LOCK) {
        val next = maxOf(now, prefs.getLong(KEY_LAST_ID, 0L) + 1)
        prefs.edit().putLong(KEY_LAST_ID, next).apply()
        next
    }

    fun add(event: SecurityEvent) = synchronized(LOCK) {
        val updated = (listOf(event) + all()).take(MAX_EVENTS)
        prefs.edit().putString(KEY_EVENTS, encode(updated)).apply()
    }

    fun all(): List<SecurityEvent> = synchronized(LOCK) {
        prefs.getString(KEY_EVENTS, null)?.let { decode(it) } ?: emptyList()
    }

    fun clear() = synchronized(LOCK) {
        prefs.edit().remove(KEY_EVENTS).apply()
    }

    fun unseenCount(lastSeenAt: Long): Int = all().count { it.timestamp > lastSeenAt && it.severity != Severity.INFO }

    companion object {
        private val LOCK = Any()
        private const val KEY_EVENTS = "events_json"
        private const val KEY_LAST_ID = "events_last_id"
        const val MAX_EVENTS = 100

        internal fun encode(events: List<SecurityEvent>): String {
            val array = JSONArray()
            events.forEach { e ->
                array.put(
                    JSONObject()
                        .put("id", e.id)
                        .put("t", e.timestamp)
                        .put("s", e.severity.name)
                        .put("k", e.kind.name)
                        .put("ti", e.title)
                        .put("d", e.details)
                        .put("p", e.packageName ?: JSONObject.NULL)
                        .put("a", e.settingsAction ?: JSONObject.NULL)
                )
            }
            return array.toString()
        }

        internal fun decode(json: String): List<SecurityEvent> = try {
            val array = JSONArray(json)
            (0 until array.length()).mapNotNull { index ->
                val o = array.optJSONObject(index) ?: return@mapNotNull null
                runCatching {
                    SecurityEvent(
                        id = o.getLong("id"),
                        timestamp = o.getLong("t"),
                        severity = Severity.valueOf(o.getString("s")),
                        kind = EventKind.valueOf(o.getString("k")),
                        title = o.getString("ti"),
                        details = o.getString("d"),
                        packageName = if (o.isNull("p")) null else o.getString("p"),
                        settingsAction = if (o.isNull("a")) null else o.getString("a")
                    )
                }.getOrNull()
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
}
