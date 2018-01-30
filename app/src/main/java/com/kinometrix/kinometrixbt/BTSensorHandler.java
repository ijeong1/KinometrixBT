package com.kinometrix.kinometrixbt;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;

public class BTSensorHandler extends Handler {

    private boolean keep_going = false;
    public static final int REQUEST_WRITE_EXTERNAL_STORAGE_PERMISSION = 99;
    private final String LOG_TAG = BTSensorHandler.class.getSimpleName();
    private BufferedWriter buff = null;
    private boolean is_first = true;

    @Override
    public void handleMessage(Message msg) {
        //Check if we are supposed to keep going or not
        if (msg.what == -1)
        {
            keep_going = false;
            try{
                buff.close();
            }catch (Exception e)
            {
                Log.e(LOG_TAG, "Couldn't close log file ", e);
            }

            try
            {
                TSSBTSensor.getInstance().stopStreaming();
            }
            catch( Exception e)
            {
                return;
            }

        }
        else if (msg.what == 287 && keep_going == false)
        {
            checkRuntimeWriteExternalStoragePermission(MainActivity.context, MainActivity.activity);
            keep_going = true;
            //Call yourself again in a bit
            Message tmp_message = new Message();
            tmp_message.what = 1;
            sendMessageDelayed(tmp_message, 250);
        }
        else if(msg.what == 1)
        {
            if (keep_going)
            {
                try
                {
                    if(!TSSBTSensor.getInstance().is_streaming)
                    {
                        TSSBTSensor.getInstance().startStreaming();
                    }
                }
                catch( Exception e)
                {
                    return;
                }

                //Update the GL scene based on the sensor's orientation
                float[] orient;
                try {
                    orient = TSSBTSensor.getInstance().getSensorStreamingData();
                    MainActivity.sensordata = orient;
                    // Call yourself again in a bit
                    sendMessageDelayed(obtainMessage(1,0,0), 100);
                } catch (Exception e) {
                    return;
                }
            }
        }else if(msg.what == 2){
            if (keep_going)
            {
                if(is_first) {
                    try {
                        if (!TSSBTSensor.getInstance().is_streaming) {
                            TSSBTSensor.getInstance().startStreaming();
                        }
                    } catch (Exception e) {
                        return;
                    }
                    if (isExternalStorageWritable()) {
                        try {
                            File externalDirectory = Environment.getExternalStorageDirectory();
                            File appDirectory = new File(externalDirectory, MainActivity.DIRECTORY_NAME);
                            File logFile = new File(appDirectory, MainActivity.logfile);

                            // create app folder
                            if (!appDirectory.exists()) {
                                boolean status = appDirectory.mkdirs();
                                Log.e(LOG_TAG, "appDirectory created: " + status);
                            }

                            // create log file
                            if (!logFile.exists()) {
                                boolean status = logFile.createNewFile();
                                Log.e(LOG_TAG, "logFile.createNewFile() created: " + status);
                            }
                            buff = new BufferedWriter(new FileWriter(logFile, true));
                        } catch (IOException e) {
                            Log.e(LOG_TAG, "Couldn't create log file ", e);
                        }
                    } else {
                        Log.e(LOG_TAG, "No permission for writing logs");
                    }
                    is_first=false;
                }

                //Update the GL scene based on the sensor's orientation
                float[] orient;
                try {
                    orient = TSSBTSensor.getInstance().getSensorStreamingData();
                    MainActivity.sensordata = orient;
                    //MainActivity.dataset = orient;
                    buff.append(System.currentTimeMillis() +", " + orient[4] + ", " + orient[5] + ", "  + orient[5] + ", "
                            + orient[6] + ", " + orient[7] + ", " + orient[8] + ", "
                            + orient[9] + ", " + orient[10] + ", " + orient[11] + "\n");
                    // Call yourself again in a bit
                    sendMessageDelayed(obtainMessage(2,0,0), 100);
                } catch (Exception e) {
                    return;
                }
            }
        }else if(msg.what == 3){
            if (keep_going)
            {
                try
                {
                    if(!TSSBTSensor.getInstance().is_streaming)
                    {
                        TSSBTSensor.getInstance().startStreaming();
                    }
                }
                catch( Exception e)
                {
                    return;
                }

                try{
                    buff.close();
                }catch(Exception e){
                    Log.e(LOG_TAG, "couldn't close the BufferWriter");
                }
                //Update the GL scene based on the sensor's orientation
                float[] orient;
                try {
                    orient = TSSBTSensor.getInstance().getSensorStreamingData();
                    MainActivity.sensordata = orient;
                    // Call yourself again in a bit
                    sendMessageDelayed(obtainMessage(1,0,0), 100);
                } catch (Exception e) {
                    return;
                }
            }
        }
    }

    /* Checks if external storage is available for read and write */
    private boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            return true;
        }
        return false;
    }

    public boolean checkRuntimeWriteExternalStoragePermission(Context context, final Activity activity) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {

            if (ActivityCompat.shouldShowRequestPermissionRationale(activity,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE)) {

                new AlertDialog.Builder(activity)
                        .setTitle(R.string.write_external_storage_permission_title)
                        .setMessage(R.string.write_external_storage_permission_text)
                        .setPositiveButton(R.string.btn_ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                //Prompt the user once explanation has been shown
                                requestForWriteExternalStoragePermission(activity);
                            }
                        })
                        .create()
                        .show();

            } else {
                requestForWriteExternalStoragePermission(activity);
            }
            Log.e(LOG_TAG, "checkRuntimeWriteExternalStoragePermission() FALSE");
            return false;
        } else {
            Log.e(LOG_TAG, "checkRuntimeWriteExternalStoragePermission() TRUE");
            return true;
        }
    }

    private void requestForWriteExternalStoragePermission(Activity activity) {
        ActivityCompat.requestPermissions(activity, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                REQUEST_WRITE_EXTERNAL_STORAGE_PERMISSION);
    }
}
