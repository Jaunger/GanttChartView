# GanttChartView

A customizable Android library for displaying Gantt charts with interactive task management, multiple time scales, and export capabilities.

---

## Overview

GanttChartView is an Android library that provides a flexible and interactive Gantt chart component. It supports multiple time scales (hour, day, month), task management, and export functionality to CSV, PDF, and PNG formats. A demo app is included to showcase all features.

---

## Features

- **Multiple Time Scales**: Hour, Day, and Month views with automatic data loading
- **Interactive Task Management**: Add, edit, and delete tasks with swipe gestures and touch interactions
- **Export Capabilities**: Export charts to CSV, PDF, and PNG formats with file sharing (no FileProvider or xml setup required)
- **Customizable UI**: Color-coded tasks, assignee info, and Material Design styling
- **Gesture Support**: Swipe gestures for task interactions
- **Responsive Design**: Adapts to different screen sizes and orientations
- **Built-in Task Dialog**: Integrated task creation/editing dialog with color picker
- **Grid System**: Customizable grid with time-based layout and visual indicators
- **Time Range Control**: Set visible time ranges for different views

---

## Project Structure

```
DemoGanttApp/
├── app/                # Demo application
│   └── ...
├── GanttChartView/     # Library module
│   └── ...
├── build.gradle.kts    # Root project configuration
└── ...
```

---

## Requirements

