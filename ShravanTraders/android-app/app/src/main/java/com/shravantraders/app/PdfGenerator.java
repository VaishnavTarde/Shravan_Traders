package com.shravantraders.app;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.pdf.PdfDocument;
import android.os.Environment;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class PdfGenerator {

    public static File generateInvoicePdf(Context context, JSONObject sale, String customerName) throws Exception {
        PdfDocument document = new PdfDocument();
        // A4 size in points: 595 x 842
        PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(595, 842, 1).create();
        PdfDocument.Page page = document.startPage(pageInfo);
        Canvas canvas = page.getCanvas();

        Paint headerPaint = new Paint();
        headerPaint.setColor(Color.rgb(232, 93, 4)); // Orange accent
        headerPaint.setTextSize(22);
        headerPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));

        Paint subHeaderPaint = new Paint();
        subHeaderPaint.setColor(Color.rgb(51, 65, 85));
        subHeaderPaint.setTextSize(11);

        Paint boldPaint = new Paint();
        boldPaint.setColor(Color.rgb(15, 23, 42));
        boldPaint.setTextSize(11);
        boldPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));

        Paint textPaint = new Paint();
        textPaint.setColor(Color.rgb(51, 65, 85));
        textPaint.setTextSize(11);

        Paint linePaint = new Paint();
        linePaint.setColor(Color.rgb(203, 213, 225));
        linePaint.setStrokeWidth(1);

        // Right-aligned paints for amounts
        Paint rightAlignPaint = new Paint(textPaint);
        rightAlignPaint.setTextAlign(Paint.Align.RIGHT);
        Paint rightAlignBold = new Paint(boldPaint);
        rightAlignBold.setTextAlign(Paint.Align.RIGHT);

        int logoSize = 90;
        int logoTop = 42;
        drawCrispLogo(context, canvas, 40, logoTop, logoSize);

        SharedPreferences prefs = context.getSharedPreferences("ShopPrefs", Context.MODE_PRIVATE);
        String shopName = prefs.getString("shopName", "SHRAVAN TRADERS (श्रवण ट्रेडर्स)");
        String shopDesc = prefs.getString("shopDesc", "Cattle Feed Shop • Kendal Bk., Taluka - Rahuri, Dist - Ahilyanagar");
        String shopContact = prefs.getString("shopContact", "Proprietor: Vinayak Bhau Tarde | Contact: +91 8390529481");

        // Header Text aligned to the right of the logo
        int textLeft = 40 + logoSize + 14;
        int y = 68;
        canvas.drawText(shopName, textLeft, y, headerPaint);
        y += 24;
        canvas.drawText(shopDesc, textLeft, y, subHeaderPaint);
        y += 18;
        canvas.drawText(shopContact, textLeft, y, subHeaderPaint);

        y = Math.max(y + 20, logoTop + logoSize + 12);
        canvas.drawLine(40, y, 555, y, linePaint);
        y += 25;

        // Invoice Meta
        String invoiceNo = sale.optString("invoiceNo", "ST-0000");
        String dateStr = sale.optString("createdAt", "");
        if (dateStr.length() >= 10) {
            dateStr = dateStr.substring(0, 10);
        } else {
            dateStr = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        }

        canvas.drawText("TAX INVOICE / BILL", 40, y, boldPaint);
        canvas.drawText("Invoice No: " + invoiceNo, 360, y, boldPaint);
        y += 18;
        canvas.drawText("Customer: " + (customerName != null && !customerName.isEmpty() ? customerName : "Walk-in Customer"), 40, y, textPaint);
        canvas.drawText("Date: " + dateStr, 360, y, textPaint);
        y += 18;
        
        String paymentMethod = sale.optString("paymentMethod", "Cash");
        if (paymentMethod.equalsIgnoreCase("upi")) paymentMethod = "Online / UPI";
        
        canvas.drawText("Payment Method: " + paymentMethod.toUpperCase(Locale.US), 40, y, boldPaint);
        y += 25;

        // Table Header with Borders
        Paint headerBgPaint = new Paint();
        headerBgPaint.setColor(Color.rgb(241, 245, 249));
        Paint strokePaint = new Paint();
        strokePaint.setColor(Color.rgb(203, 213, 225));
        strokePaint.setStyle(Paint.Style.STROKE);
        strokePaint.setStrokeWidth(1);

        canvas.drawRect(40, y, 555, y + 25, headerBgPaint);
        canvas.drawRect(40, y, 555, y + 25, strokePaint);
        canvas.drawText("S.No", 45, y + 17, boldPaint);
        canvas.drawText("Item / Product", 75, y + 17, boldPaint);
        canvas.drawText("Unit", 275, y + 17, boldPaint);
        canvas.drawText("Qty", 360, y + 17, rightAlignBold);
        canvas.drawText("Rate (₹)", 450, y + 17, rightAlignBold);
        canvas.drawText("Total (₹)", 555, y + 17, rightAlignBold);
        y += 25;

        // Items
        JSONArray items = sale.optJSONArray("items");
        if (items != null) {
            for (int i = 0; i < items.length(); i++) {
                JSONObject item = items.getJSONObject(i);
                
                String nameMr = item.optString("nameMr", "");
                String nameEn = item.optString("nameEn", "");
                String name = "";
                if (!nameMr.isEmpty() && !nameEn.isEmpty()) {
                    name = nameMr + " (" + nameEn + ")";
                } else if (!nameMr.isEmpty()) {
                    name = nameMr;
                } else {
                    name = nameEn.isEmpty() ? "Product" : nameEn;
                }
                
                String unit = item.optString("unit", "bag");
                double qty = item.optDouble("quantity", 1);
                double price = item.optDouble("unitPrice", 0);
                double total = item.optDouble("lineTotal", qty * price);

                y += 20;
                canvas.drawText(String.valueOf(i + 1), 45, y, textPaint);
                
                // Truncate long names safely to prevent column overlaps
                if (name.length() > 28) name = name.substring(0, 26) + "...";
                canvas.drawText(name, 75, y, textPaint);
                
                canvas.drawText(unit, 275, y, textPaint);
                canvas.drawText(String.format(Locale.US, "%.2f", qty), 360, y, rightAlignPaint);
                canvas.drawText(String.format(Locale.US, "%.2f", price), 450, y, rightAlignPaint);
                canvas.drawText(String.format(Locale.US, "%.2f", total), 555, y, rightAlignPaint);

                y += 10;
                canvas.drawLine(40, y, 555, y, linePaint);
            }
        }

        y += 20;

        // Summary Calculations
        double subtotal = sale.optDouble("subtotal", 0);
        double discount = sale.optDouble("discount", 0);
        double newSaleTotal = sale.optDouble("total", subtotal - discount);
        double previousBalance = sale.optDouble("previousBalance", 0);
        double totalPayable = newSaleTotal + previousBalance;
        double paidAmount = sale.optDouble("paidAmount", newSaleTotal);
        double remaining = totalPayable - paidAmount;

        canvas.drawText("Subtotal:", 380, y, textPaint);
        canvas.drawText(String.format(Locale.US, "₹%.2f", subtotal), 555, y, rightAlignPaint);
        y += 18;

        if (discount > 0) {
            canvas.drawText("Discount:", 380, y, textPaint);
            canvas.drawText(String.format(Locale.US, "- ₹%.2f", discount), 555, y, rightAlignPaint);
            y += 18;
        }

        canvas.drawText("New Sale Total:", 380, y, boldPaint);
        canvas.drawText(String.format(Locale.US, "₹%.2f", newSaleTotal), 555, y, rightAlignBold);
        y += 18;

        if (previousBalance > 0) {
            canvas.drawText("Previous Balance:", 380, y, textPaint);
            canvas.drawText(String.format(Locale.US, "₹%.2f", previousBalance), 555, y, rightAlignPaint);
            y += 18;
            
            canvas.drawText("Total Payable:", 380, y, boldPaint);
            canvas.drawText(String.format(Locale.US, "₹%.2f", totalPayable), 555, y, rightAlignBold);
            y += 18;
        }

        double cashAmt = sale.optDouble("cashAmount", 0);
        double upiAmt = sale.optDouble("upiAmount", 0);
        String upiRef = sale.optString("upiRefNo", "");
        String payMethod = sale.optString("paymentMethod", "cash");

        if (cashAmt > 0 || upiAmt > 0) {
            canvas.drawText("Payment Breakdown:", 380, y, boldPaint);
            y += 16;
            if (cashAmt > 0) {
                canvas.drawText(" Cash Received:", 380, y, textPaint);
                canvas.drawText(String.format(Locale.US, "₹%.2f", cashAmt), 555, y, rightAlignPaint);
                y += 16;
            }
            if (upiAmt > 0) {
                String upiLabel = upiRef.isEmpty() ? " UPI Received:" : " UPI (" + upiRef + "):";
                canvas.drawText(upiLabel, 380, y, textPaint);
                canvas.drawText(String.format(Locale.US, "₹%.2f", upiAmt), 555, y, rightAlignPaint);
                y += 16;
            }
            canvas.drawText(" Total Received Now:", 380, y, boldPaint);
            canvas.drawText(String.format(Locale.US, "₹%.2f", paidAmount), 555, y, rightAlignBold);
            y += 18;
        } else {
            canvas.drawText("Paid Amount (" + payMethod.toUpperCase() + "):", 380, y, textPaint);
            canvas.drawText(String.format(Locale.US, "₹%.2f", paidAmount), 555, y, rightAlignPaint);
            y += 18;
        }

        if (remaining > 0) {
            Paint alertPaint = new Paint(boldPaint);
            alertPaint.setColor(Color.rgb(220, 38, 38));
            alertPaint.setTextAlign(Paint.Align.RIGHT);
            canvas.drawText("Outstanding Credit:", 380, y, boldPaint);
            canvas.drawText(String.format(Locale.US, "₹%.2f", remaining), 555, y, alertPaint);
            y += 18;
        } else if (remaining < 0) {
            canvas.drawText("Advance / Overpaid:", 380, y, textPaint);
            canvas.drawText(String.format(Locale.US, "₹%.2f", Math.abs(remaining)), 555, y, rightAlignPaint);
            y += 18;
        }

        y += 20;
        canvas.drawText("Amount in Words: " + convertNumberToWords(totalPayable), 40, y, boldPaint);

        y += 30;
        canvas.drawLine(40, y, 555, y, linePaint);
        y += 40;

        canvas.drawText("Thank you for your business! / आमच्याशी व्यवसाय केल्याबद्दल धन्यवाद!", 40, y, subHeaderPaint);
        canvas.drawText("Authorized Signatory (Shravan Traders)", 340, y, boldPaint);

        document.finishPage(page);

        File dir = new File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "Invoices");
        if (!dir.exists()) dir.mkdirs();

        File file = new File(dir, "Invoice_" + invoiceNo + ".pdf");
        try (FileOutputStream out = new FileOutputStream(file)) {
            document.writeTo(out);
        } finally {
            document.close();
        }

        return file;
    }

    public static File generateReportPdf(Context context, JSONObject summaryData, String dateRangeTitle) throws Exception {
        PdfDocument document = new PdfDocument();
        PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(595, 842, 1).create();
        int pageNum = 1;
        PdfDocument.Page page = document.startPage(pageInfo);
        Canvas canvas = page.getCanvas();

        Paint headerPaint = new Paint();
        headerPaint.setColor(Color.rgb(232, 93, 4));
        headerPaint.setTextSize(22);
        headerPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));

        Paint subHeaderPaint = new Paint();
        subHeaderPaint.setColor(Color.rgb(71, 85, 105));
        subHeaderPaint.setTextSize(11);

        Paint boldPaint = new Paint();
        boldPaint.setColor(Color.rgb(15, 23, 42));
        boldPaint.setTextSize(12);
        boldPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));

        Paint textPaint = new Paint();
        textPaint.setColor(Color.rgb(51, 65, 85));
        textPaint.setTextSize(11);

        Paint linePaint = new Paint();
        linePaint.setColor(Color.rgb(203, 213, 225));
        linePaint.setStrokeWidth(1);

        Paint rightAlignPaint = new Paint(textPaint);
        rightAlignPaint.setTextAlign(Paint.Align.RIGHT);

        Paint rightAlignBold = new Paint(boldPaint);
        rightAlignBold.setTextAlign(Paint.Align.RIGHT);

        Paint cardBgPaint = new Paint();
        cardBgPaint.setColor(Color.rgb(248, 250, 252));

        Paint cardBorderPaint = new Paint();
        cardBorderPaint.setColor(Color.rgb(203, 213, 225));
        cardBorderPaint.setStyle(Paint.Style.STROKE);
        cardBorderPaint.setStrokeWidth(1);

        Paint headerBgPaint = new Paint();
        headerBgPaint.setColor(Color.rgb(241, 245, 249));

        int logoSize = 90;
        int logoTop = 42;
        drawCrispLogo(context, canvas, 40, logoTop, logoSize);

        SharedPreferences prefs = context.getSharedPreferences("ShopPrefs", Context.MODE_PRIVATE);
        String shopName = prefs.getString("shopName", "SHRAVAN TRADERS (श्रवण ट्रेडर्स)");

        // Document Header aligned to the right of the logo
        int textLeft = 40 + logoSize + 14;
        int y = 68;
        canvas.drawText(shopName, textLeft, y, headerPaint);
        y += 24;
        canvas.drawText("FINANCIAL REPORT | Period: " + dateRangeTitle, textLeft, y, boldPaint);
        y += 18;
        canvas.drawText("Generated on: " + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(new Date()), textLeft, y, subHeaderPaint);

        y = Math.max(y + 20, logoTop + logoSize + 12);
        canvas.drawLine(40, y, 555, y, linePaint);
        y += 20;

        // Executive Summary Card Container
        JSONObject t = summaryData.optJSONObject("totals");
        if (t != null) {
            canvas.drawText("Executive Financial Summary", 40, y, boldPaint);
            y += 12;

            int cardTop = y;
            int cardHeight = 175;
            canvas.drawRoundRect(new RectF(40, cardTop, 555, cardTop + cardHeight), 8, 8, cardBgPaint);
            canvas.drawRoundRect(new RectF(40, cardTop, 555, cardTop + cardHeight), 8, 8, cardBorderPaint);

            int sy = cardTop + 22;
            String[][] leftMetrics = {
                {"Sales Revenue:", String.format(Locale.US, "₹%.2f", t.optDouble("salesRevenue", 0))},
                {"Cost of Goods Sold (COGS):", String.format(Locale.US, "₹%.2f", t.optDouble("costOfGoodsSold", 0))},
                {"Gross Profit:", String.format(Locale.US, "₹%.2f", t.optDouble("grossProfit", 0))},
                {"Net Profit:", String.format(Locale.US, "₹%.2f", t.optDouble("grossProfit", t.optDouble("netProfit", 0)))}
            };

            for (String[] row : leftMetrics) {
                canvas.drawText(row[0], 55, sy, textPaint);
                canvas.drawText(row[1], 280, sy, rightAlignBold);
                sy += 28;
            }

            sy = cardTop + 22;
            String[][] rightMetrics = {
                {"Cash Collected:", String.format(Locale.US, "₹%.2f", t.optDouble("cashCollected", 0))},
                {"UPI Collected:", String.format(Locale.US, "₹%.2f", t.optDouble("upiCollected", 0))},
                {"Total Received:", String.format(Locale.US, "₹%.2f", t.optDouble("paymentsReceived", 0))},
                {"Outstanding Credit:", String.format(Locale.US, "₹%.2f", t.optDouble("totalOutstandingCredit", 0))},
                {"Inventory Stock Value:", String.format(Locale.US, "₹%.2f", t.optDouble("stockValue", 0))}
            };

            for (String[] row : rightMetrics) {
                canvas.drawText(row[0], 310, sy, textPaint);
                canvas.drawText(row[1], 540, sy, rightAlignBold);
                sy += 28;
            }

            y = cardTop + cardHeight + 25;
        }

        // Sales History Table
        JSONArray sales = summaryData.optJSONArray("sales");
        if (sales != null && sales.length() > 0) {
            if (y > 720) {
                document.finishPage(page);
                pageInfo = new PdfDocument.PageInfo.Builder(595, 842, ++pageNum).create();
                page = document.startPage(pageInfo);
                canvas = page.getCanvas();
                y = 50;
            }

            canvas.drawText("Sales History", 40, y, boldPaint);
            y += 12;

            y = drawSalesTableHeader(canvas, y, headerBgPaint, cardBorderPaint, boldPaint, rightAlignBold);

            for (int i = 0; i < sales.length(); i++) {
                JSONObject s = sales.getJSONObject(i);
                if (y > 780) {
                    document.finishPage(page);
                    pageInfo = new PdfDocument.PageInfo.Builder(595, 842, ++pageNum).create();
                    page = document.startPage(pageInfo);
                    canvas = page.getCanvas();
                    y = 50;
                    y = drawSalesTableHeader(canvas, y, headerBgPaint, cardBorderPaint, boldPaint, rightAlignBold);
                }

                String date = s.optString("createdAt", "");
                if (date.length() >= 10) date = date.substring(0, 10);

                String inv = s.optString("invoiceNo", "");
                if (inv.length() > 18) inv = inv.substring(0, 16) + "...";

                double total = s.optDouble("total", 0);
                double paid = s.optDouble("paidAmount", 0);
                double due = total - paid;

                canvas.drawText(date, 45, y, textPaint);
                canvas.drawText(inv, 120, y, textPaint);
                canvas.drawText(String.format(Locale.US, "%.2f", total), 350, y, rightAlignPaint);
                canvas.drawText(String.format(Locale.US, "%.2f", paid), 450, y, rightAlignPaint);

                if (due > 0) {
                    Paint alertPaint = new Paint(rightAlignPaint);
                    alertPaint.setColor(Color.rgb(220, 38, 38));
                    canvas.drawText(String.format(Locale.US, "%.2f", due), 555, y, alertPaint);
                } else {
                    canvas.drawText(String.format(Locale.US, "%.2f", due), 555, y, rightAlignPaint);
                }

                y += 8;
                canvas.drawLine(40, y, 555, y, linePaint);
                y += 16;
            }
            y += 20;
        }

        // Pending Collections Table
        JSONArray customerBalances = summaryData.optJSONArray("customerBalances");
        if (customerBalances != null && customerBalances.length() > 0) {
            if (y > 720) {
                document.finishPage(page);
                pageInfo = new PdfDocument.PageInfo.Builder(595, 842, ++pageNum).create();
                page = document.startPage(pageInfo);
                canvas = page.getCanvas();
                y = 50;
            }

            canvas.drawText("Pending Collections (Customer Balances)", 40, y, boldPaint);
            y += 12;

            y = drawCollectionsTableHeader(canvas, y, headerBgPaint, cardBorderPaint, boldPaint, rightAlignBold);

            for (int i = 0; i < customerBalances.length(); i++) {
                JSONObject c = customerBalances.getJSONObject(i);
                if (y > 780) {
                    document.finishPage(page);
                    pageInfo = new PdfDocument.PageInfo.Builder(595, 842, ++pageNum).create();
                    page = document.startPage(pageInfo);
                    canvas = page.getCanvas();
                    y = 50;
                    y = drawCollectionsTableHeader(canvas, y, headerBgPaint, cardBorderPaint, boldPaint, rightAlignBold);
                }

                String name = c.optString("name", "");
                if (name.length() > 22) name = name.substring(0, 20) + "...";

                String mobile = c.optString("mobile", "");
                if (mobile.length() > 12) mobile = mobile.substring(0, 12);

                double billed = c.optDouble("billed", 0);
                double paid = c.optDouble("paid", 0);
                double balance = c.optDouble("balance", 0);

                canvas.drawText(name, 45, y, textPaint);
                canvas.drawText(mobile, 215, y, textPaint);
                canvas.drawText(String.format(Locale.US, "%.0f", billed), 370, y, rightAlignPaint);
                canvas.drawText(String.format(Locale.US, "%.0f", paid), 460, y, rightAlignPaint);

                Paint alertPaint = new Paint(rightAlignPaint);
                alertPaint.setColor(Color.rgb(220, 38, 38));
                canvas.drawText(String.format(Locale.US, "%.0f", balance), 555, y, alertPaint);

                y += 8;
                canvas.drawLine(40, y, 555, y, linePaint);
                y += 16;
            }
            y += 20;
        }

        document.finishPage(page);

        File dir = new File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "Reports");
        if (!dir.exists()) dir.mkdirs();

        File file = new File(dir, "Report_" + System.currentTimeMillis() + ".pdf");
        try (FileOutputStream out = new FileOutputStream(file)) {
            document.writeTo(out);
        } finally {
            document.close();
        }

        return file;
    }

    private static int drawSalesTableHeader(Canvas canvas, int y, Paint bgPaint, Paint borderPaint, Paint boldPaint, Paint rightAlignBold) {
        canvas.drawRect(40, y, 555, y + 24, bgPaint);
        canvas.drawRect(40, y, 555, y + 24, borderPaint);
        canvas.drawText("Date", 45, y + 16, boldPaint);
        canvas.drawText("Invoice No", 120, y + 16, boldPaint);
        canvas.drawText("Total (₹)", 350, y + 16, rightAlignBold);
        canvas.drawText("Paid (₹)", 450, y + 16, rightAlignBold);
        canvas.drawText("Due (₹)", 555, y + 16, rightAlignBold);
        return y + 36;
    }

    private static int drawExpensesTableHeader(Canvas canvas, int y, Paint bgPaint, Paint borderPaint, Paint boldPaint, Paint rightAlignBold) {
        canvas.drawRect(40, y, 555, y + 24, bgPaint);
        canvas.drawRect(40, y, 555, y + 24, borderPaint);
        canvas.drawText("Date", 45, y + 16, boldPaint);
        canvas.drawText("Title / Description", 120, y + 16, boldPaint);
        canvas.drawText("Category", 310, y + 16, boldPaint);
        canvas.drawText("Amount (₹)", 555, y + 16, rightAlignBold);
        return y + 36;
    }

    private static int drawCollectionsTableHeader(Canvas canvas, int y, Paint bgPaint, Paint borderPaint, Paint boldPaint, Paint rightAlignBold) {
        canvas.drawRect(40, y, 555, y + 24, bgPaint);
        canvas.drawRect(40, y, 555, y + 24, borderPaint);
        canvas.drawText("Customer Name", 45, y + 16, boldPaint);
        canvas.drawText("Mobile", 215, y + 16, boldPaint);
        canvas.drawText("Billed (₹)", 370, y + 16, rightAlignBold);
        canvas.drawText("Paid (₹)", 460, y + 16, rightAlignBold);
        canvas.drawText("Pending (₹)", 555, y + 16, rightAlignBold);
        return y + 36;
    }

    public static File generateCustomerPassbookPdf(Context context, JSONObject stmt, String customerName) throws Exception {
        return generateCustomerStatementPdf(context, stmt);
    }

    public static File generateCustomerStatementPdf(Context context, JSONObject stmt) throws Exception {
        PdfDocument document = new PdfDocument();
        PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(595, 842, 1).create();
        PdfDocument.Page page = document.startPage(pageInfo);
        Canvas canvas = page.getCanvas();

        Paint headerPaint = new Paint();
        headerPaint.setColor(Color.rgb(232, 93, 4));
        headerPaint.setTextSize(22);
        headerPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));

        Paint subHeaderPaint = new Paint();
        subHeaderPaint.setColor(Color.rgb(51, 65, 85));
        subHeaderPaint.setTextSize(11);

        Paint boldPaint = new Paint();
        boldPaint.setColor(Color.rgb(15, 23, 42));
        boldPaint.setTextSize(11);
        boldPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));

        Paint textPaint = new Paint();
        textPaint.setColor(Color.rgb(51, 65, 85));
        textPaint.setTextSize(11);

        Paint linePaint = new Paint();
        linePaint.setColor(Color.rgb(203, 213, 225));
        linePaint.setStrokeWidth(1);

        Paint rightAlignPaint = new Paint(textPaint);
        rightAlignPaint.setTextAlign(Paint.Align.RIGHT);
        Paint rightAlignBold = new Paint(boldPaint);
        rightAlignBold.setTextAlign(Paint.Align.RIGHT);

        int logoSize = 90;
        int logoTop = 42;
        drawCrispLogo(context, canvas, 40, logoTop, logoSize);

        SharedPreferences prefs = context.getSharedPreferences("ShopPrefs", Context.MODE_PRIVATE);
        String shopName = prefs.getString("shopName", "SHRAVAN TRADERS (श्रवण ट्रेडर्स)");
        String shopDesc = prefs.getString("shopDesc", "Cattle Feed Shop • Kendal Bk., Taluka - Rahuri, Dist - Ahilyanagar");
        String shopContact = prefs.getString("shopContact", "Proprietor: Vinayak Bhau Tarde | Contact: +91 8390529481");

        int textLeft = 40 + logoSize + 14;
        int y = 68;
        canvas.drawText(shopName, textLeft, y, headerPaint);
        y += 24;
        canvas.drawText(shopDesc, textLeft, y, subHeaderPaint);
        y += 18;
        canvas.drawText(shopContact, textLeft, y, subHeaderPaint);

        y = Math.max(y + 20, logoTop + logoSize + 12);
        canvas.drawLine(40, y, 555, y, linePaint);
        y += 25;

        JSONObject customer = stmt.optJSONObject("customer");
        String name = customer != null ? customer.optString("name", "Unknown") : "Unknown";
        String mobile = customer != null ? customer.optString("mobile", "") : "";

        canvas.drawText("CUSTOMER LEDGER / STATEMENT", 40, y, boldPaint);
        y += 18;
        canvas.drawText("Customer Name: " + name, 40, y, textPaint);
        canvas.drawText("Mobile: " + mobile, 360, y, textPaint);
        y += 18;
        canvas.drawText("Generated on: " + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(new Date()), 40, y, textPaint);
        y += 25;

        // Summary
        JSONObject summary = stmt.optJSONObject("summary");
        if (summary != null) {
            canvas.drawText("Total Billed: ₹" + String.format(Locale.US, "%.0f", summary.optDouble("totalBilled", 0)), 40, y, boldPaint);
            canvas.drawText("Total Paid: ₹" + String.format(Locale.US, "%.0f", summary.optDouble("totalPaid", 0)), 220, y, boldPaint);
            
            Paint redBold = new Paint(boldPaint);
            redBold.setColor(Color.rgb(220, 38, 38));
            canvas.drawText("Remaining Due: ₹" + String.format(Locale.US, "%.0f", summary.optDouble("creditBalance", 0)), 400, y, redBold);
        }
        y += 25;

        // Table Header
        Paint headerBgPaint = new Paint();
        headerBgPaint.setColor(Color.rgb(241, 245, 249));
        Paint strokePaint = new Paint();
        strokePaint.setColor(Color.rgb(203, 213, 225));
        strokePaint.setStyle(Paint.Style.STROKE);
        strokePaint.setStrokeWidth(1);

        canvas.drawRect(40, y, 555, y + 25, headerBgPaint);
        canvas.drawRect(40, y, 555, y + 25, strokePaint);
        canvas.drawText("Date", 45, y + 17, boldPaint);
        canvas.drawText("Ref/Type", 110, y + 17, boldPaint);
        canvas.drawText("Description", 185, y + 17, boldPaint);
        canvas.drawText("Debit (₹)", 380, y + 17, rightAlignBold);
        canvas.drawText("Credit (₹)", 470, y + 17, rightAlignBold);
        canvas.drawText("Balance", 555, y + 17, rightAlignBold);
        y += 25;

        JSONArray tx = stmt.optJSONArray("transactions");
        if (tx != null) {
            for (int i = 0; i < tx.length(); i++) {
                JSONObject t = tx.optJSONObject(i);
                if (t == null) continue;
                
                y += 20;
                if (y > 780) {
                    document.finishPage(page);
                    pageInfo = new PdfDocument.PageInfo.Builder(595, 842, 1).create();
                    page = document.startPage(pageInfo);
                    canvas = page.getCanvas();
                    y = 50;

                    canvas.drawRect(40, y, 555, y + 25, headerBgPaint);
                    canvas.drawRect(40, y, 555, y + 25, strokePaint);
                    canvas.drawText("Date", 45, y + 17, boldPaint);
                    canvas.drawText("Ref/Type", 110, y + 17, boldPaint);
                    canvas.drawText("Description", 185, y + 17, boldPaint);
                    canvas.drawText("Debit (₹)", 380, y + 17, rightAlignBold);
                    canvas.drawText("Credit (₹)", 470, y + 17, rightAlignBold);
                    canvas.drawText("Balance", 555, y + 17, rightAlignBold);
                    y += 25;
                }
                
                String date = t.optString("date", "");
                if (date.length() >= 10) date = date.substring(0, 10);
                
                String ref = t.optString("reference", "");
                if (ref.length() > 9) ref = ref.substring(0, 8) + "..";
                
                String desc = t.optString("description", "");
                if (desc.length() > 22) desc = desc.substring(0, 20) + "..";

                canvas.drawText(date, 45, y, textPaint);
                canvas.drawText(ref, 110, y, textPaint);
                canvas.drawText(desc, 185, y, textPaint);
                
                double debit = t.optDouble("debit", 0);
                double credit = t.optDouble("credit", 0);
                double bal = t.optDouble("runningBalance", 0);
                
                if (debit > 0) canvas.drawText(String.format(Locale.US, "%.0f", debit), 380, y, rightAlignPaint);
                if (credit > 0) canvas.drawText(String.format(Locale.US, "%.0f", credit), 470, y, rightAlignPaint);
                canvas.drawText(String.format(Locale.US, "%.0f", bal), 555, y, rightAlignPaint);

                y += 8;
                canvas.drawLine(40, y, 555, y, linePaint);
            }
        }
        y += 30;
        
        canvas.drawText("Thank you for your business! / आमच्याशी व्यवसाय केल्याबद्दल धन्यवाद!", 40, y, subHeaderPaint);

        document.finishPage(page);

        File dir = new File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "Ledgers");
        if (!dir.exists()) dir.mkdirs();

        File file = new File(dir, "Statement_" + name.replace(" ", "_") + "_" + System.currentTimeMillis() + ".pdf");
        try (FileOutputStream out = new FileOutputStream(file)) {
            document.writeTo(out);
        } finally {
            document.close();
        }

        return file;
    }

    public static String convertNumberToWords(double amount) {
        long n = (long) Math.round(amount);
        if (n == 0) return "Zero Rupees Only";
        
        String[] units = {"", "One", "Two", "Three", "Four", "Five", "Six", "Seven", "Eight", "Nine", "Ten", "Eleven", "Twelve", "Thirteen", "Fourteen", "Fifteen", "Sixteen", "Seventeen", "Eighteen", "Nineteen"};
        String[] tens = {"", "", "Twenty", "Thirty", "Forty", "Fifty", "Sixty", "Seventy", "Eighty", "Ninety"};
        
        if (n < 20) return units[(int)n] + " Rupees Only";
        if (n < 100) return tens[(int)(n / 10)] + ((n % 10 != 0) ? " " + units[(int)(n % 10)] : "") + " Rupees Only";
        if (n < 1000) return units[(int)(n / 100)] + " Hundred" + ((n % 100 != 0) ? " and " + convertNumberToWords(n % 100).replace(" Rupees Only", "") : "") + " Rupees Only";
        if (n < 100000) return convertNumberToWords(n / 1000).replace(" Rupees Only", "") + " Thousand" + ((n % 1000 != 0) ? " " + convertNumberToWords(n % 1000).replace(" Rupees Only", "") : "") + " Rupees Only";
        if (n < 10000000) return convertNumberToWords(n / 100000).replace(" Rupees Only", "") + " Lakh" + ((n % 100000 != 0) ? " " + convertNumberToWords(n % 100000).replace(" Rupees Only", "") : "") + " Rupees Only";
        return convertNumberToWords(n / 10000000).replace(" Rupees Only", "") + " Crore" + ((n % 10000000 != 0) ? " " + convertNumberToWords(n % 10000000).replace(" Rupees Only", "") : "") + " Rupees Only";
    }

    private static void drawCrispLogo(Context context, Canvas canvas, int left, int top, int size) {
        try {
            BitmapFactory.Options opts = new BitmapFactory.Options();
            opts.inScaled = false;
            opts.inPreferredConfig = Bitmap.Config.ARGB_8888;
            Bitmap logoBmp = BitmapFactory.decodeResource(context.getResources(), R.drawable.shravan_traders_logo, opts);
            if (logoBmp != null) {
                Paint logoPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
                logoPaint.setDither(true);
                Rect srcRect = new Rect(0, 0, logoBmp.getWidth(), logoBmp.getHeight());
                Rect dstRect = new Rect(left, top, left + size, top + size);
                canvas.drawBitmap(logoBmp, srcRect, dstRect, logoPaint);
            }
        } catch (Exception ignored) {}
    }
}
