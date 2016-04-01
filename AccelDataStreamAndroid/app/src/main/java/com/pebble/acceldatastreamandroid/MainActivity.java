package com.pebble.acceldatastreamandroid;

import android.annotation.TargetApi;
import android.os.Environment;
import android.support.v7.app.ActionBar;
import android.content.Context;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.getpebble.android.kit.PebbleKit;
import com.getpebble.android.kit.util.PebbleDictionary;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getName();

    // UUID must match that of the watchapp
    private static final UUID APP_UUID = UUID.fromString("40f5f000-e15c-4eb6-974d-36c3b4dcf2e9"); // dit is die von Christoph
    //private final static UUID APP_UUID = UUID.fromString("609a1291-dc9b-49a6-ab29-978ce04e7a1d"); // dit is die von dem Kollegen hecate

    private static final int SAMPLES_PER_UPDATE = 5;   // Must match the watchapp value
    private static final int ELEMENTS_PER_PACKAGE = 3;

    private ActionBar mActionBar;
    private TextView mTextView;

    private Button bToggleSaveComment;
    private EditText bCommentText;

    private PebbleKit.PebbleDataReceiver msgDataReceiver;
    private PebbleKit.PebbleDataLogReceiver dataLoggingReceiver;

    private long sampleCount;

    private File externalDirectory;
    private FileWriter logFile;
    private BufferedWriter bufferedWriter;

    private String logFileName;
    private String logFullPath;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mActionBar = getSupportActionBar();
        mActionBar.setTitle("Pebble AccelDataStream");

        mTextView = (TextView)findViewById(R.id.output);



        if(!openFile()){
            Log.e(TAG, "Error opening file, closing app!");
            android.os.Process.killProcess(android.os.Process.myPid());
            System.exit(1);
        }

        bToggleSaveComment = (Button)findViewById(R.id.button);

        bToggleSaveComment.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                bCommentText = (EditText) findViewById(R.id.editText);
                if (isFileOpen()) {
                    String comment = bCommentText.getText().toString();
                    Date date = new Date();
                    try {
                        bufferedWriter.write(date + "," + comment + "\n");
                    } catch (IOException e) {
                        Log.e(TAG, "Saving comment failed: " + e.getLocalizedMessage());
                        e.printStackTrace();
                    }
                }
                bCommentText.getText().clear();
            }
        });


    }

    @Override
    protected void onResume() {
        super.onResume();

        if(!isFileOpen()){
            openFile();
        }

        msgDataReceiver = new PebbleKit.PebbleDataReceiver(APP_UUID) {

            @Override
            public void receiveData(Context context, int transactionId, PebbleDictionary data) {
                PebbleKit.sendAckToPebble(context, transactionId);

                // Build a string of all payload data for display
                StringBuilder builder = new StringBuilder();
                builder.append("Most recent packet (X Y Z): \n");
                for(int i = 0; i < SAMPLES_PER_UPDATE; i++) {
                    for(int j = 0; j < ELEMENTS_PER_PACKAGE; j++) {
                        if(data.getInteger((i * ELEMENTS_PER_PACKAGE) + j) != null) {
                            builder.append(" " + data.getInteger((i * ELEMENTS_PER_PACKAGE) + j).intValue());
                        } else {
                            Log.e(TAG, "Item " + i + " does not exist");
                        }
                    }
                    builder.append("\n");
                }

                //mTextView.setText(builder.toString());
                mTextView.setText("Have received some data!");
            }

        };

        dataLoggingReceiver = new PebbleKit.PebbleDataLogReceiver(APP_UUID) {
                @Override
                public void receiveData(Context context, UUID logUuid, Long timestamp, Long tag, byte[] data) {
                    System.out.println("ReceiveData!!!");
                    try {
                        AccelData accelData = AccelData.createFromBytes(data);

                        if(isFileOpen()) {
                            bufferedWriter.write(accelData.timestamp + "," + accelData.x + "," + accelData.y + "," + accelData.z + "\n");
                        }

                        System.out.println("Data logging fired:" + accelData.did_vibrate + "," + accelData.timestamp + "," + accelData.x + "," + accelData.y + "," + accelData.z);

                        sampleCount ++;


                    } catch (Exception e) {
                        Log.e(TAG, "Data log receiver failed: " + e.getLocalizedMessage());
                        e.printStackTrace();
                    }
                }

                @Override
                public void onFinishSession(android.content.Context context, UUID logUuid, Long timestamp, Long tag) {

                    mTextView.setText("Received data log at " + timestamp + " Sample count: " + sampleCount);

                    //just to make sure we save!
                    closeFile();
                    openFile();

                }
            };


        PebbleKit.registerReceivedDataHandler(getApplicationContext(), msgDataReceiver);
        PebbleKit.registerDataLogReceiver(getApplicationContext(), dataLoggingReceiver);

    }

    @Override
    protected void onPause() {
        super.onPause();

        if(msgDataReceiver != null) {
            try {
                unregisterReceiver(msgDataReceiver);
                msgDataReceiver = null;
            } catch (Exception e) {
                Log.e(TAG, "Error unregistering message receiver: " + e.getLocalizedMessage());
                e.printStackTrace();
            }
        }


    }

    @Override
    protected void onStop(){
        super.onStop();

        if(dataLoggingReceiver != null) {
            try {
                unregisterReceiver(dataLoggingReceiver);
                dataLoggingReceiver = null;
            } catch (Exception e) {
                Log.e(TAG, "Error unregistering data logging receiver: " + e.getLocalizedMessage());
                e.printStackTrace();
            }
        }

        closeFile();
    }

    private File getLogStorageDir(String logsName)
    {

        if (android.os.Build.VERSION.SDK_INT >= 19) {
            // Get the directory for the user's public pictures directory.
            File file = new File(getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), logsName);
            if (!file.mkdirs())
            {

            }
            return file;
        }else {
            File file = new File(getExternalFilesDir(Environment.getExternalStorageDirectory() + "/Documents"), logsName);
            if (!file.mkdirs())
            {

            }
            return file;
        }

    }



    private boolean closeFile(){
        if(isFileOpen()) {
            try {
                bufferedWriter.close();
                logFile.close();

                bufferedWriter = null;
                logFile = null;
                return true;

            } catch (IOException e) {
                Log.e(TAG, "Error writing file: " + e.getLocalizedMessage());
                e.printStackTrace();

                return false;
            }
        }

        return true;


    }

    private boolean isFileOpen(){
        if (bufferedWriter!=null){
            return true;
        }else{
            return false;
        }
    }

    private boolean openFile(){
        logFileName = new SimpleDateFormat("yyyy_MM_dd-hh_mm_ss").format(new Date());
        externalDirectory = getLogStorageDir("AccelDataStreamAndroid");
        logFullPath = externalDirectory.getAbsolutePath() + "/" + logFileName;

        try {
            logFile = new FileWriter(logFullPath);
            bufferedWriter = new BufferedWriter(logFile);
            return true;
        } catch (IOException e) {
            Log.e(TAG, "Error openening log file: " + e.getLocalizedMessage());
            e.printStackTrace();
            return false;
        }
    }
}
