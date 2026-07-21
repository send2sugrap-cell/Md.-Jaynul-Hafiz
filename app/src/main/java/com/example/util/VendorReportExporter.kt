package com.example.util

import android.content.Context
import android.print.PrintAttributes
import android.print.PrintManager
import android.webkit.WebView
import android.webkit.WebViewClient
import com.example.data.Vendor
import com.example.data.VendorPayment
import java.text.SimpleDateFormat
import java.util.*

object VendorReportExporter {

    fun exportPaymentReport(
        context: Context,
        payments: List<VendorPayment>,
        vendors: List<Vendor>,
        startDate: String,
        endDate: String
    ) {
        val htmlContent = generatePaymentReportHtml(payments, vendors, startDate, endDate)
        
        val webView = WebView(context)
        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                val printManager = context.getSystemService(Context.PRINT_SERVICE) as PrintManager
                val jobName = "Vendor_Payment_Report_${startDate}_to_${endDate}"
                val printAdapter = webView.createPrintDocumentAdapter(jobName)
                printManager.print(jobName, printAdapter, PrintAttributes.Builder().build())
            }
        }
        webView.loadDataWithBaseURL(null, htmlContent, "text/html", "UTF-8", null)
    }

    private fun generatePaymentReportHtml(
        payments: List<VendorPayment>,
        vendors: List<Vendor>,
        startDate: String,
        endDate: String
    ): String {
        val totalAmount = payments.sumOf { it.amount }
        val vendorMap = vendors.associateBy { it.id }

        val rowsHtml = payments.joinToString("") { payment ->
            val vendor = vendorMap[payment.vendorId]
            """
            <tr>
                <td>${payment.date}</td>
                <td>${vendor?.name ?: "Unknown"} (${vendor?.companyName ?: "N/A"})</td>
                <td>${payment.paymentMethod}</td>
                <td>${payment.mrNo ?: "-"}</td>
                <td>৳${payment.amount}</td>
            </tr>
            """
        }

        return """
        <!DOCTYPE html>
        <html>
        <head>
            <style>
                body { font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; padding: 20px; color: #333; }
                .header { text-align: center; margin-bottom: 30px; border-bottom: 2px solid #eee; padding-bottom: 20px; }
                h1 { margin: 0; color: #1a73e8; font-size: 24px; }
                .meta { color: #666; font-size: 14px; margin-top: 5px; }
                table { width: 100%; border-collapse: collapse; margin-top: 20px; }
                th { background-color: #f8f9fa; text-align: left; padding: 12px; border-bottom: 2px solid #dee2e6; font-size: 13px; }
                td { padding: 12px; border-bottom: 1px solid #eee; font-size: 13px; }
                .total-row { font-weight: bold; background-color: #f1f8ff; }
                .footer { margin-top: 40px; font-size: 12px; text-align: center; color: #999; }
                .summary { margin-top: 20px; text-align: right; }
                .summary-box { display: inline-block; padding: 15px; background: #f1f8ff; border-radius: 8px; border: 1px solid #d1e3ff; }
            </style>
        </head>
        <body>
            <div class="header">
                <h1>Vendor Payment Report</h1>
                <p class="meta">Date Range: $startDate to $endDate</p>
                <p class="meta">Generated on: ${SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())}</p>
            </div>

            <table>
                <thead>
                    <tr>
                        <th>Date</th>
                        <th>Vendor</th>
                        <th>Method</th>
                        <th>MR No.</th>
                        <th>Amount</th>
                    </tr>
                </thead>
                <tbody>
                    $rowsHtml
                </tbody>
            </table>

            <div class="summary">
                <div class="summary-box">
                    <strong>Total Outgoings:</strong>
                    <span style="font-size: 18px; color: #d93025; margin-left: 10px;">৳$totalAmount</span>
                </div>
            </div>

            <div class="footer">
                <p>Sucharu Graphics - Business Management System</p>
            </div>
        </body>
        </html>
        """.trimIndent()
    }
}
