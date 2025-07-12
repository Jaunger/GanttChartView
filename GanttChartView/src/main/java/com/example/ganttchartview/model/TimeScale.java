package com.example.ganttchartview.model;

/** Supported time-axis granularities for the chart */
public enum TimeScale {
    /** 1 column = 1 hour  (what you already call “DAY”)   */
    HOUR(0, "hour"),
    /** 1 column = 1 day   */
    DAY (1, "day"),
    /** 1 column = 1 month */
    MONTH(2, "month");

    public final int attrIndex;   // keeps enum ↔︎ XML attribute in sync
    public final String xmlName;  // string that appears in attrs.xml

    TimeScale(int idx, String xmlName) {
        this.attrIndex = idx;
        this.xmlName   = xmlName;
    }

    /** Convert the <enum> value read from attrs.xml into the enum */
    public static TimeScale fromAttrIndex(int idx) {
        for (TimeScale s : values())
            if (s.attrIndex == idx) return s;
        throw new IllegalArgumentException("Unknown scale index: " + idx);
    }
}