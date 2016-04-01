package com.pebble.acceldatastreamandroid;

import android.support.v7.app.ActionBar;
import android.content.Context;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import com.getpebble.android.kit.PebbleKit;
import com.getpebble.android.kit.util.PebbleDictionary;

import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getName();

    // UUID must match that of the watchapp
    //private static final UUID APP_UUID = UUID.fromString("bb039a8e-f72f-43fc-85dc-fd2516c7f328");
    private final static UUID APP_UUID = UUID.fromString("609a1291-dc9b-49a6-ab29-978ce04e7a1d"); // dit is die von dem Kollegen hecate

    private static final int SAMPLES_PER_UPDATE = 5;   // Must match the watchapp value
    private static final int ELEMENTS_PER_PACKAGE = 3;

    private ActionBar mActionBar;
    private TextView mTextView;

    private PebbleKit.PebbleDataReceiver msgDataReceiver;
    private PebbleKit.PebbleDataLogReceiver dataLoggingReceiver;

    private long sampleCount;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mActionBar = getSupportActionBar();
        mActionBar.setTitle("Pebble AccelDataStream");

        mTextView = (TextView)findViewById(R.id.output);



    }

    @Override
    protected void onResume() {
        super.onResume();



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

                mTextView.setText(builder.toString());
            }

        };

        dataLoggingReceiver = new PebbleKit.PebbleDataLogReceiver(APP_UUID) {
                @Override
                public void receiveData(Context context, UUID logUuid, Long timestamp, Long tag, byte[] data) {
                    System.out.println("ReceiveData!!!");
                    try {
                        AccelData accelData = AccelData.createFromBytes(data);

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
    }
}
