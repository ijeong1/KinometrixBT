package com.kinometrix.kinometrixbt;

import android.Manifest;
import android.accounts.AccountManager;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Environment;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.TextInputEditText;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.Toast;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.Legend.LegendForm;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.components.YAxis.AxisDependency;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.google.android.gms.common.api.GoogleApiClient;

public class MainActivity extends AppCompatActivity{
    private View mContentView;
    private Button mTareButton, mToggleButton, mQuitButton, mGoogleDriveButton;
    private TextInputEditText mFirstName, mLastName;
    private Spinner mBodyPart, mExerciseType, mSide;
    private boolean mExamToggle;
    public static float[] sensordata = new float[13];
    public static String DIRECTORY_NAME = "Kinometrix";

    //Chart
    private LineChart mChart, mChart2, mChart3;
    private Thread mthread;

    //Bluetooth Sensor Handler
    private BTSensorHandler btSensorHandler = new BTSensorHandler();

    //private boolean did_initialize = false;
    private boolean is_polling = false;

    //OUTFILE
    public static String logfile;
    public static Context context;
    public static Activity activity;

    private void mHidePart2Runnable() {
        mContentView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE
                | View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
    }

    private View mControlsView;
    private void mShowPart2Runnable(){
        mControlsView.setVisibility(View.VISIBLE);
    }

    private boolean mVisible;

   @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        //------------------ Chart
       mChart = findViewById(R.id.chart1);
       mChart2 = findViewById(R.id.chart2);
       mChart3 = findViewById(R.id.chart3);

       // enable description text
       mChart.getDescription().setEnabled(true);
       mChart2.getDescription().setEnabled(true);
       mChart3.getDescription().setEnabled(true);

       // enable touch gestures
       //mChart.setTouchEnabled(true);

       // enable scaling and dragging
       //mChart.setDragEnabled(true);
       //mChart.setScaleEnabled(true);
       mChart.setDrawGridBackground(false);
       mChart2.setDrawGridBackground(false);
       mChart3.setDrawGridBackground(false);

       // if disabled, scaling can be done on x- and y-axis separately
       //mChart.setPinchZoom(true);

       // set an alternative background color
       //mChart.setBackgroundColor(Color.LTGRAY);

       LineData data = new LineData();
       LineData data2 = new LineData();
       LineData data3 = new LineData();

       // add empty data
       mChart.setData(data);
       mChart2.setData(data2);
       mChart3.setData(data3);

       // get the legend (only possible after setting data)
       Legend l = mChart.getLegend();
       Legend l2 = mChart2.getLegend();
       Legend l3 = mChart3.getLegend();

       // modify the legend ...
       l.setForm(LegendForm.LINE);
       l.setTextColor(Color.WHITE);

       l2.setForm(LegendForm.LINE);
       l2.setTextColor(Color.WHITE);

       l3.setForm(LegendForm.LINE);
       l3.setTextColor(Color.WHITE);

       XAxis xl = mChart.getXAxis();
       xl.setTextColor(Color.WHITE);
       xl.setDrawGridLines(false);
       xl.setAvoidFirstLastClipping(true);
       xl.setEnabled(true);

       XAxis xl2 = mChart2.getXAxis();
       xl2.setTextColor(Color.WHITE);
       xl2.setDrawGridLines(false);
       xl2.setAvoidFirstLastClipping(true);
       xl2.setEnabled(true);

       XAxis xl3 = mChart3.getXAxis();
       xl3.setTextColor(Color.WHITE);
       xl3.setDrawGridLines(false);
       xl3.setAvoidFirstLastClipping(true);
       xl3.setEnabled(true);

       YAxis leftAxis = mChart.getAxisLeft();
       leftAxis.setTextColor(Color.WHITE);
       leftAxis.setDrawGridLines(true);

       YAxis rightAxis = mChart.getAxisRight();
       rightAxis.setEnabled(false);

       YAxis leftAxis2 = mChart2.getAxisLeft();
       leftAxis2.setTextColor(Color.WHITE);
       leftAxis2.setDrawGridLines(true);

