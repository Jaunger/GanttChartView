package com.example.ganttchartview.core;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.StateListDrawable;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.StateSet;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.AppCompatEditText;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.appcompat.widget.LinearLayoutCompat;
import androidx.core.view.AccessibilityDelegateCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat;

import com.example.ganttchartview.core.layout.GridPainter;
import com.example.ganttchartview.core.layout.TaskBlockFactory;
import com.example.ganttchartview.core.ui.DialogStyler;
import com.example.ganttchartview.listener.OnTaskClickListener;
import com.example.ganttchartview.model.GanttTask;
import com.example.ganttchartview.listener.OnTaskActionListener;
import com.example.ganttchartview.R;
import com.example.ganttchartview.model.TaskColor;
import com.example.ganttchartview.model.TimeScale;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Predicate;

import com.example.ganttchartview.core.layout.TrackPacker;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;

/**
 * Simple, highly-customisable Gantt chart View supporting DAY / WEEK
 * (and a stub MONTH) scales.  All visual parameters can be tweaked
 * via XML attributes: {@code rowHeight}, {@code unitWidth},
 * {@code labelTextSize}, {@code gridColor}, {@code taskPressedColor},
 * and {@code timeScale}.
 */
public class GanttChartView extends HorizontalScrollView {

    private final List<GanttTask> allTasks = new ArrayList<>();
    private Predicate<GanttTask> filterPredicate = t -> true;   // show all
    /* ---------- configurable defaults ---------- */

    private int customStartHour = 8;   // first visible hour (DAY scale)
    private int customEndHour = 20;  // last visible hour  (DAY scale)

    private int labelWidth = 180; // px – width of task title column

    /* ---------- styleable fields (set via XML) ---------- */

    private int hourWidth = 120; // px – will be overwritten by unitWidth
    private int rowHeight = 72;  // px
    private int unitWidth = 120; // px
    private float labelTextSize = 14f; // px
    private int gridColor = Color.parseColor("#EEEEEE");
    private int taskPressedColor = Color.parseColor("#FFDDDD");
    private float headerTextSize = labelTextSize * 1.25f;

    // --- pinch-to-zoom ---
    private ScaleGestureDetector scaleDetector;
    private static final int MIN_UNIT_DP = 60;
    private static final int MAX_UNIT_DP = 240;

    /* ---------- runtime ---------- */

    private TimeScale timeScale = TimeScale.DAY;
    private static final String[] MONTH_NAMES = {"Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"};

    private LinearLayoutCompat headerRow;
    private LinearLayoutCompat gridContainer;
    private ScrollView vScroll;

    private OnTaskClickListener onTaskClickListener;
    private OnTaskActionListener onTaskActionListener;

    /* ---------- constructors ---------- */

    public GanttChartView(Context context) {
        super(context);
        init(context, null);
    }

