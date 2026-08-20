package com.example.minihollowknight

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF

class Spell(var x: Float, var y: Float, val movingRight: Boolean) {
    val speed = 26f
    var isActive = true
    val width = 90f
    val height = 45f

    private val outerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(160, 240, 250, 255)
    }
    private val corePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
    }
    private val trailPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(80, 180, 220, 255)
    }

    fun update(screenWidth: Float) {
        x += if (movingRight) speed else -speed
        if (x < -150f || x > screenWidth + 150f) {
            isActive = false
        }
    }

    fun draw(canvas: Canvas) {
        val rect = RectF(x, y, x + width, y + height)
        val trailRect = if (movingRight) {
            RectF(x - 40f, y + 10f, x + width, y + height - 10f)
        } else {
            RectF(x, y + 10f, x + width + 40f, y + height - 10f)
        }

        canvas.drawRoundRect(trailRect, 15f, 15f, trailPaint)
        canvas.drawRoundRect(rect, 25f, 25f, outerPaint)
        canvas.drawRoundRect(RectF(x + 10f, y + 8f, x + width - 10f, y + height - 8f), 15f, 15f, corePaint)
    }
}
