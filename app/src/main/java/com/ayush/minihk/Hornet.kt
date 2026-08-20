package com.ayush.minihk

import android.graphics.*
import kotlin.random.Random

class Hornet(private val sheet: Bitmap) {
    var x = 1400f
    var y = 500f
    var vx = 0f
    var vy = 0f
    var health = 150
    val maxHealth = 150
    var facingRight = false
    var state = State.IDLE

    val width = 190f
    val height = 210f

    private var stateTimer = 0
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    private val needlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = 6f
    }

    enum class State { IDLE, PREPARE_JUMP, DIVE_ATTACK, THROW_NEEDLE, RETREAT }

    fun update(targetX: Float, groundY: Float) {
        facingRight = targetX > x
        stateTimer++

        when (state) {
            State.IDLE -> {
                vx = 0f
                if (stateTimer > 50) {
                    stateTimer = 0
                    state = if (Random.nextBoolean()) State.PREPARE_JUMP else State.THROW_NEEDLE
                }
            }
            State.PREPARE_JUMP -> {
                vy = -24f
                vx = if (facingRight) 10f else -10f
                x += vx
                y += vy
                if (y < 220f) {
                    state = State.DIVE_ATTACK
                    stateTimer = 0
                }
            }
            State.DIVE_ATTACK -> {
                vy = 26f
                vx = if (facingRight) 20f else -20f
                x += vx
                y += vy
                if (y + height >= groundY) {
                    y = groundY - height
                    state = State.RETREAT
                    stateTimer = 0
                }
            }
            State.THROW_NEEDLE -> {
                vx = 0f
                if (stateTimer > 40) {
                    state = State.RETREAT
                    stateTimer = 0
                }
            }
            State.RETREAT -> {
                vx = if (facingRight) -12f else 12f
                x += vx
                if (stateTimer > 30) {
                    state = State.IDLE
                    stateTimer = 0
                }
            }
        }
    }

    fun getHitbox(): RectF = RectF(x, y, x + width, y + height)

    fun draw(canvas: Canvas) {
        val dest = RectF(x, y, x + width, y + height)
        val src = Rect(0, 0, sheet.width, sheet.height)

        canvas.save()
        if (facingRight) {
            canvas.scale(-1f, 1f, dest.centerX(), dest.centerY())
        }
        canvas.drawBitmap(sheet, src, dest, paint)
        canvas.restore()

        if (state == State.THROW_NEEDLE) {
            val needleEndX = if (facingRight) x + 350f else x - 350f
            canvas.drawLine(dest.centerX(), dest.centerY(), needleEndX, dest.centerY(), needlePaint)
        }
    }
}
