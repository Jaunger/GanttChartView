# GanttChartView

A customizable Android library for displaying Gantt charts with interactive task management, multiple time scales, and export capabilities.

## Overview

GanttChartView is an Android library that provides a flexible and interactive Gantt chart component. It supports multiple time scales (hour, day, month), task management, and export functionality to CSV and PDF formats.

## Features

- **Multiple Time Scales**: Support for Hour, Day, and Month views with automatic data loading
- **Interactive Task Management**: Add, edit, and delete tasks with swipe gestures and touch interactions
- **Export Capabilities**: Export charts to CSV and PDF formats with file sharing
- **Customizable UI**: Color-coded tasks with assignee information and Material Design styling
- **Gesture Support**: Swipe gestures for task interactions (left/right swipes)
- **Responsive Design**: Adapts to different screen sizes and orientations
- **Built-in Task Dialog**: Integrated task creation and editing dialog with color picker
- **Grid System**: Customizable grid with time-based layout and visual indicators
- **Time Range Control**: Set visible time ranges for different views (e.g., 8-20 hours for daily view)

## Project Structure

```
GanttChartView/
├── app/                          # Demo application
│   ├── src/main/
│   │   ├── java/com/example/demoganttapp/
│   │   │   └── MainActivity.java     # Demo app showcasing library features
│   │   ├── res/
│   │   │   ├── layout/
│   │   │   │   └── activity_main.xml # Demo app layout
│   │   │   ├── values/
│   │   │   │   ├── colors.xml        # App colors
│   │   │   │   ├── strings.xml       # App strings
│   │   │   │   └── themes.xml        # App themes
│   │   │   ├── drawable/             # App icons and drawables
│   │   │   └── xml/
│   │   │       └── filepaths.xml     # File provider paths
│   │   └── AndroidManifest.xml       # App manifest with FileProvider
│   └── build.gradle.kts              # Demo app configuration
├── GanttChartView/               # Library module
│   ├── src/main/
│   │   ├── java/com/example/ganttchartview/
│   │   │   ├── core/                 # Core library components
│   │   │   │   ├── GanttChartView.java
│   │   │   │   ├── export/
│   │   │   │   │   └── ExportUtils.java
│   │   │   │   ├── interaction/
│   │   │   │   │   └── BlockGestureHelper.java
│   │   │   │   ├── layout/
│   │   │   │   │   ├── GridPainter.java
│   │   │   │   │   ├── TaskBlockFactory.java
│   │   │   │   │   └── TrackPacker.java
│   │   │   │   └── ui/
│   │   │   │       ├── TaskDialog.java
│   │   │   │       └── DialogStyler.java
│   │   │   ├── listener/
│   │   │   │   ├── OnTaskActionListener.java
│   │   │   │   └── OnTaskClickListener.java
│   │   │   └── model/
│   │   │       ├── GanttTask.java
│   │   │       ├── TaskColor.java
│   │   │       └── TimeScale.java
│   │   ├── res/
│   │   │   ├── values/
│   │   │   │   ├── attrs.xml         # Custom attributes
│   │   │   │   ├── colors.xml        # Library colors
│   │   │   │   ├── strings.xml       # Library strings
│   │   │   │   └── styles.xml        # Library styles
│   │   │   ├── layout/
│   │   │   │   └── dialog_edit_task.xml
│   │   │   └── drawable/
│   │   │       ├── dialog_positive_bg.xml
│   │   │       ├── gantt_button_outlined.xml
│   │   │       ├── grid_cell_background.xml
│   │   │       ├── task_block_background.xml
│   │   │       └── task_block_selector.xml
│   │   └── AndroidManifest.xml       # Library manifest
│   └── build.gradle.kts              # Library configuration
└── build.gradle.kts                  # Root project configuration
```

## Requirements

- **Minimum SDK**: 28 (Android 9.0)
- **Target SDK**: 35 (Android 15)
- **Java Version**: 11
- **Dependencies**: AndroidX AppCompat, Material Design, ConstraintLayout

## Installation

### Adding the Library to Your Project

1. Include the library module in your project:
```gradle
implementation(project(":GanttChartView"))
```

2. Add the required dependencies to your `build.gradle.kts`:
```gradle
dependencies {
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
}
```

## Usage

### Basic Implementation

