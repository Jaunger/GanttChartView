package com.example.ganttchartview.listener;

import com.example.ganttchartview.model.GanttTask;

public interface OnTaskActionListener {
    void onEdit(GanttTask task);
    void onDelete(GanttTask task);
    /**
     * @param dir -> +1 = swipe-right, -1 = swipe-left
     */
    void onSwipe(GanttTask task, int dir);
}
