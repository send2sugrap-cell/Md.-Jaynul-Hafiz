package com.example.util

import android.content.Context
import android.webkit.WebView
import android.webkit.WebViewClient
import android.print.PrintAttributes
import android.print.PrintManager
import com.example.data.Client
import com.example.data.Order
import java.text.SimpleDateFormat
import java.util.*

object HtmlPrintExporter {

    fun printInvoice(
        context: Context,
        client: Client,
        orders: List<Order>,
        totalAmount: Double,
        advanceAmount: Double,
        date: String,
        settings: InvoiceSettings
    ) {
        val htmlContent = generateInvoiceHtml(context, client, orders, totalAmount, advanceAmount, date, settings)
        
        val webView = WebView(context)
        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                val printManager = context.getSystemService(Context.PRINT_SERVICE) as PrintManager
                val printAdapter = webView.createPrintDocumentAdapter("Invoice_${client.name}")
                val jobName = "Invoice_${client.name}_${System.currentTimeMillis()}"
                printManager.print(jobName, printAdapter, PrintAttributes.Builder().build())
            }
        }
        
        webView.loadDataWithBaseURL(null, htmlContent, "text/html", "UTF-8", null)
    }

    fun generateInvoiceHtml(
        context: Context,
        client: Client,
        orders: List<Order>,
        totalAmount: Double,
        advanceAmount: Double,
        date: String,
        settings: InvoiceSettings
    ): String {
        val dueAmount = totalAmount - advanceAmount
        
        val logoBase64 = settings.logoUri?.let { uriString ->
            try {
                val uri = android.net.Uri.parse(uriString)
                val inputStream = context.contentResolver.openInputStream(uri)
                val bytes = inputStream?.readBytes()
                inputStream?.close()
                bytes?.let { android.util.Base64.encodeToString(it, android.util.Base64.DEFAULT) }
            } catch (e: Exception) {
                null
            }
        }

        val logoHtml = if (logoBase64 != null) {
            """<img src="data:image/png;base64,$logoBase64" style="max-height: 80px; margin-bottom: 10px; display: block;">"""
        } else ""

        val itemsHtml = orders.joinToString("") { order ->
            """
            <tr>
                <td>${order.jobName}<br><small>${order.specifications}</small></td>
                <td style="text-align: center;">${order.quantity}</td>
                <td style="text-align: right;">৳${String.format("%.2f", order.perUnitCost)}</td>
                <td style="text-align: right;">৳${String.format("%.2f", order.totalAmount)}</td>
            </tr>
            """.trimIndent()
        }

        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <style>
                    body {
                        font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
                        padding: 40px;
                        color: #1e293b;
                    }
                    .header {
                        display: flex;
                        justify-content: space-between;
                        margin-bottom: 40px;
                    }
                    .company-info h1 {
                        margin: 0;
                        color: #0f172a;
                        font-size: 24px;
                    }
                    .invoice-title {
                        text-align: right;
                    }
                    .invoice-title h2 {
                        margin: 0;
                        color: #0f172a;
                        font-size: 28px;
                        letter-spacing: 2px;
                    }
                    .details {
                        display: flex;
                        justify-content: space-between;
                        margin-bottom: 40px;
                    }
                    .details div {
                        width: 48%;
                    }
                    .details h4 {
                        margin: 0 0 8px 0;
                        color: #94a3b8;
                        font-size: 12px;
                        text-transform: uppercase;
                        letter-spacing: 1px;
                    }
                    table {
                        width: 100%;
                        border-collapse: collapse;
                        margin-bottom: 40px;
                    }
                    th {
                        background-color: #1e293b;
                        color: white;
                        text-align: left;
                        padding: 12px;
                        font-size: 14px;
                    }
                    td {
                        padding: 12px;
                        border-bottom: 1px solid #f1f5f9;
                        font-size: 14px;
                    }
                    .totals {
                        width: 300px;
                        margin-left: auto;
                    }
                    .totals div {
                        display: flex;
                        justify-content: space-between;
                        padding: 8px 0;
                    }
                    .totals .grand-total {
                        border-top: 2px solid #1e293b;
                        font-weight: bold;
                        font-size: 18px;
                        margin-top: 8px;
                    }
                    
                    /* Specific request: CSS Media Query for Print */
                    @media print {
                        .no-print, .ui-controls, button, input, .form-control {
                            display: none !important;
                        }
                        body {
                            padding: 0;
                            margin: 0;
                        }
                        @page {
                            margin: 1cm;
                        }
                        .invoice-container {
                            border: none;
                            box-shadow: none;
                        }
                    }
                </style>
            </head>
            <body>
                <div class="invoice-container">
                    <div class="header">
                        <div class="company-info">
                            $logoHtml
                            <h1>SUCHARU GRAPHICS</h1>
                            <p>Premium Printing Solutions</p>
                        </div>
                        <div class="invoice-title">
                            <h2>INVOICE</h2>
                            <p>INV-${date.replace("-", "")}-${client.id}</p>
                        </div>
                    </div>
                    
                    <div class="details">
                        <div>
                            <h4>BILLED TO</h4>
                            <p><strong>${client.name}</strong><br>
                            ${if (client.company.isNotBlank()) "${client.company}<br>" else ""}
                            Phone: ${client.mobile}</p>
                        </div>
                        <div style="text-align: right;">
                            <h4>INVOICE DETAILS</h4>
                            <p>Date: ${date}<br>
                            Status: ${if (dueAmount <= 0) "Paid" else "Due"}</p>
                        </div>
                    </div>
                    
                    <table>
                        <thead>
                            <tr>
                                <th>বিবরণ (Description)</th>
                                <th style="text-align: center;">পরিমাণ (Qty)</th>
                                <th style="text-align: right;">দাম (Rate)</th>
                                <th style="text-align: right;">টাকা (Total)</th>
                            </tr>
                        </thead>
                        <tbody>
                            $itemsHtml
                        </tbody>
                    </table>
                    
                    <div class="totals">
                        <div>
                            <span>Subtotal:</span>
                            <span>৳${String.format("%.2f", totalAmount)}</span>
                        </div>
                        <div>
                            <span>Advance Paid:</span>
                            <span>৳${String.format("%.2f", advanceAmount)}</span>
                        </div>
                        <div class="grand-total">
                            <span>Due Balance:</span>
                            <span>৳${String.format("%.2f", dueAmount)}</span>
                        </div>
                    </div>
                </div>
                
                <!-- Demonstrating the hiding of UI elements if they were added -->
                <div class="ui-controls no-print" style="margin-top: 50px; padding: 20px; background: #f8fafc; border: 1px dashed #cbd5e1;">
                    <p style="margin: 0; font-size: 12px; color: #64748b;">This section (UI Controls) is visible on screen but hidden during printing via @media print CSS.</p>
                    <button style="margin-top: 10px; padding: 8px 16px;">Hidden Button</button>
                    <input type="text" placeholder="Hidden Input" style="margin-top: 10px; padding: 8px;">
                </div>
            </body>
            </html>
        """.trimIndent()
    }
}
