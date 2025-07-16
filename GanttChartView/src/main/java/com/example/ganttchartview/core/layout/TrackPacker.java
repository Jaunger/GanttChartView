package com.example.ganttchartview.core.layout;

import android.text.TextUtils;

import com.example.ganttchartview.model.GanttTask;
import com.example.ganttchartview.model.TimeScale;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

public class TrackPacker {
    private static final long EPS = 1_000;

    /** Returns a map task → trackIndex (0-based). */
    public static Map<GanttTask,Integer> pack(List<GanttTask> tasks) {
        tasks.sort(Comparator.comparing(GanttTask::getStart));
        List<Date> trackEnd   = new ArrayList<>();
        Map<GanttTask,Integer> trackOf = new HashMap<>();

        for (GanttTask t : tasks) {
            int track = 0;
            while (track < trackEnd.size()
                    && t.getStart().getTime() <= trackEnd.get(track).getTime() + EPS) {
                track++;
            }
            if (track == trackEnd.size()) trackEnd.add(t.getEnd());
            else                           trackEnd.set(track, t.getEnd());
            trackOf.put(t, track);
        }
        return trackOf;
    }

    public static float[] offsetAndSpan(GanttTask t,
                                        TimeScale scale,
                                        int customStartUnit) {

        final long HOUR_MS = 3_600_000L;
        final long DAY_MS  = 24L * HOUR_MS;

        Calendar cs = Calendar.getInstance(), ce = Calendar.getInstance();
        cs.setTime(t.getStart());   ce.setTime(t.getEnd());

        switch (scale) {

            case HOUR: {
                Calendar base = (Calendar) cs.clone();
                base.set(Calendar.HOUR_OF_DAY, customStartUnit);
                base.set(Calendar.MINUTE, 0); base.set(Calendar.SECOND, 0); base.set(Calendar.MILLISECOND, 0);

                float off  = (t.getStart().getTime() - base.getTimeInMillis()) / (float) HOUR_MS;
                float span = (t.getEnd()  .getTime() - t.getStart().getTime()) / (float) HOUR_MS;
                return new float[]{off, Math.max(span, 1f/60f)};
            }

            case DAY: {
                Calendar sDay = (Calendar) cs.clone();
                sDay.set(Calendar.HOUR_OF_DAY, 0);
                sDay.set(Calendar.MINUTE,      0);
                sDay.set(Calendar.SECOND,      0);
                sDay.set(Calendar.MILLISECOND, 0);

                Calendar eDay = (Calendar) ce.clone();
                eDay.set(Calendar.HOUR_OF_DAY, 0);
                eDay.set(Calendar.MINUTE,      0);
                eDay.set(Calendar.SECOND,      0);
                eDay.set(Calendar.MILLISECOND, 0);

                float off  = sDay.get(Calendar.DAY_OF_WEEK) - Calendar.SUNDAY;

                long diffDays = (eDay.getTimeInMillis() - sDay.getTimeInMillis()) / DAY_MS;
                float span = diffDays + 1;

                return new float[]{off, span};
            }

            case MONTH: {
                float daysInMonth = cs.getActualMaximum(Calendar.DAY_OF_MONTH);
                float off = (cs.get(Calendar.MONTH) - (customStartUnit - 1))
                        + (cs.get(Calendar.DAY_OF_MONTH)-1
                        + (cs.get(Calendar.HOUR_OF_DAY)+cs.get(Calendar.MINUTE)/60f)/24f)
                        / daysInMonth;

                float span = (ce.getTimeInMillis() - cs.getTimeInMillis()) / (30f * DAY_MS);
                return new float[]{off, Math.max(span, 1f/30f)};
            }
        }
        throw new AssertionError("Unhandled scale "+scale);
    }

    public static Map<String,List<GanttTask>> group(List<GanttTask> src,
                                                    Predicate<GanttTask> filter){
        Map<String,List<GanttTask>> out = new LinkedHashMap<>();
        for (GanttTask t : src){
            if (!filter.test(t)) continue;
            String key = TextUtils.isEmpty(t.getAssignedTo())
                    ? t.getTitle() : t.getAssignedTo();
            out.computeIfAbsent(key,k->new ArrayList<>()).add(t);
        }
        return out;
    }
    private TrackPacker() {}
}
