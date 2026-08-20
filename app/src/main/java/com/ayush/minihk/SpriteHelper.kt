package com.ayush.minihk

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import java.io.InputStream

object SpriteHelper {
    fun loadAndChromaKey(context: Context, filename: String, keyType: KeyType): Bitmap {
        val inputStream: InputStream = context.assets.open(filename)
        val original = BitmapFactory.decodeStream(inputStream)
        inputStream.close()

        val width = original.width
        val height = original.height
        val pixels = IntArray(width * height)
        original.getPixels(pixels, 0, width, 0, 0, width, height)

        for (i in pixels.indices) {
            val color = pixels[i]
            val r = Color.red(color)
            val g = Color.green(color)
            val b = Color.blue(color)

            when (keyType) {
                KeyType.HORNET_TEAL -> {
                    if (g > 70 && b > 80 && r < 50) {
                        pixels[i] = Color.TRANSPARENT
                    }
                }
                KeyType.KNIGHT_WHITE -> {
                    if (r > 240 && g > 240 && b > 240) {
                        pixels[i] = Color.TRANSPARENT
                    }
                }
                KeyType.NONE -> {}
            }
        }

        val result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        result.setPixels(pixels, 0, width, 0, 0, width, height)
        return result
    }

    enum class KeyType { NONE, HORNET_TEAL, KNIGHT_WHITE }
}
