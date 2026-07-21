package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.widget.Toast
import com.example.data.Client
import com.example.data.Order
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import com.google.zxing.common.BitMatrix
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

data class InvoiceSettings(
    val showLetterhead: Boolean,
    val showTaxInfo: Boolean,
    val showBankDetails: Boolean,
    val qrCodeData: String = "",
    val isBanglaMode: Boolean = true,
    val logoUri: String? = null
)

fun generateInvoicePdf(
    context: Context,
    client: Client,
    orders: List<Order>,
    totalAmount: Double,
    advanceAmount: Double,
    date: String,
    isA5: Boolean = false,
    settings: InvoiceSettings = InvoiceSettings(true, true, true)
): File? {
    val document = PdfDocument()
    // A4: 595x842, A5: 421x595
    val width = if (isA5) 421 else 595
    val height = if (isA5) 595 else 842
    val pageInfo = PdfDocument.PageInfo.Builder(width, height, 1).create()
    val page = document.startPage(pageInfo)
    val canvas: Canvas = page.canvas
    val paint = Paint()

    // Draw content (proportional scaling for A4 / A5)
    val scale = if (isA5) 0.7f else 1.0f
    var y = 50f * scale
    if (settings.showLetterhead) {
        // Draw Logo if available
        settings.logoUri?.let { uriString ->
            try {
                val uri = android.net.Uri.parse(uriString)
                val inputStream = context.contentResolver.openInputStream(uri)
                val bitmap = android.graphics.BitmapFactory.decodeStream(inputStream)
                inputStream?.close()
                bitmap?.let {
                    val logoWidth = 60f * scale
                    val logoHeight = (it.height.toFloat() / it.width.toFloat()) * logoWidth
                    val destRect = android.graphics.RectF(50f * scale, y - (15f * scale), 50f * scale + logoWidth, y - (15f * scale) + logoHeight)
                    canvas.drawBitmap(it, null, destRect, null)
                    y += logoHeight + (10f * scale)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        paint.textSize = 22f * scale
        paint.isFakeBoldText = true
        canvas.drawText("SUCHARU GRAPHICS", 50f * scale, y, paint)
        paint.isFakeBoldText = false
        y += 28f * scale
        paint.textSize = 11f * scale
        canvas.drawText("Modern Offset & Digital Printing", 50f * scale, y, paint)
        y += 35f * scale
    }

    paint.textSize = 18f * scale
    canvas.drawText("INVOICE", 50f * scale, y, paint)
    y += 28f * scale
    
    paint.textSize = 13f * scale
    canvas.drawText("Billed to: ${client.name}", 50f * scale, y, paint)
    y += 18f * scale
    canvas.drawText("Date: $date", 50f * scale, y, paint)
    y += 45f * scale

    paint.textSize = 10f * scale
    canvas.drawText("বিবরণ (Description)", 50f * scale, y, paint)
    canvas.drawText("পরিমাণ (Qty)", 300f * scale, y, paint)
    canvas.drawText("দাম (Rate)", 400f * scale, y, paint)
    canvas.drawText("টাকা (Total)", 500f * scale, y, paint)
    y += 18f * scale
    
    orders.forEach { order ->
        if (y < height - 120f * scale) {
            canvas.drawText("${order.jobName} - ${order.category}", 50f * scale, y, paint)
            canvas.drawText(order.quantity, 300f * scale, y, paint)
            canvas.drawText("৳${String.format("%.2f", order.perUnitCost)}", 400f * scale, y, paint)
            canvas.drawText("৳${String.format("%.2f", order.totalAmount)}", 500f * scale, y, paint)
            y += 18f * scale
        }
    }
    
    y += 15f * scale
    canvas.drawText("Total Amount:", 300f * scale, y, paint)
    canvas.drawText("৳${String.format("%.2f", totalAmount)}", 500f * scale, y, paint)
    if (settings.showTaxInfo) {
        y += 18f * scale
        canvas.drawText("Tax (10%):", 300f * scale, y, paint)
        canvas.drawText("৳${String.format("%.2f", totalAmount * 0.1)}", 500f * scale, y, paint)
    }
    y += 18f * scale
    canvas.drawText("Advance Paid:", 300f * scale, y, paint)
    canvas.drawText("৳${String.format("%.2f", advanceAmount)}", 500f * scale, y, paint)
    y += 18f * scale
    paint.isFakeBoldText = true
    canvas.drawText("Due Balance:", 300f * scale, y, paint)
    canvas.drawText("৳${String.format("%.2f", totalAmount - advanceAmount)}", 500f * scale, y, paint)
    paint.isFakeBoldText = false

    if (settings.showBankDetails) {
        y += 40f * scale
        paint.textSize = 11f * scale
        canvas.drawText("Bank Account Details:", 50f * scale, y, paint)
        y += 18f * scale
        canvas.drawText("Bank: DBBL, Account: 1234567890", 50f * scale, y, paint)
    }
    
    if (settings.qrCodeData.isNotEmpty()) {
        val qrSize = (100 * scale).toInt()
        val qrBitmap = generateQRCode(settings.qrCodeData, qrSize)
        canvas.drawBitmap(qrBitmap, width - (120f * scale), height - (120f * scale), null)
    }

    document.finishPage(page)

    val file = File(context.getExternalFilesDir(null), "invoice_${System.currentTimeMillis()}.pdf")
    try {
        document.writeTo(FileOutputStream(file))
        Toast.makeText(context, "PDF Saved: ${file.absolutePath}", Toast.LENGTH_LONG).show()
        return file
    } catch (e: IOException) {
        e.printStackTrace()
        Toast.makeText(context, "Error saving PDF", Toast.LENGTH_SHORT).show()
        return null
    } finally {
        document.close()
    }
}

fun generateQRCode(text: String, size: Int): Bitmap {
    val bitMatrix: BitMatrix = MultiFormatWriter().encode(text, BarcodeFormat.QR_CODE, size, size)
    val width = bitMatrix.width
    val height = bitMatrix.height
    val pixels = IntArray(width * height)
    for (y in 0 until height) {
        for (x in 0 until width) {
            pixels[y * width + x] = if (bitMatrix[x, y]) 0xFF000000.toInt() else 0xFFFFFFFF.toInt()
        }
    }
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    bitmap.setPixels(pixels, 0, width, 0, 0, width, height)
    return bitmap
}
