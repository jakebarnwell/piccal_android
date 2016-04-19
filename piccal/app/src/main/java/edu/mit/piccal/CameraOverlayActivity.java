package edu.mit.piccal;

import android.app.Activity;
import android.content.Context;
import android.content.res.AssetManager;
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

import com.googlecode.tesseract.android.TessBaseAPI;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

// Taken from https://github.com/commonsguy/cw-advandroid/tree/master/Camera/Preview
//  and https://github.com/commonsguy/cw-advandroid/tree/master/Camera/Picture

public class CameraOverlayActivity extends Activity {
    private SurfaceView preview = null;
    private ImageView imview = null;
    private int imViewWidth, imViewHeight;
    private SurfaceHolder previewHolder = null;
    private Camera camera = null;
    private boolean inPreview = false;
    private boolean cameraConfigured = false;
    private final static String LOG_HEADER = "piccal - CameraOverlay";

    private enum State {CAMERA_PREVIEW, IMAGE_REVIEW};

    // For OCR:
    private TessBaseAPI baseApi;
    public static final String PACKAGE_NAME = "edu.mit.piccal";
    public static final String DATA_PATH = Environment
            .getExternalStorageDirectory().toString() + "/piccal/";

    // You should have the trained data file in assets folder
    // You can get them at:
    // http://code.google.com/p/tesseract-ocr/downloads/list
    public static final String LANG = "eng";


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.content_camera_overlay);

        preview = (SurfaceView)findViewById(R.id.preview);
        previewHolder = preview.getHolder();
        previewHolder.addCallback(surfaceCallback);
        previewHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

        imview = (ImageView)findViewById(R.id.imageView);

        // Create base API for OCR
        baseApi = initOCR(LANG, DATA_PATH);
    }

    private TessBaseAPI initOCR(String lang, String data_path) {
        String[] paths = new String[] { data_path, data_path + "tessdata/" };

        for (String path : paths) {
            File dir = new File(path);
            if (!dir.exists()) {
                if (!dir.mkdirs()) {
                    Log.v(LOG_HEADER, "ERROR: Creation of directory " + path + " on sdcard failed");
                    return null;
                } else {
                    Log.v(LOG_HEADER, "Created directory " + path + " on sdcard");
                }
            }

        }

        // lang.traineddata file with the app (in assets folder)
        // You can get them at:
        // http://code.google.com/p/tesseract-ocr/downloads/list
        // This area needs work and optimization
        if (!(new File(data_path + "tessdata/" + lang + ".traineddata")).exists()) {
            try {

                AssetManager assetManager = getAssets();
                InputStream in = assetManager.open("tessdata/" + lang + ".traineddata");
                //GZIPInputStream gin = new GZIPInputStream(in);
                OutputStream out = new FileOutputStream(data_path
                        + "tessdata/" + lang + ".traineddata");

                // Transfer bytes from in to out
                byte[] buf = new byte[1024];
                int len;
                //while ((lenf = gin.read(buff)) > 0) {
                while ((len = in.read(buf)) > 0) {
                    out.write(buf, 0, len);
                }
                in.close();
                //gin.close();
                out.close();

                Log.v(LOG_HEADER, "Copied " + lang + " traineddata");
            } catch (IOException e) {
                Log.e(LOG_HEADER, "Was unable to copy " + lang + " traineddata " + e.toString());
            }
        }

        TessBaseAPI api = new TessBaseAPI();
        api.setDebug(true);
        Log.d(LOG_HEADER, data_path);
        api.init(data_path, lang);

        return api;
    }

    public void onClick_takePicture(View view) {
        if(inPreview) {
            imViewWidth = preview.getWidth();
            imViewHeight = preview.getHeight();
            camera.takePicture(null, null, photoCallback);
        }

    }

    Camera.PictureCallback photoCallback = new Camera.PictureCallback() {
        public void onPictureTaken(byte[] data, Camera camera) {
            // Start to save the photo
            new SavePhotoTask().execute(data);

            // Get bitmap file to load into ImageView
            Bitmap bitmap_image = getBitmap(data);

            // Stop camera preview
            stopPreview();

            // Now hide the camera preview, show the ImageView to view the taken image
            if(preview.getVisibility() == View.VISIBLE) {
                hide(preview);
                show(imview);
            }

            if(bitmap_image != null) {
                imview.setImageBitmap(bitmap_image);

                // Do OCR:
                String extracted_text = extractTextFrom(bitmap_image, baseApi);
                Toast.makeText(getApplicationContext(), extracted_text, Toast.LENGTH_SHORT).show();
                //baseApi.end();
            }
        }
    };

    private String extractTextFrom(Bitmap bmp, TessBaseAPI api) {
        api.setImage(bmp);
        String text = api.getUTF8Text();
        Log.d(LOG_HEADER, "Recognized text: " + text);
        return text;
    }

    private Bitmap getBitmap(byte[] jpeg) {
        // Get the dimensions of the View
        int targetW = imViewWidth;
        int targetH = imViewHeight;

        // Get the dimensions of the bitmap
        BitmapFactory.Options bmOptions = new BitmapFactory.Options();
        bmOptions.inJustDecodeBounds = true;
        int photoW = bmOptions.outWidth;
        int photoH = bmOptions.outHeight;

        // Determine how much to scale down the image
        int scaleFactor = Math.min(photoW/targetW, photoH/targetH);

        // Decode the image file into a Bitmap sized to fill the View
        bmOptions.inJustDecodeBounds = false;
        bmOptions.inSampleSize = scaleFactor << 1;
        bmOptions.inPurgeable = true;

        Bitmap bitmap = BitmapFactory.decodeByteArray(jpeg, 0, jpeg.length, bmOptions);

        Matrix mtx = new Matrix();
        mtx.postRotate(90);

        // Rotating Bitmap
        Bitmap rotatedBMP = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), mtx, true);

        if (rotatedBMP != bitmap) {
            bitmap.recycle();
        }

        return rotatedBMP;
    }

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
            File photo = new File(Environment.getExternalStorageDirectory(), "photo.jpg");

            if (photo.exists()) {
                photo.delete();
            }

            try {
                FileOutputStream fos = new FileOutputStream(photo.getPath());

                fos.write(jpeg[0]);
                fos.close();
            }
            catch (java.io.IOException e) {
                Log.e(LOG_HEADER, "Exception in SavePhotoTask: ", e);
            }

            return(null);
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        camera = Camera.open();
        startPreview();
    }

    @Override
    public void onPause() {
        stopPreview();

        super.onPause();
    }

    private Camera.Size getBestPreviewSize(int width, int height,
                                           Camera.Parameters parameters) {
        Camera.Size result = null;

        for (Camera.Size size : parameters.getSupportedPreviewSizes()) {
            if (size.width <= width && size.height <= height) {
                if (result == null) {
                    result = size;
                }
                else {
                    int resultArea = result.width*result.height;
                    int newArea = size.width*size.height;

                    if (newArea > resultArea) {
                        result = size;
                    }
                }
            }
        }

        return(result);
    }

    private void initPreview(int width, int height) {
        if (camera != null && previewHolder.getSurface() != null) {
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
                Camera.Parameters parameters = camera.getParameters();
                Camera.Size size = getBestPreviewSize(width, height,
                        parameters);

                if (size != null) {
                    parameters.setPreviewSize(size.width, size.height);
                    camera.setParameters(parameters);
                    cameraConfigured = true;
                }
            }
        }
    }

    private void startPreview() {
        if (cameraConfigured && camera != null) {
            setCameraOrientation();
            camera.startPreview();
            inPreview = true;
        }
    }

    public void stopPreview() {
        if(inPreview) {
            camera.stopPreview();
            camera.release();
        }

        camera = null;
        inPreview = false;
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