package com.example.ganttchartview.core.export;

import android.content.ContentValues;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.format.DateFormat;
import android.view.View;
import android.view.ViewGroup;
import android.widget.HorizontalScrollView;
import android.widget.ScrollView;

import androidx.annotation.NonNull;

import com.example.ganttchartview.core.GanttChartView;
import com.example.ganttchartview.model.GanttTask;
import com.example.ganttchartview.model.TimeScale;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Locale;

/**
 * 💾 **ExportUtils** – one-shot helpers that save the current Gantt data
 * as <br>
 * • CSV  → 📂 <strong>Documents/GanttExports</strong><br>
 * • PDF  → 📂 <strong>Documents/GanttExports</strong> (full-height snapshot)<br>
 * • PNG  → 🖼 <strong>Pictures/GanttSnapshots</strong> (MediaStore)   <br>
 * <p>
 * All files are written to <em>public</em> collections, so they appear
 * instantly in the system **Files** / **Gallery** apps – no SAF, no
 * `FileProvider` required.  Each method returns the final {@link Uri}
 * for further sharing if you like.
 */
public final class ExportUtils {

    /* ──────────────────────────────  CSV  ───────────────────────────── */

    /**
     * Exports the supplied task list as a CSV file in the public
     * **Documents/GanttExports** collection.
     *
     * @param ctx       app / activity context
     * @param tasks     list of tasks to export
     * @param scale     time scale (HOUR, DAY, MONTH) for date formatting
     * @param baseName  filename stem (no extension!)
     *
     * @return content Uri pointing at **Documents/GanttExports/\*.csv**
     *
     * @throws Exception on I/O errors
     */
    @NonNull
    public static Uri exportCsv(Context  ctx,
                                List<GanttTask> tasks,
                                TimeScale scale,          // ⬅️  new arg
                                String baseName) throws Exception {

        // choose a per-scale formatter
        final java.text.DateFormat fmt =
                (scale == TimeScale.HOUR)  ? new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()) :
                        (scale == TimeScale.DAY )  ? new java.text.SimpleDateFormat("EEE HH:mm",        Locale.getDefault()) :
                                /* MONTH */                  new java.text.SimpleDateFormat("dd MMM",          Locale.getDefault());

        String time = DateFormat.format("yyyyMMdd_HHmmss",
                System.currentTimeMillis()).toString();
        String fileName = baseName + "_" + time + ".csv";

        File outDir = ensureDir(Environment.DIRECTORY_DOCUMENTS, "GanttExports");
        File out    = new File(outDir, fileName);

