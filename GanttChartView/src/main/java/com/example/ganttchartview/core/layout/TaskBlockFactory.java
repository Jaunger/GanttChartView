package com.example.ganttchartview.core.layout;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.StateListDrawable;
import android.util.StateSet;
import android.view.Gravity;
import android.widget.FrameLayout;

import androidx.appcompat.widget.AppCompatTextView;

import com.example.ganttchartview.core.interaction.BlockGestureHelper;
import com.example.ganttchartview.core.ui.TaskDialog;
import com.example.ganttchartview.listener.OnTaskActionListener;
import com.example.ganttchartview.listener.OnTaskClickListener;
import com.example.ganttchartview.model.GanttTask;
import com.example.ganttchartview.model.TaskColor;
import com.example.ganttchartview.model.TimeScale;

public final class TaskBlockFactory {

    public static AppCompatTextView build(Context ctx,
                                          GanttTask task,
                                          int pxLeft,
                                          int pxWidth,
                                          int rowHeight,
                                          int pressedColor,
                                          int hourWidth,
                                          OnTaskClickListener clickL,
                                          OnTaskActionListener actionL, TimeScale timeScale) {

        AppCompatTextView v = new AppCompatTextView(ctx){
            @Override public boolean performClick() {
                super.performClick();
                if (clickL != null) {
                    clickL.onTaskClick(task);
                } else {
                    TaskDialog.showDetails(getContext(), task, timeScale);
                }
                return true;
            }
        };
        int fill = (task.getColor()==0) ? TaskColor.BLUE.argb : task.getColor();
        v.setBackground(createSelector(fill, pressedColor));
        v.setText(task.getTitle());
        v.setTextColor(Color.BLACK);
        v.setGravity(Gravity.CENTER);
        v.setMaxLines(2);
        v.setEllipsize(android.text.TextUtils.TruncateAt.END);
        v.setPadding(12,4,12,4);

        FrameLayout.LayoutParams lp =
                new FrameLayout.LayoutParams(pxWidth, rowHeight);
        lp.leftMargin = pxLeft;
        v.setLayoutParams(lp);

        BlockGestureHelper.attach(v, task, ctx,
                ignored -> { if (clickL!=null) clickL.onTaskClick(task); },
                actionL, hourWidth);

        return v;
    }


    private static Drawable createSelector(int normal, int pressed){
        GradientDrawable n = round(normal), p = round(pressed);
        StateListDrawable s = new StateListDrawable();
        s.addState(new int[]{android.R.attr.state_pressed}, p);
        s.addState(StateSet.WILD_CARD, n);
        return s;
    }
    private static GradientDrawable round(int c){
        GradientDrawable g = new GradientDrawable();
        g.setColor(c);
        g.setCornerRadius(8f);      // same radius as before
        return g;
    }



    private TaskBlockFactory(){ }
}