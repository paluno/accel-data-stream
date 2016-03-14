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
    private static final UUID APP_UUID = UUID.fromString("bb039a8e-f72f-43fc-85dc-fd2516c7f328");

    private static final int SAMPLES_PER_UPDATE = 5;   // Must match the watchapp value
    private static final int ELEMENTS_PER_PACKAGE = 3;

    private ActionBar mActionBar;
    private TextView mTextView;

    private PebbleKit.PebbleDataReceiver mDataReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mActionBar = getSupportActionBar();
        mActionBar.setTitle("AccelDataStream");

        mTextView = (TextView)findViewById(R.id.output);
    }

    @Override
    protected void onResume() {
        super.onResume();

        mDataReceiver = new PebbleKit.PebbleDataReceiver(APP_UUID) {

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
        PebbleKit.registerReceivedDataHandler(getApplicationContext(), mDataReceiver);
    }

    @Override
    protected void onPause() {
        super.onPause();

        if(mDataReceiver != null) {
            try {
                unregisterReceiver(mDataReceiver);
                mDataReceiver = null;
            } catch (Exception e) {
                Log.e(TAG, "Error unregistering receiver: " + e.getLocalizedMessage());
                e.printStackTrace();
            }
        }
    }
}
