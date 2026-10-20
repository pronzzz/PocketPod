package io.github.pronzzz.wheelhouse.data

import android.app.ActivityManager
import android.content.Context
import android.content.SharedPreferences

/** All user settings in one place, backed by SharedPreferences. */
class Prefs(context: Context) {

    val sp: SharedPreferences = context.getSharedPreferences("wheelhouse", Context.MODE_PRIVATE)

    private val lowRamDevice: Boolean = run {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        am.isLowRamDevice || am.memoryClass <= 96
    }

    // Appearance
    var skin: String by StringPref("skin", "silver")
    var darkScreen: Boolean by BoolPref("dark_screen", false)
    var backlight: Int by IntPref("backlight", -1) // -1 = follow system, else 10..100

    // Wheel
    var clickSound: Boolean by BoolPref("click_sound", true)
    var haptics: Boolean by BoolPref("haptics", true)
    var wheelSensitivity: Int by IntPref("wheel_sensitivity", 1) // 0 low, 1 medium, 2 high

    // Appliance behaviour
    var immersive: Boolean by BoolPref("immersive", true)
    var awakeWhileCharging: Boolean by BoolPref("awake_charging", true)
    var autoNowPlaying: Boolean by BoolPref("auto_now_playing", true)
    var showApps: Boolean by BoolPref("show_apps", true)
    var liteMode: Boolean by BoolPref("lite_mode", lowRamDevice)
    var homePromptShown: Boolean by BoolPref("home_prompt_shown", false)

    // Playback
    var shuffle: Boolean by BoolPref("shuffle", false)
    var repeatMode: Int by IntPref("repeat", 0) // 0 off, 1 one, 2 all

    // Saved queue, restored (paused) after a reboot or app restart
    var queueIds: String by StringPref("queue_ids", "")
    var queueIndex: Int by IntPref("queue_index", 0)
    var queuePosition: Long by LongPref("queue_position", 0L)

    fun resetAppearanceAndBehaviour() {
        val keep = setOf("queue_ids", "queue_index", "queue_position", "home_prompt_shown")
        val e = sp.edit()
        sp.all.keys.filterNot { it in keep }.forEach { e.remove(it) }
        e.apply()
    }

    private inner class BoolPref(val key: String, val def: Boolean) {
        operator fun getValue(t: Any, p: kotlin.reflect.KProperty<*>) = sp.getBoolean(key, def)
        operator fun setValue(t: Any, p: kotlin.reflect.KProperty<*>, v: Boolean) = sp.edit().putBoolean(key, v).apply()
    }

    private inner class IntPref(val key: String, val def: Int) {
        operator fun getValue(t: Any, p: kotlin.reflect.KProperty<*>) = sp.getInt(key, def)
        operator fun setValue(t: Any, p: kotlin.reflect.KProperty<*>, v: Int) = sp.edit().putInt(key, v).apply()
    }

    private inner class LongPref(val key: String, val def: Long) {
        operator fun getValue(t: Any, p: kotlin.reflect.KProperty<*>) = sp.getLong(key, def)
        operator fun setValue(t: Any, p: kotlin.reflect.KProperty<*>, v: Long) = sp.edit().putLong(key, v).apply()
    }

    private inner class StringPref(val key: String, val def: String) {
        operator fun getValue(t: Any, p: kotlin.reflect.KProperty<*>): String = sp.getString(key, def) ?: def
        operator fun setValue(t: Any, p: kotlin.reflect.KProperty<*>, v: String) = sp.edit().putString(key, v).apply()
    }
}
