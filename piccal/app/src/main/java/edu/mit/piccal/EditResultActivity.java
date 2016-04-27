package edu.mit.piccal;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.res.AssetManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.provider.CalendarContract;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.TimePicker;
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
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;




public class EditResultActivity extends AppCompatActivity {
    public static final String PACKAGE_NAME = "edu.mit.piccal";
    private static final String TAG = "piccal_log";

    static final int REQUEST_TAKE_PHOTO = 1;
    static final int ADD_CALENDAR_EVENT = 2;

    // Photo path from MainActivity
    String mCurrentPhotoPath;

    // Tesseract Base Api
    public static final String DATA_PATH = Environment
            .getExternalStorageDirectory().toString() + "/piccal/";
    public static final String LANG = "eng";
    private TessBaseAPI baseApi = new TessBaseAPI();

    private ImageView mPopupImageView;
    private boolean mPicLoaded;

    private boolean popupShowing = false;

    // Used to help us figure out when a user adds a cal event or not
    private long calendarEventId;

    private ProgressDialog progDialog;



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
        mPopupImageView = (ImageView) findViewById(R.id.iv_popup);
        mPicLoaded = false;

        // Set onTouch listener for view-popup-image button
        TextView viewImageText = (TextView)findViewById(R.id.text_viewImage);
        ((FrameLayout)findViewById(R.id.text_frame)).setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                int action = event.getAction();
                switch (action) {
                    case MotionEvent.ACTION_DOWN:
                        if (!popupShowing) {
                            mPopupImageView.bringToFront();
                            mPopupImageView.requestLayout();
                            mPopupImageView.invalidate();
                            mPopupImageView.setVisibility(View.VISIBLE);
                            ((Button)findViewById(R.id.btn_add2Calendar)).setVisibility(View.INVISIBLE);
                            ((Button)findViewById(R.id.btn_retry)).setVisibility(View.INVISIBLE);
                            popupShowing = true;
                        }
                        break;
                    case MotionEvent.ACTION_UP:
                        if (popupShowing) {
                            mPopupImageView.setVisibility(View.INVISIBLE);
                            ((Button)findViewById(R.id.btn_add2Calendar)).setVisibility(View.VISIBLE);
                            ((Button)findViewById(R.id.btn_retry)).setVisibility(View.VISIBLE);
                            popupShowing = false;
                        }
                        break;
                    default:
                        break;
                }
                return true;
            }
        });

        // Set Time Picker to be 24 hour mode (takes less space)
        ((TimePicker)findViewById(R.id.timePicker)).setIs24HourView(true);
        // Set Time Picker to point to the correct time
        setTimePicker((TimePicker)findViewById(R.id.timePicker), Calendar.getInstance());

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

        if (!mPicLoaded) {
            Bitmap rotatedBitmap = getRotatedBitmap();

            Bitmap scaledBitmap = getImageViewBitmap(rotatedBitmap, mPopupImageView);
            mPopupImageView.setImageBitmap(scaledBitmap);

            // Recycle the rotatedBitmap if it's not being used to draw
            if(rotatedBitmap != scaledBitmap) {
                rotatedBitmap.recycle();
            }

            new ExtractTextTask().execute(mCurrentPhotoPath);
            mPicLoaded = true;
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
        //bitmap.recycle();
        return rotatedBMP;
    }

    private Bitmap getImageViewBitmap(Bitmap rotatedBitmap, ImageView imview){
        int targetW = imview.getWidth();
        int targetH = imview.getHeight();
        String s = "w, h = " + targetW + ", " + targetH;
        Log.d(TAG, s);

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
            //bitmap.recycle();
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

    public void addToCalendar(View view) {
        String title = ((EditText) findViewById(R.id.editTextTitle)).getText().toString();
        String descr = ((EditText) findViewById(R.id.editTextDescription)).getText().toString();
        String loc = ((EditText) findViewById(R.id.editTextLocation)).getText().toString();

        DatePicker dp = (DatePicker)findViewById(R.id.datePicker);
        TimePicker tp = (TimePicker)findViewById(R.id.timePicker);
        int year = dp.getYear(), month = dp.getMonth(), day = dp.getDayOfMonth();
        int hour = tp.getCurrentHour(), minute = tp.getCurrentMinute();
        Log.d(TAG, "From date picker, extracted (year,month,day,hour,min) = ("
                + year + "," + month + "," + day + "," + hour + "," + minute + ")");

        Date date = new Date(year, month, day, hour, minute);
        Calendar temp = Calendar.getInstance();
        temp.set(year, month, day, hour, minute);
        Log.d(TAG, "Adding Date " + temp.toString() + " (epoch time = " + Long.toString(temp.getTimeInMillis()) + ") to calendar");

        long startTime = temp.getTimeInMillis();
        long endTime = startTime + (1000L * 3600L);

        Intent dispatchedEvent = addEvent(title, startTime, endTime, descr, loc);
    }

    public Intent addEvent(String title, long start_time, long end_time, String descr, String loc) {
        calendarEventId = getNewEventId(getContentResolver());
        Intent intent = new Intent(Intent.ACTION_INSERT)
                .setData(CalendarContract.Events.CONTENT_URI)
                .putExtra(CalendarContract.Events.TITLE, title)
                .putExtra(CalendarContract.Events.EVENT_LOCATION, loc)
                .putExtra(CalendarContract.Events.DESCRIPTION, descr)
                .putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, start_time)
                .putExtra(CalendarContract.EXTRA_EVENT_END_TIME, end_time);

        if (intent.resolveActivity(this.getPackageManager()) != null) {
            startActivityForResult(intent, ADD_CALENDAR_EVENT);
        }

        return intent;
    }

    public void retakePicture(View view) {
        dispatchTakePictureIntent();
        return;
    }


    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        // Ensure that there's a camera activity to handle the intent
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            // Create the File where the photo should go
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                // Error occurred while creating the File

            }
            // Continue only if the File was successfully created
            if (photoFile != null) {
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT,
                        Uri.fromFile(photoFile));
                startActivityForResult(takePictureIntent, REQUEST_TAKE_PHOTO);
            }
        }
    }


    /**
     * http://developer.android.com/training/camera/photobasics.html
     */
    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        String storageDir = Environment.getExternalStorageDirectory() + "/picupload";
        File dir = new File(storageDir);
        if (!dir.exists())
            dir.mkdir();

        File image = new File(storageDir + "/" + imageFileName + ".jpg");

        // Save a file: path for use with ACTION_VIEW intents
        mCurrentPhotoPath = image.getAbsolutePath();
        Log.i(TAG, "photo path = " + mCurrentPhotoPath);
        return image;
    }



    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.i(TAG, "onActivityResult: " + this);
        if (requestCode == REQUEST_TAKE_PHOTO && resultCode == Activity.RESULT_OK) {
            mPicLoaded = false;
            onWindowFocusChanged(true);
        } else if (requestCode == ADD_CALENDAR_EVENT) {
            long previous_eventId = getLastEventId(getContentResolver());
            Log.d(TAG, "(newEventId,previousEventId) = (" + calendarEventId + "," + previous_eventId +")");

            // If the IDs are equal, the event was successfully added. Else, the user cancelled.
            if(previous_eventId == calendarEventId) {
                // Success screen
                Intent successIntent = new Intent(this, SuccessActivity.class);
                startActivity(successIntent);
            } else {
                // go back to the previous edit results screen
                ;
            }


        }
    }

    // From http://stackoverflow.com/questions/9761584/how-can-i-find-out-the-result-of-my-calendar-intent#9925153
    public static long getNewEventId(ContentResolver cr) {
        Cursor cursor = cr.query(CalendarContract.Events.CONTENT_URI, new String [] {"MAX(_id) as max_id"}, null, null, "_id");
        cursor.moveToFirst();
        long max_val = cursor.getLong(cursor.getColumnIndex("max_id"));
        return max_val+1;
    }

    public static long getLastEventId(ContentResolver cr) {
        Cursor cursor = cr.query(CalendarContract.Events.CONTENT_URI, new String [] {"MAX(_id) as max_id"}, null, null, "_id");
        cursor.moveToFirst();
        long max_val = cursor.getLong(cursor.getColumnIndex("max_id"));
        return max_val;
    }

    private class ExtractTextTask extends AsyncTask<String, Integer, String> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            progDialog = new ProgressDialog(EditResultActivity.this);
            progDialog.setMessage("Please Wait. Analyzing image...");
            progDialog.setIndeterminate(false);
            progDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            progDialog.setCancelable(true);
            progDialog.show();
	}

	protected String doInBackground(String... paths) {
            String imagePath = paths[0];
            Bitmap processedBitmap = preprocessOpenCV(imagePath);
            String extractedText = getBitmapText(processedBitmap);
            return extractedText;
        }

        protected void onPostExecute(String result) {
            //Context context = getApplicationContext();
            //Toast.makeText(context, result, Toast.LENGTH_LONG).show();
            Log.d(TAG, result.replaceAll("\n", " "));
            super.onPostExecute(result);
            populateTextEdits(result);
            progDialog.dismiss();
        }
    }


    private void populateTextEdits(String ocrText) {
        EventExtractor ee = new EventExtractor();
        Event event = ee.extractInfoFromPoster(ocrText);
        Log.d(TAG, "Extracted (start,end) = (" + event.start.toString() + "," + event.end.toString() + ")" +
                " from EventExtractor");
        Calendar cal = Calendar.getInstance();
        cal.setTime(event.start);
        setTimePicker((TimePicker) findViewById(R.id.timePicker), cal);
        setDatePicker((DatePicker) findViewById(R.id.datePicker), cal);

        EditText titleEditText = (EditText) findViewById(R.id.editTextTitle);
        titleEditText.setText(ocrText);

    }

    private void setTimePicker(TimePicker tp, Calendar c) {
        int hour = c.get(Calendar.HOUR_OF_DAY);
        int mins = c.get(Calendar.MINUTE);
        tp.setCurrentHour(hour);
        tp.setCurrentMinute(mins);
        Log.d(TAG, "Setting TimePicker (hour,minute) to (" + hour + "," + mins + ")");
    }

    private void setDatePicker(DatePicker dp, Calendar c) {
        int year = c.get(Calendar.YEAR);
        int month = c.get(Calendar.MONTH);
        int day = c.get(Calendar.DATE);
        dp.updateDate(year, month, day);
        Log.d(TAG, "Setting DatePicker (year,month,day) to (" + year + "," + month + "," + day + ")");
    }



}
