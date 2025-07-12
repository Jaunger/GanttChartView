package com.example.ganttchartview.core.ui;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.widget.Button;

import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;

import com.example.ganttchartview.R;
import com.example.ganttchartview.model.GanttTask;
import com.example.ganttchartview.model.TimeScale;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.text.SimpleDateFormat;
import java.util.Locale;

/**
 * Stateless helper – *shows* and immediately returns the built dialog.
 */
public final class TaskDialog {

    private TaskDialog() {
    }

    public static void showDetails(Context ctx,
                                   GanttTask task,
                                   TimeScale scale) {

        SimpleDateFormat fmt =
                (scale == TimeScale.HOUR) ? new SimpleDateFormat("HH:mm", Locale.getDefault()) :
                        (scale == TimeScale.DAY) ? new SimpleDateFormat("EEE  dd MMM", Locale.getDefault()) :
                                new SimpleDateFormat("MMM yyyy", Locale.getDefault());

        StringBuilder msg = new StringBuilder()
                .append("When: ")
                .append(fmt.format(task.getStart()))
                .append(" – ")
                .append(fmt.format(task.getEnd()));

        if (!task.getAssignedTo().isEmpty())
            msg.append("\nAssigned to: ").append(task.getAssignedTo());
        if (!task.getInfo().isEmpty())
            msg.append("\n\n").append(task.getInfo());

        AlertDialog dlg = new MaterialAlertDialogBuilder(ctx,
                R.style.Widget_Gantt_Dialog)
                .setTitle(task.getTitle())
                .setMessage(msg.toString())
                .setPositiveButton("CLOSE", null)
                .create();

        dlg.setOnShowListener(di -> {
            Button b = dlg.getButton(AlertDialog.BUTTON_POSITIVE);
            b.setAllCaps(false);
            b.setTypeface(Typeface.DEFAULT_BOLD);
            b.setTextColor(Color.WHITE);
            b.setBackgroundTintList(ColorStateList.valueOf(
                    ContextCompat.getColor(ctx, R.color.gantt_task_blue_dark)));
            b.setBackgroundResource(R.drawable.dialog_positive_bg);
        });

        dlg.show();
    }
}