package com.ayush.minihk

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Rect

class Knight(private val sheet: Bitmap) {
    var x = 300f
    var y = 700f
    var vx = 0f
    var vy = 0f
    var isGrounded = false
    var facingRight = true
    var isSlashing = false
    var health = 5
    var maxHealth = 5

    private val speed = 12f
    private val jumpForce = -26f
    private val gravity = 1.1f

    private var animTimer = 0f
    private var currentFrame = 0
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    private val flipMatrix = Matrix()

    private val frameWidth = sheet.width / 13
    private val frameHeight = sheet.height / 13

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

        val playerBottom = y + frameHeight * 1.8f
        if (playerBottom >= groundY) {
            y = groundY - frameHeight * 1.8f
            vy = 0f
            isGrounded = true
        }

        if (attackRequested && !isSlashing) {
            isSlashing = true
            animTimer = 0f
            currentFrame = 0
        }

        animTimer += 0.2f
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
        val col = currentFrame % 6

        val srcLeft = (col * frameWidth).coerceIn(0, sheet.width - frameWidth)
        val srcTop = (row * frameHeight).coerceIn(0, sheet.height - frameHeight)
        val srcRect = Rect(srcLeft, srcTop, srcLeft + frameWidth, srcTop + frameHeight)

        val destW = frameWidth * 2.2f
        val destH = frameHeight * 2.2f

        canvas.save()
        if (!facingRight) {
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
