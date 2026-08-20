package com.example.minihollowknight

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color

object SpriteHelper {
    fun loadAssetBitmap(context: Context, filename: String): Bitmap? {
        return try {
            context.assets.open(filename).use { inputStream ->
                BitmapFactory.decodeStream(inputStream)
            }
        } catch (_: Exception) {
            null
        }
    }

    fun removeColorKey(src: Bitmap, targetColor: Int, tolerance: Int = 30): Bitmap {
        val copy = src.copy(Bitmap.Config.ARGB_8888, true)
        val width = copy.width
        val height = copy.height
        val pixels = IntArray(width * height)
        copy.getPixels(pixels, 0, width, 0, 0, width, height)

        val tr = Color.red(targetColor)
        val tg = Color.green(targetColor)
        val tb = Color.blue(targetColor)

        for (i in pixels.indices) {
            val c = pixels[i]
            val r = Color.red(c)
            val g = Color.green(c)
            val b = Color.blue(c)
            if (Math.abs(r - tr) < tolerance && Math.abs(g - tg) < tolerance && Math.abs(b - tb) < tolerance) {
                pixels[i] = Color.TRANSPARENT
            }
        }
        copy.setPixels(pixels, 0, width, 0, 0, width, height)
        return copy
    }
}
