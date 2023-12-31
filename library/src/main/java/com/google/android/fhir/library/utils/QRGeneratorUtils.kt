package com.google.android.fhir.library.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.Drawable
import androidx.core.content.ContextCompat
import com.google.android.fhir.library.R
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter

class QRGeneratorUtils {

  fun createQRCodeBitmap(content: String): Bitmap {
    val hints = mutableMapOf<EncodeHintType, Any>()
    hints[EncodeHintType.MARGIN] = 2
    val qrCodeWriter = QRCodeWriter()
    val bitMatrix = qrCodeWriter.encode(content, BarcodeFormat.QR_CODE, 512, 512, hints)
    val width = bitMatrix.width
    val height = bitMatrix.height
    val qrCodeBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)

    for (x in 0 until width) {
      for (y in 0 until height) {
        qrCodeBitmap.setPixel(x, y, if (bitMatrix[x, y]) Color.BLACK else Color.WHITE)
      }
    }
    return qrCodeBitmap
  }

  fun createLogoBitmap(context: Context, qrCodeBitmap: Bitmap): Bitmap {
    val logoScale = 0.4
    val logoDrawable = ContextCompat.getDrawable(context, R.drawable.smart_logo)
    val logoAspectRatio =
      logoDrawable!!.intrinsicWidth.toFloat() / logoDrawable.intrinsicHeight.toFloat()
    val width = qrCodeBitmap.width
    val logoWidth = (width * logoScale).toInt()
    val logoHeight = (logoWidth / logoAspectRatio).toInt()

    return convertDrawableToBitmap(logoDrawable, logoWidth, logoHeight)
  }

  fun overlayLogoOnQRCode(qrCodeBitmap: Bitmap, logoBitmap: Bitmap): Bitmap {
    val centerX = (qrCodeBitmap.width - logoBitmap.width) / 2
    val centerY = (qrCodeBitmap.height - logoBitmap.height) / 2

    val backgroundBitmap =
      Bitmap.createBitmap(logoBitmap.width, logoBitmap.height, Bitmap.Config.RGB_565)
    backgroundBitmap.eraseColor(Color.WHITE)

    val canvas = Canvas(backgroundBitmap)
    canvas.drawBitmap(logoBitmap, 0f, 0f, null)

    val finalBitmap = Bitmap.createBitmap(qrCodeBitmap)
    val finalCanvas = Canvas(finalBitmap)
    finalCanvas.drawBitmap(backgroundBitmap, centerX.toFloat(), centerY.toFloat(), null)

    return finalBitmap
  }

  private fun convertDrawableToBitmap(drawable: Drawable, width: Int, height: Int): Bitmap {
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    drawable.setBounds(0, 0, canvas.width, canvas.height)
    drawable.draw(canvas)
    return bitmap
  }

}