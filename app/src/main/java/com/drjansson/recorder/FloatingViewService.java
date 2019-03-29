package com.drjansson.recorder;

import android.app.Activity;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.media.projection.MediaProjectionManager;
import android.os.IBinder;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;

import static com.drjansson.recorder.MainActivity.REQUEST_CODE_SCREEN_CAPTURE;

public class FloatingViewService extends Service implements View.OnClickListener {
    private static final String TAG = FloatingViewService.class.getName();
    private static final boolean DEBUG = true;

    private WindowManager mWindowManager;
    private View mFloatingView;
    private View collapsedView;
    private View expandedView;
    private int lastEvent = 0;

    private static final String BASE = "com.drjansson.recorder.ScreenRecorderService.";
    public static final String ACTION_SCREEN_RECORD = BASE + "ACTION_START";
    public static final String ACTION_SCREEN_RECORD_STOP = BASE + "ACTION_STOP";

    //Context mainActivity = null;


    public FloatingViewService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        //getting the widget layout from xml using layout inflater
        mFloatingView = LayoutInflater.from(this).inflate(R.layout.layout_floating_widget, null);

        //setting the layout parameters
        final WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT);


        //getting windows services and adding the floating view to it
        mWindowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        mWindowManager.addView(mFloatingView, params);


        //getting the collapsed and expanded view from the floating view
        collapsedView = mFloatingView.findViewById(R.id.layoutCollapsed);
        expandedView = mFloatingView.findViewById(R.id.layoutExpanded);

        //adding click listener to close button and expanded view
        mFloatingView.findViewById(R.id.buttonClose).setOnClickListener(this);
        //mFloatingView.findViewById(R.id.layoutCollapsed).setOnClickListener(this);
        expandedView.setOnClickListener(this);
        //collapsedView.setOnClickListener(this);

        //adding an touchlistener to make drag movement of the floating widget
        mFloatingView.findViewById(R.id.relativeLayoutParent).setOnTouchListener(new View.OnTouchListener() {
            private int initialX;
            private int initialY;
            private float initialTouchX;
            private float initialTouchY;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        initialX = params.x;
                        initialY = params.y;
                        initialTouchX = event.getRawX();
                        initialTouchY = event.getRawY();

                        lastEvent = MotionEvent.ACTION_DOWN;
                        return true;

                    case MotionEvent.ACTION_UP:
                        //when the drag is ended switching the state of the widget
                        if(lastEvent == MotionEvent.ACTION_DOWN) {
                            collapsedView.setVisibility(View.GONE);
                            expandedView.setVisibility(View.VISIBLE);
                            startScreenRecord();
                        }
                        lastEvent = MotionEvent.ACTION_UP;
                        return true;

                    case MotionEvent.ACTION_MOVE:
                        //this code is helping the widget to move around the screen with fingers
                        params.x = initialX + (int) (event.getRawX() - initialTouchX);
                        params.y = initialY + (int) (event.getRawY() - initialTouchY);
                        mWindowManager.updateViewLayout(mFloatingView, params);

                        lastEvent = MotionEvent.ACTION_MOVE;
                        return true;
                }
                return false;
            }
        });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mFloatingView != null) mWindowManager.removeView(mFloatingView);
    }

    /*@Override
    public int onStartCommand(Intent intent, int flags, int startId){
        //mainActivity = intent.getClass();
        return 0;
    }*/

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.layoutExpanded:
                //switching views
                collapsedView.setVisibility(View.VISIBLE);
                expandedView.setVisibility(View.GONE);
                stopScreenRecord();
                break;

            /*case R.id.collapsed_iv:
                collapsedView.setVisibility(View.GONE);
                expandedView.setVisibility(View.VISIBLE);
                startScreenRecord();
                break;*/

            case R.id.buttonClose:
                //closing the widget
                stopSelf();
                break;
        }
    }

    private void startScreenRecord(){
        if(DEBUG) Log.i(TAG, "start recording");
        final MediaProjectionManager manager
                = (MediaProjectionManager)getSystemService(Context.MEDIA_PROJECTION_SERVICE);
        final Intent permissionIntent = manager != null ? manager.createScreenCaptureIntent() : new Intent();
        startActivity(permissionIntent, null);//REQUEST_CODE_SCREEN_CAPTURE);

        final Intent intent = new Intent(); //this, MainActivity.class);
        intent.setAction(FloatingViewService.ACTION_SCREEN_RECORD);
        //intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        sendBroadcast(intent);

    }

    private void stopScreenRecord(){
        final Intent intent = new Intent();//this, MainActivity.class);
        intent.setAction(FloatingViewService.ACTION_SCREEN_RECORD_STOP);
        //intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        sendBroadcast(intent);
    }
}