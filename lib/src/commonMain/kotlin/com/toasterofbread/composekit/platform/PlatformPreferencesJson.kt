package com.toasterofbread.composekit.platform

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.decodeFromStream
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.float
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long

open class PlatformPreferencesJson(private val file: PlatformFile): PlatformPreferences {
    private val data: MutableMap<String, JsonElement> by lazy {
        loadData()
    }
    private var listeners: MutableList<PlatformPreferencesListener> = mutableListOf()

    private fun onKeyChanged(key: String) {
        for (listener in listeners) {
            listener.onChanged(this, key)
        }
    }

    protected open fun loadData(): MutableMap<String, JsonElement> {
        if (!file.exists) {
            return mutableMapOf()
        }

        return file.inputStream().use { stream ->
            Json.decodeFromStream(stream)
        }
    }
    private fun saveData() {
        file.createFile()
        file.outputStream().writer().use { writer ->
            writer.write(Json.encodeToString(data))
            writer.flush()
        }
    }

    override fun addListener(listener: PlatformPreferencesListener): PlatformPreferencesListener {
        listeners.add(listener)
        return listener
    }

    override fun removeListener(listener: PlatformPreferencesListener) {
        listeners.remove(listener)
    }

    override fun getString(key: String, defValue: String?): String? =
        data.get(key)?.jsonPrimitive?.takeIf { it.isString }?.content ?: defValue

    @Suppress("UNCHECKED_CAST")
    override fun getStringSet(key: String, defValues: Set<String>?): Set<String>? =
        data.get(key)?.jsonArray?.map { it.jsonPrimitive.content }?.toSet() ?: defValues

    override fun getInt(key: String, defValue: Int?): Int? =
        data.get(key)?.jsonPrimitive?.int ?: defValue

    override fun getLong(key: String, defValue: Long?): Long? =
        data.get(key)?.jsonPrimitive?.long ?: defValue

    override fun getFloat(key: String, defValue: Float?): Float? =
        data.get(key)?.jsonPrimitive?.float ?: defValue

    override fun getBoolean(key: String, defValue: Boolean?): Boolean? =
        data.get(key)?.jsonPrimitive?.boolean ?: defValue

    override operator fun contains(key: String): Boolean =
        data.containsKey(key)

    override fun edit(action: PlatformPreferences.Editor.() -> Unit) {
        val changed: MutableSet<String> = mutableSetOf()
        val editor: EditorImpl = EditorImpl(data, changed)
        action(editor)
        saveData()

        for (key in changed) {
            onKeyChanged(key)
        }
    }

    open class EditorImpl(private val data: MutableMap<String, JsonElement>, private val changed: MutableSet<String>): PlatformPreferences.Editor {
        override fun putString(key: String, value: String): PlatformPreferences.Editor {
            data[key] = Json.encodeToJsonElement(value)
            changed.add(key)
            return this
        }

        override fun putStringSet(
            key: String,
            values: Set<String>,
        ): PlatformPreferences.Editor {
            data[key] = Json.encodeToJsonElement(values)
            changed.add(key)
            return this
        }

        override fun putInt(key: String, value: Int): PlatformPreferences.Editor {
            data[key] = Json.encodeToJsonElement(value)
            changed.add(key)
            return this
        }

        override fun putLong(key: String, value: Long): PlatformPreferences.Editor {
            data[key] = Json.encodeToJsonElement(value)
            changed.add(key)
            return this
        }

        override fun putFloat(key: String, value: Float): PlatformPreferences.Editor {
            data[key] = Json.encodeToJsonElement(value)
            changed.add(key)
            return this
        }

        override fun putBoolean(key: String, value: Boolean): PlatformPreferences.Editor {
            data[key] = Json.encodeToJsonElement(value)
            changed.add(key)
            return this
        }

        override fun remove(key: String): PlatformPreferences.Editor {
            data.remove(key)
            changed.add(key)
            return this
        }

        override fun clear(): PlatformPreferences.Editor {
            changed.addAll(data.keys)
            data.clear()
            return this
        }
    }
}
