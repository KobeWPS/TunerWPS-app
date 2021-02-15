package com.wps.tuner

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet

abstract class AbstractCentView : androidx.appcompat.widget.AppCompatImageView {

    private var context1: Context
    protected var paint: Paint? = null
    var width1: Int = 0
    var height1: Int = 0
    var cents = 0f
    var animCents = 0f
    var needleColor1: Int = 0
    protected val mHandler = Handler(Looper.getMainLooper())
    protected var animDuration = 0
    protected var animStartMs: Long = 0
    protected var animEndMs: Long = 0
    protected var animStartCents = 0f
    protected var animEndCents = 0f
    var animating = false

    private val animate: Runnable = object : Runnable {
        override fun run() {
            val now = System.currentTimeMillis()
            val timePos = (now - animStartMs).toFloat() / (animEndMs - animStartMs)
            animCents = animStartCents + (animEndCents - animStartCents) * timePos
            invalidate()
            if (now < animEndMs) mHandler.postDelayed(this, 15) else endAnim()
        }
    }

    constructor (c: Context) : super(c) {
        context1 = c
        initialize()
    }

    /* This and the next constructor are necessary for inflating from XML */
    constructor(c: Context, attrs: AttributeSet?) : super(c, attrs) {
        context1 = c
        initialize()
    }

    constructor(c: Context, attrs: AttributeSet?, defStyle: Int) : super(c, attrs, defStyle) {
        context1 = c
        initialize()
    }

    private fun initialize() {
        paint = Paint()
        paint!!.isAntiAlias = true
        paint!!.strokeCap = Paint.Cap.ROUND
        paint!!.strokeJoin = Paint.Join.ROUND
        paint!!.style = Paint.Style.FILL
        paint!!.isDither = true
        setCents1(0f)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        width1 = w
        height1 = h
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        drawStatic(canvas)
        drawNeedle(canvas) // Draw second, so it's on top
    }

    protected abstract fun drawStatic(canvas: Canvas?)
    protected abstract fun drawNeedle(canvas: Canvas?)
    private fun startAnim(c: Float) {
        endAnim()
        animating = true
        animStartMs = System.currentTimeMillis()
        animEndMs = animStartMs + animDuration
        animStartCents = cents
        animEndCents = c
        animCents = cents // In case View is invalidated before animate.run() is called
        mHandler.post(animate)
    }

    protected fun endAnim() {
        animating = false
        mHandler.removeCallbacks(animate)
    }

    fun setCents1(c: Float) {
        if (c == cents) return
        if (animDuration <= 0) invalidate() else startAnim(c)

        // Don't set until after calling startAnim(); it needs old value of cents
        cents = c
    }

    fun setAnimationDuration(ms: Int) {
        animDuration = ms
    }
    
    protected fun setNeedleColor(c: Int) {
        needleColor1 = c
        invalidate()
    }
}