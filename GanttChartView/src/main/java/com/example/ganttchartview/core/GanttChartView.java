package com.example.ganttchartview.core;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.Rect;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
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
import java.util.Objects;
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
    private boolean hasFilter = false;  // track if filter is active

    private int customStartHour = 8;   // first visible hour (DAY scale)
    private int customEndHour = 20;  // last visible hour  (DAY scale)

    private final int labelWidth = 180; // px – width of task title column


    private int hourWidth = 120; // px – will be overwritten by unitWidth
    private int rowHeight = 72;  // px
    private int unitWidth = 120; // px
    private float labelTextSize = 14f; // px
    private int gridColor = Color.parseColor("#EEEEEE");
    private int taskPressedColor = Color.parseColor("#FFDDDD");
    private float headerTextSize = labelTextSize * 1.25f;

    private ScaleGestureDetector scaleDetector;
    private static final int MIN_UNIT_DP = 60;
    private static final int MAX_UNIT_DP = 240;


    private TimeScale timeScale = TimeScale.DAY;
    private static final String[] MONTH_NAMES = {"Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"};

    private LinearLayoutCompat headerRow;
    private LinearLayoutCompat gridContainer;
    private ScrollView vScroll;

    private OnTaskClickListener onTaskClickListener;
    private OnTaskActionListener onTaskActionListener;

    /**
     * Constructs a GanttChartView with the given context.
     * @param context The context to use.
     */
    public GanttChartView(Context context) {
        super(context);
        init(context, null);
    }

    /**
     * Constructs a GanttChartView with the given context and attributes.
     * @param context The context to use.
     * @param attrs The attribute set from XML.
     */
    public GanttChartView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    /**
     * Constructs a GanttChartView with the given context, attributes, and style.
     * @param context The context to use.
     * @param attrs The attribute set from XML.
     * @param defStyleAttr The default style attribute.
     */
    public GanttChartView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    /**
     * Initializes the view, attributes, UI, and pinch-to-zoom.
     * @param ctx The context.
     * @param attrs The attribute set from XML.
     */
    private void init(Context ctx, @Nullable AttributeSet attrs) {
        setHorizontalScrollBarEnabled(false);
        setOnTaskActionListener(fgt); // default actions for task blocks
        applyXmlAttrs(ctx, attrs);
        buildUi(ctx);
        initPinchZoom(ctx);
        drawHeaderRow();
    }

    /**
     * Applies XML attributes to configure the view.
     * @param ctx The context.
     * @param attrs The attribute set from XML.
     */
    private void applyXmlAttrs(Context ctx, @Nullable AttributeSet attrs) {
        headerTextSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 16, getResources().getDisplayMetrics());
        if (attrs == null) return;
        try (TypedArray a = ctx.obtainStyledAttributes(attrs, R.styleable.GanttChartView)) {
            timeScale = TimeScale.fromAttrIndex(a.getInt(R.styleable.GanttChartView_timeScale, 0));
            rowHeight = a.getDimensionPixelSize(R.styleable.GanttChartView_rowHeight, rowHeight);
            labelTextSize = a.getDimension(R.styleable.GanttChartView_labelTextSize, labelTextSize);
            hourWidth = unitWidth = a.getDimensionPixelSize(R.styleable.GanttChartView_unitWidth, unitWidth);
            gridColor = a.getColor(R.styleable.GanttChartView_gridColor, gridColor);
            taskPressedColor = a.getColor(R.styleable.GanttChartView_taskPressedColor, taskPressedColor);
            headerTextSize = a.getDimension(R.styleable.GanttChartView_headerTextSize, headerTextSize);
        }
    }

    /**
     * Initializes pinch-to-zoom gesture detection.
     * @param ctx The context.
     */
    private void initPinchZoom(Context ctx) {
        scaleDetector = new ScaleGestureDetector(ctx, new ScaleGestureDetector.SimpleOnScaleGestureListener() {
            @Override
            public boolean onScale(@NonNull ScaleGestureDetector detector) {
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

    /**
     * Builds the UI layout for the Gantt chart.
     * @param ctx The context.
     */
    private void buildUi(Context ctx) {

        LinearLayoutCompat outer = new LinearLayoutCompat(ctx);
        outer.setOrientation(LinearLayoutCompat.VERTICAL);

        headerRow = new LinearLayoutCompat(ctx);
        headerRow.setOrientation(LinearLayoutCompat.HORIZONTAL);
        headerRow.setPadding(labelWidth, 0, 0, 0);
        headerRow.setFocusable(true);
        ViewCompat.setAccessibilityDelegate(headerRow, new AccessibilityDelegateCompat() {
            @Override
            public void onInitializeAccessibilityNodeInfo(@NonNull View host, @NonNull AccessibilityNodeInfoCompat info) {
                super.onInitializeAccessibilityNodeInfo(host, info);
                info.setCollectionInfo(AccessibilityNodeInfoCompat.CollectionInfoCompat.obtain(1, getColumnCount(), false));
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

    /**
     * Handles touch events, including pinch-to-zoom and click.
     * @param ev The motion event.
     * @return true if the event was handled.
     */
    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        scaleDetector.onTouchEvent(ev);

        if (ev.getAction() == MotionEvent.ACTION_UP && !scaleDetector.isInProgress()) {
            performClick();
        }

        return super.onTouchEvent(ev);
    }

    /**
     * Handles click events for accessibility.
     * @return true if the click was handled.
     */
    @Override
    public boolean performClick() {
        super.performClick();
        return true;
    }

    /**
     * Adds a new task to the Gantt chart.
     * @param t The {@link GanttTask} to add.
     */
    public void addTask(GanttTask t) {
        allTasks.add(t);
        drawGrid();
        post(() -> vScroll.smoothScrollTo(0, vScroll.getChildAt(0).getBottom()));
    }

    /**
     * Draws the header row (time, days, or months) based on the current time scale.
     */
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
     * Re-draws the entire chart (HOUR / DAY / MONTH) based on the current filter and time scale.
     */
    private void drawGrid() {

        gridContainer.removeAllViews();

        Map<String, List<GanttTask>> groups = TrackPacker.group(allTasks, filterPredicate);

        int zebraRow = 0;
        int cols = getColumnCount();

        for (List<GanttTask> tasks : groups.values()) {

            tasks.sort(Comparator.comparing(GanttTask::getStart));

            Map<GanttTask, Integer> trackOf = TrackPacker.pack(tasks);
            int trackCount = Collections.max(trackOf.values()) + 1;

            List<FrameLayout> overlays = GridPainter.buildRows(getContext(), gridContainer, trackCount, cols, labelWidth, rowHeight, hourWidth, gridColor, zebraRow);

            for (GanttTask t : tasks) {

                float[] os = TrackPacker.offsetAndSpan(t, timeScale, customStartHour);
                float off = os[0];
                float span = Math.min(os[1], cols - off);
                if (off >= cols || span <= 0) continue;

                int pxLeft = Math.round(off * hourWidth);
                int pxWidth = Math.max(Math.round(span * hourWidth), dpToPx(3));

                Integer track = trackOf.get(t);
                if (track == null) continue;

                AppCompatTextView block = TaskBlockFactory.build(getContext(), t, pxLeft, pxWidth, rowHeight, taskPressedColor, hourWidth, onTaskClickListener, onTaskActionListener, timeScale);

                overlays.get(track).addView(block);

                LinearLayoutCompat row = (LinearLayoutCompat) gridContainer.getChildAt(zebraRow + track);
                AppCompatTextView lbl = (AppCompatTextView) row.getChildAt(0);
                if (TextUtils.isEmpty(lbl.getText())) lbl.setText(t.getTitle());
            }

            zebraRow += trackCount;
        }

        updateScrollHeight(zebraRow);
    }

    /**
     * Returns the number of columns for the current time scale.
     * @return Number of columns.
     */
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


    /**
     * Converts dp units to pixels.
     * @param dp The value in dp.
     * @return The value in pixels.
     */
    private int dpToPx(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density + 0.5f);
    }


    /**
     * Dynamically updates the height of the vertical scroll area to fit the number of rows,
     * ensuring it does not exceed the available screen space and never collapses below a single row.
     * This method accounts for system insets and is posted to the UI thread for layout safety.
     *
     * @param rowCount The number of rows to display in the grid.
     */
    private void updateScrollHeight(int rowCount) {
        if (vScroll == null) return;

        final int wanted = Math.max(rowCount, 1) * rowHeight;

        post(() -> {
            int[] loc = new int[2];
            vScroll.getLocationOnScreen(loc);
            int vScrollTop = loc[1];

            Rect win = new Rect();
            getWindowVisibleDisplayFrame(win);
            int screenBottom = win.bottom;

            int insetBottom = 0;
            if (android.os.Build.VERSION.SDK_INT >= 30) {
                WindowInsets in = Objects.requireNonNull(ViewCompat.getRootWindowInsets(this)).toWindowInsets();
                if (in != null) insetBottom = in.getInsets(WindowInsets.Type.systemBars()).bottom;
            }

            int available = screenBottom - insetBottom - vScrollTop - dpToPx(8);

            int finalH = Math.min(wanted, available);
            if (finalH < rowHeight) finalH = rowHeight;

            ViewGroup.LayoutParams lp = vScroll.getLayoutParams();
            if (lp.height != finalH) {
                lp.height = finalH;
                vScroll.setLayoutParams(lp);
            }
        });
    }

    /**
     * Sets the list of tasks to be displayed in the Gantt chart.
     * @param newTasks List of {@link GanttTask} to display. Pass null or empty to clear.
     */
    public void setTasks(List<GanttTask> newTasks) {
        allTasks.clear();
        if (newTasks != null) allTasks.addAll(newTasks);
        drawGrid();
    }

    /**
     * Sets the visible time range for the HOUR view.
     * @param startHour First visible hour (e.g., 8 for 8am)
     * @param endHour Last visible hour (e.g., 20 for 8pm)
     */
    public void setTimeRange(int startHour, int endHour) {
        customStartHour = startHour;
        customEndHour = endHour;
        drawHeaderRow();
        drawGrid();
    }

    /**
     * Sets the time scale (HOUR, DAY, or MONTH) for the chart.
     * @param scale The {@link TimeScale} to use
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

    /**
     * Sets a custom filter for which tasks are visible in the chart.
     * @param p Predicate to filter tasks. Pass null to show all tasks.
     */
    public void setFilter(Predicate<GanttTask> p) {
        filterPredicate = (p != null) ? p : (t -> true);
        hasFilter = (p != null);  // track state explicitly
        drawGrid();
    }

    /**
     * Removes any active task filter and shows all tasks.
     */
    public void clearFilter() {
        setFilter(null);
    }

    /**
     * Filters tasks by assigned user.
     * @param user The user to filter by. Pass null to show all tasks.
     */
    public void filterByUser(String user) {
        setFilter(t -> user == null || user.equals(t.getAssignedTo()));
    }

    /**
     * Filters tasks by color.
     * @param color The color (ARGB int) to filter by.
     */
    public void filterByColor(int color) {
        setFilter(t -> t.getColor() == color);
    }

    /**
     * Filters tasks by date range.
     * @param startDate Start date (inclusive)
     * @param endDate End date (inclusive)
     */
    public void filterByDateRange(Date startDate, Date endDate) {
        setFilter(t -> t.getStart().after(startDate) && t.getEnd().before(endDate));
    }

    /**
     * Filters tasks by minimum duration.
     * @param minDurationMs Minimum duration in milliseconds
     */
    public void filterByMinDuration(long minDurationMs) {
        setFilter(t -> (t.getEnd().getTime() - t.getStart().getTime()) >= minDurationMs);
    }

    /**
     * Checks if any filter is currently active.
     * @return true if a filter is active, false otherwise
     */
    public boolean hasActiveFilter() {
        return hasFilter;  // simple boolean check
    }

    public TimeScale getTimeScale() {
        return timeScale;
    }

    public static class FilterManager<T> {
        private Predicate<T> filterPredicate;
        private boolean isDefaultFilterActive; // Flag

        public void setFilterPredicate(Predicate<T> predicate) {
            this.filterPredicate = predicate;
            this.isDefaultFilterActive = false; // Any custom predicate is not default
        }

        public void setDefaultPassThroughFilter() {
            this.filterPredicate = t -> true; // Set to a pass-through
            this.isDefaultFilterActive = true;
        }

        public void clearFilter() {
            this.filterPredicate = null;
            this.isDefaultFilterActive = false;
        }

        public boolean hasActiveFilter() {
            // An active filter is one that's set and not the default pass-through one
            return filterPredicate != null && !isDefaultFilterActive;
        }
    }

    /**
     * Gets the count of currently visible (filtered) tasks.
     * @return Number of visible tasks
     */
    public int getVisibleTaskCount() {
        return (int) allTasks.stream().filter(filterPredicate).count();
    }

    /**
     * Sets a listener for task click events.
     * @param l The {@link OnTaskClickListener} to use
     */
    public void setOnTaskClickListener(OnTaskClickListener l) {
        onTaskClickListener = l;
    }

    /**
     * Sets a listener for task action events (edit, delete, swipe).
     * @param l The {@link OnTaskActionListener} to use
     */
    public void setOnTaskActionListener(OnTaskActionListener l) {
        onTaskActionListener = l;
    }

    /**
     * Opens the task creation dialog, pre-filled with a 1-hour task.
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


    /**
     * Shows the full edit dialog for a task (add or edit mode).
     * @param task The task to edit.
     * @param isNew True if creating a new task, false if editing.
     */
    private void showFullEditDialog(GanttTask task, boolean isNew) {

        View form = LayoutInflater.from(getContext()).inflate(R.layout.dialog_edit_task, null, false);

        AppCompatEditText titleIn = form.findViewById(R.id.editTitle);
        AppCompatEditText assignIn = form.findViewById(R.id.editAssigned);
        AppCompatEditText infoIn = form.findViewById(R.id.editInfo);
        Spinner colorIn = form.findViewById(R.id.spinnerColor);
        MaterialButton btnStart = form.findViewById(R.id.btnStart);
        MaterialButton btnEnd = form.findViewById(R.id.btnEnd);

        titleIn.setText(task.getTitle());
        assignIn.setText(task.getAssignedTo());
        infoIn.setText(task.getInfo());

        TaskColor[] palette = TaskColor.values();
        colorIn.setAdapter(new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_dropdown_item, palette));
        colorIn.setSelection(TaskColor.from(task.getColor()).ordinal());

        Calendar calStart = Calendar.getInstance(), calEnd = Calendar.getInstance();
        calStart.setTime(task.getStart());
        calEnd.setTime(task.getEnd());
        updateLabel(btnStart, calStart.getTime());
        updateLabel(btnEnd, calEnd.getTime());

        View.OnClickListener openPicker = v -> DialogStyler.show(v.getId() == R.id.btnStart ? calStart : calEnd, timeScale, (MaterialButton) v, getContext());

        btnStart.setOnClickListener(openPicker);
        btnEnd.setOnClickListener(openPicker);

        AlertDialog dlg = new MaterialAlertDialogBuilder(getContext(), R.style.Widget_Gantt_Dialog).setTitle(isNew ? "New task" : "Edit task").setView(form).setPositiveButton(isNew ? "ADD" : "SAVE", null)   // overrides below
                .setNegativeButton("CANCEL", null).create();

        dlg.setOnShowListener(di -> {
            Button pos = dlg.getButton(AlertDialog.BUTTON_POSITIVE);
            Button neg = dlg.getButton(AlertDialog.BUTTON_NEGATIVE);

            DialogStyler.applyPrimary(pos, getContext());
            DialogStyler.applySecondary(neg, getContext());

            pos.setOnClickListener(v -> {
                if (!calEnd.getTime().after(calStart.getTime())) {
                    Toast.makeText(getContext(), "End must be after start", Toast.LENGTH_SHORT).show();
                    return;
                }

                task.setTitle(Objects.requireNonNull(titleIn.getText()).toString().trim());
                task.setAssignedTo(Objects.requireNonNull(assignIn.getText()).toString().trim());
                task.setInfo(Objects.requireNonNull(infoIn.getText()).toString().trim());
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

    /**
     * Updates the label of a MaterialButton with a formatted date.
     * @param b The button to update.
     * @param t The date to display.
     */
    private void updateLabel(MaterialButton b, Date t) {
        SimpleDateFormat fmt = (timeScale == TimeScale.HOUR) ? new SimpleDateFormat("HH:mm", Locale.getDefault()) : new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        b.setText(fmt.format(t));
    }

    /**
     * Returns a copy of all tasks (unfiltered).
     * @return List of all {@link GanttTask}
     */
    public List<GanttTask> getAllTasks() {
        return new ArrayList<>(allTasks);
    }

    /**
     * Sets the visible month range for the MONTH view.
     * @param startMonth First visible month (1-12)
     * @param endMonth Last visible month (1-12)
     */
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

            Snackbar.make(GanttChartView.this, "Task deleted", Snackbar.LENGTH_LONG).setAction("UNDO", v -> {
                allTasks.add(Math.min(idx, allTasks.size()), task);
                drawGrid();
            }).show();
        }

        @Override
        public void onSwipe(GanttTask task, int direction) {
            // no-op, handled by BlockGestureHelper
        }
    };

}