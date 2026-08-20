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
    @Volatile private var isPlaying = false

    private var bgBitmap: Bitmap? = null
    private var knight: Knight? = null
    private var hornet: Hornet? = null

    private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    private val uiPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 38f
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
        try {
            val bgStream = context.assets.open("background.png")
            bgBitmap = BitmapFactory.decodeStream(bgStream)
            bgStream.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        val knightBitmap = SpriteHelper.loadAndChromaKey(context, "knight.png", SpriteHelper.KeyType.KNIGHT_WHITE)
        val hornetBitmap = SpriteHelper.loadAndChromaKey(context, "hornet.png", SpriteHelper.KeyType.HORNET_TEAL)

        knight = Knight(knightBitmap)
        hornet = Hornet(hornetBitmap)
    }

    override fun run() {
        while (isPlaying) {
            if (!holder.surface.isValid) continue

            val canvas = holder.lockCanvas() ?: continue

            val groundY = height * 0.84f
            val moveDir = (if (rightPressed) 1f else 0f) - (if (leftPressed) 1f else 0f)

            knight?.update(moveDir, jumpPressed, attackPressed, groundY)
            hornet?.update(knight?.x ?: 0f, groundY)

            jumpPressed = false
            attackPressed = false

            // Clear Background
            if (bgBitmap != null) {
                val destBg = Rect(0, 0, width, height)
                canvas.drawBitmap(bgBitmap!!, null, destBg, bgPaint)
            } else {
                canvas.drawColor(Color.rgb(18, 22, 34))
            }

            // Draw Entities
            knight?.draw(canvas)
            hornet?.draw(canvas)

            // Draw HUD & Controls
            drawUI(canvas)

            holder.unlockCanvasAndPost(canvas)

            try {
                Thread.sleep(16)
            } catch (_: Exception) {}
        }
    }

    private fun drawUI(canvas: Canvas) {
        uiPaint.color = Color.WHITE
        canvas.drawText("MASKS: ${knight?.health ?: 5} / 5", 60f, 75f, uiPaint)

        uiPaint.color = Color.rgb(230, 80, 80)
        canvas.drawText("HORNET SENTINEL: ${hornet?.health ?: 100} HP", width - 560f, 75f, uiPaint)

        val btnY = height - 170f
        val btnH = 110f

        // D-PAD
        canvas.drawRoundRect(60f, btnY, 180f, btnY + btnH, 20f, 20f, buttonPaint)
        canvas.drawRoundRect(60f, btnY, 180f, btnY + btnH, 20f, 20f, buttonBorderPaint)
        uiPaint.color = Color.BLACK
        canvas.drawText("<", 105f, btnY + 70f, uiPaint)

        canvas.drawRoundRect(210f, btnY, 330f, btnY + btnH, 20f, 20f, buttonPaint)
        canvas.drawRoundRect(210f, btnY, 330f, btnY + btnH, 20f, 20f, buttonBorderPaint)
        canvas.drawText(">", 255f, btnY + 70f, uiPaint)

        // Action Buttons
        canvas.drawRoundRect(width - 340f, btnY, width - 210f, btnY + btnH, 20f, 20f, buttonPaint)
        canvas.drawRoundRect(width - 340f, btnY, width - 210f, btnY + btnH, 20f, 20f, buttonBorderPaint)
        canvas.drawText("JUMP", width - 315f, btnY + 70f, uiPaint)

        canvas.drawRoundRect(width - 180f, btnY, width - 50f, btnY + btnH, 20f, 20f, buttonPaint)
        canvas.drawRoundRect(width - 180f, btnY, width - 50f, btnY + btnH, 20f, 20f, buttonBorderPaint)
        canvas.drawText("NAIL", width - 155f, btnY + 70f, uiPaint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val action = event.actionMasked
        val index = event.actionIndex
        val touchX = event.getX(index)
        val touchY = event.getY(index)
        val btnY = height - 170f

        when (action) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> {
                if (touchY >= btnY - 20f) {
                    if (touchX in 50f..190f) leftPressed = true
                    if (touchX in 200f..340f) rightPressed = true
                    if (touchX in (width - 350f)..(width - 200f)) jumpPressed = true
                    if (touchX in (width - 190f)..width.toFloat()) attackPressed = true
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP -> {
                if (touchX in 50f..190f) leftPressed = false
                if (touchX in 200f..340f) rightPressed = false
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
        } catch (_: Exception) {}
    }
}