    public GanttChartView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public GanttChartView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }
    /* ---------- initialisation ---------- */

    private void init(Context ctx, @Nullable AttributeSet attrs) {
        setHorizontalScrollBarEnabled(false);

        applyXmlAttrs(ctx, attrs);
        buildUi(ctx);
        initPinchZoom(ctx);

        drawHeaderRow();
    }

    private void applyXmlAttrs(Context ctx, @Nullable AttributeSet attrs) {
        headerTextSize = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_SP, 16, getResources().getDisplayMetrics());

        if (attrs == null) return;

        TypedArray a = ctx.obtainStyledAttributes(attrs, R.styleable.GanttChartView);
        try {
            timeScale = TimeScale.fromAttrIndex(
                    a.getInt(R.styleable.GanttChartView_timeScale, 0));
            rowHeight = a.getDimensionPixelSize(
                    R.styleable.GanttChartView_rowHeight, rowHeight);
            labelTextSize = a.getDimension(
                    R.styleable.GanttChartView_labelTextSize, labelTextSize);
            hourWidth = unitWidth = a.getDimensionPixelSize(
                    R.styleable.GanttChartView_unitWidth, unitWidth);
            gridColor = a.getColor(
                    R.styleable.GanttChartView_gridColor, gridColor);
            taskPressedColor = a.getColor(
                    R.styleable.GanttChartView_taskPressedColor, taskPressedColor);
            headerTextSize = a.getDimension(
                    R.styleable.GanttChartView_headerTextSize, headerTextSize);
        } finally {
            a.recycle();
        }
    }

    private void initPinchZoom(Context ctx) {
        scaleDetector = new ScaleGestureDetector(ctx,
                new ScaleGestureDetector.SimpleOnScaleGestureListener() {
                    @Override
                    public boolean onScale(
                            @NonNull ScaleGestureDetector detector) {
                        float newPx = hourWidth * detector.getScaleFactor();
                        float dp = newPx / getResources().getDisplayMetrics().density;
                        dp = Math.max(MIN_UNIT_DP, Math.min(MAX_UNIT_DP, dp));
                        newPx = dp * getResources().getDisplayMetrics().density;

                        if ((int) newPx != hourWidth) {
                            hourWidth = unitWidth = (int) newPx;
                            drawHeaderRow();
                            drawGrid();
                        }
                        return true;
                    }
                });
    }

    private void buildUi(Context ctx) {

        LinearLayoutCompat outer = new LinearLayoutCompat(ctx);
        outer.setOrientation(LinearLayoutCompat.VERTICAL);

        headerRow = new LinearLayoutCompat(ctx);
        headerRow.setOrientation(LinearLayoutCompat.HORIZONTAL);
        headerRow.setPadding(labelWidth, 0, 0, 0);
        headerRow.setFocusable(true);
        ViewCompat.setAccessibilityDelegate(headerRow,
                new AccessibilityDelegateCompat() {
                    @Override
                    public void onInitializeAccessibilityNodeInfo(
                            @NonNull View host, @NonNull AccessibilityNodeInfoCompat info) {
                        super.onInitializeAccessibilityNodeInfo(host, info);
                        info.setCollectionInfo(
                                AccessibilityNodeInfoCompat.CollectionInfoCompat
                                        .obtain(1, getColumnCount(), false));
                    }
                });
        outer.addView(headerRow);

        vScroll = new ScrollView(ctx);
        vScroll.setVerticalScrollBarEnabled(false);

        gridContainer = new LinearLayoutCompat(ctx);
        gridContainer.setOrientation(LinearLayoutCompat.VERTICAL);

        vScroll.addView(gridContainer);
        outer.addView(vScroll);

        addView(outer);
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        // Pass event to the pinch detector (already there)
        scaleDetector.onTouchEvent(ev);

        // If this was a single-tap ACTION_UP, fire performClick()
        if (ev.getAction() == MotionEvent.ACTION_UP && !scaleDetector.isInProgress()) {
            performClick();
        }

        // Still let HorizontalScrollView handle scrolling/fling
        return super.onTouchEvent(ev);
    }

    @Override
    public boolean performClick() {
        // Let the superclass (HorizontalScrollView) handle default click actions
        super.performClick();
        // Return true because we handled the click
        return true;
    }

    public void addTask(GanttTask t) {
        allTasks.add(t);
        drawGrid();
        post(() -> vScroll.smoothScrollTo(0, vScroll.getChildAt(0).getBottom()));
    }

    private void drawHeaderRow() {
        headerRow.removeAllViews();
        int cellH = rowHeight;    // ← use same height as grid rows

        if (timeScale == TimeScale.HOUR) {
            for (int h = customStartHour; h <= customEndHour; h++) {
                GridPainter.addHeaderCell(getContext(), headerRow, String.format(Locale.getDefault(), "%02d:00", h), hourWidth, headerTextSize, cellH);
            }

        } else if (timeScale == TimeScale.DAY) {
            String[] days = {"Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"};
            for (String d : days) {
                GridPainter.addHeaderCell(getContext(), headerRow, d, hourWidth, headerTextSize, cellH);
            }

        } else {                // MONTH
            for (int m = customStartHour; m <= customEndHour; m++) {
                GridPainter.addHeaderCell(getContext(), headerRow, MONTH_NAMES[m - 1], hourWidth, headerTextSize, cellH);
            }
        }
    }

    /**
     * Re-draw the whole chart (HOUR / DAY / MONTH)
     */
    private void drawGrid() {

        gridContainer.removeAllViews();

        /* 1 ─ filter & group (helper lives in TrackPacker) */
        Map<String, List<GanttTask>> groups =
                TrackPacker.group(allTasks, filterPredicate);

        int zebraRow = 0;                          // for alternating backgrounds
        int cols = getColumnCount();           // hour / weekday / month columns

        /* 2 ─ render every group (= Assigned-to or fallback Title) */
        for (List<GanttTask> tasks : groups.values()) {

            tasks.sort(Comparator.comparing(GanttTask::getStart));

            Map<GanttTask, Integer> trackOf = TrackPacker.pack(tasks);
            int trackCount = Collections.max(trackOf.values()) + 1;

            /* build rows + faint grid in one call */
            List<FrameLayout> overlays = GridPainter.buildRows(
                    getContext(), gridContainer,
                    trackCount, cols,
                    labelWidth, rowHeight, hourWidth,
                    gridColor, zebraRow);

            /* 3 ─ drop each coloured task block */
            for (GanttTask t : tasks) {

                float[] os = TrackPacker.offsetAndSpan(t, timeScale, customStartHour);
                float off = os[0];
                float span = Math.min(os[1], cols - off);
                if (off >= cols || span <= 0) continue;

                int pxLeft = Math.round(off * hourWidth);
                int pxWidth = Math.max(Math.round(span * hourWidth), dpToPx(3));

                int track = trackOf.get(t);
                AppCompatTextView block = TaskBlockFactory.build(
                        getContext(), t,
                        pxLeft, pxWidth, rowHeight,
                        taskPressedColor, hourWidth,
                        onTaskClickListener, fgt, timeScale);

                overlays.get(track).addView(block);

                /* label column = first title per track */
                LinearLayoutCompat row =
                        (LinearLayoutCompat) gridContainer.getChildAt(zebraRow + track);
                AppCompatTextView lbl = (AppCompatTextView) row.getChildAt(0);
                if (TextUtils.isEmpty(lbl.getText())) lbl.setText(t.getTitle());
            }

            zebraRow += trackCount;
        }

        updateScrollHeight(zebraRow);
    }

    private int getColumnCount() {
        switch (timeScale) {
            case HOUR:
                return customEndHour - customStartHour + 1; // hours
            case DAY:
                return 7;
            case MONTH:
                return customEndHour - customStartHour + 1; // months ❹
            default:
                return 1;
        }
    }


    private int dpToPx(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density + 0.5f);
    }


    private void updateScrollHeight(int rowCount) {
        if (vScroll == null) return;
        int maxVisibleRows = 8;
        int height = Math.min(Math.max(rowCount, 1), maxVisibleRows) * rowHeight;
        vScroll.setLayoutParams(new LinearLayoutCompat.LayoutParams(LayoutParams.MATCH_PARENT, height));
    }


    public void setTasks(List<GanttTask> newTasks) {
        allTasks.clear();
        if (newTasks != null) allTasks.addAll(newTasks);
        drawGrid();
    }

    public void setTimeRange(int startHour, int endHour) {
        customStartHour = startHour;
        customEndHour = endHour;
        drawHeaderRow();
        drawGrid();
    }

    /**
     * switch between HOUR / DAY / MONTH
     */
    public void setTimeScale(TimeScale scale) {
        if (scale == null) scale = TimeScale.DAY;

        switch (scale) {
            case MONTH:
                if (customStartHour < 1 || customEndHour > 12) setMonthRange(1, 12);
                break;

            case HOUR:
                if (customEndHour <= customStartHour) setTimeRange(8, 20);
                break;

            case DAY:
                break;
        }

        if (scale != timeScale) {
            timeScale = scale;
            drawHeaderRow();
            drawGrid();
        }
    }

    public void setFilter(Predicate<GanttTask> p) {
        filterPredicate = (p != null) ? p : (t -> true);
        drawGrid();
    }

    public void clearFilter() {
        setFilter(null);
    }

    /* convenience */
    public void filterByUser(String user) {
        setFilter(t -> user == null || user.equals(t.getAssignedTo()));
    }

    public void setOnTaskClickListener(OnTaskClickListener l) {
        onTaskClickListener = l;
    }

    public void setOnTaskActionListener(OnTaskActionListener l) {
        onTaskActionListener = l;
    }


    /**
     * Opens the editor pre-filled with “now → +1 h” and colour BLUE.
     */
    public void openNewTaskDialog() {

        Calendar c = Calendar.getInstance();
        Date start = c.getTime();
        c.add(Calendar.HOUR_OF_DAY, 1);
        Date end = c.getTime();

        GanttTask draft = new GanttTask("New task",               // provisional title
                start, end, TaskColor.BLUE.argb, "",                       // info
                ""                        // assigned
        );
        showFullEditDialog(draft, true);
    }

    /* -----------------------------------------------------------
     *  GanttChartView – new, shorter showFullEditDialog()
     * ----------------------------------------------------------- */
    private void showFullEditDialog(GanttTask task, boolean isNew) {

        View form = LayoutInflater.from(getContext())
                .inflate(R.layout.dialog_edit_task, null, false);

        // --- bind views ---------------------------------------------------
        AppCompatEditText titleIn = form.findViewById(R.id.editTitle);
        AppCompatEditText assignIn = form.findViewById(R.id.editAssigned);
        AppCompatEditText infoIn = form.findViewById(R.id.editInfo);
        Spinner colorIn = form.findViewById(R.id.spinnerColor);
        MaterialButton btnStart = form.findViewById(R.id.btnStart);
        MaterialButton btnEnd = form.findViewById(R.id.btnEnd);

        // --- populate -----------------------------------------------------
        titleIn.setText(task.getTitle());
        assignIn.setText(task.getAssignedTo());
        infoIn.setText(task.getInfo());

        TaskColor[] palette = TaskColor.values();
        colorIn.setAdapter(new ArrayAdapter<>(getContext(),
                android.R.layout.simple_spinner_dropdown_item, palette));
        colorIn.setSelection(TaskColor.from(task.getColor()).ordinal());

        /* calendars that mutate live when pickers return */
        Calendar calStart = Calendar.getInstance(), calEnd = Calendar.getInstance();
        calStart.setTime(task.getStart());
        calEnd.setTime(task.getEnd());
        updateLabel(btnStart, calStart.getTime());
        updateLabel(btnEnd, calEnd.getTime());

        View.OnClickListener openPicker = v ->
                DialogStyler.show(v.getId() == R.id.btnStart ? calStart : calEnd,
                        timeScale, (MaterialButton) v, getContext());

        btnStart.setOnClickListener(openPicker);
        btnEnd.setOnClickListener(openPicker);

        // --- build dialog -------------------------------------------------
        AlertDialog dlg = new MaterialAlertDialogBuilder(
                getContext(), R.style.Widget_Gantt_Dialog)
                .setTitle(isNew ? "New task" : "Edit task")
                .setView(form)
                .setPositiveButton(isNew ? "ADD" : "SAVE", null)   // overrides below
                .setNegativeButton("CANCEL", null)
                .create();

        dlg.setOnShowListener(di -> {
            Button pos = dlg.getButton(AlertDialog.BUTTON_POSITIVE);
            Button neg = dlg.getButton(AlertDialog.BUTTON_NEGATIVE);

            DialogStyler.applyPrimary(pos, getContext());
            DialogStyler.applySecondary(neg, getContext());

            pos.setOnClickListener(v -> {
                if (!calEnd.getTime().after(calStart.getTime())) {
                    Toast.makeText(getContext(),
                            "End must be after start", Toast.LENGTH_SHORT).show();
                    return;
                }
                // copy fields back
                task.setTitle(titleIn.getText().toString().trim());
                task.setAssignedTo(assignIn.getText().toString().trim());
                task.setInfo(infoIn.getText().toString().trim());
                task.setColor(palette[colorIn.getSelectedItemPosition()].argb);
                task.setStart(calStart.getTime());
                task.setEnd(calEnd.getTime());

                if (isNew) addTask(task);
                else drawGrid();
                dlg.dismiss();
            });
        });

        dlg.show();
    }

    private void updateLabel(MaterialButton b, Date t) {
        SimpleDateFormat fmt = (timeScale == TimeScale.HOUR) ? new SimpleDateFormat("HH:mm", Locale.getDefault()) : new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        b.setText(fmt.format(t));
    }

    public List<GanttTask> getAllTasks() {
        return new ArrayList<>(allTasks);
    }

    public void setMonthRange(int startMonth, int endMonth) {
        startMonth = Math.max(1, Math.min(12, startMonth));
        endMonth = Math.max(1, Math.min(12, endMonth));
        if (startMonth > endMonth) {
            int tmp = startMonth;
            startMonth = endMonth;
            endMonth = tmp;
        }

        this.customStartHour = startMonth;
        this.customEndHour = endMonth;
        drawHeaderRow();
        drawGrid();
    }


    OnTaskActionListener fgt = new OnTaskActionListener() {
        @Override
        public void onEdit(GanttTask task) {
            showFullEditDialog(task, false);
        }

        @Override
        public void onDelete(GanttTask task) {
            int idx = allTasks.indexOf(task);
            allTasks.remove(task);
            drawGrid();

            Snackbar.make(GanttChartView.this,
                            "Task deleted", Snackbar.LENGTH_LONG)
                    .setAction("UNDO", v -> {
                        allTasks.add(Math.min(idx, allTasks.size()), task);
                        drawGrid();
                    })
                    .show();
        }

        @Override
        public void onSwipe(GanttTask task, int direction) {
            // no-op, handled by BlockGestureHelper
        }
    };

}