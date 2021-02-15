/*
    Copyright (c) 2015-2017 Darshan Computing, LLC

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.
*/
package com.wps.tuner

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.app.Dialog
import android.content.ComponentName
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.content.res.Resources
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.preference.PreferenceManager
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import android.widget.Toast
import be.tarsos.dsp.pitch.PitchDetectionHandler
import be.tarsos.dsp.pitch.PitchProcessor
import be.tarsos.dsp.pitch.PitchProcessor.PitchEstimationAlgorithm
import kotlin.math.abs
import kotlin.math.roundToInt

class TunerActivity : Activity() {
    private var settings: SharedPreferences? = null
    private var res: Resources? = null
    private var detector: PitchDetector? = null
    private var pp: PitchProcessor? = null
    private var micUnavailableDialogShowing = false
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        when (requestCode) {
            PR_RECORD_AUDIO -> {
                if (grantResults.size > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    init()
                } else {
                    Toast.makeText(
                        this,
                        res!!.getString(R.string.need_mic_permission),
                        Toast.LENGTH_SHORT
                    ).show()
                    checkMicPermission()
                }
                return
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.home_wrapper)
        res = resources
        settings = PreferenceManager.getDefaultSharedPreferences(this)
        setTitle(R.string.app_full_name)
    }

    private fun checkMicPermission(): Boolean {
        if (Build.VERSION.SDK_INT < 23) return true
        if (checkSelfPermission(P_RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(P_RECORD_AUDIO), PR_RECORD_AUDIO)
            return false
        }
        return true
    }

    private fun init() {
        val centView = findViewById<View>(R.id.cent_view) as AbstractCentView
        centView.setAnimationDuration(1000 / (SAMPLE_RATE / SAMPLES))
        centView.needleColor1 = NULL_NEEDLE_COLOR
        val default_a4_hz = res!!.getString(R.string.default_a4_hz)
        var a4_hz = settings!!.getString(SettingsActivity.KEY_A4_HZ, default_a4_hz)
        if ("other" == a4_hz)
            a4_hz = settings!!.getString(SettingsActivity.KEY_A4_HZ_OTHER, default_a4_hz)
        val a4f = a4_hz!!.toFloat()
        val n = Note(a4f)
        val text = findViewById<View>(R.id.a4Hz) as TextView
        if (a4f.toDouble() != 440.0) {
            text.text = "A4=$a4f Hz"
            text.visibility = View.VISIBLE
        } else {
            text.visibility = View.INVISIBLE
        }
        val pitchInHz_tv = findViewById<View>(R.id.pitchInHz) as TextView
        val note_tv = findViewById<View>(R.id.note) as TextView
        val tooFlat_tv = findViewById<View>(R.id.tooFlat) as TextView
        val tooSharp_tv = findViewById<View>(R.id.tooSharp) as TextView
        tooFlat_tv.visibility = View.INVISIBLE
        tooSharp_tv.visibility = View.INVISIBLE

        // Initially based on http://0110.be/posts/TarsosDSP_on_Android_-_Audio_Processing_in_Java_on_Android
        val pdh = PitchDetectionHandler { result, e ->
            val hz = result.pitch
            n.fromHz(hz)
            runOnUiThread {
                var text: TextView
                val hzStr: String
                val color: Int
                if (n.isNull) {
                    hzStr = "∅ Hz"
                    color = NULL_NEEDLE_COLOR
                    tooFlat_tv.visibility = View.INVISIBLE
                    tooSharp_tv.visibility = View.INVISIBLE
                } else {
                    hzStr = "" + (hz * 10).roundToInt() / 10.0 + " Hz"
                    if (abs(n.cents) < IN_TUNE_CENTS) {
                        color = Color.GREEN
                        tooFlat_tv.visibility = View.INVISIBLE
                        tooSharp_tv.visibility = View.INVISIBLE
                    } else {
                        color = Color.YELLOW
                        if (settings!!.getBoolean(
                                        SettingsActivity.KEY_FLAT_SHARP_HINT,
                                false
                            )
                        ) {
                            if (n.cents < 0) {
                                tooFlat_tv.visibility = View.VISIBLE
                                tooSharp_tv.visibility = View.INVISIBLE
                            } else {
                                tooFlat_tv.visibility = View.INVISIBLE
                                tooSharp_tv.visibility = View.VISIBLE
                            }
                        }
                    }
                }

                //text = (TextView) findViewById(R.id.pitchInHz);
                pitchInHz_tv.text = hzStr
                //text = (TextView) findViewById(R.id.note);
                note_tv.text = "" + n.name
                note_tv.setTextColor(color)
                centView.cents = n.cents
                centView.needleColor1 = color
            }
        }

        // 1.0 multiplier
        // Normal buffer; should be medium response speed, medium ability to detect low pitch
        //   Seems like a very good default still.

        // 2.0 multiplier
        // Large buffer; should be slow response, more able to detect low pitch
        //   Does seem somewhat more accurate at very low pitches.  May be favorable in some
        //    circumstances to have needle move less frequently.  So a worthwhile option to
        //    have while sticking with normal value by default.
        val bufMult = 1.0f
        // if (settings.getBoolean(SettingsActivity.KEY_LARGER_BUFFER, false))
        //     bufMult = 2.0f;
        val bufSize = (SAMPLES * bufMult).toInt()
        pp = PitchProcessor(ALGORITHM, SAMPLE_RATE.toFloat(), bufSize, pdh)
        detector =
            PitchDetector(SAMPLE_RATE, bufSize, 0 /* SAMPLES / 2 */, pp!!)
        checkMicAvailable()
    }

