package com.ayush.minihk

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.view.SurfaceView

class GameView(context: Context) : SurfaceView(context), SurfaceHolder.Callback, Runnable {
    private var gameThread: Thread? = null
    @Volatile private var isRunning = false

    private var bgBitmap: Bitmap? = null
    private lateinit var knight: Knight
    private lateinit var hornet: Hornet
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
        isRunning = true
        gameThread = Thread(this).apply { start() }
    }

    fun pause() {
        isRunning = false
        try {
            gameThread?.join()
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
        val moveDir = (if (rightPressed) 1f else 0f) - (if (leftPressed) 1f else 0f)
        knight.update(moveDir, jumpPressed, attackPressed, groundY)
        hornet.update(knight.x, groundY)

        // Handle Spell Cast (Vengeful Spirit)
        if (spellPressed && knight.soul >= 33) {
            knight.soul -= 33
            spells.add(Spell(knight.x + if (knight.facingRight) 100f else -60f, knight.y + 50f, knight.facingRight))
        }

        // Update Projectiles & Spell Collisions
        val iterator = spells.iterator()
        while (iterator.hasNext()) {
            val spell = iterator.next()
            spell.update(width.toFloat())
            if (RectF.intersects(RectF(spell.x, spell.y, spell.x + spell.width, spell.y + spell.height), hornet.getHitbox())) {
                hornet.health -= 25
                spell.isActive = false
            }
            if (!spell.isActive) iterator.remove()
        }

        // Nail Hitbox Collision
        val slashBox = knight.getAttackHitbox()
        if (slashBox != null && RectF.intersects(slashBox, hornet.getHitbox())) {
            hornet.health -= 1
            if (knight.soul < knight.maxSoul) knight.soul = (knight.soul + 2).coerceAtMost(knight.maxSoul)
        }

        // Boss Contact Damage
        if (RectF.intersects(RectF(knight.x, knight.y, knight.x + knight.width, knight.y + knight.height), hornet.getHitbox())) {
            if (hornet.state == Hornet.State.DIVE_ATTACK) {
                knight.health -= 1
                knight.x += if (hornet.facingRight) 120f else -120f
            }
        }

        if (hornet.health <= 0) gameState = GameState.VICTORY
        if (knight.health <= 0) gameState = GameState.GAME_OVER

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

        knight.draw(canvas)
        hornet.draw(canvas)
        spells.forEach { it.draw(canvas) }

        drawHUD(canvas)
    }

    private fun drawHUD(canvas: Canvas) {
        // Soul Vessel Circle
        val vesselX = 70f
        val vesselY = 70f
        val vesselRadius = 40f
        buttonPaint.color = Color.argb(160, 20, 24, 35)
        canvas.drawCircle(vesselX, vesselY, vesselRadius, buttonPaint)
        val fillPercent = knight.soul.toFloat() / knight.maxSoul
        canvas.drawCircle(vesselX, vesselY, vesselRadius * fillPercent, soulFillPaint)
        canvas.drawCircle(vesselX, vesselY, vesselRadius, buttonBorderPaint)

        // Health Masks
        for (i in 0 until knight.maxHealth) {
            val maskX = 140f + (i * 45f)
            uiPaint.color = if (i < knight.health) Color.WHITE else Color.DKGRAY
            canvas.drawRoundRect(RectF(maskX, 45f, maskX + 35f, 95f), 12f, 12f, uiPaint)
        }

        // Hornet Boss Bar
        val barW = 400f
        val barH = 22f
        val barX = width - barW - 60f
        val barY = 65f
        uiPaint.color = Color.rgb(220, 70, 80)
        uiPaint.textSize = 30f
        canvas.drawText("HORNET SENTINEL", barX, barY - 12f, uiPaint)
        buttonPaint.color = Color.DKGRAY
        canvas.drawRect(barX, barY, barX + barW, barY + barH, buttonPaint)
        val hpPercent = (hornet.health.toFloat() / hornet.maxHealth).coerceAtLeast(0f)
        uiPaint.color = Color.rgb(220, 60, 70)
        canvas.drawRect(barX, barY, barX + (barW * hpPercent), barY + barH, uiPaint)

        // Controls
        val btnY = height - 170f
        val btnH = 110f

        // D-Pad
        drawButton(canvas, 60f, btnY, 180f, btnY + btnH, "<")
        drawButton(canvas, 210f, btnY, 330f, btnY + btnH, ">")

        // Jump, Nail, Spell
        drawButton(canvas, width - 480f, btnY, width - 360f, btnY + btnH, "SPELL")
        drawButton(canvas, width - 330f, btnY, width - 210f, btnY + btnH, "JUMP")
        drawButton(canvas, width - 180f, btnY, width - 60f, btnY + btnH, "NAIL")
    }

    private fun drawButton(canvas: Canvas, left: Float, top: Float, right: Float, bottom: Float, text: String) {
        buttonPaint.color = Color.argb(120, 255, 255, 255)
        canvas.drawRoundRect(left, top, right, bottom, 20f, 20f, buttonPaint)
        canvas.drawRoundRect(left, top, right, bottom, 20f, 20f, buttonBorderPaint)
        uiPaint.color = Color.BLACK
        uiPaint.textSize = if (text.length > 4) 28f else 36f
        val textWidth = uiPaint.measureText(text)
        canvas.drawText(text, ((left + right) / 2) - (textWidth / 2), ((top + bottom) / 2) + 12f, uiPaint)
    }

    private fun drawMenu(canvas: Canvas) {
        canvas.drawColor(Color.rgb(10, 14, 22))
        uiPaint.color = Color.WHITE
        uiPaint.textSize = 64f
        val title = "HOLLOW KNIGHT"
        canvas.drawText(title, (width / 2f) - (uiPaint.measureText(title) / 2f), height * 0.35f, uiPaint)

        uiPaint.textSize = 34f
        uiPaint.color = Color.rgb(200, 60, 70)
        val subtitle = "HORNET DUEL"
        canvas.drawText(subtitle, (width / 2f) - (uiPaint.measureText(subtitle) / 2f), height * 0.46f, uiPaint)

        uiPaint.color = Color.LTGRAY
        uiPaint.textSize = 30f
        val prompt = "TAP ANYWHERE TO START"
        canvas.drawText(prompt, (width / 2f) - (uiPaint.measureText(prompt) / 2f), height * 0.72f, uiPaint)
    }

    private fun drawEndScreen(canvas: Canvas, msg: String, color: Int) {
        canvas.drawColor(Color.argb(190, 0, 0, 0))
        uiPaint.color = color
        uiPaint.textSize = 60f
        canvas.drawText(msg, (width / 2f) - (uiPaint.measureText(msg) / 2f), height * 0.45f, uiPaint)
        uiPaint.color = Color.WHITE
        uiPaint.textSize = 30f
        val retry = "TAP TO RESTART"
        canvas.drawText(retry, (width / 2f) - (uiPaint.measureText(retry) / 2f), height * 0.65f, uiPaint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val action = event.actionMasked
        val index = event.actionIndex
        val touchX = event.getX(index)
        val touchY = event.getY(index)

        if (action == MotionEvent.ACTION_DOWN) {
            if (gameState == GameState.MENU || gameState == GameState.VICTORY || gameState == GameState.GAME_OVER) {
                initGame()
                gameState = GameState.PLAYING
                return true
            }
        }

        val btnY = height - 170f
        when (action) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> {
                if (touchY >= btnY - 30f) {
                    if (touchX in 50f..190f) leftPressed = true
                    if (touchX in 200f..340f) rightPressed = true
                    if (touchX in (width - 490f)..(width - 350f)) spellPressed = true
                    if (touchX in (width - 340f)..(width - 200f)) jumpPressed = true
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
        resume()
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {}

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        pause()
    }
}
