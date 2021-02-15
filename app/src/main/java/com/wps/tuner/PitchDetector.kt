package com.wps.tuner

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import be.tarsos.dsp.AudioDispatcher
import be.tarsos.dsp.io.TarsosDSPAudioFormat
import be.tarsos.dsp.io.TarsosDSPAudioInputStream
import be.tarsos.dsp.io.android.AndroidAudioInputStream
import be.tarsos.dsp.pitch.PitchProcessor

class PitchDetector     /* sampleRate:      The requested sample rate.
     * audioBufferSize: The size of the audio buffer (in samples).
     * bufferOverlap:   The size of the overlap (in samples).
     */(
    private val mSampleRate: Int,
    private val mAudioBufferSize: Int,
    private val mBufferOverlap: Int,
    private val mPp: PitchProcessor
) {
    var dispatcher: AudioDispatcher? = null
    private var audioInputStream: AudioRecord? = null
    private var started = false

    // Initially based on AudioDispatcherFactory.java from TarsosDSP
    private fun initMic(): Boolean {
        if (AudioRecord.getMinBufferSize(
                mSampleRate, AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            ) < 0
        ) {
            return false
        }
        audioInputStream = AudioRecord(
            MediaRecorder.AudioSource.MIC, mSampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            mAudioBufferSize * 2
        )
        if (audioInputStream!!.state != AudioRecord.STATE_INITIALIZED) {
            return false
        }
        val format = TarsosDSPAudioFormat(mSampleRate.toFloat(), 16, 1, true, false)
        val audioStream: TarsosDSPAudioInputStream =
            AndroidAudioInputStream(audioInputStream, format)
        dispatcher = AudioDispatcher(audioStream, mAudioBufferSize, mBufferOverlap)
        return true
    }

    fun start(): Boolean {
        started = false
        if (!initMic()) return started
        dispatcher!!.addAudioProcessor(mPp)
        audioInputStream!!.startRecording()
        if (audioInputStream!!.recordingState != AudioRecord.RECORDSTATE_RECORDING) {
            return started
        }
        Thread(dispatcher, "Audio Dispatcher").start()
        started = true
        return started
    }

    fun started(): Boolean {
        return started
    }

    fun stop() {
        started = false
        if (dispatcher == null) return
        dispatcher!!.stop()
        try {
            audioInputStream!!.stop()
        } catch (e: Exception) {
            /* Sometimes AudioRecord.stop() throws an exception.  I don't know why, but it seems safe
                 to ignore it here since we're done with audioInputStream anyway.
             */
        }
        audioInputStream!!.release()
        audioInputStream = null
    }
}