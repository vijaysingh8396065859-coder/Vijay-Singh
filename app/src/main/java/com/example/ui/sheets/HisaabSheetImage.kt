package com.example.ui.sheets

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream

/** Sheet ki ek pankti: label, rakam ka text, aur kya ye katauti (minus) hai. */
data class SheetRow(
    val label: String,
    val valueText: String,
    val isDeduction: Boolean = false
)

/**
 * Supplier hisaab ki proper SHEET (photo) banata hai — WhatsApp par simple
 * text ki jagah ek saaf-suthri image jaati hai. Native Canvas isliye use
 * kiya hai taaki Hindi (Devanagari) sahi shape me render ho.
 */
object HisaabSheetImage {

    private const val WIDTH = 1080
    private const val MARGIN = 64f

    fun createSheetBitmap(
        supplierName: String,
        cycleText: String,
        rows: List<SheetRow>,
        totalLabel: String,
        totalText: String,
        noteText: String
    ): Bitmap {
        val headerH = 210f
        val titleBlockH = 170f
        val rowH = 74f
        val totalH = 110f
        val footerH = 90f

        val notePaint = TextPaint().apply {
            color = Color.rgb(0x44, 0x4A, 0x52)
            textSize = 36f
            typeface = android.graphics.Typeface.DEFAULT
        }
        val noteLayout = StaticLayout.Builder
            .obtain(noteText, 0, noteText.length, notePaint, (WIDTH - 2 * MARGIN).toInt())
            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .setLineSpacing(6f, 1.05f)
            .build()
        val noteH = noteLayout.height + 56f

        val height = (headerH + titleBlockH + rows.size * rowH + totalH + noteH + footerH).toInt()
        val bitmap = Bitmap.createBitmap(WIDTH, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.WHITE)

        // --- Header band (sky blue) ---
        val bandPaint = Paint().apply { color = Color.rgb(0x2E, 0x8B, 0xC4) }
        canvas.drawRect(0f, 0f, WIDTH.toFloat(), headerH, bandPaint)
        val titlePaint = Paint().apply {
            color = Color.WHITE
            textSize = 62f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            isAntiAlias = true
        }
        canvas.drawText("Milk Diary", MARGIN, 96f, titlePaint)
        val tagPaint = Paint().apply {
            color = Color.rgb(0xE3, 0xF2, 0xFD)
            textSize = 34f
            isAntiAlias = true
        }
        canvas.drawText("आपका विश्वास ही हमारी पहचान है", MARGIN, 158f, tagPaint)

        var y = headerH + 74f
        val namePaint = Paint().apply {
            color = Color.rgb(0x1A, 0x1A, 0x1A)
            textSize = 50f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            isAntiAlias = true
        }
        canvas.drawText("$supplierName — 10 दिन का हिसाब", MARGIN, y, namePaint)
        y += 56f
        val cyclePaint = Paint().apply {
            color = Color.rgb(0x5A, 0x6B, 0x77)
            textSize = 36f
            isAntiAlias = true
        }
        canvas.drawText(cycleText, MARGIN, y, cyclePaint)
        y += 44f

        val linePaint = Paint().apply {
            color = Color.rgb(0xD5, 0xDE, 0xE4)
            strokeWidth = 2f
        }
        canvas.drawLine(MARGIN, y, WIDTH - MARGIN, y, linePaint)
        y += 20f

        // --- Rows ---
        val labelPaint = Paint().apply {
            color = Color.rgb(0x24, 0x2B, 0x31)
            textSize = 40f
            isAntiAlias = true
        }
        val valuePaint = Paint().apply {
            color = Color.rgb(0x24, 0x2B, 0x31)
            textSize = 40f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            isAntiAlias = true
            textAlign = Paint.Align.RIGHT
        }
        val deductPaint = Paint().apply {
            color = Color.rgb(0xB0, 0x2A, 0x2A)
            textSize = 40f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            isAntiAlias = true
            textAlign = Paint.Align.RIGHT
        }
        rows.forEach { row ->
            y += rowH - 18f
            canvas.drawText(row.label, MARGIN, y, labelPaint)
            canvas.drawText(
                row.valueText,
                WIDTH - MARGIN,
                y,
                if (row.isDeduction) deductPaint else valuePaint
            )
            y += 18f
        }

        // --- Total ---
        canvas.drawLine(MARGIN, y + 8f, WIDTH - MARGIN, y + 8f, linePaint)
        y += 78f
        val totalLabelPaint = Paint().apply {
            color = Color.rgb(0x1A, 0x1A, 0x1A)
            textSize = 46f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            isAntiAlias = true
        }
        val totalValuePaint = Paint().apply {
            color = Color.rgb(0x1B, 0x6A, 0x3B)
            textSize = 52f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            isAntiAlias = true
            textAlign = Paint.Align.RIGHT
        }
        canvas.drawText(totalLabel, MARGIN, y, totalLabelPaint)
        canvas.drawText(totalText, WIDTH - MARGIN, y, totalValuePaint)
        y += 44f

        // --- Note ---
        canvas.save()
        canvas.translate(MARGIN, y)
        noteLayout.draw(canvas)
        canvas.restore()
        y += noteH - 10f

        // --- Footer ---
        val footerPaint = Paint().apply {
            color = Color.rgb(0x5A, 0x6B, 0x77)
            textSize = 32f
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(
            "— Milk Diary • शुद्ध दूध • साफ हिसाब —",
            WIDTH / 2f,
            y + 30f,
            footerPaint
        )
        return bitmap
    }

    /**
     * Sheet ko WhatsApp par bhejo — number ho to seedha usi chat me,
     * warna share chooser. Caption me chhota sa note jaata hai.
     */
    fun shareSheet(
        context: Context,
        bitmap: Bitmap,
        supplierName: String,
        phoneDigits: String,
        caption: String
    ) {
        val dir = File(context.cacheDir, "sheets").apply { mkdirs() }
        val file = File(dir, "hisaab_${supplierName.replace(" ", "_")}.png")
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
        val uri = FileProvider.getUriForFile(
            context,
            context.packageName + ".fileprovider",
            file
        )
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_TEXT, caption)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        if (phoneDigits.length >= 10) {
            sendIntent.setPackage("com.whatsapp")
            sendIntent.putExtra("jid", "91${phoneDigits.takeLast(10)}@s.whatsapp.net")
        }
        try {
            context.startActivity(sendIntent)
        } catch (e: Exception) {
            val chooser = Intent.createChooser(
                sendIntent.apply { setPackage(null) },
                "Sheet bhejo"
            )
            context.startActivity(chooser)
        }
    }
}
