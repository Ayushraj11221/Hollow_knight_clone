package com.example.minihollowknight

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.view.SurfaceView
import kotlin.math.hypot

class GameView(context: Context, attrs: AttributeSet? = null) : SurfaceView(context, attrs), SurfaceHolder.Callback, Runnable {

    @Volatile private var isPlaying = false
    private var gameThread: Thread? = null

    // Direct initialization (Eliminates UninitializedPropertyAccessException)
    private val knight = Knight(180f, 400f)
    private val hornet = Hornet(800f, 400f)

    private var leftTouchId = -1
    private var joystickBase = PointF(180f, 550f)
    private var joystickPos = PointF(180f, 550f)

    private val jumpBtn = RectF()
    private val attackBtn = RectF()
    private val dashBtn = RectF()

    private val paint = Paint().apply { isAntiAlias = true }
    private val textPaint = Paint().apply {
        color = Color.WHITE
        textSize = 30f
        typeface = Typeface.MONOSPACE
        isAntiAlias = true
    }

    init {
        holder.addCallback(this)
        isFocusable = true
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        resume()
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, w: Int, h: Int) {
        val groundY = (h - 140f).coerceAtLeast(350f)
        knight.y = groundY - knight.height / 2f
        hornet.x = (w - 250f).coerceAtLeast(400f)
        hornet.y = groundY - hornet.height / 2f
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        pause()
    }

    fun resume() {
        if (!isPlaying) {
            isPlaying = true
            gameThread = Thread(this).apply { start() }
        }
    }

    fun pause() {
        isPlaying = false
        try {
            gameThread?.join(500)
            gameThread = null
        } catch (_: Exception) {}
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
        val screenW = if (width > 0) width.toFloat() else 1920f
        val screenH = if (height > 0) height.toFloat() else 1080f
        val groundY = (screenH - 140f).coerceAtLeast(350f)

        val dx = joystickPos.x - joystickBase.x
        if (Math.abs(dx) > 20f && !knight.isDashing) {
            knight.vx = (dx / 75f).coerceIn(-1f, 1f) * 12f
            knight.facingRight = knight.vx > 0
        } else if (!knight.isDashing) {
            knight.vx = 0f
        }

        knight.update(groundY, screenW)
        hornet.update(knight.x, knight.y, groundY, screenW)

        val slashBox = knight.getSlashHitbox()
        if (slashBox != null && RectF.intersects(slashBox, hornet.getHitbox())) {
            if (hornet.hp > 0) {
                hornet.hp = (hornet.hp - 1).coerceAtLeast(0)
                hornet.x += if (knight.facingRight) 15f else -15f
            }
        }

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

        try {
            val screenW = width.toFloat()
            val screenH = height.toFloat()
            val groundY = (screenH - 140f).coerceAtLeast(350f)

            canvas.drawColor(Color.parseColor("#070712"))

            paint.style = Paint.Style.FILL
            paint.color = Color.parseColor("#12121E")
            canvas.drawRect(0f, groundY, screenW, screenH, paint)

            paint.color = Color.parseColor("#00E5FF")
            paint.strokeWidth = 3f
            canvas.drawLine(0f, groundY, screenW, groundY, paint)

            knight.draw(canvas, paint)
            hornet.draw(canvas, paint)

            for (i in 0 until knight.maxHp) {
                paint.style = if (i < knight.hp) Paint.Style.FILL else Paint.Style.STROKE
                paint.color = Color.WHITE
                paint.strokeWidth = 3f
                canvas.drawCircle(60f + i * 36f, 50f, 12f, paint)
            }

            val barW = 380f
            val barH = 12f
            val barX = screenW / 2f - barW / 2f
            val barY = 40f

            paint.style = Paint.Style.FILL
            paint.color = Color.parseColor("#33FFFFFF")
            canvas.drawRoundRect(RectF(barX, barY, barX + barW, barY + barH), 6f, 6f, paint)

            paint.color = Color.parseColor("#B71C1C")
            val fillW = (hornet.hp.toFloat() / hornet.maxHp.toFloat()) * barW
            canvas.drawRoundRect(RectF(barX, barY, barX + fillW, barY + barH), 6f, 6f, paint)

            canvas.drawText("HORNET // PROTECTOR", barX + 60f, barY + 38f, textPaint)

            drawControls(canvas, screenW, screenH)
        } finally {
            holder.unlockCanvasAndPost(canvas)
        }
    }

    private fun drawControls(canvas: Canvas, w: Float, h: Float) {
        joystickBase.set(160f, h - 140f)
        if (leftTouchId == -1) joystickPos.set(joystickBase)

        paint.style = Paint.Style.STROKE
        paint.color = Color.parseColor("#44FFFFFF")
        paint.strokeWidth = 4f
        canvas.drawCircle(joystickBase.x, joystickBase.y, 70f, paint)

        paint.style = Paint.Style.FILL
        paint.color = Color.parseColor("#6600E5FF")
        canvas.drawCircle(joystickPos.x, joystickPos.y, 32f, paint)

        jumpBtn.set(w - 320f, h - 170f, w - 230f, h - 80f)
        attackBtn.set(w - 210f, h - 170f, w - 120f, h - 80f)
        dashBtn.set(w - 100f, h - 170f, w - 10f, h - 80f)

        paint.color = Color.parseColor("#44FFFFFF")
        canvas.drawRoundRect(jumpBtn, 16f, 16f, paint)
        canvas.drawRoundRect(attackBtn, 16f, 16f, paint)
        canvas.drawRoundRect(dashBtn, 16f, 16f, paint)

        canvas.drawText("JUMP", jumpBtn.left + 8f, jumpBtn.centerY() + 10f, textPaint)
        canvas.drawText("NAIL", attackBtn.left + 8f, attackBtn.centerY() + 10f, textPaint)
        canvas.drawText("DASH", dashBtn.left + 8f, dashBtn.centerY() + 10f, textPaint)
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
                        if (dist < 75f) {
                            joystickPos.set(px, py)
                        } else {
                            val ratio = 75f / dist
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
