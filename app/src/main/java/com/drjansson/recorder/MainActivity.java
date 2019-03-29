package com.drjansson.recorder;

import android.Manifest;
import android.app.Activity;
import android.app.IntentService;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.media.projection.MediaProjectionManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.Toast;
import android.widget.ToggleButton;

import java.lang.ref.WeakReference;

public class MainActivity extends AppCompatActivity {

    Button btnScreenCapture;
    ToggleButton btnRecord;
    ToggleButton btnStopRecord;
    Button btnCreateWidget;

    private static final String TAG = MainActivity.class.getName();
    private static final boolean DEBUG = true;

    public static final int REQUEST_CODE_SCREEN_CAPTURE = 1;
    private static final int REQUEST_CODE_PERMISSIONS_EXTERNAL_STORAGE = 5678;
    private MyBroadcastReceiver mReceiver;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });


        btnScreenCapture = findViewById(R.id.btnScreenCapture);
        btnRecord = findViewById(R.id.btnRecord);
        btnStopRecord = findViewById(R.id.btnStopRecord);
        btnCreateWidget = findViewById(R.id.btnCreateWidget);

        btnScreenCapture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Intent intent = new Intent(MainActivity.this, ScreenshotImageActivity.class);
                intent.putExtra("Start", true);
                startActivity(intent);

            }
        });

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {

            requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE,
                            Manifest.permission.READ_EXTERNAL_STORAGE},
                    REQUEST_CODE_PERMISSIONS_EXTERNAL_STORAGE);
        }


        btnCreateWidget.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
                    //startService(new Intent(MainActivity.this, FloatingViewService.class));
                    //finish();
                if (Settings.canDrawOverlays(MainActivity.this)) {
                    startService(new Intent(MainActivity.this, FloatingViewService.class));
                    //finish();
                } else {
                    askOVERLAYPermission();
                    Toast.makeText(MainActivity.this, "You need System Alert Window Permission to do this", Toast.LENGTH_SHORT).show();
                }

            }
        });

        updateRecording(false, false);
        if (mReceiver == null)
            mReceiver = new MyBroadcastReceiver(this);

        //regReceiver();

        /*Intent startIntent = getIntent();
        String action = startIntent.getAction();
        if(action != null) {

            if (FloatingViewService.ACTION_SCREEN_RECORD.equals(action)) {
               askSCREENCAPTUREPermission();
            } else if (FloatingViewService.ACTION_SCREEN_RECORD_STOP.equals(action)) {
                    stopScreenRecorder();
            }
        }*/

    }

    @Override
    protected void onResume() {
        super.onResume();
        if (DEBUG) Log.v(TAG, "onResume:");
        regReceiver();
        queryRecordingStatus();
    }

    @Override
    protected void onPause() {
        if (DEBUG) Log.v(TAG, "onPause:");
        //unregisterReceiver(mReceiver);
        super.onPause();
    }

    @Override
    protected void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        if (DEBUG) Log.v(TAG, "onActivityResult:resultCode=" + resultCode + ",data=" + data + " ,RequestCode= "+requestCode);
        super.onActivityResult(requestCode, resultCode, data);
        if (REQUEST_CODE_SCREEN_CAPTURE == requestCode) {
            if (resultCode != Activity.RESULT_OK) {
                // when no permission
                Toast.makeText(this, "permission denied", Toast.LENGTH_LONG).show();
                return;
            }
            startScreenRecorder(resultCode, data);
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void regReceiver(){
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(ScreenRecorderService.ACTION_QUERY_STATUS_RESULT);
        intentFilter.addAction(FloatingViewService.ACTION_SCREEN_RECORD);
        intentFilter.addAction(FloatingViewService.ACTION_SCREEN_RECORD_STOP);
        registerReceiver(mReceiver, intentFilter);
    }

    private void askOVERLAYPermission() {
        Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:" + getPackageName()));
        startActivityForResult(intent, 2084);
    }

    public void askSCREENCAPTUREPermission(){
        if(DEBUG) Log.i(TAG, "start recording");
        final MediaProjectionManager manager
                = (MediaProjectionManager)getSystemService(Context.MEDIA_PROJECTION_SERVICE);
        final Intent permissionIntent = manager != null ? manager.createScreenCaptureIntent() : new Intent();
        startActivityForResult(permissionIntent, REQUEST_CODE_SCREEN_CAPTURE);
    }

    private final CompoundButton.OnCheckedChangeListener mOnCheckedChangeListener = new CompoundButton.OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(final CompoundButton buttonView, final boolean isChecked) {
            switch (buttonView.getId()) {
                case R.id.btnRecord:
                    if (isChecked) {
                        askSCREENCAPTUREPermission();
                    } else {
                        stopScreenRecorder();
                    }
                    break;
                case R.id.btnStopRecord:
                    if (isChecked) {
                        final Intent intent = new Intent(MainActivity.this, ScreenRecorderService.class);
                        intent.setAction(ScreenRecorderService.ACTION_PAUSE);
                        startService(intent);
                    } else {
                        final Intent intent = new Intent(MainActivity.this, ScreenRecorderService.class);
                        intent.setAction(ScreenRecorderService.ACTION_RESUME);
                        startService(intent);
                    }
                    break;
            }
        }
    };

    private void queryRecordingStatus() {
        if (DEBUG) Log.v(TAG, "queryRecording:");
        final Intent intent = new Intent(this, ScreenRecorderService.class);
        intent.setAction(ScreenRecorderService.ACTION_QUERY_STATUS);
        startService(intent);
    }

    public void startScreenRecorder(final int resultCode, final Intent data) {
        final Intent intent = new Intent(this, ScreenRecorderService.class);
        intent.setAction(ScreenRecorderService.ACTION_START);
        intent.putExtra(ScreenRecorderService.EXTRA_RESULT_CODE, resultCode);
        intent.putExtras(data);
        startService(intent);
    }

    private void stopScreenRecorder(){
        if(DEBUG) Log.i(TAG, "stop recording");
        final Intent intent = new Intent(MainActivity.this, ScreenRecorderService.class);
        intent.setAction(ScreenRecorderService.ACTION_STOP);
        startService(intent);
    }

    private void updateRecording(final boolean isRecording, final boolean isPausing) {
        if (DEBUG) Log.v(TAG, "updateRecording:isRecording=" + isRecording + ",isPausing=" + isPausing);
        btnRecord.setOnCheckedChangeListener(null);
        btnStopRecord.setOnCheckedChangeListener(null);
        try {
            btnRecord.setChecked(isRecording);
            //btnStopRecord.setEnabled(isRecording);
            btnStopRecord.setChecked(isPausing);
        } finally {
            btnRecord.setOnCheckedChangeListener(mOnCheckedChangeListener);
            btnStopRecord.setOnCheckedChangeListener(mOnCheckedChangeListener);
        }
    }

    private static final class MyBroadcastReceiver extends BroadcastReceiver {
        private final WeakReference<MainActivity> mWeakParent;
        public MyBroadcastReceiver(final MainActivity parent) {
            mWeakParent = new WeakReference<>(parent);
        }

        @Override
        public void onReceive(final Context context, final Intent intent) {
            if (DEBUG) Log.v(TAG, "onReceive:" + intent);
            final MainActivity parent = mWeakParent.get();
            final String action = intent.getAction();
            if (ScreenRecorderService.ACTION_QUERY_STATUS_RESULT.equals(action)) {
                final boolean isRecording = intent.getBooleanExtra(ScreenRecorderService.EXTRA_QUERY_RESULT_RECORDING, false);
                final boolean isPausing = intent.getBooleanExtra(ScreenRecorderService.EXTRA_QUERY_RESULT_PAUSING, false);
                if (parent != null) {
                    parent.updateRecording(isRecording, isPausing);
                }
            }else if (FloatingViewService.ACTION_SCREEN_RECORD.equals(action)) {
                if (parent != null) {
                    parent.askSCREENCAPTUREPermission();
                }
            }else if (FloatingViewService.ACTION_SCREEN_RECORD_STOP.equals(action)) {
                if (parent != null) {
                    parent.stopScreenRecorder();
                }
            }
        }
    }


}
