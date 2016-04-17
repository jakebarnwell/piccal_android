package edu.mit.piccal;

import android.app.Activity;
import android.content.pm.ActivityInfo;
import android.hardware.Camera;
import android.graphics.PixelFormat;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;

import java.io.IOException;

public class CameraOverlayActivity extends AppCompatActivity {

    private Camera mCamera = null;
    private CameraView mCameraView = null;
    private final static String LOG_HEADER = "piccal - CameraOverlay";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(LOG_HEADER, "Camera Overlay Activity onCreate.");

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

    }

    public void onCameraButtonClick(View view) {
        try{
            mCamera = Camera.open();//you can use open(int) to use different cameras
        } catch (Exception e){
            Log.d("ERROR", "Failed to get camera: " + e.getMessage());
        }

        Button camerabutton = (Button)findViewById(R.id.camera_button);
        Log.d(LOG_HEADER, "camerabutton is null : " + (camerabutton == null));

        if(mCamera != null) {
            mCameraView = new CameraView(this, mCamera);//create a SurfaceView to show camera data
            FrameLayout camera_view = (FrameLayout)findViewById(R.id.camera_view);
            Log.d(LOG_HEADER, "mCameraView is : " + mCameraView.toString());
            if(camera_view == null) {
                Log.d(LOG_HEADER, "camera_view is null");
            } else {
                Log.d(LOG_HEADER, "camera_view is null : " + camera_view.toString());
            }
            camera_view.addView(mCameraView);//add the SurfaceView to the layout
            // the above line is where we get a NullPointerException
        }

        //btn to close the application
        Button imgClose = (Button)findViewById(R.id.camera_button);
        imgClose.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
//                System.exit(0);
                ;
            }
        });
    }

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log.d(LOG_HEADER, "Camera Overlay Activity on create view.");
        return null;
    }

    protected void onActivityCreated(Bundle savedInstanceState) {
        Log.d(LOG_HEADER, "Camera Overlay Activity on activity created.");
    }
//    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
//        ;
//    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.d(LOG_HEADER, "Camera Overlay Activity onStart.");

//        try{
//            mCamera = Camera.open();//you can use open(int) to use different cameras
//        } catch (Exception e){
//            Log.d("ERROR", "Failed to get camera: " + e.getMessage());
//        }
//
//        Button camerabutton = (Button)findViewById(R.id.camera_button);
//        Log.d(LOG_HEADER, "camerabutton is null : " + (camerabutton == null));
//
//
//        if(mCamera != null) {
//            mCameraView = new CameraView(this, mCamera);//create a SurfaceView to show camera data
//            FrameLayout camera_view = (FrameLayout)findViewById(R.id.camera_view);
//            Log.d(LOG_HEADER, "mCameraView is : " + mCameraView.toString());
//            if(camera_view == null) {
//                Log.d(LOG_HEADER, "camera_view is null");
//            } else {
//                Log.d(LOG_HEADER, "camera_view is null : " + camera_view.toString());
//            }
//            camera_view.addView(mCameraView);//add the SurfaceView to the layout
//            // the above line is where we get a NullPointerException
//        }
//
//        //btn to close the application
//        Button imgClose = (Button)findViewById(R.id.camera_button);
//        imgClose.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
////                System.exit(0);
//                ;
//            }
//        });
    }


}