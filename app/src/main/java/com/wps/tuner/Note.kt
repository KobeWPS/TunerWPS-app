/*
    Copyright (c) 2015 Darshan Computing, LLC

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

import kotlin.math.abs
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.roundToInt

/*
  TarsosDSP's apparent range of what it can recognize:
  Low:  F1      @ ~43.6535
  High: G♯7/A♭7 @ ~3322.44

  (TODO: Look into this and tweak library code?)
*/
class Note {
    var name: String? = null
        private set
    var cents = 0f
        private set
    var isNull = false
        private set
    private var lastHz = 0f
    private var lastCents // Last actual measured cents, so we don't keep averaging with last average
            = 0f
    private var a4Hz: Float

    constructor() {
        a4Hz = 440.0f
        fromHz(-1f)
    }

    constructor(a4: Float) {
        a4Hz = a4
        fromHz(-1f)
    }

    // Sets / updates note; if same note (same name and same octave), averages last and current cents
    fun fromHz(hz: Float) {
        if (hz < 0) {
            isNull = true
            name = "—" //("☺";//"//";//"∅";//"N/A")
            cents = 0f
            return
        }
        isNull = false
        val semi = log2((hz / a4Hz).toDouble().pow(12.0))
        val roundedSemi = semi.roundToInt()
        val note = (roundedSemi % 12 + 12) % 12 // Modulus can be negative in Java
        val newName = notes[note]
        val newCents = (semi - roundedSemi) * 100

        if (newName == name && abs(hz - lastHz) / hz < 0.5) {
            cents =  (lastCents + newCents) / 2
        } else {
            cents = newCents
        }
        name = newName
        lastHz = hz
        lastCents = newCents
    }

    private fun log2(n: Double): Float {
        return (ln(n) / ln(2.0)).toFloat()
    }

    companion object {
        private val notes = arrayOf(
            "A",
            "A♯ / B♭",
            "B",
            "C",
            "C♯ / D♭",
            "D",
            "D♯ / E♭",
            "E",
            "F",
            "F♯ / G♭",
            "G",
            "G♯ / A♭"
        )
    }
}