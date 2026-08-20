package com.ayush.minihk

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.view.SurfaceView

class GameView(context: Context) : SurfaceView(context), SurfaceHolder.Callback, Runnable {
    private var gameThread: Thread? = null
    private var isPlaying = false

    private lateinit var bgBitmap: Bitmap
    private lateinit var knight: Knight
    private lateinit var hornet: Hornet

    private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    private val uiPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 42f
        color = Color.WHITE
        isFakeBoldText = true
    }
    private val buttonPaint = Paint().apply {
        color = Color.argb(120, 255, 255, 255)
        style = Paint.Style.FILL
    }
    private val buttonBorderPaint = Paint().apply {
        color = Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = 4f
    }

    private var leftPressed = false
    private var rightPressed = false
    private var jumpPressed = false
    private var attackPressed = false

    init {
        holder.addCallback(this)
        isFocusable = true
    }

    private fun initGame() {
        val bgStream = context.assets.open("background.png")
        bgBitmap = BitmapFactory.decodeStream(bgStream)
        bgStream.close()

        val knightBitmap = SpriteHelper.loadAndChromaKey(context, "knight.png", SpriteHelper.KeyType.KNIGHT_WHITE)
        val hornetBitmap = SpriteHelper.loadAndChromaKey(context, "hornet.png", SpriteHelper.KeyType.HORNET_TEAL)

        knight = Knight(knightBitmap)
        hornet = Hornet(hornetBitmap)
    }

    override fun run() {
        while (isPlaying) {
            if (!holder.surface.isValid) continue

            val canvas = holder.lockCanvas() ?: continue

            val groundY = height * 0.82f
            val moveDir = (if (rightPressed) 1f else 0f) - (if (leftPressed) 1f else 0f)

            knight.update(moveDir, jumpPressed, attackPressed, groundY)
            hornet.update(knight.x, groundY)

            jumpPressed = false
            attackPressed = false

            // Draw Background
            val destBg = Rect(0, 0, width, height)
            canvas.drawBitmap(bgBitmap, null, destBg, bgPaint)

            // Draw Characters
            knight.draw(canvas)
            hornet.draw(canvas)

            // Draw UI
            drawUI(canvas)

            holder.unlockCanvasAndPost(canvas)

            try {
                Thread.sleep(16)
            } catch (e: Exception) {
                // Handled safely
            }
        }
    }

    private fun drawUI(canvas: Canvas) {
        // Player Health
        uiPaint.color = Color.WHITE
        canvas.drawText("MASKS: ${knight.health} / ${knight.maxHealth}", 60f, 80f, uiPaint)

        // Boss Health
        uiPaint.color = Color.RED
        canvas.drawText("HORNET SENTINEL: ${hornet.health} HP", width - 580f, 80f, uiPaint)

        // D-PAD
        val btnY = height - 180f
        canvas.drawRoundRect(60f, btnY, 190f, btnY + 120f, 20f, 20f, buttonPaint)
        canvas.drawRoundRect(60f, btnY, 190f, btnY + 120f, 20f, 20f, buttonBorderPaint)
        uiPaint.color = Color.BLACK
        canvas.drawText("<", 110f, btnY + 75f, uiPaint)

        canvas.drawRoundRect(220f, btnY, 350f, btnY + 120f, 20f, 20f, buttonPaint)
        canvas.drawRoundRect(220f, btnY, 350f, btnY + 120f, 20f, 20f, buttonBorderPaint)
        canvas.drawText(">", 270f, btnY + 75f, uiPaint)

        // Actions
        canvas.drawRoundRect(width - 350f, btnY, width - 220f, btnY + 120f, 20f, 20f, buttonPaint)
        canvas.drawRoundRect(width - 350f, btnY, width - 220f, btnY + 120f, 20f, 20f, buttonBorderPaint)
        canvas.drawText("JUMP", width - 330f, btnY + 75f, uiPaint)

        canvas.drawRoundRect(width - 190f, btnY, width - 60f, btnY + 120f, 20f, 20f, buttonPaint)
        canvas.drawRoundRect(width - 190f, btnY, width - 60f, btnY + 120f, 20f, 20f, buttonBorderPaint)
        canvas.drawText("NAIL", width - 170f, btnY + 75f, uiPaint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val action = event.actionMasked
        val index = event.actionIndex
        val touchX = event.getX(index)
        val touchY = event.getY(index)

        val btnY = height - 180f

        when (action) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> {
                if (touchY >= btnY && touchY <= btnY + 120f) {
                    if (touchX in 60f..190f) leftPressed = true
                    if (touchX in 220f..350f) rightPressed = true
                    if (touchX in (width - 350f)..(width - 220f)) jumpPressed = true
                    if (touchX in (width - 190f)..(width - 60f)) attackPressed = true
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP -> {
                if (touchX in 60f..190f) leftPressed = false
                if (touchX in 220f..350f) rightPressed = false
            }
        }
        return true
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        initGame()
        isPlaying = true
        gameThread = Thread(this).apply { start() }
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {}

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        isPlaying = false
        try {
            gameThread?.join()
        } catch (e: Exception) {}
    }
}
