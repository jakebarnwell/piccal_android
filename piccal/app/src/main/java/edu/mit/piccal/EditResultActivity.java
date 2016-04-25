package edu.mit.piccal;

import android.content.Context;
import android.content.Intent;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.os.AsyncTask;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.googlecode.tesseract.android.TessBaseAPI;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.core.Core;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class EditResultActivity extends AppCompatActivity {
    public static final String PACKAGE_NAME = "edu.mit.piccal";
    private static final String TAG = "piccal_log";

    // Photo path from MainActivity
    String mCurrentPhotoPath;

    // Tesseract Base Api
    public static final String DATA_PATH = Environment
            .getExternalStorageDirectory().toString() + "/piccal/";
    public static final String LANG = "eng";
    private TessBaseAPI baseApi = new TessBaseAPI();

    private ImageView mImageView;
    private boolean picLoaded;

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                {
                    Log.i(TAG, "OpenCV loaded successfully");
//                    mTakePhoto.setOnClickListener(MainActivity.this);
                } break;
                default:
                {
                    super.onManagerConnected(status);
                }
                break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            mCurrentPhotoPath = extras.getString("PHOTO_PATH");
        }

        initializeTessBaseApi();

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_result);
        mImageView = (ImageView) findViewById(R.id.imageview);
        picLoaded = false;
    }

    @Override
    protected void onResume() {
        super.onResume();

        //OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_8, this, mLoaderCallback);
        if (!OpenCVLoader.initDebug()) {
            Log.d(TAG, "Internal OpenCV library not found. Using OpenCV Manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_8, this, mLoaderCallback);
        } else {
            Log.d(TAG, "OpenCV library found inside package. Using it!");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);

        if (!picLoaded) {
            Bitmap rotatedBitmap = getRotatedBitmap();
            Bitmap scaledBitmap = getImageViewBitmap(rotatedBitmap);
            mImageView.setImageBitmap(scaledBitmap);

            new ExtractTextTask().execute(mCurrentPhotoPath);
            picLoaded = true;
        }
    }

    private Bitmap getRotatedBitmap(){
        BitmapFactory.Options bmOptions = new BitmapFactory.Options();
        bmOptions.inJustDecodeBounds = false;
        bmOptions.inPurgeable = true;
        Bitmap bitmap = BitmapFactory.decodeFile(mCurrentPhotoPath, bmOptions);

        ExifInterface exif = null;
        try {
            exif = new ExifInterface(mCurrentPhotoPath);
        } catch (IOException e) {
            e.printStackTrace();
        }
        int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_UNDEFINED);

        Bitmap rotatedBMP = rotateBitmap(bitmap, orientation);
        bitmap.recycle();
        return rotatedBMP;
    }

    private Bitmap getImageViewBitmap(Bitmap rotatedBitmap){
        int targetW = mImageView.getWidth();
        int targetH = mImageView.getHeight();

        Bitmap resizedBitmap = getResizedBitmap(rotatedBitmap, targetW, targetH);
        return resizedBitmap;
    }

    public Bitmap getResizedBitmap(Bitmap bm, int newWidth, int newHeight) {
        int width = bm.getWidth();
        int height = bm.getHeight();
        float scaleWidth = ((float) newWidth) / width;
        float scaleHeight = ((float) newHeight) / height;
        // CREATE A MATRIX FOR THE MANIPULATION
        Matrix matrix = new Matrix();
        // RESIZE THE BIT MAP
        matrix.postScale(scaleWidth, scaleHeight);

        // "RECREATE" THE NEW BITMAP
        Bitmap resizedBitmap = Bitmap.createBitmap(
                bm, 0, 0, width, height, matrix, false);
        //bm.recycle();
        return resizedBitmap;
    }

    private void setPic(Bitmap scaledBitmap) {
        // Get the dimensions of the View
        int targetW = mImageView.getWidth();
        int targetH = mImageView.getHeight();
        Log.d(TAG, Integer.toString(targetH));
        Log.d(TAG, Integer.toString(targetW));

        // Get the dimensions of the bitmap
        BitmapFactory.Options bmOptions = new BitmapFactory.Options();
        bmOptions.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(mCurrentPhotoPath, bmOptions);
        int photoW = bmOptions.outWidth;
        int photoH = bmOptions.outHeight;

        // Determine how much to scale down the image
        int scaleFactor = Math.min(photoW / targetW, photoH / targetH);

        // Decode the image file into a Bitmap sized to fill the View
        bmOptions.inJustDecodeBounds = false;
        bmOptions.inSampleSize = scaleFactor << 1;
        bmOptions.inPurgeable = true;

        //Bitmap bitmap = preprocessOpenCV(mCurrentPhotoPath);
        Bitmap bitmap = BitmapFactory.decodeFile(mCurrentPhotoPath, bmOptions);

        ExifInterface exif = null;
        try {
            exif = new ExifInterface(mCurrentPhotoPath);
        } catch (IOException e) {
            e.printStackTrace();
        }
        int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_UNDEFINED);

        Bitmap rotatedBMP = rotateBitmap(bitmap, orientation);


