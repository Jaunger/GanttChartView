package com.example.ganttchartview.core.export;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Environment;
import android.view.View;

import androidx.core.content.FileProvider;

import com.example.ganttchartview.model.GanttTask;
import com.example.ganttchartview.core.GanttChartView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

/**
 * Static helper for one-shot exports (called from activities/fragments).
 * ⚠️  All methods return a content-URI ready to be shared with an Intent.
 */
public class ExportUtils {

    /**
     * Exports a list of Gantt tasks to a CSV file and returns a sharable URI.
     * @param ctx The context.
     * @param tasks The list of tasks to export.
     * @param fileName The base file name (without extension).
     * @return A content URI for the exported CSV file.
     * @throws IOException If writing the file fails.
     */
    public static Uri exportCsv(Context ctx,
                                List<GanttTask> tasks,
                                String fileName) throws IOException {

        File csv = createFile(ctx, fileName + ".csv");
        try (FileOutputStream out = new FileOutputStream(csv)) {
            out.write("Title,Assigned,Start,End,Info\n".getBytes());
            for (GanttTask t : tasks) {
                String line = String.format("\"%s\",\"%s\",%s,%s,\"%s\"\n",
                        safe(t.getTitle()),
                        safe(t.getAssignedTo()),
                        t.getFormattedStart(),
                        t.getFormattedEnd(),
                        safe(t.getInfo()));
                out.write(line.getBytes());
            }
        }
        return uriFromFile(ctx, csv);
    }

    /* ---------- PDF (bitmap snapshot) ---------- */

    /**
     * Exports the current Gantt chart as a PDF snapshot and returns a sharable URI.
     * @param ctx The context.
     * @param chart The GanttChartView to export.
     * @param fileName The base file name (without extension).
     * @return A content URI for the exported PDF file.
     * @throws IOException If writing the file fails.
     */
    public static Uri exportPdf(Context ctx,
                                GanttChartView chart,
                                String fileName) throws IOException {

        // Render chart into bitmap
        Bitmap bmp = Bitmap.createBitmap(
                chart.getWidth(), chart.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(bmp);
        chart.draw(c);

        // Put bitmap into single-page PDF
        PdfDocument pdf = new PdfDocument();
        PdfDocument.PageInfo info = new PdfDocument.PageInfo
                .Builder(bmp.getWidth(), bmp.getHeight(), 1).create();
        PdfDocument.Page page = pdf.startPage(info);
        page.getCanvas().drawBitmap(bmp, 0, 0, null);
        pdf.finishPage(page);

        File pdfFile = createFile(ctx, fileName + ".pdf");
        try (FileOutputStream out = new FileOutputStream(pdfFile)) {
            pdf.writeTo(out);
        }
        pdf.close();
        bmp.recycle();

        return uriFromFile(ctx, pdfFile);
    }

    /**
     * Renders a View (usually the chart container) into a Bitmap.
     * @param root The root view to render.
     * @return The rendered Bitmap.
     */
    public static Bitmap toBitmap(View root) {
        root.clearFocus();
        Bitmap bmp = Bitmap.createBitmap(root.getWidth(), root.getHeight(),
                Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(bmp);
        root.draw(c);
        return bmp;
    }

    /**
     * Saves a Bitmap as a PNG file in Pictures/GanttSnapshots and returns the File.
     * @param ctx The context.
     * @param bmp The Bitmap to save.
     * @param fileNameNoExt The base file name (without extension).
     * @return The File object for the saved PNG.
     * @throws IOException If writing the file fails.
     */
    public static File savePng(Context ctx, Bitmap bmp, String fileNameNoExt)
            throws IOException {

        File dir = new File(ctx.getExternalFilesDir(
                Environment.DIRECTORY_PICTURES), "GanttSnapshots");
        if (!dir.exists() && !dir.mkdirs())
            throw new IOException("Cannot create dir: " + dir);

        File out = new File(dir, fileNameNoExt + ".png");
        try (FileOutputStream fos = new FileOutputStream(out)) {
            bmp.compress(Bitmap.CompressFormat.PNG, 100, fos);
        }
        return out;
    }

    /**
     * Shares a PNG file with the system share sheet.
     * @param ctx The context.
     * @param pngFile The PNG file to share.
     */
    public static void sharePng(Context ctx, File pngFile) {
        Uri uri = FileProvider.getUriForFile(
                ctx, ctx.getPackageName() + ".fileprovider", pngFile);

        Intent send = new Intent(Intent.ACTION_SEND)
                .setType("image/png")
                .putExtra(Intent.EXTRA_STREAM, uri)
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        ctx.startActivity(Intent.createChooser(send, "Share snapshot…"));
    }

    /* ---------- internal ---------- */

    /**
     * Escapes a string for safe CSV output.
     * @param s The string to escape.
     * @return The escaped string.
     */
    private static String safe(String s) { return s == null ? "" : s.replace("\"","\"\""); }

    /**
     * Creates a file in Documents/GanttExports for export.
     * @param ctx The context.
     * @param fname The file name.
     * @return The created File.
     * @throws IOException If the directory or file cannot be created.
     */
    private static File createFile(Context ctx, String fname) throws IOException {
        File dir = new File(ctx.getExternalFilesDir(
                Environment.DIRECTORY_DOCUMENTS), "GanttExports");
        if (!dir.exists() && !dir.mkdirs()) throw new IOException("mkdirs failed");
        return new File(dir, fname);
    }

    /**
     * Returns a content URI for a file using FileProvider.
     * @param ctx The context.
     * @param f The file.
     * @return The content URI.
     */
    private static Uri uriFromFile(Context ctx, File f) {
        return FileProvider.getUriForFile(ctx,
                ctx.getPackageName() + ".fileprovider", f);
    }

    private ExportUtils() {}
}