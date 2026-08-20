package com.example.minihollowknight

import android.graphics.*
import kotlin.math.abs

class Knight(var x: Float, var y: Float) {
    var vx = 0f
    var vy = 0f
    var isGrounded = false
    var facingRight = true
    var isSlashing = false
    var slashTimer = 0
    var isDashing = false
    var dashTimer = 0
    var hp = 5
    var maxHp = 5
    var invulnerableTimer = 0

    val width = 60f
    val height = 90f

    fun update(groundY: Float, screenWidth: Float) {
        if (invulnerableTimer > 0) invulnerableTimer--

        // Dash Physics
        if (isDashing) {
            dashTimer--
            vx = if (facingRight) 28f else -28f
            vy = 0f
            if (dashTimer <= 0) isDashing = false
        } else {
            // Normal Gravity
            vy += 1.3f
        }

        x += vx
        y += vy

        // Floor collision
        if (y + height / 2f >= groundY) {
            y = groundY - height / 2f
            vy = 0f
            isGrounded = true
        } else {
            isGrounded = false
        }

        x = x.coerceIn(width / 2f, screenWidth - width / 2f)

        if (isSlashing) {
            slashTimer--
            if (slashTimer <= 0) isSlashing = false
        }
    }

    fun jump() {
        if (isGrounded) {
            vy = -25f
            isGrounded = false
        }
    }

    fun dash() {
        if (!isDashing && dashTimer <= 0) {
            isDashing = true
            dashTimer = 12
        }
    }

    fun slash() {
        if (!isSlashing) {
            isSlashing = true
            slashTimer = 10
        }
    }

    fun getHitbox(): RectF {
        return RectF(x - width / 2f, y - height / 2f, x + width / 2f, y + height / 2f)
    }

    fun getSlashHitbox(): RectF? {
        if (!isSlashing) return null
        val slashWidth = 110f
        return if (facingRight) {
            RectF(x, y - 50f, x + slashWidth, y + 50f)
        } else {
            RectF(x - slashWidth, y - 50f, x, y + 50f)
        }
    }

    fun draw(canvas: Canvas, paint: Paint) {
        if (invulnerableTimer % 4 >= 2) return // Flicker on damage

        // Cloak
        paint.style = Paint.Style.FILL
        paint.color = Color.parseColor("#1B1B2F")
        canvas.drawOval(RectF(x - 26f, y - 10f, x + 26f, y + 45f), paint)

        // Head Mask
        paint.color = Color.WHITE
        canvas.drawOval(RectF(x - 22f, y - 50f, x + 22f, y - 5f), paint)

        // Horns
        val hornPath = Path().apply {
            moveTo(x - 16f, y - 40f)
            lineTo(x - 28f, y - 75f)
            lineTo(x - 8f, y - 45f)
            moveTo(x + 16f, y - 40f)
            lineTo(x + 28f, y - 75f)
            lineTo(x + 8f, y - 45f)
        }
        canvas.drawPath(hornPath, paint)

        // Void Eyes
        paint.color = Color.BLACK
        val eyeOffset = if (facingRight) 4f else -4f
        canvas.drawOval(RectF(x - 14f + eyeOffset, y - 35f, x - 4f + eyeOffset, y - 18f), paint)
        canvas.drawOval(RectF(x + 4f + eyeOffset, y - 35f, x + 14f + eyeOffset, y - 18f), paint)

        // Nail Slash Arc
        if (isSlashing) {
            val slashPaint = Paint().apply {
                isAntiAlias = true
                style = Paint.Style.STROKE
                strokeCap = Paint.Cap.ROUND
            }
            val dir = if (facingRight) 1f else -1f
            val arcRect = RectF(x + dir * 15f - 75f, y - 60f, x + dir * 15f + 75f, y + 50f)

            slashPaint.color = Color.parseColor("#44FFFFFF")
            slashPaint.strokeWidth = 22f
            canvas.drawArc(arcRect, if (facingRight) -60f else 120f, 120f, false, slashPaint)

            slashPaint.color = Color.parseColor("#00E5FF")
            slashPaint.strokeWidth = 6f
            canvas.drawArc(arcRect, if (facingRight) -60f else 120f, 120f, false, slashPaint)

            slashPaint.color = Color.WHITE
            slashPaint.strokeWidth = 2.5f
            canvas.drawArc(arcRect, if (facingRight) -60f else 120f, 120f, false, slashPaint)
        }
    }
}
