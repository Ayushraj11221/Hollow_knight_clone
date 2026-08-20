package com.ayush.minihk

import android.content.Context
import android.graphics.*
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.view.SurfaceView

class GameView(context: Context) : SurfaceView(context), SurfaceHolder.Callback, Runnable {
    private var gameThread: Thread? = null
    @Volatile private var isRunning = false

    private var bgBitmap: Bitmap? = null
    private var knight: Knight? = null
    private var hornet: Hornet? = null
    private val spells = mutableListOf<Spell>()

    private enum class GameState { MENU, PLAYING, VICTORY, GAME_OVER }
    private var gameState = GameState.MENU

    private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    private val uiPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { isFakeBoldText = true }
    private val buttonPaint = Paint().apply {
        color = Color.argb(120, 255, 255, 255)
        style = Paint.Style.FILL
    }
    private val buttonBorderPaint = Paint().apply {
        color = Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = 4f
    }
    private val soulFillPaint = Paint().apply { color = Color.WHITE }

    private var leftPressed = false
    private var rightPressed = false
    private var jumpPressed = false
    private var attackPressed = false
    private var spellPressed = false

    init {
        holder.addCallback(this)
        isFocusable = true
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        initGame()
        resume()
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, w: Int, h: Int) {}

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        pause()
    }

    private fun initGame() {
        try {
            val bgStream = context.assets.open("background.png")
            bgBitmap = BitmapFactory.decodeStream(bgStream)
            bgStream.close()
        } catch (_: Exception) {}

        val knightBitmap = SpriteHelper.loadAndChromaKey(context, "knight.png", SpriteHelper.KeyType.KNIGHT_WHITE)
        val hornetBitmap = SpriteHelper.loadAndChromaKey(context, "hornet.png", SpriteHelper.KeyType.HORNET_TEAL)

        knight = Knight(knightBitmap)
        hornet = Hornet(hornetBitmap)
        spells.clear()
    }

    fun resume() {
        if (!isRunning) {
            isRunning = true
            gameThread = Thread(this).apply { start() }
        }
    }

    fun pause() {
        isRunning = false
        try {
            gameThread?.join(500)
            gameThread = null
        } catch (_: Exception) {}
    }

    override fun run() {
        while (isRunning) {
            if (!holder.surface.isValid) continue
            val canvas = holder.lockCanvas() ?: continue

            val groundY = height * 0.84f

            when (gameState) {
                GameState.MENU -> drawMenu(canvas)
                GameState.PLAYING -> {
                    updateGame(groundY)
                    renderGame(canvas)
                }
                GameState.VICTORY -> drawEndScreen(canvas, "HORNET DEFEATED", Color.YELLOW)
                GameState.GAME_OVER -> drawEndScreen(canvas, "SHADE DESTROYED", Color.RED)
            }

            holder.unlockCanvasAndPost(canvas)

            try {
                Thread.sleep(16)
            } catch (_: Exception) {}
        }
    }

    private fun updateGame(groundY: Float) {
        val k = knight ?: return
        val h = hornet ?: return

        val moveDir = (if (rightPressed) 1f else 0f) - (if (leftPressed) 1f else 0f)
        k.update(moveDir, jumpPressed, attackPressed, groundY)
        h.update(k.x, groundY)

        if (spellPressed && k.soul >= 33) {
            k.soul -= 33
            spells.add(Spell(k.x + if (k.facingRight) 100f else -60f, k.y + 50f, k.facingRight))
        }

        val iterator = spells.iterator()
        while (iterator.hasNext()) {
            val spell = iterator.next()
            spell.update(width.toFloat())
            if (RectF.intersects(RectF(spell.x, spell.y, spell.x + spell.width, spell.y + spell.height), h.getHitbox())) {
                h.health -= 25
                spell.isActive = false
            }
            if (!spell.isActive) iterator.remove()
        }

        val slashBox = k.getAttackHitbox()
        if (slashBox != null && RectF.intersects(slashBox, h.getHitbox())) {
            h.health -= 1
            if (k.soul < k.maxSoul) k.soul = (k.soul + 2).coerceAtMost(k.maxSoul)
        }

        if (RectF.intersects(RectF(k.x, k.y, k.x + k.width, k.y + k.height), h.getHitbox())) {
            if (h.state == Hornet.State.DIVE_ATTACK) {
                k.health -= 1
                k.x += if (h.facingRight) 120f else -120f
            }
        }

        if (h.health <= 0) gameState = GameState.VICTORY
        if (k.health <= 0) gameState = GameState.GAME_OVER

        jumpPressed = false
        attackPressed = false
        spellPressed = false
    }

    private fun renderGame(canvas: Canvas) {
        if (bgBitmap != null) {
            canvas.drawBitmap(bgBitmap!!, null, Rect(0, 0, width, height), bgPaint)
        } else {
            canvas.drawColor(Color.rgb(14, 18, 28))
        }

        knight?.draw(canvas)
        hornet?.draw(canvas)
        spells.forEach { it.draw(canvas) }

        drawHUD(canvas)
    }

    private fun drawHUD(canvas: Canvas) {
        val k = knight ?: return
        val h = hornet ?: return

        val vesselX = 70f
        val vesselY = 70f
        val vesselRadius = 40f
        buttonPaint.color = Color.argb(160, 20, 24, 35)
        canvas.drawCircle(vesselX, vesselY, vesselRadius, buttonPaint)
        val fillPercent = k.soul.toFloat() / k.maxSoul
        canvas.drawCircle(vesselX, vesselY, vesselRadius * fillPercent, soulFillPaint)
        canvas.drawCircle(vesselX, vesselY, vesselRadius, buttonBorderPaint)

        for (i in 0 until k.maxHealth) {
            val maskX = 140f + (i * 45f)
            uiPaint.color = if (i < k.health) Color.WHITE else Color.DKGRAY
            canvas.drawRoundRect(RectF(maskX, 45f, maskX + 35f, 95f), 12f, 12f, uiPaint)
        }

        val barW = 400f
        val barH = 22f
        val barX = width - barW - 60f
        val barY = 65f
        uiPaint.color = Color.rgb(220, 70, 80)
        uiPaint.textSize = 30f
        canvas.drawText("HORNET SENTINEL", barX, barY - 12f, uiPaint)
        buttonPaint.color = Color.DKGRAY
        canvas.drawRect(barX, barY, barX + barW, barY + barH, buttonPaint)
        val hpPercent = (h.health.toFloat() / h.maxHealth).coerceAtLeast(0f)
        uiPaint.color = Color.rgb(220, 60, 70)
        canvas.drawRect(barX, barY, barX + (barW * hpPercent), barY + barH, uiPaint)

        val btnY = height - 170f
        val btnH = 110f

        drawButton(canvas, 60f, btnY, 180f, btnY + btnH, "<")
        drawButton(canvas, 210f, btnY, 330f, btnY + btnH, ">")

        drawButton(canvas, width - 480f, btnY, width - 360f, btnY + btnH, "SPELL")
        drawButton(canvas, width - 340f, btnY, width - 220f, btnY + btnH, "NAIL")
        drawButton(canvas, width - 200f, btnY, width - 80f, btnY + btnH, "JUMP")
    }

    private fun drawButton(canvas: Canvas, l: Float, t: Float, r: Float, b: Float, text: String) {
        val rect = RectF(l, t, r, b)
        buttonPaint.color = Color.argb(120, 255, 255, 255)
        canvas.drawRoundRect(rect, 20f, 20f, buttonPaint)
        canvas.drawRoundRect(rect, 20f, 20f, buttonBorderPaint)

        uiPaint.color = Color.WHITE
        uiPaint.textSize = 28f
        val textW = uiPaint.measureText(text)
        canvas.drawText(text, rect.centerX() - textW / 2f, rect.centerY() + 10f, uiPaint)
    }

    private fun drawMenu(canvas: Canvas) {
        canvas.drawColor(Color.rgb(10, 12, 18))
        uiPaint.color = Color.WHITE
        uiPaint.textSize = 60f
        val title = "HOLLOW KNIGHT // HORNET DUEL"
        canvas.drawText(title, width / 2f - uiPaint.measureText(title) / 2f, height * 0.4f, uiPaint)

        uiPaint.textSize = 34f
        uiPaint.color = Color.rgb(0, 229, 255)
        val start = "TAP ANYWHERE TO CHALLENGE"
        canvas.drawText(start, width / 2f - uiPaint.measureText(start) / 2f, height * 0.6f, uiPaint)
    }

    private fun drawEndScreen(canvas: Canvas, msg: String, color: Int) {
        canvas.drawColor(Color.argb(200, 0, 0, 0))
        uiPaint.color = color
        uiPaint.textSize = 55f
        canvas.drawText(msg, width / 2f - uiPaint.measureText(msg) / 2f, height * 0.45f, uiPaint)

        uiPaint.color = Color.WHITE
        uiPaint.textSize = 32f
        val retry = "TAP TO RESTART"
        canvas.drawText(retry, width / 2f - uiPaint.measureText(retry) / 2f, height * 0.65f, uiPaint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val action = event.actionMasked
        val btnY = height - 170f
        val btnH = 110f

        if (gameState != GameState.PLAYING) {
            if (action == MotionEvent.ACTION_DOWN) {
                initGame()
                gameState = GameState.PLAYING
            }
            return true
        }

        val leftBtn = RectF(60f, btnY, 180f, btnY + btnH)
        val rightBtn = RectF(210f, btnY, 330f, btnY + btnH)
        val spellBtn = RectF(width - 480f, btnY, width - 360f, btnY + btnH)
        val nailBtn = RectF(width - 340f, btnY, width - 220f, btnY + btnH)
        val jumpBtn = RectF(width - 200f, btnY, width - 80f, btnY + btnH)

        when (action) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN, MotionEvent.ACTION_MOVE -> {
                leftPressed = false
                rightPressed = false
                for (i in 0 until event.pointerCount) {
                    val x = event.getX(i)
                    val y = event.getY(i)
                    if (leftBtn.contains(x, y)) leftPressed = true
                    if (rightBtn.contains(x, y)) rightPressed = true
                    if (spellBtn.contains(x, y) && action != MotionEvent.ACTION_MOVE) spellPressed = true
                    if (nailBtn.contains(x, y) && action != MotionEvent.ACTION_MOVE) attackPressed = true
                    if (jumpBtn.contains(x, y) && action != MotionEvent.ACTION_MOVE) jumpPressed = true
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP -> {
                leftPressed = false
                rightPressed = false
            }
        }
        return true
    }
}
