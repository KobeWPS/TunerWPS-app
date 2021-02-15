package com.wps.tuner

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import kotlin.math.cos
import kotlin.math.sin

class ArcCentView : AbstractCentView {
    private var needle_len = 0f
    private var needle_base_x = 0f
    private var needle_base_y = 0f

    constructor(c: Context?) : super(c!!)

    /* This and the next constructor are necessary for inflating from XML */
    constructor(c: Context?, attrs: AttributeSet?) : super(c!!, attrs)
    constructor(c: Context?, attrs: AttributeSet?, defStyle: Int) : super(c!!, attrs, defStyle)

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        needle_len = h * NEEDLE_LEN
        needle_base_x = (width / 2).toFloat()
        needle_base_y = height * (1 - NEEDLE_BASE_Y)
    }

    override fun drawNeedle(canvas: Canvas?) {
        val c: Float // deleted =
        if (animating) c = animCents else c = cents
        paint!!.color = needleColor1
        paint!!.strokeWidth = height * NEEDLE_WIDTH
        val theta = MAX_ANGLE * c / 50
        canvas!!.drawLine(
            needle_base_x,
            needle_base_y,
            needle_base_x + needle_len * sin(theta.toDouble()).toFloat(),
            needle_base_y - needle_len * cos(theta.toDouble()).toFloat(),
            paint!!
        )
    }

    override fun drawStatic(canvas: Canvas?) {
        val sw = height * NEEDLE_WIDTH * 2.5f // original was 2.7f
        paint!!.color = resources.getColor(R.color.static_needle)
        paint!!.strokeWidth = sw
        canvas!!.drawLine(needle_base_x, height * 0.1f, needle_base_x, sw, paint!!)
        drawNeedleBase(canvas)
    }

    private fun drawNeedleBase(canvas: Canvas?) {
        paint!!.color = needleColor1
        canvas!!.drawCircle(
            needle_base_x, needle_base_y, NEEDLE_BASE_DISC_RADIUS * height,
            paint!!
        )
        paint!!.strokeWidth = height * NEEDLE_WIDTH * 1.5f
        paint!!.style = Paint.Style.STROKE
/*
        // this circle is just for show, looks a bit nicer on broader screens but otherwise redundent.
        canvas.drawCircle(
            needle_base_x, needle_base_y, NEEDLE_BASE_CIRC_RADIUS * height,
            paint!!
        )

 */
        paint!!.style = Paint.Style.FILL
    }

    companion object {
        private const val MAX_ANGLE = 22.0f / 180 * Math.PI.toFloat()

        // Constants as ratio of View height
        private const val NEEDLE_LEN = 0.85f
        private const val NEEDLE_WIDTH = 0.005f
        private const val NEEDLE_BASE_Y = 0.1f
        private const val NEEDLE_BASE_DISC_RADIUS = 0.015f
        private const val NEEDLE_BASE_CIRC_RADIUS = 0.03f // needed if the other circle is wanted.
    }
}