- **Minimum SDK**: 28 (Android 9.0)
- **Target SDK**: 35 (Android 15)
- **Java Version**: 11
- **Gradle**: 8.0+ recommended
- **Dependencies**: AndroidX AppCompat, Material Design, ConstraintLayout
- **Dependency Management**: This project uses a [Gradle Version Catalog](https://docs.gradle.org/current/userguide/version_catalogs.html) (`libs.versions.toml`). If you are not using a version catalog, replace `libs.appcompat` etc. with the actual dependency coordinates (see below).

---

## Installation

### 1. As a Module (Recommended for Local Development)

1. Clone or copy the `GanttChartView` module into your project.
2. Add the module to your `settings.gradle.kts`:
   ```kotlin
   include(":GanttChartView")
   ```
3. Add the dependency in your app's `build.gradle.kts`:
   ```kotlin
   implementation(project(":GanttChartView"))
   ```

### 2. Using JitPack (Recommended for Most Users)

You can use JitPack to easily add the library as a dependency:

1. Add the JitPack repository to your root `build.gradle`:
   ```gradle
   allprojects {
       repositories {
           ...
           maven { url 'https://jitpack.io' }
       }
   }
   ```
2. Add the dependency (replace `Tag` with the latest release):
   ```gradle
   implementation 'com.github.Jaunger:GanttChartView:Tag'
   ```

### 3. As a Maven Dependency (If Published Elsewhere)

If you publish the library to another Maven repository, add the dependency as:
```kotlin
implementation("com.github.Jaunger:GanttChartView:1.0.0")
```
(Adjust group/artifact/version as needed.)

---

## Dependencies

If you use a version catalog (recommended):
```kotlin
dependencies {
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
}
```
If not, use standard coordinates:
```kotlin
dependencies {
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.11.0")
    implementation("androidx.activity:activity:1.8.2")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
}
```

---

## Usage

### 1. Add GanttChartView to Your Layout
```xml
<com.example.ganttchartview.core.GanttChartView
    android:id="@+id/gantt"
    android:layout_width="match_parent"
    android:layout_height="match_parent" />
```

### 2. Initialize in Your Activity
```java
GanttChartView gantt = findViewById(R.id.gantt);
gantt.setTimeScale(TimeScale.DAY);
List<GanttTask> tasks = new ArrayList<>();
tasks.add(new GanttTask("Task 1", startDate, endDate, Color.BLUE, "Info", "Assignee"));
gantt.setTasks(tasks);
```

### 3. Creating Tasks
```java
GanttTask task = new GanttTask(
    "Task Title", startDate, endDate, Color.parseColor("#F06292"), "Additional info", "Alice"
);
```

### 4. Time Scales
```java
gantt.setTimeScale(TimeScale.HOUR);  // Hour view
gantt.setTimeScale(TimeScale.DAY);   // Day view
gantt.setTimeScale(TimeScale.MONTH); // Month view
```

### 5. Customizable Attributes (XML)
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
- `rowHeight` (dimension): Height of each task row
- `timeScale` (enum): Initial time scale - "hour", "day", or "month"
- `gridColor` (color): Color of the grid lines
- `labelTextSize` (dimension): Text size for time labels
- `unitWidth` (dimension): Width of each time unit
- `headerTextSize` (dimension): Text size for header labels
- `taskPressedColor` (color): Color when tasks are pressed

### 6. Event Listeners
```java
gantt.setOnTaskActionListener(new OnTaskActionListener() {
    @Override
    public void onEdit(GanttTask task) { /* ... */ }
    @Override
    public void onDelete(GanttTask task) { /* ... */ }
    @Override
    public void onSwipe(GanttTask task, int direction) { /* ... */ }
});
```

### 7. Filtering
```java
gantt.filterByUser("Alice");
gantt.clearFilter();
gantt.setFilter(task -> task.getEnd().getTime() - task.getStart().getTime() > 2 * 60 * 60 * 1000);
gantt.filterByColor(Color.BLUE);
// ... see API Reference for more
```

### 8. Export Functionality (CSV, PDF, PNG)
Export and sharing work out-of-the-box. **No FileProvider or xml setup is required.**

```java
Uri csvUri = ExportUtils.exportCsv(context, gantt.getAllTasks(), gantt.getTimeScale(), "tasks");
Uri pdfUri = ExportUtils.exportPdf(context, gantt, "gantt_snapshot");
Uri pngUri = ExportUtils.savePng(context, gantt, "gantt_snapshot");
```
- Files are saved to public Documents/GanttExports or Pictures/GanttSnapshots.
- Files appear instantly in system Files/Gallery apps.
- You can share the resulting Uri directly (see demo app for example).

---

## Demo Application

The included demo app (`app` module) showcases all library features:
- **Mode Switching**: Tap "MODE" to cycle through HOUR → DAY → MONTH
- **Sample Data**: Pre-loaded tasks for each time scale
- **Export Buttons**: Export chart to CSV, PDF, or PNG
- **Interactive Features**: Swipe gestures, task management

### Quick Start for Demo App
1. Open the project in Android Studio
2. Build and run the `app` module
3. Explore features: switch modes, add/edit tasks, swipe, export, etc.

---

## Screenshots
<!-- Row 1 (4 items) -->
<p align="center">
  <a href="https://github.com/user-attachments/assets/6eea411c-fe08-4eb4-94ee-4cd3412f3158">
    <img src="https://github.com/user-attachments/assets/6eea411c-fe08-4eb4-94ee-4cd3412f3158" alt="Hour view" width="22%" loading="lazy" />
  </a>
  <a href="https://github.com/user-attachments/assets/7838ef5f-edea-42db-82f0-5736359158af">
    <img src="https://github.com/user-attachments/assets/7838ef5f-edea-42db-82f0-5736359158af" alt="Day view" width="22%" loading="lazy" />
  </a>
     <a href="https://github.com/user-attachments/assets/8c29b416-45c0-47fe-9eba-559bb088ce93">
    <img src="https://github.com/user-attachments/assets/8c29b416-45c0-47fe-9eba-559bb088ce93" alt="Month view (new)" width="22%" loading="lazy" />
  </a>
  <a href="https://github.com/user-attachments/assets/b683ee15-034b-4e0b-9262-b9b19fe4e312">
    <img src="https://github.com/user-attachments/assets/b683ee15-034b-4e0b-9262-b9b19fe4e312" alt="Week/Day view (alt)" width="22%" loading="lazy" />
  </a>
</p>

<!-- Row 2 (3 items) -->
<p align="center">
  <a href="https://github.com/user-attachments/assets/d4ed2f28-0a0e-45fc-bfd1-42de0993c82b">
    <img src="https://github.com/user-attachments/assets/d4ed2f28-0a0e-45fc-bfd1-42de0993c82b" alt="Month view (alt)" width="22%" loading="lazy" />
  </a>
  <a href="https://github.com/user-attachments/assets/b984bf52-8b26-41e8-8704-e2679977a2bd">
    <img src="https://github.com/user-attachments/assets/b984bf52-8b26-41e8-8704-e2679977a2bd" alt="Filters & actions" width="22%" loading="lazy" />
  </a>
  <a href="https://github.com/user-attachments/assets/4a8ff877-667f-4e95-89f3-577d65c307ea">
    <img src="https://github.com/user-attachments/assets/4a8ff877-667f-4e95-89f3-577d65c307ea" alt="Export & save" width="22%" loading="lazy" />
  </a>
</p>

---

## API Reference

### GanttChartView
- `setTimeScale(TimeScale scale)`
- `setTasks(List<GanttTask> tasks)`
- `getAllTasks()`
- `setOnTaskActionListener(OnTaskActionListener listener)`
- `openNewTaskDialog()`
- `setTimeRange(int start, int end)`
- `setFilter(Predicate<GanttTask> filter)`
- `clearFilter()`
- `filterByUser(String user)`
- `filterByColor(int color)`
- `filterByDateRange(Date start, Date end)`
- `filterByMinDuration(long minDurationMs)`
- `hasActiveFilter()`
- `getVisibleTaskCount()`

### GanttTask
- `title`, `start`, `end`, `color`, `info`, `assignedTo`

### TimeScale
- `HOUR`, `DAY`, `MONTH`

### OnTaskActionListener
- `onEdit(GanttTask task)`
- `onDelete(GanttTask task)`
- `onSwipe(GanttTask task, int dir)`

### OnTaskClickListener
- `onTaskClick(GanttTask task)`

### TaskColor (enum)
- `PINK`, `BLUE`, `GREEN`, `YELLOW`, `PURPLE`, `TEAL`
- Use for consistent color assignment to tasks.

**For full API details, see the source code in the `GanttChartView` module.**

---

## Built-in Resources

**Colors:**
- `gantt_grid`, `gantt_grid_dark`, `gantt_background_alt`
- `gantt_text_primary`, `gantt_text_secondary`
- `task_blue`, `task_green`, `task_yellow`, `task_red`, `task_purple`, `task_pressed`, `gantt_onPrimary`, `gantt_task_blue_dark`

**Styles:**
- `Widget.Gantt.TaskBlock`: Task block styling
- `Widget.Gantt.HeaderCell`: Header cell styling
- `Widget.Gantt.Dialog`: Dialog styling
- `Gantt.PositiveButton`: Positive button styling

---

## Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Add tests if applicable
5. Submit a pull request

---

## License

This project is licensed under the MIT License - see the LICENSE file for details.

---

## Support

For issues, questions, or contributions, please open an issue on the project repository. 
