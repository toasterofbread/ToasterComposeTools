package dev.toastbits.composekit.platform

import kotlin.properties.PropertyDelegateProvider
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.floatOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.int
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import kotlinx.serialization.KSerializer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import dev.toastbits.composekit.utils.composable.OnChangedEffect

@Suppress("IMPLICIT_CAST_TO_ANY", "UNCHECKED_CAST")
abstract class PreferencesGroup(
    open val group_key: String?,
    val prefs: PlatformPreferences
) {
    protected open fun getUnregisteredProperties(): List<PreferencesProperty<*>> = emptyList()

    fun getAllProperties(): List<PreferencesProperty<*>> = all_properties + getUnregisteredProperties()

    protected inline fun <reified T: Any> property(
        noinline getName: () -> String,
        noinline getDescription: () -> String?,
        noinline getDefaultValue: () -> T,
        noinline isHidden: () -> Boolean = { false }
    ): PropertyDelegateProvider<Any?, PreferencesProperty<T>> {
        val defaultValueProvider: () -> T = getDefaultValue
        return PropertyDelegateProvider { _, property ->
            check(T::class !is Enum<*>) { "Enum property '$property' must use enumProperty()" }

            val property: PreferencesProperty<T> =
                object : PrefsProperty<T>(key = property.name) {
                    override val name: String get() = getName()
                    override val description: String? get() = getDescription()

                    override fun getDefaultValue(): T = defaultValueProvider()
                    override fun isHidden(): Boolean = isHidden()
                }
            onPropertyAdded(property)
            return@PropertyDelegateProvider property
        }
    }

    protected inline fun <reified T: Enum<T>> enumProperty(
        noinline getName: () -> String,
        noinline getDescription: () -> String?,
        noinline getDefaultValue: () -> T,
        noinline isHidden: () -> Boolean = { false }
    ): PropertyDelegateProvider<Any?, PreferencesProperty<T>> {
        val defaultValueProvider: () -> T = getDefaultValue
        return PropertyDelegateProvider { _, property ->
            val property: PreferencesProperty<T> =
                object : EnumPrefsProperty<T>(
                    key = property.name,
                    entries = enumValues<T>().toList()
                ) {
                    override val name: String get() = getName()
                    override val description: String? get() = getDescription()

                    override fun getDefaultValue(): T = defaultValueProvider()
                    override fun isHidden(): Boolean = isHidden()
                }
            onPropertyAdded(property)
            return@PropertyDelegateProvider property
        }
    }

    protected inline fun <reified T: Any> serialisableProperty(
        noinline getName: () -> String,
        noinline getDescription: () -> String?,
        noinline getDefaultValue: () -> T
    ): PropertyDelegateProvider<Any?, PreferencesProperty<T>> {
        val defaultValueProvider: () -> T = getDefaultValue
        return PropertyDelegateProvider { _, property ->
            val property: PreferencesProperty<T> =
                object : SerialisablePrefsProperty<T>(
                    key = property.name,
                    serialiser = serializer<T>()
                ) {
                    override val name: String get() = getName()
                    override val description: String? get() = getDescription()

                    override fun getDefaultValue(): T = defaultValueProvider()
                }
            onPropertyAdded(property)
            return@PropertyDelegateProvider property
        }
    }

    protected inline fun <reified T: Any> nullableSerialisableProperty(
        noinline getName: () -> String,
        noinline getDescription: () -> String?,
        noinline getDefaultValue: () -> T?
    ): PropertyDelegateProvider<Any?, PreferencesProperty<T?>> {
        val defaultValueProvider: () -> T? = getDefaultValue
        return PropertyDelegateProvider { _, property ->
            val property: PreferencesProperty<T?> =
                object : SerialisablePrefsProperty<T?>(
                    key = property.name,
                    serialiser = serializer<T?>()
                ) {
                    override val name: String get() = getName()
                    override val description: String? get() = getDescription()

                    override fun getDefaultValue(): T? = defaultValueProvider()
                }
            onPropertyAdded(property)
            return@PropertyDelegateProvider property
        }
    }

    private val all_properties: MutableList<PreferencesProperty<*>> = mutableListOf()

    fun onPropertyAdded(property: PreferencesProperty<*>) {
        all_properties.add(property)
    }

    private fun formatPropertyKey(property_key: String): String {
        if (group_key == null) {
            return property_key
        }
        return group_key + "_" + property_key
    }

    @Suppress("UNCHECKED_CAST")
    protected abstract inner class PrefsProperty<T>(key: String): PreferencesProperty<T> {
        override val key: String = formatPropertyKey(key)

        override fun get(): T =
            when (val default_value: T = getDefaultValue()) {
                is Boolean -> prefs.getBoolean(key, default_value)
                is Float -> prefs.getFloat(key, default_value)
                is Int -> prefs.getInt(key, default_value)
                is Long -> prefs.getLong(key, default_value)
                is String -> prefs.getString(key, default_value)
                is Set<*> -> prefs.getStringSet(key, default_value as Set<String>)
                is Enum<*> -> throw IllegalStateException("Use EnumPrefsProperty")
                else -> throw NotImplementedError("$key $default_value ${default_value!!::class.simpleName}")
            } as T

        @Suppress("UNNECESSARY_NOT_NULL_ASSERTION")
        override fun set(value: T, editor: PlatformPreferences.Editor?) =
            (editor ?: prefs).edit {
                when (value) {
                    null -> remove(key)
                    is Boolean -> putBoolean(key, value)
                    is Float -> putFloat(key, value)
                    is Int -> putInt(key, value)
                    is Long -> putLong(key, value)
                    is String -> putString(key, value)
                    is Set<*> -> putStringSet(key, value as Set<String>)
                    is Enum<*> -> throw IllegalStateException("Use EnumPrefsProperty")
                    else -> throw NotImplementedError("$key ${value!!::class.simpleName}")
                }
            }

        override fun set(data: JsonElement, editor: PlatformPreferences.Editor?) =
            when (data) {
                is JsonArray -> set(data.map { it.jsonPrimitive.content }.toSet() as T, editor)
                is JsonPrimitive -> {
                    val value: T = (
                        data.booleanOrNull
                        ?: data.intOrNull
                        ?: data.longOrNull
                        ?: data.floatOrNull
                        ?: data.contentOrNull
                    ) as T

                    set(value, editor)
                }
                is JsonObject -> throw IllegalStateException("PrefsProperty ($this) data is JsonObject ($data)")
            }

        override fun reset() =
            prefs.edit {
                remove(key)
            }

        override fun serialise(value: Any?): JsonElement =
            when (value) {
                null -> JsonPrimitive(null)
                is Boolean -> JsonPrimitive(value)
                is Float -> JsonPrimitive(value)
                is Int -> JsonPrimitive(value)
                is Long -> JsonPrimitive(value)
                is String -> JsonPrimitive(value)
                is Set<*> -> JsonArray((value as Set<String>).map { JsonPrimitive(it) })
                is Enum<*> -> throw IllegalStateException("Use EnumPrefsProperty")
                else -> throw NotImplementedError("$key ${value::class.simpleName}")
            }

        @Composable
        override fun observe(): MutableState<T> {
            val state: MutableState<T> = remember { mutableStateOf(get()) }
            var set_to: T by remember { mutableStateOf(state.value) }

            LaunchedEffect(state.value) {
                if (state.value != set_to) {
                    set_to = state.value
                    set(set_to)
                }
            }

            OnChangedEffect(this) {
                state.value = get()
            }

            DisposableEffect(this) {
                val listener: PlatformPreferencesListener =
                    prefs.addListener(
                        PlatformPreferencesListener { _, key ->
                            if (key == this@PrefsProperty.key) {
                                set_to = get()
                                state.value = set_to
                            }
                        }
                    )

                onDispose {
                    prefs.removeListener(listener)
                }
            }

            return state
        }

        override fun toString(): String =
            "PrefsProperty<T>(key=$key, name=$name, description=$description)"
    }

    protected abstract inner class EnumPrefsProperty<T: Enum<T>>(
        key: String,
        val entries: List<T>
    ): PrefsProperty<T>(key) {
        override fun get(): T =
            entries[prefs.getInt(key, getDefaultValue().ordinal)!!]

        override fun set(value: T, editor: PlatformPreferences.Editor?) =
            (editor ?: prefs).edit {
                putInt(key, value.ordinal)
            }

        override fun set(data: JsonElement, editor: PlatformPreferences.Editor?) =
            set(entries[data.jsonPrimitive.int], editor)

        override fun serialise(value: Any?): JsonElement =
            JsonPrimitive((value as T?)?.ordinal)

        override fun toString(): String =
            "EnumPrefsProperty(key=$key, name=$name, description=$description)"
    }

    protected abstract inner class SerialisablePrefsProperty<T>(
        key: String,
        val serialiser: KSerializer<T>
    ): PrefsProperty<T>(key) {
        override fun get(): T =
            prefs.getSerialisable(key, getDefaultValue(), serialiser)

        override fun set(value: T, editor: PlatformPreferences.Editor?) =
            (editor ?: prefs).edit {
                putSerialisable(key, value, serialiser)
            }

        override fun set(data: JsonElement, editor: PlatformPreferences.Editor?) =
            set(
                Json {
                    ignoreUnknownKeys = true
                    explicitNulls = false
                }.decodeFromJsonElement(serialiser, data),
                editor
            )

        override fun serialise(value: Any?): JsonElement =
            Json.encodeToJsonElement(serialiser, value as T)

        override fun toString(): String =
            "SerialisablePrefsProperty(key=$key, name=$name, description=$description)"
    }
}

private fun Any.edit(action: PlatformPreferences.Editor.() -> Unit) {
    if (this is PlatformPreferences.Editor) {
        action(this)
    }
    else if (this is PlatformPreferences) {
        edit {
            action(this)
        }
    }
    else {
        throw NotImplementedError(this::class.toString())
    }
}
