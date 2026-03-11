package com.clinical.assessment.utils

import android.graphics.Bitmap
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import com.google.zxing.common.BitMatrix
import com.journeyapps.barcodescanner.BarcodeEncoder

object QRCodeGenerator {
    fun generateQRCode(text: String, width: Int = 512, height: Int = 512): Bitmap? {
        return try {
            val multiFormatWriter = MultiFormatWriter()
            val bitMatrix: BitMatrix = multiFormatWriter.encode(text, BarcodeFormat.QR_CODE, width, height)
            val barcodeEncoder = BarcodeEncoder()
            barcodeEncoder.createBitmap(bitMatrix)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
