package com.example.ganttchartview.model;

import java.util.Date;

public class GanttTask {
    private String title;
    private Date start;
    private Date end;
    private int color;
    private String info; // NEW
    private String assignedTo;

    public GanttTask(String title, Date start, Date end, int color, String info, String assignedTo) {
        this.title = title;
        this.start = start;
        this.end = end;
        this.color = color;
        this.info = info;
        this.assignedTo = assignedTo;
    }

    public String getTitle() {
        return title;
    }

    public Date getStart() {
        return start;
    }

    public Date getEnd() {
        return end;
    }

    public int getColor() {
        return color;
    }

    public String getInfo() {
        return info;
    }

    public void setAssignedTo(String assignedTo) {
        if (assignedTo == null || assignedTo.isEmpty()) {
            return; // Invalid assignedTo
        }
        this.assignedTo = assignedTo;
    }

    public void setInfo(String info) {
        if (info == null || info.isEmpty()) {
            return; // Invalid info
        }
        this.info = info;
    }

    public String getAssignedTo() {
        return assignedTo;
    }

    public void setTitle(String title) {
        if (title == null || title.isEmpty()) {
            return; // Invalid title
        }
        this.title = title;
    }

    public void setStart(Date start) {
        this.start = start;
    }

    public void setEnd(Date end) {
        this.end = end;
    }

    public void setColor(int color) {
        this.color = color;
    }

    public String getFormattedStart() {
        return String.format("%1$tH:%1$tM", start);
    }

    public String getFormattedEnd() {
        return String.format("%1$tH:%1$tM", end);
    }
}