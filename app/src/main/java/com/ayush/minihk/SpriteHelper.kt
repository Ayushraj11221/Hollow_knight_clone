package com.ayush.minihk

import android.content.Context
import android.graphics.*

object SpriteHelper {

    fun loadBitmap(context: Context, filename: String): Bitmap? {
        return try {
            context.assets.open(filename).use { inputStream ->
                val options = BitmapFactory.Options().apply {
                    inPreferredConfig = Bitmap.Config.ARGB_8888
                }
                BitmapFactory.decodeStream(inputStream, null, options)
            }
        } catch (_: Exception) {
            null
        }
    }

    fun removeChromaKey(src: Bitmap, targetColor: Int, tolerance: Int = 45): Bitmap {
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

    // Extract a normalized percentage region from a sprite sheet
    fun cropFrame(sheet: Bitmap?, rx: Float, ry: Float, rw: Float, rh: Float): Bitmap? {
        if (sheet == null) return null
        return try {
            val sx = (sheet.width * rx).toInt().coerceIn(0, sheet.width - 1)
            val sy = (sheet.height * ry).toInt().coerceIn(0, sheet.height - 1)
            val sw = (sheet.width * rw).toInt().coerceAtMost(sheet.width - sx)
            val sh = (sheet.height * rh).toInt().coerceAtMost(sheet.height - sy)
            Bitmap.createBitmap(sheet, sx, sy, sw, sh)
        } catch (_: Exception) {
            null
        }
    }
}