       YAxis rightAxis2 = mChart2.getAxisRight();
       rightAxis2.setEnabled(false);

       YAxis leftAxis3 = mChart3.getAxisLeft();
       leftAxis3.setTextColor(Color.WHITE);
       leftAxis3.setDrawGridLines(true);

       YAxis rightAxis3 = mChart3.getAxisRight();
       rightAxis3.setEnabled(false);
       //---------------End Chart

        //Disable that annoying greed LED blinking everytime a sensor command it sent
        /*
        try {
            TSSBTSensor sensor = TSSBTSensor.getInstance();
            sensor.setLEDMode(1);
            //sensor.led_object = led_obj;
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }*/

        //Start the sensor
        Message start_again_message = new Message();
        start_again_message.what = 287;
        btSensorHandler.sendMessage(start_again_message);
        is_polling = true;

        // -----------------------------------

        mVisible = true;
        mControlsView = findViewById(R.id.fullscreen_content_controls);
        mContentView = findViewById(R.id.fullscreen_content);


        // Set up the user interaction to manually show or hide the system UI.
        mContentView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                toggle();
            }
        });

        //TARE BUTTON
        mTareButton = findViewById(R.id.tare_button);
        mTareButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    TSSBTSensor.getInstance().setTareCurrentOrient();
                    Toast.makeText(MainActivity.this, R.string.tare_button, Toast.LENGTH_SHORT).show();
                } catch (Exception e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        });

        mFirstName = findViewById(R.id.TextEdit_FirstName);
        mLastName = findViewById(R.id.TextEdit_LastName);
        mBodyPart = findViewById(R.id.spinnerBodyPart);
        mExerciseType = findViewById(R.id.spinnerExerciseType);
        mSide = findViewById(R.id.spinnerSide);
        context = getApplicationContext();
        activity = this;

        mExamToggle = true;
        mToggleButton = findViewById(R.id.toggle_button);
        mToggleButton.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                if (mExamToggle && !mFirstName.getText().toString().matches("") && !mLastName.getText().toString().matches("")) {
                    mToggleButton.setText(R.string.toggle_button2);
                    mExamToggle = false;
                    //Toast.makeText(MainActivity.this, mLastName.getText().toString() + ", " + mFirstName.getText().toString(), Toast.LENGTH_SHORT).show();

                    logfile = mFirstName.getText().toString() + "_" + mFirstName.getText().toString() + "_" + mBodyPart.getSelectedItem().toString() + "_"
                            + mExerciseType.getSelectedItem().toString() + "_" + mSide.getSelectedItem().toString() + "_" + System.currentTimeMillis() + ".txt";

                    //Start the GL updating
                    Message start_exercise_message = new Message();
                    start_exercise_message.what = 2;
                    btSensorHandler.removeCallbacksAndMessages(null);
                    btSensorHandler.sendMessage(start_exercise_message);
                    mGoogleDriveButton.setVisibility(View.GONE);
                } else {
                    mToggleButton.setText(R.string.toggle_button1);
                    //Toast.makeText(MainActivity.this, R.string.toggle_button1, Toast.LENGTH_SHORT).show();
                    mExamToggle = true;
                    Message stop_exercise_message = new Message();
                    stop_exercise_message.what = 3;
                    btSensorHandler.removeCallbacksAndMessages(null);
                    btSensorHandler.sendMessage(stop_exercise_message);
                    mGoogleDriveButton.setVisibility(View.VISIBLE);
                }
            }
        });

        //mQuitButton
        mQuitButton = findViewById(R.id.quit_button);
        mQuitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

       mGoogleDriveButton = findViewById(R.id.googledrive_button);
       mGoogleDriveButton.setOnClickListener(new View.OnClickListener() {
           @Override
           public void onClick(View v) {
               Intent intent = new Intent(getApplicationContext(), GoogleDriveActivity.class);
               startActivity(intent);
           }
       });

        feedMultiple();
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        // Trigger the initial hide() shortly after the activity has been
        // created, to briefly hint to the user that UI controls
        // are available.
        hide();
    }

    private void toggle() {
        if (mVisible) {
            hide();
        } else {
            show();
        }
    }

    private void hide() {
        // Hide UI first
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.hide();
        }
        mControlsView.setVisibility(View.GONE);
        mVisible = false;

        // Schedule a runnable to remove the status and navigation bar after a delay
        mHidePart2Runnable();
    }

    @SuppressLint("InlinedApi")
    private void show() {
        // Show the system bar
        mContentView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
        mVisible = true;

        // Schedule a runnable to display UI elements after a delay
        mShowPart2Runnable();
    }

    @Override
    public void onDestroy(){
        if(is_polling)
        {
            Message quit_message = new Message();
            quit_message.what = -1;
            btSensorHandler.sendMessage(quit_message);
            is_polling = false;
        }
        try {
            TSSBTSensor.getInstance().close();
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        super.onDestroy();
        int pid = android.os.Process.myPid();
        android.os.Process.killProcess(pid);

        //Kill Graph Thread
        if (mthread != null) {
            mthread.interrupt();
        }
        //

        try{
            trimCache(this);
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    @Override
    public void onPause() {
        if(is_polling)
        {
            Message quit_message = new Message();
            quit_message.what = -1;
            btSensorHandler.sendMessage(quit_message);
            is_polling = false;
        }
        try {
            TSSBTSensor.getInstance().close();
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        //Kill Graph Thread
        if (mthread != null) {
            mthread.interrupt();
        }
        super.onPause();
    }

    @Override
    public void onStart(){
        //Start the GL updating
        if(!is_polling)
        {
            Message start_again_message = new Message();
            start_again_message.what = 287;
            btSensorHandler.sendMessage(start_again_message);
            is_polling = true;
        }
        super.onStart();
    }

    @Override
    public void onStop()
    {
        //Stop our orientation polling thread
        if(is_polling)
        {
            Message stop_message = new Message();
            stop_message.what = -1;
            btSensorHandler.sendMessage(stop_message);
            is_polling = false;
        }
        super.onStop();
    }

    //--------------------------- Delete Cache data----------------------------------------------
    public static void trimCache(Context context) {
        try {
            File dir = context.getCacheDir();
            deleteDir(dir);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static boolean deleteDir(File dir) {
        if (dir != null && dir.isDirectory()) {
            String[] children = dir.list();
            for (int i = 0; i < children.length; i++) {
                boolean success = deleteDir(new File(dir, children[i]));
                if (!success) {
                    return false;
                }
            }
            return dir.delete();
        }
        else {
            return false;
        }
    }
    //-------------------------------------------------------------------------------------------

    private void addEntry() {

        LineData data = mChart.getData();
        LineData data2 = mChart2.getData();
        LineData data3 = mChart3.getData();

        if (data != null) {

            ILineDataSet setGX = data.getDataSetByIndex(0);
            ILineDataSet setGY = data.getDataSetByIndex(1);
            ILineDataSet setGZ = data.getDataSetByIndex(2);

            ILineDataSet setEX = data2.getDataSetByIndex(0);
            ILineDataSet setEY = data2.getDataSetByIndex(1);
            ILineDataSet setEZ = data2.getDataSetByIndex(2);

            ILineDataSet setAX = data3.getDataSetByIndex(0);
            ILineDataSet setAY = data3.getDataSetByIndex(1);
            ILineDataSet setAZ = data3.getDataSetByIndex(2);

            // set.addEntry(...); // can be called as well

            if (setGX == null  || setEX == null || setAX == null) {
                List<LineDataSet> sets = createSet();
                setGX = sets.get(0);
                setGY = sets.get(1);
                setGZ = sets.get(2);
                setEX = sets.get(3);
                setEY = sets.get(4);
                setEZ = sets.get(5);
                setAX = sets.get(6);
                setAY = sets.get(7);
                setAZ = sets.get(8);

                data.addDataSet(setGX);
                data.addDataSet(setGY);
                data.addDataSet(setGZ);

                data2.addDataSet(setEX);
                data2.addDataSet(setEY);
                data2.addDataSet(setEZ);

                data3.addDataSet(setAX);
                data3.addDataSet(setAY);
                data3.addDataSet(setAZ);
            }

            try{
                data.addEntry(new Entry(setGX.getEntryCount(), sensordata[4]), 0);
                data.addEntry(new Entry(setGY.getEntryCount(), sensordata[5]), 1);
                data.addEntry(new Entry(setGZ.getEntryCount(), sensordata[6]), 2);
                data2.addEntry(new Entry(setEX.getEntryCount(), sensordata[7]), 0);
                data2.addEntry(new Entry(setEY.getEntryCount(), sensordata[8]), 1);
                data2.addEntry(new Entry(setEZ.getEntryCount(), sensordata[9]), 2);
                data3.addEntry(new Entry(setAX.getEntryCount(), sensordata[10]), 0);
                data3.addEntry(new Entry(setAY.getEntryCount(), sensordata[11]), 1);
                data3.addEntry(new Entry(setAZ.getEntryCount(), sensordata[12]), 2);


            }catch(Exception e){
                data.addEntry(new Entry(setGX.getEntryCount(), (float) 0), 0);
                data.addEntry(new Entry(setGY.getEntryCount(), (float) 0), 1);
                data.addEntry(new Entry(setGZ.getEntryCount(), (float) 0), 2);
                data2.addEntry(new Entry(setEX.getEntryCount(), (float) 0), 0);
                data2.addEntry(new Entry(setEY.getEntryCount(), (float) 0), 1);
                data2.addEntry(new Entry(setEZ.getEntryCount(), (float) 0), 2);
                data3.addEntry(new Entry(setAX.getEntryCount(), (float) 0), 0);
                data3.addEntry(new Entry(setAY.getEntryCount(), (float) 0), 1);
                data3.addEntry(new Entry(setAZ.getEntryCount(), (float) 0), 2);
                e.printStackTrace();
            }

            data.notifyDataChanged();
            data2.notifyDataChanged();
            data3.notifyDataChanged();

            // let the chart know it's data has changed
            mChart.notifyDataSetChanged();
            mChart2.notifyDataSetChanged();
            mChart3.notifyDataSetChanged();

            // limit the number of visible entries
            mChart.setVisibleXRangeMaximum(120);
            mChart2.setVisibleXRangeMaximum(120);
            mChart3.setVisibleXRangeMaximum(120);
            // mChart.setVisibleYRange(30, AxisDependency.LEFT);

            // move to the latest entry
            mChart.moveViewToX(data.getEntryCount());
            mChart2.moveViewToX(data2.getEntryCount());
            mChart3.moveViewToX(data3.getEntryCount());

            // this automatically refreshes the chart (calls invalidate())
            // mChart.moveViewTo(data.getXValCount()-7, 55f,
            // AxisDependency.LEFT);
        }
    }

    private List<LineDataSet> createSet() {
        LineDataSet setGX = new LineDataSet(null, "Gyro X");
        LineDataSet setGY = new LineDataSet(null, "Gyro Y");
        LineDataSet setGZ = new LineDataSet(null, "Gyro Z");

        LineDataSet setEX = new LineDataSet(null, "Yaw");
        LineDataSet setEY = new LineDataSet(null, "Pitch");
        LineDataSet setEZ = new LineDataSet(null, "Roll");

        LineDataSet setAX = new LineDataSet(null, "Accel X");
        LineDataSet setAY = new LineDataSet(null, "Accel Y");
        LineDataSet setAZ = new LineDataSet(null, "Accel Z");

        //Gyro SETs
        setGX.setAxisDependency(AxisDependency.LEFT);
        setGX.setLineWidth(2f);
        setGX.setFillAlpha(65);
        setGX.setValueTextSize(9f);
        setGX.setDrawValues(false);
        setGX.setDrawCircles(false);

        setGY.setAxisDependency(AxisDependency.LEFT);
        setGY.setLineWidth(2f);
        setGY.setFillAlpha(65);
        setGY.setValueTextSize(9f);
        setGY.setDrawValues(false);
        setGY.setDrawCircles(false);

        setGZ.setAxisDependency(AxisDependency.LEFT);
        setGZ.setLineWidth(2f);
        setGZ.setFillAlpha(65);
        setGZ.setValueTextSize(9f);
        setGZ.setDrawValues(false);
        setGZ.setDrawCircles(false);

        //Euler SETs
        setEX.setAxisDependency(AxisDependency.LEFT);
        setEX.setLineWidth(2f);
        setEX.setFillAlpha(65);
        setEX.setValueTextSize(9f);
        setEX.setDrawValues(false);
        setEX.setDrawCircles(false);

        setEY.setAxisDependency(AxisDependency.LEFT);
        setEY.setLineWidth(2f);
        setEY.setFillAlpha(65);
        setEY.setValueTextSize(9f);
        setEY.setDrawValues(false);
        setEY.setDrawCircles(false);

        setEZ.setAxisDependency(AxisDependency.LEFT);
        setEZ.setLineWidth(2f);
        setEZ.setFillAlpha(65);
        setEZ.setValueTextSize(9f);
        setEZ.setDrawValues(false);
        setEZ.setDrawCircles(false);

        //Accel
        //Euler SETs
        setAX.setAxisDependency(AxisDependency.LEFT);
        setAX.setLineWidth(2f);
        setAX.setFillAlpha(65);
        setAX.setValueTextSize(9f);
        setAX.setDrawValues(false);
        setAX.setDrawCircles(false);

        setAY.setAxisDependency(AxisDependency.LEFT);
        setAY.setLineWidth(2f);
        setAY.setFillAlpha(65);
        setAY.setValueTextSize(9f);
        setAY.setDrawValues(false);
        setAY.setDrawCircles(false);

        setAZ.setAxisDependency(AxisDependency.LEFT);
        setAZ.setLineWidth(2f);
        setAZ.setFillAlpha(65);
        setAZ.setValueTextSize(9f);
        setAZ.setDrawValues(false);
        setAZ.setDrawCircles(false);

        //Gyro
        setGX.setColor(Color.RED);
        setGY.setColor(Color.BLUE);
        setGZ.setColor(Color.GREEN);

        setGX.setValueTextColor(Color.RED);
        setGY.setValueTextColor(Color.BLUE);
        setGZ.setValueTextColor(Color.GREEN);

        //Euler
        setEX.setColor(Color.RED);
        setEY.setColor(Color.BLUE);
        setEZ.setColor(Color.GREEN);

        setEX.setValueTextColor(Color.RED);
        setEY.setValueTextColor(Color.BLUE);
        setEZ.setValueTextColor(Color.GREEN);

        //Accel
        setAX.setColor(Color.RED);
        setAY.setColor(Color.BLUE);
        setAZ.setColor(Color.GREEN);

        setAX.setValueTextColor(Color.RED);
        setAY.setValueTextColor(Color.BLUE);
        setAZ.setValueTextColor(Color.GREEN);

        List<LineDataSet> dataSets = new ArrayList<>();
        dataSets.add(setGX);
        dataSets.add(setGY);
        dataSets.add(setGZ);
        dataSets.add(setEX);
        dataSets.add(setEY);
        dataSets.add(setEZ);
        dataSets.add(setAX);
        dataSets.add(setAY);
        dataSets.add(setAZ);
        return dataSets;
    }

    private void feedMultiple() {

        if (mthread != null)
            mthread.interrupt();

        final Runnable runnable = new Runnable() {

            @Override
            public void run() {
                addEntry();
            }
        };

        mthread = new Thread(new Runnable() {

            @Override
            public void run() {
                while(true) {

                    // Don't generate garbage runnables inside the loop.
                    runOnUiThread(runnable);

                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }
            }
        });

        mthread.start();
    }
}
