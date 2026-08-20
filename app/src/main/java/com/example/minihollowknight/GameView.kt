package com.example.minihollowknight

import android.content.Context
import android.graphics.*
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.view.SurfaceView

class GameView(context: Context) : SurfaceView(context), SurfaceHolder.Callback, Runnable {
    private var gameThread: Thread? = null
    @Volatile private var isRunning = false
    @Volatile private var assetsLoaded = false

    private var bgBitmap: Bitmap? = null
    private var knight: Knight? = null
    private var hornet: Hornet? = null
    private val spells = mutableListOf<Spell>()

    private enum class GameState { LOADING, MENU, PLAYING, VICTORY, GAME_OVER }
    private var gameState = GameState.LOADING

    private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    private val uiPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { isFakeBoldText = true }
    private val buttonPaint = Paint().apply {
        color = Color.argb(130, 255, 255, 255)
        style = Paint.Style.FILL
    }
    private val buttonBorderPaint = Paint().apply {
        color = Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = 4f
    }
    private val soulFillPaint = Paint().apply { color = Color.WHITE }
    private val maskPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    private var leftPressed = false
    private var rightPressed = false
    private var jumpPressed = false
    private var attackPressed = false
    private var spellPressed = false
    private var invulnerableTimer = 0

    init {
        holder.addCallback(this)
        isFocusable = true
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        if (!assetsLoaded) {
            Thread {
                loadAssets()
                assetsLoaded = true
                gameState = GameState.MENU
            }.start()
        }
        resume()
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, w: Int, h: Int) {}

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        pause()
    }

    private fun loadAssets() {
        bgBitmap = SpriteHelper.loadBitmap(context, "background.png", sampleSize = 1)
        val knightSheet = SpriteHelper.loadBitmap(context, "knight.png", sampleSize = 2)
        val hornetSheet = SpriteHelper.loadBitmap(context, "hornet.png", sampleSize = 2)

        knight = Knight(knightSheet)
        hornet = Hornet(hornetSheet)
        spells.clear()
        invulnerableTimer = 0
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

            val groundY = height * 0.82f

            when (gameState) {
                GameState.LOADING -> drawLoading(canvas)
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

        if (invulnerableTimer > 0) invulnerableTimer--

        val moveDir = (if (rightPressed) 1f else 0f) - (if (leftPressed) 1f else 0f)
        k.update(moveDir, jumpPressed, attackPressed, groundY)
        h.update(k.x, groundY)

        if (spellPressed && k.soul >= 33) {
            k.soul -= 33
            spells.add(Spell(k.x + if (k.facingRight) 110f else -70f, k.y + 40f, k.facingRight))
        }

        val iterator = spells.iterator()
        while (iterator.hasNext()) {
            val spell = iterator.next()
            spell.update(width.toFloat())
            if (RectF.intersects(RectF(spell.x, spell.y, spell.x + spell.width, spell.y + spell.height), h.getHitbox())) {
                h.health -= 35
                spell.isActive = false
            }
            if (!spell.isActive) iterator.remove()
        }

        val slashBox = k.getAttackHitbox()
        if (slashBox != null && RectF.intersects(slashBox, h.getHitbox())) {
            h.health -= 10
            if (k.soul < k.maxSoul) {
                k.soul = (k.soul + 11).coerceAtMost(k.maxSoul)
            }
        }

        if (invulnerableTimer <= 0 && RectF.intersects(RectF(k.x, k.y, k.x + k.width, k.y + k.height), h.getHitbox())) {
            k.health -= 1
            invulnerableTimer = 40
            k.vy = -14f
            k.x += if (h.facingRight) 100f else -100f
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

        if (invulnerableTimer % 4 < 2) {
            knight?.draw(canvas)
        }
        hornet?.draw(canvas)
        spells.forEach { it.draw(canvas) }

        drawHUD(canvas)
    }

    private fun drawHUD(canvas: Canvas) {
        val k = knight ?: return
        val h = hornet ?: return

        val vesselX = 75f
        val vesselY = 75f
        val vesselRadius = 42f
        buttonPaint.color = Color.argb(180, 20, 24, 35)
        canvas.drawCircle(vesselX, vesselY, vesselRadius, buttonPaint)
        val fillPercent = k.soul.toFloat() / k.maxSoul
        canvas.drawCircle(vesselX, vesselY, vesselRadius * fillPercent, soulFillPaint)
        canvas.drawCircle(vesselX, vesselY, vesselRadius, buttonBorderPaint)

        for (i in 0 until k.maxHealth) {
            val maskX = 150f + (i * 48f)
            val maskY = 75f
            val isActive = i < k.health

            maskPaint.style = Paint.Style.FILL
            maskPaint.color = if (isActive) Color.WHITE else Color.rgb(45, 50, 65)
            canvas.drawOval(RectF(maskX - 16f, maskY - 24f, maskX + 16f, maskY + 24f), maskPaint)

            maskPaint.color = if (isActive) Color.BLACK else Color.rgb(20, 22, 30)
            canvas.drawOval(RectF(maskX - 10f, maskY - 6f, maskX - 2f, maskY + 8f), maskPaint)
            canvas.drawOval(RectF(maskX + 2f, maskY - 6f, maskX + 10f, maskY + 8f), maskPaint)
        }

        val barW = 420f
        val barH = 20f
        val barX = width - barW - 60f
        val barY = 65f

        uiPaint.color = Color.rgb(220, 70, 80)
        uiPaint.textSize = 26f
        canvas.drawText("HORNET // PROTECTOR", barX, barY - 10f, uiPaint)

        buttonPaint.color = Color.rgb(30, 35, 45)
        canvas.drawRoundRect(RectF(barX, barY, barX + barW, barY + barH), 6f, 6f, buttonPaint)

        val hpPercent = (h.health.toFloat() / h.maxHealth).coerceIn(0f, 1f)
        uiPaint.color = Color.rgb(215, 45, 55)
        canvas.drawRoundRect(RectF(barX, barY, barX + (barW * hpPercent), barY + barH), 6f, 6f, uiPaint)

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

    private fun drawLoading(canvas: Canvas) {
        canvas.drawColor(Color.rgb(8, 10, 15))
        uiPaint.color = Color.rgb(0, 229, 255)
        uiPaint.textSize = 40f
        val msg = "ENTERING HALLOWNEST..."
        canvas.drawText(msg, width / 2f - uiPaint.measureText(msg) / 2f, height / 2f, uiPaint)
    }

    private fun drawMenu(canvas: Canvas) {
        canvas.drawColor(Color.rgb(10, 12, 18))
        uiPaint.color = Color.WHITE
        uiPaint.textSize = 55f
        val title = "HOLLOW KNIGHT // HORNET DUEL"
        canvas.drawText(title, width / 2f - uiPaint.measureText(title) / 2f, height * 0.4f, uiPaint)

        uiPaint.textSize = 30f
        uiPaint.color = Color.rgb(0, 229, 255)
        val start = "TAP ANYWHERE TO CHALLENGE"
        canvas.drawText(start, width / 2f - uiPaint.measureText(start) / 2f, height * 0.6f, uiPaint)
    }

    private fun drawEndScreen(canvas: Canvas, msg: String, color: Int) {
        canvas.drawColor(Color.argb(210, 0, 0, 0))
        uiPaint.color = color
        uiPaint.textSize = 55f
        canvas.drawText(msg, width / 2f - uiPaint.measureText(msg) / 2f, height * 0.45f, uiPaint)

        uiPaint.color = Color.WHITE
        uiPaint.textSize = 30f
        val retry = "TAP TO RETRY"
        canvas.drawText(retry, width / 2f - uiPaint.measureText(retry) / 2f, height * 0.65f, uiPaint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val action = event.actionMasked
        val btnY = height - 170f
        val btnH = 110f

        if (gameState != GameState.PLAYING) {
            if (action == MotionEvent.ACTION_DOWN && assetsLoaded) {
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
