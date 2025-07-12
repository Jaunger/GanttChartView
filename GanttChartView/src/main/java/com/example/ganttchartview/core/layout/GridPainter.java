package com.example.ganttchartview.core.layout;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;

import androidx.appcompat.widget.AppCompatTextView;
import androidx.appcompat.widget.LinearLayoutCompat;

import java.util.ArrayList;
import java.util.List;

public class GridPainter {


    /**
     * Adds a header cell to the given row with the specified label, width, and text size.
     *
     * @param ctx        The context to create the TextView.
     * @param row        The LinearLayoutCompat row to which the header cell will be added.
     * @param label      The text label for the header cell.
     * @param widthPx    The width of the header cell in pixels.
     * @param textSizePx The text size of the header cell in pixels.
     */
    public static void addHeaderCell(Context ctx, LinearLayoutCompat row,
                                     String label, int widthPx, float textSizePx, int cellHeightPx) {
        AppCompatTextView tv = new AppCompatTextView(ctx);
        tv.setText(label);
        tv.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSizePx);
        tv.setWidth(widthPx);
        tv.setHeight(cellHeightPx);
        tv.setGravity(Gravity.CENTER);
        tv.setTypeface(null, android.graphics.Typeface.BOLD);
        row.addView(tv);
    }

    /**
     * Creates a GradientDrawable for a cell background with the specified stroke color.
     *
     * @param strokeColor The color of the stroke.
     * @return A GradientDrawable with the specified stroke color and transparent fill.
     */
    public static GradientDrawable cellBackground(int strokeColor) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(Color.TRANSPARENT);
        d.setStroke(1, strokeColor);
        return d;
    }

    public static List<FrameLayout> buildRows(Context ctx,
                                              LinearLayoutCompat parent,
                                              int trackCount,
                                              int columnCount,
                                              int labelWidthPx,
                                              int rowHeightPx,
                                              int unitWidthPx,
                                              int gridStrokeColor,
                                              int zebraOffset) {

        List<FrameLayout> overlays = new ArrayList<>(trackCount);

        for (int tr = 0; tr < trackCount; tr++) {
            /* zebra container row */
            LinearLayoutCompat row = new LinearLayoutCompat(ctx);
            row.setOrientation(LinearLayoutCompat.HORIZONTAL);
            row.setBackgroundColor(((zebraOffset + tr) & 1) == 0 ?
                    0xFFF9F9F9 : 0xFFFFFFFF);

            /* label placeholder */
            AppCompatTextView lbl = new AppCompatTextView(ctx);
            lbl.setWidth(labelWidthPx);
            lbl.setHeight(rowHeightPx);
            lbl.setGravity(Gravity.CENTER_VERTICAL);
            lbl.setTypeface(null, android.graphics.Typeface.BOLD);
            lbl.setPadding(8, 0, 8, 0);
            row.addView(lbl);

            /* overlay for grid-cells + later task blocks */
            FrameLayout overlay = new FrameLayout(ctx);
            overlay.setLayoutParams(new LinearLayoutCompat.LayoutParams(
                    columnCount * unitWidthPx, rowHeightPx));

            for (int c = 0; c < columnCount; c++) {
                View cell = new View(ctx);
                FrameLayout.LayoutParams lp =
                        new FrameLayout.LayoutParams(unitWidthPx, rowHeightPx);
                lp.leftMargin = c * unitWidthPx;
                cell.setLayoutParams(lp);
                cell.setBackground(cellBackground(gridStrokeColor));
                overlay.addView(cell);
            }
            row.addView(overlay);
            parent.addView(row);

            overlays.add(overlay);
        }
        return overlays;
    }

}
