package com.dasoni.musicai

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.view.View

class BeatVisualizerView(context: Context, val beatList: List<Long>) : View(context) {

    private val paint = Paint().apply {
        color = Color.MAGENTA
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        if (beatList.isEmpty()) return

        val margin = 40f
        val availableWidth = width - 2 * margin
        val totalTime = beatList.last() - beatList.first()

        var previousTime = beatList.first()
        var currentX = margin

        beatList.forEachIndexed { index, time ->
            if (index == 0) {
                currentX = margin
            } else {
                val timeGap = time - previousTime
                val gapRatio = timeGap.toFloat() / totalTime
                val gapDistance = gapRatio * availableWidth
                currentX += gapDistance
            }

            val y = height / 2f
            canvas.drawCircle(currentX, y, 20f, paint)

            previousTime = time
        }
    }


}
