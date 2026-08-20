package com.example.minihollowknight

import android.graphics.*

class Knight(private val sheet: Bitmap?) {
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

    private val speed = 16f
    private val jumpForce = -30f
    private val gravity = 1.3f

    private var slashTimer = 0
    private var animFrame = 0
    private var animTick = 0

    private var idleFrame: Bitmap? = null
    private var walkFrames = mutableListOf<Bitmap>()
    private var slashFrame: Bitmap? = null

    val width = 140f
    val height = 150f
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    private val slashPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(210, 240, 248, 255)
        style = Paint.Style.STROKE
        strokeWidth = 16f
    }

    init {
        if (sheet != null) {
            idleFrame = SpriteHelper.cropFrame(sheet, 0.01f, 0.01f, 0.07f, 0.07f)
            for (i in 0..3) {
                val f = SpriteHelper.cropFrame(sheet, 0.01f + i * 0.075f, 0.01f, 0.07f, 0.07f)
                if (f != null) walkFrames.add(f)
            }
            slashFrame = SpriteHelper.cropFrame(sheet, 0.15f, 0.32f, 0.10f, 0.08f)
        }
    }

    fun update(moveDir: Float, jumpRequested: Boolean, attackRequested: Boolean, groundY: Float) {
        vx = moveDir * speed
        x += vx

        if (vx > 0.1f) facingRight = true
        if (vx < -0.1f) facingRight = false

        if (Math.abs(vx) > 0.1f && isGrounded) {
            animTick++
            if (animTick % 5 == 0) {
                animFrame = (animFrame + 1) % (walkFrames.size.coerceAtLeast(1))
            }
        } else {
            animFrame = 0
        }

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
            RectF(x + width * 0.6f, y, x + width + 130f, y + height)
        } else {
            RectF(x - 130f, y, x + width * 0.4f, y + height)
        }
    }

    fun draw(canvas: Canvas) {
        val dest = RectF(x, y, x + width, y + height)
        val frameToDraw = when {
            isSlashing && slashFrame != null -> slashFrame
            Math.abs(vx) > 0.1f && walkFrames.isNotEmpty() -> walkFrames[animFrame]
            else -> idleFrame
        }

        if (frameToDraw != null) {
            canvas.save()
            if (!facingRight) {
                canvas.scale(-1f, 1f, dest.centerX(), dest.centerY())
            }
            canvas.drawBitmap(frameToDraw, null, dest, paint)
            canvas.restore()
        } else {
            paint.style = Paint.Style.FILL
            paint.color = Color.parseColor("#1B1B2F")
            canvas.drawOval(RectF(x + 30f, y + 50f, x + width - 30f, y + height), paint)
            paint.color = Color.WHITE
            canvas.drawOval(RectF(x + 35f, y + 10f, x + width - 35f, y + 70f), paint)
        }

        if (isSlashing) {
            val slashArc = getAttackHitbox()
            if (slashArc != null) {
                canvas.drawArc(slashArc, if (facingRight) -65f else 125f, 130f, false, slashPaint)
            }
        }
    }
}
