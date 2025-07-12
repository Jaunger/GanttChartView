package com.example.ganttchartview.model;

import android.graphics.Color;

/**
 * Centralised colour palette so blocks & dialogs stay consistent.
 * Feel free to add or rename entries – only this enum needs changing.
 */
public enum TaskColor {

    PINK   ("#F06292"),
    BLUE   ("#64B5F6"),
    GREEN  ("#81C784"),
    YELLOW ("#FFD54F"),
    PURPLE ("#BA68C8"),
    TEAL   ("#4DB6AC");

    public final int argb;

    TaskColor(String hex) { this.argb = Color.parseColor(hex); }

    /** Simple round-robin helper if caller wants “next nice colour”. */
    private static int cursor;
    public static TaskColor next() {
        TaskColor[] arr = values();
        TaskColor c = arr[cursor % arr.length];
        cursor++;
        return c;
    }

    /** Find enum by stored argb (fallback = PINK). */
    public static TaskColor from(int argb) {
        for (TaskColor tc : values()) if (tc.argb == argb) return tc;
        return PINK;
    }
}