//        String recognizedText = getBitmapText(rotatedBMP);

        mImageView.setImageBitmap(rotatedBMP);
        picLoaded = true;
    }

    public static Bitmap rotateBitmap(Bitmap bitmap, int orientation) {

        Matrix matrix = new Matrix();
        switch (orientation) {
            case ExifInterface.ORIENTATION_NORMAL:
                return bitmap;
            case ExifInterface.ORIENTATION_FLIP_HORIZONTAL:
                matrix.setScale(-1, 1);
                break;
            case ExifInterface.ORIENTATION_ROTATE_180:
                matrix.setRotate(180);
                break;
            case ExifInterface.ORIENTATION_FLIP_VERTICAL:
                matrix.setRotate(180);
                matrix.postScale(-1, 1);
                break;
            case ExifInterface.ORIENTATION_TRANSPOSE:
                matrix.setRotate(90);
                matrix.postScale(-1, 1);
                break;
            case ExifInterface.ORIENTATION_ROTATE_90:
                matrix.setRotate(90);
                break;
            case ExifInterface.ORIENTATION_TRANSVERSE:
                matrix.setRotate(-90);
                matrix.postScale(-1, 1);
                break;
            case ExifInterface.ORIENTATION_ROTATE_270:
                matrix.setRotate(-90);
                break;
            default:
                return bitmap;
        }
        try {
            Bitmap bmRotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
            bitmap.recycle();
            return bmRotated;
        }
        catch (OutOfMemoryError e) {
            e.printStackTrace();
            return null;
        }
    }

    private Bitmap preprocessOpenCV(String photoPath) {
        Mat original_image = Imgcodecs.imread(photoPath);
        Imgproc.pyrDown(original_image, original_image);
        Mat image = new Mat();
        Imgproc.cvtColor(original_image, image, Imgproc.COLOR_BGRA2GRAY, 1);
        Size window = new Size(3, 3);
        Imgproc.GaussianBlur(image, image, window, 0);
        Imgproc.adaptiveThreshold(image, image, 255, Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C, Imgproc.THRESH_BINARY, 75, 10);
        Core.bitwise_not(image, image);
        Log.i(TAG, "Prepressesing Camera Image (END)" + image.size().toString());
        Bitmap bitmap = Bitmap.createBitmap(image.cols(), image.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(image, bitmap);
        return bitmap;
    }

    public String getBitmapText(Bitmap rotatedBMP){

        baseApi.setImage(rotatedBMP);
        String recognizedText = baseApi.getUTF8Text();
        baseApi.clear();
        String cleanText = recognizedText.replaceAll("[^\\w\\s|_]", " ");
//        cleanText = cleanText.replaceAll("[a-zA-Z0-9]");
        cleanText = cleanText.replaceAll("( )+", " ");
        //baseApi.end();

        return cleanText;
    }

    public void click_addTestToCal(View view) {
        PiccalCalendar cal = new PiccalCalendar(this);
        String title = "Test Event", time_date = "Apr 22 10:30am", loc = "Killian Court";
        String descr = "This is a test event for the Piccal android app.";
        Intent dispatchedIntent = cal.addEvent(title, time_date, descr, loc);


        // Test quickAdd
        // from: https://developers.google.com/google-apps/calendar/v3/reference/events/quickAdd#examples
        // also: https://github.com/google/google-api-java-client-samples/blob/0b5c78984aedb0d837d088d84a9fc9da63938889/calendar-appengine-sample/src/main/java/com/google/api/services/samples/calendar/appengine/server/Utils.java

//        final HttpTransport httpTransport = new UrlFetchTransport();
//        final JsonFactory jsonFactory = JacksonFactory.getDefaultInstance();
//        String userId = UserServiceFactory.getUserService().getCurrentUser().getUserId();
//        Credential credential = newFlow().loadCredential(userId);
//        Calendar service = new Calendar.Builder(httpTransport, jsonFactory, credential)
//                .setApplicationName("applicationName").build();
//
//        // Quick-add an event
//        String eventText = "Appointment at Somewhere on June 3rd 10am-10:25am";
//        Event createdEvent =
//                service.events().quickAdd(calendarId, "primary").setText(eventText).execute();
//
//        System.out.println(createdEvent.getId());
    }

    /**
     * Initializes tesseract library with the english training data.
     */
    private void initializeTessBaseApi(){
        String[] paths = new String[] { DATA_PATH, DATA_PATH + "tessdata/" };

        for (String path : paths) {
            File dir = new File(path);
            if (!dir.exists()) {
                if (!dir.mkdirs()) {
                    Log.v(TAG, "ERROR: Creation of directory " + path + " on sdcard failed");
                    return;
                } else {
                    Log.v(TAG, "Created directory " + path + " on sdcard");
                }
            }

        }

        if (!(new File(DATA_PATH + "tessdata/" + LANG + ".traineddata")).exists()) {
            try {

                AssetManager assetManager = getAssets();
                InputStream in = assetManager.open("tessdata/" + LANG + ".traineddata");
                //GZIPInputStream gin = new GZIPInputStream(in);
                OutputStream out = new FileOutputStream(DATA_PATH
                        + "tessdata/" + LANG + ".traineddata");

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

                Log.v(TAG, "Copied " + LANG + " traineddata");
            } catch (IOException e) {
                Log.e(TAG, "Was unable to copy " + LANG + " traineddata " + e.toString());
            }
        }

        baseApi.setDebug(true);
        baseApi.init(DATA_PATH, LANG); // Loads language training data
    }

    private class ExtractTextTask extends AsyncTask<String, Integer, String> {
        protected String doInBackground(String... paths) {
            String imagePath = paths[0];
            Bitmap processedBitmap = preprocessOpenCV(imagePath);
            String extractedText = getBitmapText(processedBitmap);
            return extractedText;
        }

        protected void onPostExecute(String result) {
            Context context = getApplicationContext();
            Toast.makeText(context, result, Toast.LENGTH_LONG).show();
            Log.d(TAG, result.replaceAll("\n", " "));
            super.onPostExecute(result);
        }
    }
}
