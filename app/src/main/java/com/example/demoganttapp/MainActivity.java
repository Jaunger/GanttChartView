package com.example.demoganttapp;

import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.ganttchartview.core.GanttChartView;
import com.example.ganttchartview.core.export.ExportUtils;
import com.example.ganttchartview.model.GanttTask;
import com.example.ganttchartview.model.TimeScale;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

/** Tiny playground: tap “MODE” to cycle HOUR → DAY → MONTH. */
public class MainActivity extends AppCompatActivity {

    /* order in which we rotate */
    private static final TimeScale[] MODES = {
            TimeScale.HOUR, TimeScale.DAY, TimeScale.MONTH
    };

    private int modeIdx = -1;          // will start at 0 after first click
    private GanttChartView gantt;
    private Button btnMode;

    /* ------------------------------------------------------------------ */
    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(com.example.demoganttapp.R.layout.activity_main);


        gantt     = findViewById(R.id.gantt);
        btnMode   = findViewById(R.id.btnMode);
        Button btnCsv = findViewById(R.id.btnCsv);
        Button btnPdf = findViewById(R.id.btnPdf);
        // unused, but shows FAB works
        FloatingActionButton btnAdd = findViewById(R.id.fabAdd); // unused, but shows FAB works


        btnAdd.setOnClickListener(v -> {
            // unused, but shows FAB works
           gantt.openNewTaskDialog();
            Toast.makeText(this, "Add button clicked!", Toast.LENGTH_SHORT).show();
        });

        /* MODE button cycles through enum array */
        btnMode.setOnClickListener(v -> {
            modeIdx = (modeIdx + 1) % MODES.length;
            TimeScale next = MODES[modeIdx];
            btnMode.setText(next.name());     // label the button
            switchDemo(next);
        });
        btnMode.callOnClick();                // initialise with first mode

        /* export buttons */
        btnCsv.setOnClickListener(v -> {
            try {
                Uri uri = ExportUtils.exportCsv(this,
                        gantt.getAllTasks(), "tasks");
                share(uri, "text/csv");
            } catch (IOException e) { toast(e); }
        });
        btnPdf.setOnClickListener(v -> {
            try {
                Uri uri = ExportUtils.exportPdf(this,
                        gantt, "gantt_snapshot");
                share(uri, "application/pdf");
            } catch (IOException e) { toast(e); }
        });
    }

    /* ------------------------------------------------------------------ */
    private void switchDemo(TimeScale scale) {

        gantt.setTimeScale(scale);

        switch (scale) {
            case HOUR:
                loadDay();    // 1-day demo in HOUR view
                break;
            case DAY:
                loadWeek();   // 7-day demo
                break;
            case MONTH:
                loadMonth();  // year demo
                break;
        }
    }

    /* ───────────────── DEMO DATA LOADERS ──────────────────────────── */

    private void loadDay() {
        gantt.setTimeRange(8, 20);            // visible hours 08-20
        List<GanttTask> list = new ArrayList<>();
        list.add(hourTask("Spec Review", 9, 11, "#F06292", "Alice"));
        list.add(hourTask("Client Call",10, 12, "#64B5F6", "Alice"));
        list.add(hourTask("Bug Bash",   11, 13, "#81C784", "Alice"));
        list.add(hourTask("Long QA",    14, 18, "#BA68C8", "Bob"));
        gantt.setTasks(list);
    }

    private void loadWeek() {
        List<GanttTask> list = new ArrayList<>();
        list.add(dayTask ("Planning",  Calendar.MONDAY,    "#F06292"));
        list.add(dayTask ("Design",    Calendar.WEDNESDAY, "#64B5F6"));
        list.add(daySpan("Hackathon",  Calendar.FRIDAY, 2, "#81C784"));
        gantt.setTasks(list);
    }

    private void loadMonth() {
        List<GanttTask> list = new ArrayList<>();
        list.add(monthTask ("Kick-off",           Calendar.JANUARY,  "#F06292"));
        list.add(monthSpan("Road-show",           Calendar.MARCH, 3, "#64B5F6"));
        list.add(monthTask ("Black-Friday prep",  Calendar.OCTOBER, "#4DB6AC"));
        gantt.setTasks(list);
    }

    /* ───────────── tiny helpers (fresh Calendar each call) ─────────── */

    private GanttTask hourTask(String title,int h1,int h2,String hex,String user){
        Calendar c = Calendar.getInstance();
        c.set(Calendar.HOUR_OF_DAY,h1); c.set(Calendar.MINUTE,0);
        Date start = c.getTime();
        c.set(Calendar.HOUR_OF_DAY,h2);
        Date end   = c.getTime();
        return new GanttTask(title,start,end,Color.parseColor(hex),"",user);
    }

    private GanttTask dayTask(String title,int dow,String hex){
        Calendar c = Calendar.getInstance();
        c.set(Calendar.DAY_OF_WEEK, dow);
        c.set(Calendar.HOUR_OF_DAY, 9);
        Date start = c.getTime();
        c.set(Calendar.HOUR_OF_DAY,17);
        Date end   = c.getTime();
        return new GanttTask(title,start,end,Color.parseColor(hex),"","");
    }

    private GanttTask daySpan(String title,int dow,int len,String hex){
        Calendar c = Calendar.getInstance();
        c.set(Calendar.DAY_OF_WEEK, dow);
        c.set(Calendar.HOUR_OF_DAY, 9);
        Date start = c.getTime();
        c.add(Calendar.DAY_OF_YEAR, len);
        c.set(Calendar.HOUR_OF_DAY,17);
        Date end   = c.getTime();
        return new GanttTask(title,start,end,Color.parseColor(hex),"","");
    }

    private GanttTask monthTask(String title,int mon,String hex){
        Calendar c = Calendar.getInstance();
        c.set(Calendar.MONTH, mon);
        c.set(Calendar.DAY_OF_MONTH,1);
        Date start = c.getTime();
        c.set(Calendar.DAY_OF_MONTH,
                c.getActualMaximum(Calendar.DAY_OF_MONTH));
        Date end   = c.getTime();
        return new GanttTask(title,start,end,Color.parseColor(hex),"","");
    }

    private GanttTask monthSpan(String title,int mon,int len,String hex){
        Calendar c = Calendar.getInstance();
        c.set(Calendar.MONTH, mon);
        c.set(Calendar.DAY_OF_MONTH,1);
        Date start = c.getTime();
        c.add(Calendar.MONTH, len);
        c.set(Calendar.DAY_OF_MONTH,
                c.getActualMaximum(Calendar.DAY_OF_MONTH));
        Date end   = c.getTime();
        return new GanttTask(title,start,end,Color.parseColor(hex),"","");
    }

    /* ───────────── share helper & error toast ─────────────────────── */

    private void share(Uri uri,String mime){
        Intent i = new Intent(Intent.ACTION_SEND)
                .setType(mime)
                .putExtra(Intent.EXTRA_STREAM, uri)
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivity(Intent.createChooser(i,"Share via…"));
    }
    private void toast(Exception e){
        Toast.makeText(this,e.getMessage(),Toast.LENGTH_LONG).show();
    }
}