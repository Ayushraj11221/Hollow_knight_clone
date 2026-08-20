package com.ayush.minihk

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import kotlin.random.Random

class Hornet(private val sheet: Bitmap) {
    var x = 1400f
    var y = 600f
    var vx = 0f
    var vy = 0f
    var health = 100
    val maxHealth = 100
    var facingRight = false
    var state = State.IDLE

    private var stateTimer = 0
    private var animFrame = 0
    private var animTimer = 0f
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)

    private val cols = 10
    private val rows = 25
    private val frameWidth = (sheet.width / cols).coerceAtLeast(1)
    private val frameHeight = (sheet.height / rows).coerceAtLeast(1)

    private val srcRect = Rect()
    private val destRect = RectF()

    enum class State { IDLE, JUMP_ANTICIPATION, AIR_DIVE, NEEDLE_THROW, RETREAT }

    fun update(targetX: Float, groundY: Float) {
        facingRight = targetX > x
        stateTimer++

        when (state) {
            State.IDLE -> {
                vx = 0f
                vy = 0f
                if (stateTimer > 60) {
                    stateTimer = 0
                    state = if (Random.nextBoolean()) State.JUMP_ANTICIPATION else State.NEEDLE_THROW
                }
            }
            State.JUMP_ANTICIPATION -> {
                vy = -22f
                vx = if (facingRight) 8f else -8f
                y += vy
                x += vx
                if (y < 300f) {
                    state = State.AIR_DIVE
                    stateTimer = 0
                }
            }
            State.AIR_DIVE -> {
                vy = 26f
                vx = if (facingRight) 18f else -18f
                x += vx
                y += vy
                val renderHeight = 220f
                if (y + renderHeight >= groundY) {
                    y = groundY - renderHeight
                    state = State.RETREAT
                    stateTimer = 0
                }
            }
            State.NEEDLE_THROW -> {
                vx = 0f
                if (stateTimer > 45) {
                    state = State.RETREAT
                    stateTimer = 0
                }
            }
            State.RETREAT -> {
                vx = if (facingRight) -10f else 10f
                x += vx
                if (stateTimer > 35) {
                    state = State.IDLE
                    stateTimer = 0
                }
            }
        }

        animTimer += 0.25f
        if (animTimer >= 1f) {
            animTimer = 0f
            animFrame = (animFrame + 1) % 5
        }
    }

    fun draw(canvas: Canvas) {
        val row = when (state) {
            State.IDLE -> 0
            State.JUMP_ANTICIPATION -> 2
            State.AIR_DIVE -> 3
            State.NEEDLE_THROW -> 1
            State.RETREAT -> 0
        }
        val col = animFrame % 5

        val srcLeft = (col * frameWidth).coerceIn(0, sheet.width - frameWidth)
        val srcTop = (row * frameHeight).coerceIn(0, sheet.height - frameHeight)
        srcRect.set(srcLeft, srcTop, srcLeft + frameWidth, srcTop + frameHeight)

        val renderWidth = 220f
        val renderHeight = 220f
        destRect.set(x, y, x + renderWidth, y + renderHeight)

        canvas.save()
        if (facingRight) {
            canvas.scale(-1f, 1f, destRect.centerX(), destRect.centerY())
        }
        canvas.drawBitmap(sheet, srcRect, destRect, paint)
        canvas.restore()
    }
}
