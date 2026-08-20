package com.ayush.minihk

import android.graphics.*
import kotlin.random.Random

class Hornet(sheet: Bitmap?) {
    var x = 1350f
    var y = 500f
    var vx = 0f
    var vy = 0f
    var health = 150
    val maxHealth = 150
    var facingRight = false
    var state = State.IDLE

    val width = 180f
    val height = 200f

    private var stateTimer = 0
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    private val needlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = 6f
    }

    private var idleFrame: Bitmap? = null
    private var diveFrame: Bitmap? = null
    private var throwFrame: Bitmap? = null

    enum class State { IDLE, PREPARE_JUMP, DIVE_ATTACK, THROW_NEEDLE, RETREAT }

    init {
        if (sheet != null) {
            idleFrame = SpriteHelper.cropFrame(sheet, 0.01f, 0.01f, 0.12f, 0.035f)
            throwFrame = SpriteHelper.cropFrame(sheet, 0.01f, 0.44f, 0.12f, 0.035f)
            diveFrame = SpriteHelper.cropFrame(sheet, 0.01f, 0.64f, 0.14f, 0.040f)
        }
    }

    fun update(targetX: Float, groundY: Float) {
        facingRight = targetX > x
        stateTimer++

        when (state) {
            State.IDLE -> {
                vx = 0f
                if (stateTimer > 45) {
                    stateTimer = 0
                    state = if (Random.nextBoolean()) State.PREPARE_JUMP else State.THROW_NEEDLE
                }
            }
            State.PREPARE_JUMP -> {
                vy = -26f
                vx = if (facingRight) 11f else -11f
                x += vx
                y += vy
                if (y < 200f) {
                    state = State.DIVE_ATTACK
                    stateTimer = 0
                }
            }
            State.DIVE_ATTACK -> {
                vy = 28f
                vx = if (facingRight) 22f else -22f
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
                if (stateTimer > 28) {
                    state = State.IDLE
                    stateTimer = 0
                }
            }
        }
    }

    fun getHitbox(): RectF = RectF(x, y, x + width, y + height)

    fun draw(canvas: Canvas) {
        val dest = RectF(x, y, x + width, y + height)
        val frameToDraw = when (state) {
            State.DIVE_ATTACK -> diveFrame ?: idleFrame
            State.THROW_NEEDLE -> throwFrame ?: idleFrame
            else -> idleFrame
        }

        if (frameToDraw != null) {
            canvas.save()
            if (facingRight) {
                canvas.scale(-1f, 1f, dest.centerX(), dest.centerY())
            }
            canvas.drawBitmap(frameToDraw, null, dest, paint)
            canvas.restore()
        } else {
            paint.style = Paint.Style.FILL
            paint.color = Color.parseColor("#B71C1C")
            canvas.drawOval(RectF(x + 20f, y + 40f, x + width - 20f, y + height), paint)
        }

        if (state == State.THROW_NEEDLE) {
            val needleEndX = if (facingRight) x + 380f else x - 380f
            canvas.drawLine(dest.centerX(), dest.centerY(), needleEndX, dest.centerY(), needlePaint)
        }
    }
}
