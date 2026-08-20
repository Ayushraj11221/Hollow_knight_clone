package com.ayush.minihk

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
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
    val maxHealth = 5
    var soul = 33
    val maxSoul = 100

    private val speed = 15f
    private val jumpForce = -29f
    private val gravity = 1.3f

    private var slashTimer = 0
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    private val slashPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(190, 240, 248, 255)
        style = Paint.Style.STROKE
        strokeWidth = 14f
    }

    val width = 160f
    val height = 160f

    fun update(moveDir: Float, jumpRequested: Boolean, attackRequested: Boolean, groundY: Float) {
        vx = moveDir * speed
        x += vx

        if (vx > 0.1f) facingRight = true
        if (vx < -0.1f) facingRight = false

        if (!isGrounded) vy += gravity

        if (jumpRequested && isGrounded) {
            vy = jumpForce
            isGrounded = false
        }

        y += vy

        if (y + height >= groundY) {
            y = groundY - height
            vy = 0f
            isGrounded = true
        }

        if (attackRequested && !isSlashing) {
            isSlashing = true
            slashTimer = 12
        }

        if (isSlashing) {
            slashTimer--
            if (slashTimer <= 0) isSlashing = false
        }
    }

    fun getAttackHitbox(): RectF? {
        if (!isSlashing) return null
        return if (facingRight) {
            RectF(x + width * 0.7f, y, x + width + 100f, y + height)
        } else {
            RectF(x - 100f, y, x + width * 0.3f, y + height)
        }
    }

    fun draw(canvas: Canvas) {
        val dest = RectF(x, y, x + width, y + height)
        val src = Rect(0, 0, sheet.width, sheet.height)

        canvas.save()
        if (!facingRight) {
            canvas.scale(-1f, 1f, dest.centerX(), dest.centerY())
        }
        canvas.drawBitmap(sheet, src, dest, paint)
        canvas.restore()

        if (isSlashing) {
            val slashArc = getAttackHitbox()
            if (slashArc != null) {
                canvas.drawArc(slashArc, if (facingRight) -60f else 120f, 120f, false, slashPaint)
            }
        }
    }
}
