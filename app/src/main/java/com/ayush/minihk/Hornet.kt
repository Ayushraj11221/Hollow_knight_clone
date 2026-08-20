package com.ayush.minihk

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Rect
import kotlin.random.Random

class Hornet(private val sheet: Bitmap) {
    var x = 1400f
    var y = 700f
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
    private val flipMatrix = Matrix()

    private val frameWidth = (sheet.width / 10).coerceAtLeast(1)
    private val frameHeight = (sheet.height / 35).coerceAtLeast(1)

    enum class State { IDLE, JUMP_ANTICIPATION, AIR_DIVE, NEEDLE_THROW, RETREAT }

    fun update(targetX: Float, groundY: Float) {
        facingRight = targetX > x
        stateTimer++

        when (state) {
            State.IDLE -> {
                vx = 0f
                vy = 0f
                if (stateTimer > 70) {
                    stateTimer = 0
                    state = if (Random.nextBoolean()) State.JUMP_ANTICIPATION else State.NEEDLE_THROW
                }
            }
            State.JUMP_ANTICIPATION -> {
                vy = -20f
                vx = if (facingRight) 7f else -7f
                y += vy
                x += vx
                if (y < 350f) {
                    state = State.AIR_DIVE
                    stateTimer = 0
                }
            }
            State.AIR_DIVE -> {
                vy = 24f
                vx = if (facingRight) 18f else -18f
                x += vx
                y += vy
                val bottom = y + frameHeight * 1.8f
                if (bottom >= groundY) {
                    y = groundY - frameHeight * 1.8f
                    state = State.RETREAT
                    stateTimer = 0
                }
            }
            State.NEEDLE_THROW -> {
                vx = 0f
                if (stateTimer > 50) {
                    state = State.RETREAT
                    stateTimer = 0
                }
            }
            State.RETREAT -> {
                vx = if (facingRight) -9f else 9f
                x += vx
                if (stateTimer > 40) {
                    state = State.IDLE
                    stateTimer = 0
                }
            }
        }

        animTimer += 0.25f
        if (animTimer >= 1f) {
            animTimer = 0f
            animFrame = (animFrame + 1) % 6
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
        val col = animFrame % 6

        val srcLeft = (col * frameWidth).coerceIn(0, sheet.width - frameWidth)
        val srcTop = (row * frameHeight).coerceIn(0, sheet.height - frameHeight)
        val srcRect = Rect(srcLeft, srcTop, srcLeft + frameWidth, srcTop + frameHeight)

        val destW = frameWidth * 2.2f
        val destH = frameHeight * 2.2f

        canvas.save()
        if (facingRight) {
            flipMatrix.reset()
            flipMatrix.preScale(-1f, 1f)
            flipMatrix.postTranslate(x + destW, y)
            val subBitmap = Bitmap.createBitmap(sheet, srcRect.left, srcRect.top, srcRect.width(), srcRect.height())
            canvas.drawBitmap(subBitmap, flipMatrix, paint)
        } else {
            val destRect = Rect(x.toInt(), y.toInt(), (x + destW).toInt(), (y + destH).toInt())
            canvas.drawBitmap(sheet, srcRect, destRect, paint)
        }
        canvas.restore()
    }
}