        try (FileOutputStream fos = new FileOutputStream(out)) {
            fos.write("Title,Assigned,Start,End,Info\n".getBytes());
            for (GanttTask t : tasks) {
                String line = String.format("\"%s\",\"%s\",\"%s\",\"%s\",\"%s\"\n",
                        esc(t.getTitle()), esc(t.getAssignedTo()),
                        fmt.format(t.getStart()), fmt.format(t.getEnd()),
                        esc(t.getInfo()));
                fos.write(line.getBytes());
            }
        }
        return scanFile(ctx, out, "text/csv");
    }

    /* ──────────────────────────────  PDF  ───────────────────────────── */

    /**
     * Captures the <strong>entire</strong> {@link GanttChartView} – even the
     * part scrolled out of sight – into a single-page PDF.
     *
     * @param ctx        app / activity context
     * @param chart      the live view instance
     * @param baseName   filename stem (no extension!)
     * @return content Uri pointing at **Documents/GanttExports/\*.pdf**
     *
     * @throws Exception on I/O errors
     */
    @NonNull
    public static Uri exportPdf(@NonNull Context ctx,
                                @NonNull GanttChartView chart,
                                @NonNull String baseName) throws Exception {

        Bitmap bmp = renderFullSize(chart);                 // ← no cropping!

        String stamp = DateFormat.format("yyyyMMdd_HHmmss",
                System.currentTimeMillis()).toString();
        File dir = ensureDir(Environment.DIRECTORY_DOCUMENTS, "GanttExports");
        File pdfFile = new File(dir, baseName + '_' + stamp + ".pdf");

        PdfDocument pdf = new PdfDocument();
        PdfDocument.PageInfo info =
                new PdfDocument.PageInfo.Builder(bmp.getWidth(),
                        bmp.getHeight(), 1).create();

        PdfDocument.Page page = pdf.startPage(info);
        page.getCanvas().drawBitmap(bmp, 0, 0, null);
        pdf.finishPage(page);

        try (FileOutputStream fos = new FileOutputStream(pdfFile)) {
            pdf.writeTo(fos);
        }
        pdf.close();
        bmp.recycle();

        return scanFile(ctx, pdfFile, "application/pdf");
    }

    /* ──────────────────────────────  PNG  ───────────────────────────── */

    /**
     * Stores the supplied bitmap as a loss-less PNG in the public
     * **Pictures/GanttSnapshots** collection.  On Android Q+ it uses the
     * modern MediaStore API; on older releases it falls back to a direct
     * file-write + media-scan.
     *
     * @param ctx       context
     * @param gantt       bitmap to persist (caller may recycle afterwards)
     * @param baseName  filename stem (no extension!)
     *
     * @return MediaStore Uri (Q+) **or** file Uri (≤ P)
     */
    @NonNull
    public static Uri savePng(@NonNull Context ctx,
                              @NonNull  GanttChartView gantt,
                              @NonNull String baseName) throws Exception {

        String display = baseName + '_' +
                DateFormat.format("yyyyMMdd_HHmmss", System.currentTimeMillis()) + ".png";

        Bitmap bmp = renderFullSize(gantt);
        /* Android 10+ — scoped storage: write straight to MediaStore */
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ContentValues cv = new ContentValues();
            cv.put(MediaStore.Images.Media.DISPLAY_NAME, display);
            cv.put(MediaStore.Images.Media.MIME_TYPE,  "image/png");
            cv.put(MediaStore.Images.Media.RELATIVE_PATH,
                    Environment.DIRECTORY_PICTURES + "/GanttSnapshots");

            Uri uri = ctx.getContentResolver()
                    .insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, cv);
            if (uri == null) throw new Exception("MediaStore insert failed");

            try (OutputStream os = ctx.getContentResolver().openOutputStream(uri)) {
                if (os == null) throw new Exception("Cannot open output stream");
                bmp.compress(Bitmap.CompressFormat.PNG, 100, os);
            }
            return uri;
        }

        /* Pre-Android 10 — classic File API + media scan */
        File dir = ensureDir(Environment.DIRECTORY_PICTURES, "GanttSnapshots");
        File out = new File(dir, display);

        try (FileOutputStream fos = new FileOutputStream(out)) {
            bmp.compress(Bitmap.CompressFormat.PNG, 100, fos);
        }
        return scanFile(ctx, out, "image/png");
    }

    /* ─────────────────────────  public helpers  ───────────────────── */

    /**
     * Convenience: renders <em>exactly what’s on-screen right now</em> into a
     * bitmap.  (Use {@link #renderFullSize(View)} if you need off-screen
     * portions as well.)
     */
    @NonNull
    public static Bitmap toBitmap(@NonNull View v) {
        Bitmap bmp = Bitmap.createBitmap(v.getWidth(), v.getHeight(),
                Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(bmp);
        c.drawColor(Color.WHITE);    // white background for PDF/PNG
        v.draw(c);
        return bmp;
    }

    /**
     * Renders the *entire* logical content of a View to a bitmap.
     * <p>
     * If the supplied view is a {@link ScrollView} or
     * {@link HorizontalScrollView} we automatically switch to its first
     * (and usually only) child so that off-screen portions are captured.
     */
    @NonNull
    public static Bitmap renderFullSize(@NonNull View v) {

        // ── unwrap scroll-containers ───────────────────────────────
        View target = v;
        if ((v instanceof ScrollView) || (v instanceof HorizontalScrollView)) {
            ViewGroup vg = (ViewGroup) v;
            if (vg.getChildCount() > 0) target = vg.getChildAt(0);
        }

        // ── measure at *natural* size (UNSPECIFIED) ────────────────
        int wSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
        int hSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
        target.measure(wSpec, hSpec);
        target.layout(0, 0, target.getMeasuredWidth(), target.getMeasuredHeight());

        // ── draw to a fresh bitmap ─────────────────────────────────
        Bitmap bmp = Bitmap.createBitmap(target.getMeasuredWidth(),
                target.getMeasuredHeight(),
                Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(bmp);
        c.drawColor(Color.WHITE);
        target.draw(c);
        return bmp;
    }

    /* ───────────────────── internal tiny helpers ─────────────────── */

    /** Ensure a *public* sub-folder exists (e.g. Pictures/GanttSnapshots). */
    @NonNull
    private static File ensureDir(@NonNull String stdDir,
                                  @NonNull String sub) throws Exception {

        File dir = new File(Environment.getExternalStoragePublicDirectory(stdDir), sub);
        if (!dir.exists() && !dir.mkdirs())
            throw new Exception("mkdirs failed: " + dir);
        return dir;
    }

    /** Escape embedded quotes inside CSV cells. */
    @NonNull
    private static String esc(String s) { return s == null ? "" : s.replace("\"", "\"\""); }

    /** Kick MediaScanner so the new file shows up immediately. */
    @NonNull
    private static Uri scanFile(@NonNull Context ctx,
                                @NonNull File f,
                                @NonNull String mime) {

        android.media.MediaScannerConnection.scanFile(
                ctx,
                new String[]{f.getAbsolutePath()},
                new String[]{mime},
                null);
        return Uri.fromFile(f);
    }

    private ExportUtils() { /* no-instance */ }
}