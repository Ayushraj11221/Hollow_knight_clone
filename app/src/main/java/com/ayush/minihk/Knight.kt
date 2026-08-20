package com.ayush.minihk

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF

class Knight(private val sheet: Bitmap) {
    var x = 300f
    var y = 600f
    var vx = 0f
    var vy = 0f
    var isGrounded = false
    var facingRight = true
    var isSlashing = false
    var health = 5
    var maxHealth = 5

    private val speed = 14f
    private val jumpForce = -28f
    private val gravity = 1.2f

    private var animTimer = 0f
    private var currentFrame = 0
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)

    private val cols = 10
    private val rows = 10
    private val frameWidth = (sheet.width / cols).coerceAtLeast(1)
    private val frameHeight = (sheet.height / rows).coerceAtLeast(1)

    private val srcRect = Rect()
    private val destRect = RectF()

    fun update(moveDir: Float, jumpRequested: Boolean, attackRequested: Boolean, groundY: Float) {
        vx = moveDir * speed
        x += vx

        if (vx > 0.1f) facingRight = true
        if (vx < -0.1f) facingRight = false

        if (!isGrounded) {
            vy += gravity
        }

        if (jumpRequested && isGrounded) {
            vy = jumpForce
            isGrounded = false
        }

        y += vy

        val renderHeight = 180f
        if (y + renderHeight >= groundY) {
            y = groundY - renderHeight
            vy = 0f
            isGrounded = true
        }

        if (attackRequested && !isSlashing) {
            isSlashing = true
            animTimer = 0f
            currentFrame = 0
        }

        animTimer += 0.25f
        if (animTimer >= 1f) {
            animTimer = 0f
            currentFrame++
            if (isSlashing && currentFrame >= 4) {
                isSlashing = false
                currentFrame = 0
            }
        }
    }

    fun draw(canvas: Canvas) {
        val row = if (isSlashing) 4 else if (!isGrounded) 3 else if (kotlin.math.abs(vx) > 0.1f) 1 else 0
        val col = currentFrame % 5

        val srcLeft = (col * frameWidth).coerceIn(0, sheet.width - frameWidth)
        val srcTop = (row * frameHeight).coerceIn(0, sheet.height - frameHeight)
        srcRect.set(srcLeft, srcTop, srcLeft + frameWidth, srcTop + frameHeight)

        val renderWidth = 180f
        val renderHeight = 180f
        destRect.set(x, y, x + renderWidth, y + renderHeight)

        canvas.save()
        if (!facingRight) {
            canvas.scale(-1f, 1f, destRect.centerX(), destRect.centerY())
        }
        canvas.drawBitmap(sheet, srcRect, destRect, paint)
        canvas.restore()
    }
}
