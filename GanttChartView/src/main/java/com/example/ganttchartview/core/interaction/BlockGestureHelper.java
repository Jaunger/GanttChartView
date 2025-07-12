package com.example.ganttchartview.core.interaction;

import android.content.Context;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;


import androidx.annotation.NonNull;

import com.example.ganttchartview.R;
import com.example.ganttchartview.listener.OnTaskActionListener;
import com.example.ganttchartview.model.GanttTask;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.function.Consumer;

/**
 * Attaches click, long-press & swipe behaviour to a task block view.
 *  • single-tap  → clickHandler.run()
 *  • long-press → Edit / Delete chooser (or delegate to OnTaskActionListener)
 *  • swipe      → onSwipe(dir) in OnTaskActionListener
 */
public final class BlockGestureHelper {

    public static void attach(View block,
                              GanttTask task,
                              Context ctx,
                              Consumer<GanttTask> clickHandler,
                              OnTaskActionListener delegate,
                              int hourWidthPx) {

        block.setOnClickListener(v -> {
            if (clickHandler != null) clickHandler.accept(task);
        });

        final boolean[] wasLong = {false};

        /* long-press */
        block.setOnLongClickListener(v -> {
            wasLong[0] = true;

            if (delegate != null) {
                // BlockGestureHelper – themed Edit/Delete popup
                new MaterialAlertDialogBuilder(ctx, R.style.Widget_Gantt_Dialog)
                        .setTitle(task.getTitle())
                        .setItems(new CharSequence[]{"Edit", "Delete"}, (d, which) -> {
                            if (which == 0) delegate.onEdit(task);
                            else            delegate.onDelete(task);
                        })
                        .show();
            }
            return true;
        });

        /* swipe detector */
        GestureDetector swipe = new GestureDetector(ctx,
                new GestureDetector.SimpleOnGestureListener() {
                    private static final int THRESHOLD = 80, VELOCITY = 200;
                    public boolean onFling(MotionEvent e1, @NonNull MotionEvent e2,
                                           float vx, float vy) {
                        float dx = e2.getX() - e1.getX();
                        if (Math.abs(dx) > THRESHOLD && Math.abs(vx) > VELOCITY) {
                            int dir = (dx > 0) ? +1 : -1;
                            if (delegate != null) delegate.onSwipe(task, dir);
                            block.animate()
                                    .translationX(dir * hourWidthPx * 0.4f)
                                    .alpha(0.2f).setDuration(250)
                                    .withEndAction(() -> block.animate()
                                            .translationX(0).alpha(1f)
                                            .setDuration(200))
                                    .start();
                            return true;
                        }
                        return false;
                    }
                });

        /* combined touch handler */
        block.setOnTouchListener((v, ev) -> {
            if (swipe.onTouchEvent(ev)) return true;

            if (ev.getAction() == MotionEvent.ACTION_UP) {
                if (!wasLong[0]) v.performClick();
                wasLong[0] = false;
                return true;
            }
            return false;
        });
    }

    private BlockGestureHelper() {}
}