package com.example.ganttchartview.core.ui;

import android.content.Context;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import com.example.ganttchartview.R;
import com.example.ganttchartview.model.TimeScale;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.timepicker.MaterialTimePicker;
import com.google.android.material.timepicker.TimeFormat;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public final class DialogStyler {
    private DialogStyler() {}

    public static void applyPrimary(Button b, Context ctx) {
        b.setAllCaps(false);
        b.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        b.setTextColor(ContextCompat.getColor(ctx, R.color.gantt_onPrimary));
        b.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                ContextCompat.getColor(ctx, R.color.task_green)));
    }

    public static void applySecondary(Button b, Context ctx) {
        b.setAllCaps(false);
        b.setTextColor(ContextCompat.getColor(ctx, R.color.gantt_onPrimary));
        b.setBackgroundResource(R.drawable.gantt_button_outlined);
    }

    public static void show(Calendar cal,
                            TimeScale scale,
                            Button labelBtn,
                            Context ctx) {

        if (scale == TimeScale.HOUR) {
            /* ─── 24-hour time picker ─── */
            MaterialTimePicker tp = new MaterialTimePicker.Builder()
                    .setTimeFormat(TimeFormat.CLOCK_24H)
                    .setHour  (cal.get(Calendar.HOUR_OF_DAY))
                    .setMinute(cal.get(Calendar.MINUTE))
                    .build();

            tp.addOnPositiveButtonClickListener(v -> {
                cal.set(Calendar.HOUR_OF_DAY, tp.getHour());
                cal.set(Calendar.MINUTE,      tp.getMinute());
                updateLabel(labelBtn, cal.getTime(), scale);
            });
            tp.show(((AppCompatActivity) ctx).getSupportFragmentManager(), null);

        } else {                   // DAY or MONTH → date picker
            MaterialDatePicker<Long> dp = MaterialDatePicker.Builder
                    .datePicker()
                    .setSelection(cal.getTimeInMillis())
                    .build();

            dp.addOnPositiveButtonClickListener(sel -> {
                cal.setTimeInMillis(sel);
                updateLabel(labelBtn, cal.getTime(), scale);
            });
            dp.show(((AppCompatActivity) ctx).getSupportFragmentManager(), null);
        }
    }

    /* formatted “2023-08-17” or “HH:mm” depending on scale */
    private static void updateLabel(Button b, java.util.Date t, TimeScale scale) {
        SimpleDateFormat fmt = (scale == TimeScale.HOUR)
                ? new SimpleDateFormat("HH:mm",      Locale.getDefault())
                : new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        b.setText(fmt.format(t));
    }
}