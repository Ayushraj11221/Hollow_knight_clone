package com.ayush.minihk

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color

object SpriteHelper {
    fun loadAndChromaKey(context: Context, filename: String, keyType: KeyType): Bitmap {
        return try {
            val inputStream = context.assets.open(filename)
            val options = BitmapFactory.Options().apply {
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
            val original = BitmapFactory.decodeStream(inputStream, null, options)
            inputStream.close()

            if (original == null) {
                return Bitmap.createBitmap(128, 128, Bitmap.Config.ARGB_8888)
            }

            if (keyType == KeyType.NONE) return original

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
                        if (g > 60 && b > 70 && r < 60) {
                            pixels[i] = Color.TRANSPARENT
                        }
                    }
                    KeyType.KNIGHT_WHITE -> {
                        if (r > 235 && g > 235 && b > 235) {
                            pixels[i] = Color.TRANSPARENT
                        }
                    }
                    KeyType.NONE -> {}
                }
            }

            val result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            result.setPixels(pixels, 0, width, 0, 0, width, height)
            original.recycle()
            result
        } catch (e: Exception) {
            e.printStackTrace()
            Bitmap.createBitmap(128, 128, Bitmap.Config.ARGB_8888)
        }
    }

    enum class KeyType { NONE, HORNET_TEAL, KNIGHT_WHITE }
}
