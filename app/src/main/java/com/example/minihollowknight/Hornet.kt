package com.example.minihollowknight

import android.graphics.*
import kotlin.math.*
import kotlin.random.Random

class Hornet(var x: Float, var y: Float) {

    enum class State { IDLE, JUMP_ANTICIPATION, NEEDLE_THROW, NEEDLE_RETRACT, DIVE_ANTICIPATION, DIVE_ATTACK, RETREAT }

    var state = State.IDLE
    var stateTimer = 40
    var hp = 100
    val maxHp = 100
    var facingRight = false

    var vx = 0f
    var vy = 0f
    var needleX = 0f
    var needleY = 0f
    var isNeedleThrown = false

    val width = 70f
    val height = 110f

    fun update(knightX: Float, knightY: Float, groundY: Float, screenWidth: Float) {
        facingRight = knightX > x

        when (state) {
            State.IDLE -> {
                vx = 0f
                vy += 1.2f
                stateTimer--
                if (stateTimer <= 0) {
                    pickNextAttack(knightX)
                }
            }
            State.JUMP_ANTICIPATION -> {
                stateTimer--
                if (stateTimer <= 0) {
                    vy = -26f
                    vx = if (facingRight) 10f else -10f
                    state = State.RETREAT
                    stateTimer = 35
                }
            }
            State.NEEDLE_THROW -> {
                vx = 0f
                needleX += if (facingRight) 32f else -32f
                if (abs(needleX - x) > 550f) {
                    state = State.NEEDLE_RETRACT
                }
            }
            State.NEEDLE_RETRACT -> {
                val dx = x - needleX
                needleX += dx * 0.25f
                if (abs(dx) < 20f) {
                    isNeedleThrown = false
                    state = State.IDLE
                    stateTimer = 25
                }
            }
            State.DIVE_ANTICIPATION -> {
                vx = 0f
                vy = 0f
                stateTimer--
                if (stateTimer <= 0) {
                    state = State.DIVE_ATTACK
                    val dx = knightX - x
                    val dy = (groundY - 50f) - y
                    val dist = hypot(dx, dy)
                    vx = (dx / dist) * 28f
                    vy = (dy / dist) * 28f
                }
            }
            State.DIVE_ATTACK -> {
                x += vx
                y += vy
                if (y + height / 2f >= groundY) {
                    y = groundY - height / 2f
                    vx = 0f
                    vy = 0f
                    state = State.IDLE
                    stateTimer = 35
                }
            }
            State.RETREAT -> {
                vy += 1.2f
                x += vx
                y += vy
                if (y + height / 2f >= groundY) {
                    y = groundY - height / 2f
                    vy = 0f
                    vx = 0f
                    state = State.IDLE
                    stateTimer = 20
                }
            }
        }

        // Ground Clamp
        if (state != State.DIVE_ATTACK && state != State.RETREAT) {
            y += vy
            if (y + height / 2f >= groundY) {
                y = groundY - height / 2f
                vy = 0f
            }
        }
        x = x.coerceIn(100f, screenWidth - 100f)
    }

    private fun pickNextAttack(knightX: Float) {
        val r = Random.nextFloat()
        if (r < 0.45f) {
            // Needle Throw
            state = State.NEEDLE_THROW
            isNeedleThrown = true
            needleX = x
            needleY = y - 10f
        } else if (r < 0.80f) {
            // Aerial Dive
            y = 250f
            vy = 0f
            state = State.DIVE_ANTICIPATION
            stateTimer = 18
        } else {
            // Jump
            state = State.JUMP_ANTICIPATION
            stateTimer = 12
        }
    }

    fun getHitbox(): RectF {
        return RectF(x - width / 2f, y - height / 2f, x + width / 2f, y + height / 2f)
    }

    fun getNeedleHitbox(): RectF? {
        if (!isNeedleThrown) return null
        return RectF(needleX - 35f, needleY - 8f, needleX + 35f, needleY + 8f)
    }

    fun draw(canvas: Canvas, paint: Paint) {
        // Red Cloak / Dress
        paint.style = Paint.Style.FILL
        paint.color = Color.parseColor("#B71C1C")
        val cloakPath = Path().apply {
            moveTo(x, y - 35f)
            lineTo(x - 32f, y + 50f)
            lineTo(x + 32f, y + 50f)
            close()
        }
        canvas.drawPath(cloakPath, paint)

        // White Mask Head
        paint.color = Color.WHITE
        canvas.drawOval(RectF(x - 22f, y - 65f, x + 22f, y - 20f), paint)

        // Hornet Curved Horns
        val hornPath = Path().apply {
            moveTo(x - 14f, y - 55f)
            quadTo(x - 35f, y - 85f, x - 25f, y - 105f)
            lineTo(x - 6f, y - 60f)

            moveTo(x + 14f, y - 55f)
            quadTo(x + 35f, y - 85f, x + 25f, y - 105f)
            lineTo(x + 6f, y - 60f)
        }
        canvas.drawPath(hornPath, paint)

        // Slanted Black Eyes
        paint.color = Color.BLACK
        canvas.drawOval(RectF(x - 16f, y - 48f, x - 4f, y - 32f), paint)
        canvas.drawOval(RectF(x + 4f, y - 48f, x + 16f, y - 32f), paint)

        // Silk Thread & Needle
        if (isNeedleThrown) {
            paint.color = Color.WHITE
            paint.strokeWidth = 2.5f
            paint.style = Paint.Style.STROKE
            canvas.drawLine(x, y - 10f, needleX, needleY, paint)

            paint.style = Paint.Style.FILL
            paint.color = Color.parseColor("#E0E0E0")
            canvas.drawRect(needleX - 40f, needleY - 6f, needleX + 40f, needleY + 6f, paint)
        }
    }
}