1. Add the GanttChartView to your layout:
```xml
<com.example.ganttchartview.core.GanttChartView
    android:id="@+id/gantt"
    android:layout_width="match_parent"
    android:layout_height="match_parent" />
```

2. Initialize the chart in your Activity:
```java
GanttChartView gantt = findViewById(R.id.gantt);

// Set time scale (HOUR, DAY, or MONTH)
gantt.setTimeScale(TimeScale.DAY);

// Create and add tasks
List<GanttTask> tasks = new ArrayList<>();
tasks.add(new GanttTask("Task 1", startDate, endDate, Color.BLUE, "Info", "Assignee"));
gantt.setTasks(tasks);
```

### Creating Tasks

```java
// Create a task with start and end dates
GanttTask task = new GanttTask(
    "Task Title",           // Title
    startDate,              // Start date
    endDate,                // End date
    Color.parseColor("#F06292"), // Color
    "Additional info",      // Info
    "Alice"                 // Assignee
);
```

### Time Scales

The library supports three time scales:

- **HOUR**: 1 column = 1 hour (suitable for daily schedules)
- **DAY**: 1 column = 1 day (suitable for weekly/monthly planning)
- **MONTH**: 1 column = 1 month (suitable for long-term planning)

```java
gantt.setTimeScale(TimeScale.HOUR);  // Switch to hour view
gantt.setTimeScale(TimeScale.DAY);   // Switch to day view
gantt.setTimeScale(TimeScale.MONTH); // Switch to month view
```

### Customizable Attributes

The GanttChartView supports various customizable attributes that can be set in XML layouts:

```xml
<com.example.ganttchartview.core.GanttChartView
    android:id="@+id/gantt"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    app:rowHeight="48dp"
    app:timeScale="day"
    app:gridColor="#CCCCCC"
    app:labelTextSize="12sp"
    app:unitWidth="60dp"
    app:headerTextSize="14sp"
    app:taskPressedColor="#FF4081" />
```

**Available Attributes:**
- `rowHeight`: Height of each task row (dimension)
- `timeScale`: Initial time scale - "hour", "day", or "month"
- `gridColor`: Color of the grid lines (color)
- `labelTextSize`: Text size for time labels (dimension)
- `unitWidth`: Width of each time unit (dimension)
- `headerTextSize`: Text size for header labels (dimension)
- `taskPressedColor`: Color when tasks are pressed (color)

### Built-in Resources

The library includes predefined colors and styles:

