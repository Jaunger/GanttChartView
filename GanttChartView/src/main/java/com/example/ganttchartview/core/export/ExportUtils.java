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

    /* ---------- CSV ---------- */

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

    /** Renders <var>root</var> (usually outerContainer) into a Bitmap. */
    public static Bitmap toBitmap(View root) {
        root.clearFocus();
        Bitmap bmp = Bitmap.createBitmap(root.getWidth(), root.getHeight(),
                Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(bmp);
        root.draw(c);
        return bmp;
    }

    /** Saves <var>bmp</var> to <code>Pictures/GanttSnapshots</code> and returns the File. */
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

    /** Shares <var>pngFile</var> with the system share sheet. */
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

    private static String safe(String s) { return s == null ? "" : s.replace("\"","\"\""); }

    private static File createFile(Context ctx, String fname) throws IOException {
        File dir = new File(ctx.getExternalFilesDir(
                Environment.DIRECTORY_DOCUMENTS), "GanttExports");
        if (!dir.exists() && !dir.mkdirs()) throw new IOException("mkdirs failed");
        return new File(dir, fname);
    }

    private static Uri uriFromFile(Context ctx, File f) {
        return FileProvider.getUriForFile(ctx,
                ctx.getPackageName() + ".fileprovider", f);
    }

    private ExportUtils() {}
}