package com.wps.tuner

import android.content.ComponentName
import android.content.Intent
import android.content.SharedPreferences
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import android.content.res.Resources
import android.os.Build
import android.os.Bundle
import android.preference.EditTextPreference
import android.preference.ListPreference
import android.preference.PreferenceActivity
import android.preference.PreferenceScreen
import android.view.MenuItem

class SettingsActivity : PreferenceActivity(),
    OnSharedPreferenceChangeListener {
    private var res: Resources? = null
    private var mPreferenceScreen: PreferenceScreen? = null
    private var mSharedPreferences: SharedPreferences? = null

    //private static SharedPreferences sp_store;
    private val pref_screen: String? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val intent = intent
        var res = resources

        // Stranglely disabled by default for API level 14+
        if (Build.VERSION.SDK_INT >= 14) {
            actionBar!!.setHomeButtonEnabled(true)
            actionBar!!.setDisplayHomeAsUpEnabled(true)
        }
        val pm = preferenceManager
        pm.sharedPreferencesName = SETTINGS_FILE
        pm.sharedPreferencesMode = MODE_MULTI_PROCESS
        mSharedPreferences = pm.sharedPreferences
        setPrefScreen(R.xml.settings)
        title = res.getString(R.string.settings_activity_subtitle)

        setEnablednessOfA4Other()
        setA4OtherSummary()
        for (i in PARENTS.indices) setEnablednessOfDeps(i)
        for (i in LIST_PREFS.indices) updateListPrefSummary(
            LIST_PREFS[i]
        )
    }

    private fun setPrefScreen(resource: Int) {
        addPreferencesFromResource(resource)
        mPreferenceScreen = preferenceScreen
    }

    private fun restartThisScreen() {
        val comp = ComponentName(packageName, SettingsActivity::class.java.name)
        val intent = Intent().setComponent(comp)
        startActivity(intent)
        finish()
    }


    override fun onResume() {
        super.onResume()
        mSharedPreferences!!.registerOnSharedPreferenceChangeListener(this)
    }

    override fun onPause() {
        super.onPause()
        mSharedPreferences!!.unregisterOnSharedPreferenceChangeListener(this)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                finish()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String) {
        mSharedPreferences!!.unregisterOnSharedPreferenceChangeListener(this)
        if (key == KEY_A4_HZ) setEnablednessOfA4Other()
        if (key == KEY_A4_HZ_OTHER) {
            val p = mPreferenceScreen!!.findPreference(KEY_A4_HZ_OTHER) as EditTextPreference
            val text = p.text
            if (text.isEmpty()) {
                val default_a4_hz = res!!.getString(R.string.default_a4_hz)
                p.text = default_a4_hz
            }
            val f = text.toFloat()
            if (f < 220) p.text = "220.0" else if (f > 880) p.text = "880.0"
        }
        if (key == KEY_A4_HZ || key == KEY_A4_HZ_OTHER) setA4OtherSummary()
        for (i in PARENTS.indices) {
            if (key == PARENTS[i]) {
                setEnablednessOfDeps(i)
                break
            }
        }
        for (i in LIST_PREFS.indices) {
            if (key == LIST_PREFS[i]) {
                updateListPrefSummary(LIST_PREFS[i])
                break
            }
        }
        mSharedPreferences!!.registerOnSharedPreferenceChangeListener(this)
    }

    private fun setEnablednessOfA4Other() {
        val default_a4_hz = res!!.getString(R.string.default_a4_hz)
        val a4_hz = mSharedPreferences!!.getString(KEY_A4_HZ, default_a4_hz)
        mPreferenceScreen!!.findPreference(KEY_A4_HZ_OTHER).isEnabled = "other" == a4_hz
    }

    private fun setA4OtherSummary() {
        val p = mPreferenceScreen!!.findPreference(KEY_A4_HZ_OTHER) as EditTextPreference
        if (p.isEnabled) {
            p.summary = res!!.getString(R.string.currently_set_to) + p.text
        } else {
            p.summary = res!!.getString(R.string.currently_disabled)
        }
    }

    private fun setEnablednessOfDeps(index: Int) {
        val dependent = mPreferenceScreen!!.findPreference(DEPENDENTS[index])
            ?: return
        dependent.isEnabled = mSharedPreferences!!.getBoolean(PARENTS[index], false)
        updateListPrefSummary(DEPENDENTS[index])
    }

    private fun updateListPrefSummary(key: String) {
        val pref: ListPreference
        pref =
            try { /* Code is simplest elsewhere if we call this on all dependents, but some aren't ListPreferences. */
                mPreferenceScreen!!.findPreference(key) as ListPreference
            } catch (e: ClassCastException) {
                return
            }
        if (pref == null) return
        if (pref.isEnabled) {
            pref.summary = res!!.getString(R.string.currently_set_to) + pref.entry
        } else {
            pref.summary = res!!.getString(R.string.currently_disabled)
        }
    }

    companion object {
        const val SETTINGS_FILE = "com.wps.tuner_preferences"

        const val KEY_A4_HZ = "a4_hz"
        const val KEY_A4_HZ_OTHER = "a4_hz_other"
        const val KEY_FLAT_SHARP_HINT = "flat_sharp_hint"
        private val PARENTS = arrayOf<String>()
        private val DEPENDENTS = arrayOf<String>()
        private val LIST_PREFS = arrayOf(KEY_A4_HZ)

        //private int menu_res = R.menu.settings;
        private val EMPTY_OBJECT_ARRAY = arrayOf<Any>()
        private val EMPTY_CLASS_ARRAY = arrayOf<Class<*>>()
    }
}