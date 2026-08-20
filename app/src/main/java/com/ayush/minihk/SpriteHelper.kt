package com.ayush.minihk

import android.content.Context
import android.graphics.*

object SpriteHelper {

    fun loadBitmap(context: Context, filename: String, sampleSize: Int = 2): Bitmap? {
        return try {
            context.assets.open(filename).use { inputStream ->
                val options = BitmapFactory.Options().apply {
                    inSampleSize = sampleSize
                    inPreferredConfig = Bitmap.Config.RGB_565
                }
                BitmapFactory.decodeStream(inputStream, null, options)
            }
        } catch (_: Exception) {
            null
        }
    }

    fun cropFrame(sheet: Bitmap?, rx: Float, ry: Float, rw: Float, rh: Float): Bitmap? {
        if (sheet == null) return null
        return try {
            val sx = (sheet.width * rx).toInt().coerceIn(0, sheet.width - 1)
            val sy = (sheet.height * ry).toInt().coerceIn(0, sheet.height - 1)
            val sw = (sheet.width * rw).toInt().coerceAtMost(sheet.width - sx)
            val sh = (sheet.height * rh).toInt().coerceAtMost(sheet.height - sy)
            if (sw <= 0 || sh <= 0) return null
            Bitmap.createBitmap(sheet, sx, sy, sw, sh)
        } catch (_: Exception) {
            null
        }
    }
}
