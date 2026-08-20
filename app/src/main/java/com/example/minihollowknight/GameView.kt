package com.example.minihollowknight

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.view.SurfaceView
import kotlin.math.hypot

class GameView(context: Context, attrs: AttributeSet? = null) : SurfaceView(context, attrs), SurfaceHolder.Callback, Runnable {

    private var isPlaying = false
    private var gameThread: Thread? = null

    private lateinit var knight: Knight
    private lateinit var hornet: Hornet

    // Touch Controls
    private var leftTouchId = -1
    private var joystickBase = PointF(200f, 600f)
    private var joystickPos = PointF(200f, 600f)

    private val jumpBtn = RectF()
    private val attackBtn = RectF()
    private val dashBtn = RectF()

    private val paint = Paint().apply { isAntiAlias = true }
    private val textPaint = Paint().apply {
        color = Color.WHITE
        textSize = 32f
        typeface = Typeface.MONOSPACE
        isAntiAlias = true
    }

    init {
        holder.addCallback(this)
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        val groundY = (height - 140f).coerceAtLeast(350f)
        knight = Knight(180f, groundY - 50f)
        hornet = Hornet(width - 250f, groundY - 60f)

        isPlaying = true
        gameThread = Thread(this)
        gameThread?.start()
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, w: Int, h: Int) {}

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        isPlaying = false
        gameThread?.join()
    }

    override fun run() {
        while (isPlaying) {
            update()
            draw()
            try {
                Thread.sleep(16)
            } catch (_: Exception) {}
        }
    }

    private fun update() {
        val groundY = (height - 140f).coerceAtLeast(350f)

        // Joystick Input
        val dx = joystickPos.x - joystickBase.x
        if (Math.abs(dx) > 20f && !knight.isDashing) {
            knight.vx = (dx / 75f).coerceIn(-1f, 1f) * 12f
            knight.facingRight = knight.vx > 0
        } else if (!knight.isDashing) {
            knight.vx = 0f
        }

        knight.update(groundY, width.toFloat())
        hornet.update(knight.x, knight.y, groundY, width.toFloat())

        // Nail Slash vs Hornet Collision
        val slashBox = knight.getSlashHitbox()
        if (slashBox != null && RectF.intersects(slashBox, hornet.getHitbox())) {
            if (hornet.hp > 0) {
                hornet.hp = (hornet.hp - 1).coerceAtLeast(0)
                hornet.x += if (knight.facingRight) 15f else -15f // Knockback
            }
        }

        // Hornet / Needle vs Knight Collision
        if (knight.invulnerableTimer <= 0) {
            val needleBox = hornet.getNeedleHitbox()
            if (RectF.intersects(knight.getHitbox(), hornet.getHitbox()) ||
                (needleBox != null && RectF.intersects(knight.getHitbox(), needleBox))) {
                knight.hp = (knight.hp - 1).coerceAtLeast(0)
                knight.invulnerableTimer = 40
                knight.vy = -12f
                knight.vx = if (hornet.x > knight.x) -16f else 16f
            }
        }
    }

    private fun draw() {
        if (!holder.surface.isValid) return
        val canvas = holder.lockCanvas() ?: return

        val groundY = (height - 140f).coerceAtLeast(350f)

        // Background
        canvas.drawColor(Color.parseColor("#070712"))

        // Floor Platform
        paint.style = Paint.Style.FILL
        paint.color = Color.parseColor("#12121E")
        canvas.drawRect(0f, groundY, width.toFloat(), height.toFloat(), paint)

        paint.color = Color.parseColor("#00E5FF")
        paint.strokeWidth = 3f
        canvas.drawLine(0f, groundY, width.toFloat(), groundY, paint)

        // Characters
        knight.draw(canvas, paint)
        hornet.draw(canvas, paint)

        // HUD: Knight Masks (HP)
        for (i in 0 until knight.maxHp) {
            paint.style = if (i < knight.hp) Paint.Style.FILL else Paint.Style.STROKE
            paint.color = Color.WHITE
            paint.strokeWidth = 3f
            canvas.drawCircle(60f + i * 40f, 60f, 14f, paint)
        }

        // HUD: Hornet Boss Health Bar
        val barW = 400f
        val barH = 14f
        val barX = width / 2f - barW / 2f
        val barY = 50f

        paint.style = Paint.Style.FILL
        paint.color = Color.parseColor("#33FFFFFF")
        canvas.drawRoundRect(RectF(barX, barY, barX + barW, barY + barH), 6f, 6f, paint)

        paint.color = Color.parseColor("#B71C1C")
        val fillW = (hornet.hp.toFloat() / hornet.maxHp.toFloat()) * barW
        canvas.drawRoundRect(RectF(barX, barY, barX + fillW, barY + barH), 6f, 6f, paint)

        canvas.drawText("HORNET // PROTECTOR", barX + 70f, barY + 45f, textPaint)

        // Controls
        drawControls(canvas)

        holder.unlockCanvasAndPost(canvas)
    }

    private fun drawControls(canvas: Canvas) {
        joystickBase.set(180f, height - 160f)
        if (leftTouchId == -1) joystickPos.set(joystickBase)

        paint.style = Paint.Style.STROKE
        paint.color = Color.parseColor("#44FFFFFF")
        paint.strokeWidth = 4f
        canvas.drawCircle(joystickBase.x, joystickBase.y, 75f, paint)

        paint.style = Paint.Style.FILL
        paint.color = Color.parseColor("#6600E5FF")
        canvas.drawCircle(joystickPos.x, joystickPos.y, 35f, paint)

        // Right Action Buttons
        jumpBtn.set(width - 340f, height - 190f, width - 240f, height - 90f)
        attackBtn.set(width - 220f, height - 190f, width - 120f, height - 90f)
        dashBtn.set(width - 100f, height - 190f, width - 10f, height - 90f)

        paint.color = Color.parseColor("#44FFFFFF")
        canvas.drawRoundRect(jumpBtn, 20f, 20f, paint)
        canvas.drawRoundRect(attackBtn, 20f, 20f, paint)
        canvas.drawRoundRect(dashBtn, 20f, 20f, paint)

        canvas.drawText("JUMP", jumpBtn.left + 12f, jumpBtn.centerY() + 10f, textPaint)
        canvas.drawText("NAIL", attackBtn.left + 12f, attackBtn.centerY() + 10f, textPaint)
        canvas.drawText("DASH", dashBtn.left + 12f, dashBtn.centerY() + 10f, textPaint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val action = event.actionMasked
        val pIdx = event.actionIndex
        val pId = event.getPointerId(pIdx)
        val x = event.getX(pIdx)
        val y = event.getY(pIdx)

        when (action) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> {
                if (x < width / 2f && leftTouchId == -1) {
                    leftTouchId = pId
                    joystickPos.set(x, y)
                } else if (jumpBtn.contains(x, y)) {
                    knight.jump()
                } else if (attackBtn.contains(x, y)) {
                    knight.slash()
                } else if (dashBtn.contains(x, y)) {
                    knight.dash()
                }
            }
            MotionEvent.ACTION_MOVE -> {
                for (i in 0 until event.pointerCount) {
                    if (event.getPointerId(i) == leftTouchId) {
                        val px = event.getX(i)
                        val py = event.getY(i)
                        val dist = hypot(px - joystickBase.x, py - joystickBase.y)
                        if (dist < 80f) {
                            joystickPos.set(px, py)
                        } else {
                            val ratio = 80f / dist
                            joystickPos.set(
                                joystickBase.x + (px - joystickBase.x) * ratio,
                                joystickBase.y + (py - joystickBase.y) * ratio
                            )
                        }
                    }
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP -> {
                if (pId == leftTouchId) {
                    leftTouchId = -1
                    joystickPos.set(joystickBase)
                }
            }
        }
        return true
    }
}