**Colors:**
- `gantt_grid`: Grid line color (#E0E0E0)
- `gantt_text_primary`: Primary text color (#212121)
- `gantt_text_secondary`: Secondary text color (#424242)
- `task_blue`, `task_green`, `task_yellow`, `task_red`, `task_purple`: Predefined task colors
- `task_pressed`: Pressed state color (#FFFF8D)

**Styles:**
- `Widget.Gantt.TaskBlock`: Task block styling
- `Widget.Gantt.HeaderCell`: Header cell styling
- `Widget.Gantt.Dialog`: Dialog styling
- `Gantt.PositiveButton`: Positive button styling

### Event Listeners

Implement task action listeners for user interactions:

```java
gantt.setOnTaskActionListener(new OnTaskActionListener() {
    @Override
    public void onEdit(GanttTask task) {
        // Handle task edit
    }
    
    @Override
    public void onDelete(GanttTask task) {
        // Handle task deletion
    }
    
    @Override
    public void onSwipe(GanttTask task, int direction) {
        // Handle swipe gestures
        // direction > 0: right swipe, direction < 0: left swipe
    }
});
```

### Filtering Functionality

Filter tasks to show only specific ones:

```java
// Filter by user
gantt.filterByUser("Alice");

// Clear all filters
gantt.clearFilter();

// Custom filter (show only tasks longer than 2 hours)
gantt.setFilter(task -> {
    long duration = task.getEnd().getTime() - task.getStart().getTime();
    return duration > 2 * 60 * 60 * 1000; // 2 hours in milliseconds
});

// Filter by color
gantt.filterByColor(Color.BLUE);

// Filter by date range
Calendar startDate = Calendar.getInstance();
startDate.add(Calendar.DAY_OF_YEAR, -7);
Calendar endDate = Calendar.getInstance();
gantt.filterByDateRange(startDate.getTime(), endDate.getTime());

// Filter by minimum duration (2 hours)
gantt.filterByMinDuration(2 * 60 * 60 * 1000);

// Check filter status
if (gantt.hasActiveFilter()) {
    int visibleCount = gantt.getVisibleTaskCount();
    Toast.makeText(this, "Showing " + visibleCount + " tasks", Toast.LENGTH_SHORT).show();
}
```

### Export Functionality

Export your Gantt chart to CSV or PDF:

```java
// Export to CSV
Uri csvUri = ExportUtils.exportCsv(context, gantt.getAllTasks(), "tasks");

// Export to PDF
Uri pdfUri = ExportUtils.exportPdf(context, gantt, "gantt_snapshot");
```

**Note**: For PDF export functionality, you need to configure a FileProvider in your AndroidManifest.xml:

```xml
<provider
    android:name="androidx.core.content.FileProvider"
    android:authorities="${applicationId}.fileprovider"
    android:exported="false"
    android:grantUriPermissions="true">
    <meta-data
        android:name="android.support.FILE_PROVIDER_PATHS"
        android:resource="@xml/filepaths"/>
</provider>
```

And create a `res/xml/filepaths.xml` file:

```xml
<?xml version="1.0" encoding="utf-8"?>
<paths>
    <external-files-path name="documents" path="Documents/GanttExports/"/>
    <external-files-path name="pictures" path="Pictures/GanttSnapshots/"/>
</paths>
```

## Demo Application

The included demo app (`app` module) showcases all library features:

- **Mode Switching**: Tap "MODE" button to cycle through HOUR → DAY → MONTH views
- **Sample Data**: Pre-loaded tasks demonstrating different time scales
- **Export Buttons**: Export current chart to CSV or PDF
- **Interactive Features**: Swipe gestures and task management

### Running the Demo

1. Open the project in Android Studio
2. Build and run the `app` module
3. Use the interface to explore different features:
   - Tap "MODE" to cycle through HOUR → DAY → MONTH views
   - Tap the floating action button to add new tasks
   - Swipe on tasks to see gesture feedback
   - Use CSV/PDF export buttons to save and share charts
   - Each time scale loads different sample data:
     - **HOUR**: Daily schedule with 8-20 hour range
     - **DAY**: Weekly planning with Monday-Friday tasks
     - **MONTH**: Yearly overview with quarterly projects

## Screenshots

*Screenshots will be added here showing the different time scales and features of the GanttChartView.*

## API Reference

### GanttChartView

Main chart component with methods:
- `setTimeScale(TimeScale scale)`: Set the time scale
- `setTasks(List<GanttTask> tasks)`: Set the task list
- `getAllTasks()`: Get all current tasks
- `setOnTaskActionListener(OnTaskActionListener listener)`: Set event listener
- `openNewTaskDialog()`: Open task creation dialog
- `setTimeRange(int start, int end)`: Set visible time range (for hour view)
- `setFilter(Predicate<GanttTask> filter)`: Set custom filter for tasks
- `clearFilter()`: Remove all filters
- `filterByUser(String user)`: Filter tasks by assigned user
- `filterByColor(int color)`: Filter tasks by color
- `filterByDateRange(Date start, Date end)`: Filter tasks by date range
- `filterByMinDuration(long minDurationMs)`: Filter tasks by minimum duration
- `hasActiveFilter()`: Check if any filter is active
- `getVisibleTaskCount()`: Get count of currently visible tasks

### GanttTask

Task model with properties:
- `title`: Task name
- `start`: Start date/time
- `end`: End date/time
- `color`: Task color
- `info`: Additional information
- `assignedTo`: Task assignee

### TimeScale

Enum defining supported time scales:
- `HOUR`: Hourly view
- `DAY`: Daily view
- `MONTH`: Monthly view

### OnTaskActionListener

Interface for handling task interactions:
- `onEdit(GanttTask task)`: Called when a task is edited
- `onDelete(GanttTask task)`: Called when a task is deleted
- `onSwipe(GanttTask task, int dir)`: Called on swipe gestures (+1 for right, -1 for left)

### OnTaskClickListener

Interface for handling task clicks:
- `onTaskClick(GanttTask task)`: Called when a task is clicked

## Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Add tests if applicable
5. Submit a pull request

## License

This project is licensed under the MIT License - see the LICENSE file for details.

## Support

For issues, questions, or contributions, please open an issue on the project repository. 