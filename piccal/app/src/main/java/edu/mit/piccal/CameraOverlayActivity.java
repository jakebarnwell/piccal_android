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
import android.view.LayoutInflater;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;

import java.io.IOException;

public class CameraOverlayActivity extends Activity implements SurfaceHolder.Callback{

    Camera camera;
    SurfaceView surfaceView;
    SurfaceHolder surfaceHolder;
    boolean previewing=false;
    LayoutInflater controlInflater=null;

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);

        getWindow().setFormat(PixelFormat.UNKNOWN);
        surfaceView=(SurfaceView)findViewById(R.id.camerapreview);
        surfaceHolder=surfaceView.getHolder();
        surfaceHolder.addCallback(this);
        surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

        controlInflater=LayoutInflater.from(getBaseContext());
        View viewControl=controlInflater.inflate(R.layout.control, null);
        ViewGroup.LayoutParams layoutParamsControl = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.FILL_PARENT,
                ViewGroup.LayoutParams.FILL_PARENT);
        this.addContentView(viewControl,layoutParamsControl);

    }


    @Override
    public void surfaceChanged(SurfaceHolder holder,int format,int width,
                               int height){
// TODO Auto-generated method stub
        if(previewing){
            camera.stopPreview();
            previewing=false;
        }

        if(camera!=null){
            try{
                camera.setPreviewDisplay(surfaceHolder);
                camera.startPreview();
                previewing=true;
            }catch(IOException e){
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder){
// TODO Auto-generated method stub
        camera=Camera.open();
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder){
// TODO Auto-generated method stub
        camera.stopPreview();
        camera.release();
        camera=null;
        previewing=false;
    }

}