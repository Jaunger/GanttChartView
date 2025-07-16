package com.example.demoganttapp;

import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.ganttchartview.core.GanttChartView;
import com.example.ganttchartview.core.export.ExportUtils;
import com.example.ganttchartview.listener.OnTaskActionListener;
import com.example.ganttchartview.model.GanttTask;
import com.example.ganttchartview.model.TimeScale;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

/** Demo playground – tap MODE to cycle HOUR / DAY / MONTH. */
public class MainActivity extends AppCompatActivity {

    /* ─── fields ─────────────────────────────────────────────── */
    private static final TimeScale[] MODES = { TimeScale.HOUR, TimeScale.DAY, TimeScale.MONTH };
    private int modeIdx = -1;

    private GanttChartView gantt;
    private Button  btnMode, btnCsv, btnPdf, btnPng, btnAlice;
    private TextView txtTaskCount;

    private boolean aliceFilterOn = false;
    private Uri     lastSaved;                         // for long-press share

    /* ─── life-cycle ──────────────────────────────────────────── */
    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        bindViews();
        wireFab();
        wireMode();
        wireExports();
        wireAliceToggle();

        btnMode.callOnClick();
    }

    private void bindViews() {
        gantt        = findViewById(R.id.gantt);

        btnMode      = findViewById(R.id.btnMode);
        btnCsv       = findViewById(R.id.btnCsv);
        btnPdf       = findViewById(R.id.btnPdf);
        btnPng       = findViewById(R.id.btnPng);
        btnAlice     = findViewById(R.id.btnAlice);

        txtTaskCount = findViewById(R.id.txtTaskCount);
    }

    private void wireFab() {
        FloatingActionButton fab = findViewById(R.id.fabAdd);
        fab.setOnClickListener(v -> {
            gantt.openNewTaskDialog();
            updateTaskCount();
        });
    }

    private void wireMode() {
        btnMode.setOnClickListener(v -> {
            modeIdx = (modeIdx + 1) % MODES.length;
            TimeScale next = MODES[modeIdx];

            btnMode.setText(next.name());
            gantt.setTimeScale(next);
            loadDemo(next);
            updateTaskCount();
        });
    }

    private void wireExports() {

        btnCsv.setOnClickListener(v -> saveCsv());
        btnCsv.setOnLongClickListener(v -> { saveCsv(); share("text/csv"); return true; });

        btnPdf.setOnClickListener(v -> savePdf());
        btnPdf.setOnLongClickListener(v -> { savePdf(); share("application/pdf"); return true; });

        btnPng.setOnClickListener(v -> savePng());
        btnPng.setOnLongClickListener(v -> { savePng(); share("image/png"); return true; });
    }

    private void saveCsv() {
        try {
            lastSaved = ExportUtils.exportCsv(
                    this,                            // ctx
                    gantt.getAllTasks(),             // data
                    gantt.getTimeScale(),"tasks");                        // base file-name
            toast("CSV saved to Documents/GanttExports");
        } catch (Exception e) { toast(e); }
    }

    private void savePdf() {
        try {
            lastSaved = ExportUtils.exportPdf(
                    this,
                    gantt,                           // whole chart snapshot
                    "gantt_snapshot");
            toast("PDF saved to Documents/GanttExports");
        } catch (Exception e) { toast(e); }
    }

    private void savePng() {
        try {
            lastSaved = ExportUtils.savePng(
                    this,
                    gantt,     // render chart → bitmap
                    "gantt_snapshot");
            toast("PNG saved to Pictures/GanttSnapshots");
        } catch (Exception e) { toast(e); }
    }

    /* ─── SINGLE-BUTTON “Alice” filter (toggle) ───────────────── */
    private void wireAliceToggle() {
        btnAlice.setOnClickListener(v -> {
            if (aliceFilterOn) {
                gantt.clearFilter();
                btnAlice.setAlpha(1f);
            } else {
                gantt.filterByUser("Alice");
                btnAlice.setAlpha(0.4f);          // dim when active
            }
            aliceFilterOn = !aliceFilterOn;
            updateTaskCount();
        });
    }

    private void updateTaskCount() {
        txtTaskCount.setText("Tasks: " + gantt.getVisibleTaskCount());
    }

    private void loadDemo(TimeScale scale){
        switch (scale){
            case HOUR:  loadDay();   break;
            case DAY:   loadWeek();  break;
            case MONTH: loadMonth(); break;
        }
    }
    private void loadDay(){
        gantt.setTimeRange(8,20);
        List<GanttTask> l=new ArrayList<>();
        l.add(hour("Spec Review",9,11,"#F06292","Alice"));
        l.add(hour("Client Call",10,12,"#64B5F6","Alice"));
        l.add(hour("Bug Bash",11,13,"#81C784","Alice"));
        l.add(hour("QA session",14,18,"#BA68C8","Bob"));
        gantt.setTasks(l);
    }
    private void loadWeek(){
        List<GanttTask> l=new ArrayList<>();
        l.add(day ("Planning",Calendar.MONDAY,   "#F06292"));
        l.add(day ("Design",  Calendar.WEDNESDAY,"#64B5F6"));
        l.add(span("Hackathon",Calendar.FRIDAY,2,"#81C784"));
        gantt.setTasks(l);
    }
    private void loadMonth(){
        List<GanttTask> l=new ArrayList<>();
        l.add(month ("Kick-off", Calendar.JANUARY, "#F06292"));
        l.add(mSpan ("Road-show",Calendar.MARCH,3,"#64B5F6"));
        l.add(month ("BF-prep",  Calendar.OCTOBER,"#4DB6AC"));
        gantt.setTasks(l);
    }

    private GanttTask hour(String t,int h1,int h2,String hex,String user){
        Calendar c=Calendar.getInstance(); c.set(Calendar.HOUR_OF_DAY,h1);
        Date s=c.getTime(); c.set(Calendar.HOUR_OF_DAY,h2);
        return new GanttTask(t,s,c.getTime(),Color.parseColor(hex),"",user);
    }
    private GanttTask day(String t,int dow,String hex){
        Calendar c=Calendar.getInstance(); c.set(Calendar.DAY_OF_WEEK,dow);
        c.set(Calendar.HOUR_OF_DAY,9); Date s=c.getTime();
        c.set(Calendar.HOUR_OF_DAY,17);
        return new GanttTask(t,s,c.getTime(),Color.parseColor(hex),"","");
    }
    private GanttTask span(String t,int dow,int d,String hex){
        Calendar c=Calendar.getInstance(); c.set(Calendar.DAY_OF_WEEK,dow);
        c.set(Calendar.HOUR_OF_DAY,9); Date s=c.getTime();
        c.add(Calendar.DAY_OF_YEAR,d); c.set(Calendar.HOUR_OF_DAY,17);
        return new GanttTask(t,s,c.getTime(),Color.parseColor(hex),"","");
    }
    private GanttTask month(String t,int m,String hex){
        Calendar c=Calendar.getInstance(); c.set(Calendar.MONTH,m);
        c.set(Calendar.DAY_OF_MONTH,1); Date s=c.getTime();
        c.set(Calendar.DAY_OF_MONTH,c.getActualMaximum(Calendar.DAY_OF_MONTH));
        return new GanttTask(t,s,c.getTime(),Color.parseColor(hex),"","");
    }
    private GanttTask mSpan(String t,int m,int len,String hex){
        Calendar c=Calendar.getInstance(); c.set(Calendar.MONTH,m);
        c.set(Calendar.DAY_OF_MONTH,1); Date s=c.getTime();
        c.add(Calendar.MONTH,len);
        c.set(Calendar.DAY_OF_MONTH,c.getActualMaximum(Calendar.DAY_OF_MONTH));
        return new GanttTask(t,s,c.getTime(),Color.parseColor(hex),"","");
    }

    private void share(String mime){
        if(lastSaved==null){ toast("Nothing saved yet"); return; }
        Intent i=new Intent(Intent.ACTION_SEND)
                .setType(mime)
                .putExtra(Intent.EXTRA_STREAM,lastSaved)
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivity(Intent.createChooser(i,"Share via…"));
    }
    private void toast(Exception e){ toast(e.getMessage()); }
    private void toast(String m){ Toast.makeText(this,m,Toast.LENGTH_LONG).show(); }
}