    override fun onStart() {
        super.onStart()
        if (checkMicPermission()) init()
        checkMicAvailable()
    }

    private fun checkMicAvailable() {
        if (detector != null && !detector!!.started() && !detector!!.start()) {
            showDialog(DIALOG_MUST_CLOSE_MIC_UNAVAILABLE)
            micUnavailableDialogShowing = true
        }
    }

    override fun onStop() {
        super.onStop()
        if (micUnavailableDialogShowing) {
            dismissDialog(DIALOG_MUST_CLOSE_MIC_UNAVAILABLE)
            micUnavailableDialogShowing = false
        }
        if (detector != null) detector!!.stop()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater = menuInflater
        inflater.inflate(R.menu.main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_help -> {
                mStartActivity(HelpActivity::class.java)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onCreateDialog(id: Int): Dialog {
        val dialog: Dialog
        val builder = AlertDialog.Builder(this)
        when (id) {
            DIALOG_MUST_CLOSE_MIC_UNAVAILABLE -> {
                builder.setTitle(res!!.getString(R.string.mic_unavailable_title))
                    .setMessage(res!!.getString(R.string.mic_unavailable_message))
                    .setCancelable(false)
                    .setPositiveButton(
                        res!!.getString(R.string.okay)
                    ) { di, id ->
                        finishActivity(1)
                        finish()
                        di.cancel()
                    }
                dialog = builder.create()
            }
            else -> dialog = builder.show()
        }
        return dialog
    }

    private fun mStartActivity(c: Class<*>) {
        val comp = ComponentName(packageName, c.name)
        startActivityForResult(Intent().setComponent(comp), 1)
    }

    companion object {
        // 48000 and 41000 seem to work fine, too, but they don't really seem any better, and 22050 is
        //  noticeably faster on older devices
        private const val SAMPLE_RATE_DEFAULT = 22050
        private const val SAMPLE_RATE = SAMPLE_RATE_DEFAULT
        private const val SAMPLES = SAMPLE_RATE / 5
        private const val IN_TUNE_CENTS = 4.0f

        //private static final float MEDIUM_TUNE_CENTS = 12.0f;
        private const val NULL_NEEDLE_COLOR = -0x333334

        // AMDF and FFT_PITCH are unworkably slow or don't work at all
        private val ALGORITHM = PitchEstimationAlgorithm.FFT_YIN

        //private static final PitchEstimationAlgorithm ALGORITHM = PitchEstimationAlgorithm.DYNAMIC_WAVELET;
        //private static final PitchEstimationAlgorithm ALGORITHM = PitchEstimationAlgorithm.MPM;
        private const val DIALOG_MUST_CLOSE_MIC_UNAVAILABLE = 0
        private const val P_RECORD_AUDIO = Manifest.permission.RECORD_AUDIO
        private const val PR_RECORD_AUDIO = 1
    }
}