package com.ayush.minihk

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint

object SpriteHelper {
    fun loadAndChromaKey(context: Context, filename: String, keyType: KeyType): Bitmap {
        return try {
            val inputStream = context.assets.open(filename)
            val options = BitmapFactory.Options().apply {
                inPreferredConfig = Bitmap.Config.RGB_565
                inSampleSize = 1
            }
            val original = BitmapFactory.decodeStream(inputStream, null, options)
            inputStream.close()

            if (original == null) return createProceduralPlaceholder(filename)
            original
        } catch (e: Exception) {
            createProceduralPlaceholder(filename)
        }
    }

    private fun createProceduralPlaceholder(name: String): Bitmap {
        val bmp = Bitmap.createBitmap(256, 256, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        if (name.contains("knight")) {
            paint.color = Color.WHITE
            canvas.drawCircle(128f, 100f, 60f, paint)
            paint.color = Color.BLACK
            canvas.drawCircle(110f, 95f, 15f, paint)
            canvas.drawCircle(146f, 95f, 15f, paint)
        } else {
            paint.color = Color.rgb(200, 40, 50)
            canvas.drawCircle(128f, 128f, 70f, paint)
            paint.color = Color.WHITE
            canvas.drawCircle(128f, 80f, 35f, paint)
        }
        return bmp
    }

    enum class KeyType { NONE, HORNET_TEAL, KNIGHT_WHITE }
}
