package edu.mit.piccal;

import android.app.Activity;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.hardware.Camera;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;

// Taken from https://github.com/commonsguy/cw-advandroid/tree/master/Camera/Preview
//  and https://github.com/commonsguy/cw-advandroid/tree/master/Camera/Picture

public class CameraOverlayActivity extends Activity {
    private SurfaceView preview = null;
    private ImageView imview = null;
    private SurfaceHolder previewHolder = null;
    private Camera camera=null;
    private boolean inPreview=false;
    private boolean cameraConfigured=false;
    private final static String LOG_HEADER = "piccal - CameraOverlay";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.content_camera_overlay);

        preview=(SurfaceView)findViewById(R.id.preview);
        previewHolder=preview.getHolder();
        previewHolder.addCallback(surfaceCallback);
        previewHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

        imview = (ImageView)findViewById(R.id.imageView);
    }

    public void onClick_takePicture(View view) {
//        if (inPreview) {
//            camera.takePicture(null, null, photoCallback);
//            inPreview=false;
//        }
//        findViewById(R.id.preview).setVisibility(View.VISIBLE);
//        findViewById(R.id.imageView).setVisibility(View.GONE);

        if(preview.getVisibility() == View.VISIBLE) {
            hide(preview);
            show(imview);
        } else {
            show(preview);
            hide(imview);
        }
    }

    Camera.PictureCallback photoCallback = new Camera.PictureCallback() {
        public void onPictureTaken(byte[] data, Camera camera) {
            Camera.Parameters cpm  = camera.getParameters();
            new SavePhotoTask().execute(data);
            camera.startPreview();
            inPreview=true;
        }
    };

    private void show(View view) {
        if(view != null) {
            try {
                if(view.getVisibility() != View.VISIBLE) {
                    view.setVisibility(View.VISIBLE);
                }
            } catch(Exception e) {
                Log.e(LOG_HEADER, "Error getting visibility of view object: " + e.toString());
            }
        }
    }

    private void hide(View view) {
        if(view != null) {
            try {
                if(view.getVisibility() == View.VISIBLE) {
                    view.setVisibility(View.GONE);
                }
            } catch(Exception e) {
                Log.e(LOG_HEADER, "Error getting visibility of view object: " + e.toString());
            }
        }
    }

    class SavePhotoTask extends AsyncTask<byte[], String, String> {
        @Override
        protected String doInBackground(byte[]... jpeg) {
            SurfaceView previewWindow = (SurfaceView)findViewById(R.id.preview);
            stopPreview();
//            showImageView();
//            // Get the dimensions of the View
//            int targetW = previewWindow.getWidth();
//            int targetH = previewWindow.getHeight();
//
//            // Get the dimensions of the bitmap
//            BitmapFactory.Options bmOptions = new BitmapFactory.Options();
//            bmOptions.inJustDecodeBounds = true;
//            BitmapFactory.decodeFile(mCurrentPhotoPath, bmOptions);
//            int photoW = bmOptions.outWidth;
//            int photoH = bmOptions.outHeight;
//
//            // Determine how much to scale down the image
//            int scaleFactor = Math.min(photoW/targetW, photoH/targetH);
//
//            // Decode the image file into a Bitmap sized to fill the View
//            bmOptions.inJustDecodeBounds = false;
//            bmOptions.inSampleSize = scaleFactor << 1;
//            bmOptions.inPurgeable = true;
//
//            Bitmap bitmap = BitmapFactory.decodeFile(mCurrentPhotoPath, bmOptions);
//
//            Matrix mtx = new Matrix();
//            mtx.postRotate(90);
//            // Rotating Bitmap
//            Bitmap rotatedBMP = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), mtx, true);
//
//            if (rotatedBMP != bitmap) {
//                bitmap.recycle();
//            }


            /* ------------------------- */

            File photo=
                    new File(Environment.getExternalStorageDirectory(),
                            "photo.jpg");

            if (photo.exists()) {
                photo.delete();
            }

            try {
                FileOutputStream fos=new FileOutputStream(photo.getPath());

                fos.write(jpeg[0]);
                fos.close();
            }
            catch (java.io.IOException e) {
                Log.e("PictureDemo", "Exception in photoCallback", e);
            }

            return(null);
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        camera=Camera.open();
        startPreview();
    }

    @Override
    public void onPause() {
        stopPreview();

        super.onPause();
    }

    public void stopPreview() {
        if (inPreview) {
            camera.stopPreview();
        }

        camera.release();
        camera=null;
        inPreview=false;
    }

    private Camera.Size getBestPreviewSize(int width, int height,
                                           Camera.Parameters parameters) {
        Camera.Size result=null;

        for (Camera.Size size : parameters.getSupportedPreviewSizes()) {
            if (size.width<=width && size.height<=height) {
                if (result==null) {
                    result=size;
                }
                else {
                    int resultArea=result.width*result.height;
                    int newArea=size.width*size.height;

                    if (newArea>resultArea) {
                        result=size;
                    }
                }
            }
        }

        return(result);
    }

    private void initPreview(int width, int height) {
        if (camera!=null && previewHolder.getSurface()!=null) {
            try {
                camera.setPreviewDisplay(previewHolder);
            }
            catch (Throwable t) {
                Log.e(LOG_HEADER,
                        "Exception in setPreviewDisplay()", t);
                Toast
                        .makeText(CameraOverlayActivity.this, t.getMessage(), Toast.LENGTH_LONG)
                        .show();
            }

            if (!cameraConfigured) {
                Camera.Parameters parameters=camera.getParameters();
                Camera.Size size=getBestPreviewSize(width, height,
                        parameters);

                if (size!=null) {
                    parameters.setPreviewSize(size.width, size.height);
                    camera.setParameters(parameters);
                    cameraConfigured=true;
                }
            }
        }
    }

    private void startPreview() {
        if (cameraConfigured && camera!=null) {
            setCameraOrientation();
            camera.startPreview();
            inPreview=true;
        }
    }

    SurfaceHolder.Callback surfaceCallback = new SurfaceHolder.Callback() {
        public void surfaceCreated(SurfaceHolder holder) {
            // no-op -- wait until surfaceChanged()
        }

        public void surfaceChanged(SurfaceHolder holder,
                                   int format, int width,
                                   int height) {
            setCameraOrientation();
            initPreview(width, height);
            startPreview();
        }

        public void surfaceDestroyed(SurfaceHolder holder) {
            // no-op
        }
    };

    private void setCameraOrientation() {
        int rotation = this.getWindowManager().getDefaultDisplay().getRotation();
        int to_rotate = 0;

        switch(rotation) {
            case Surface.ROTATION_0: to_rotate = 90; break;
            case Surface.ROTATION_90: to_rotate = 0; break;
            case Surface.ROTATION_180: to_rotate = 270; break;
            case Surface.ROTATION_270: to_rotate = 180; break;
        }

        camera.setDisplayOrientation(to_rotate);
    }
}