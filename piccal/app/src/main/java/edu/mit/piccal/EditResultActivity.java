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

    private static final int REQUEST_TAKE_PHOTO = 1;
    private static final int ADD_CALENDAR_EVENT = 2;

    // Photo path from MainActivity
    String mCurrentPhotoPath;

    // Tesseract Base Api
    public static final String DATA_PATH = Environment
            .getExternalStorageDirectory().toString() + "/piccal/";
    public static final String LANG = "eng";
    private TessBaseAPI baseApi = new TessBaseAPI();
    private OCRService ocrService;

    private ImageView mPopupImageView;
    private boolean mPicLoaded;

    private boolean popupShowing = false;

    // Used to help us figure out when a user adds a cal event or not
    private long calendarEventId;

    private ProgressDialog progDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            mCurrentPhotoPath = extras.getString("PHOTO_PATH");
        }

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

        sendImageToServerAndWaitForResult(mCurrentPhotoPath);
    }

    public void sendImageToServerAndWaitForResult(String path) {
        ProgressDialog progDialog = new ProgressDialog(EditResultActivity.this);
        progDialog.setMessage("Please Wait. Analyzing image...");
        progDialog.setIndeterminate(false);
        progDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        progDialog.setCancelable(true);
        ServerCommunicator server = new ServerCommunicator(EditResultActivity.this, this, progDialog);
        server.send(path);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);

        if (!mPicLoaded) {
            Bitmap bitmap = getBitmap(mCurrentPhotoPath);
            Bitmap rotatedBitmap = getRotatedBitmap(bitmap, mCurrentPhotoPath);
            if(bitmap != rotatedBitmap) bitmap.recycle();
            Bitmap imviewBitmap = getImageViewBitmap(rotatedBitmap, mPopupImageView);
            if(rotatedBitmap != imviewBitmap) rotatedBitmap.recycle();
            mPopupImageView.setImageBitmap(imviewBitmap);

            mPicLoaded = true;
        }
    }

    private static Bitmap getBitmap(String path) {
        BitmapFactory.Options bmOptions = new BitmapFactory.Options();
        bmOptions.inJustDecodeBounds = false;
        bmOptions.inPurgeable = true;
        bmOptions.inSampleSize = 4;
        Bitmap bitmap = BitmapFactory.decodeFile(path, bmOptions);

        return bitmap;
    }

    private Bitmap getRotatedBitmap(Bitmap bitmap, String path){
        ExifInterface exif = null;
        try {
            exif = new ExifInterface(path);
        } catch (IOException e) {
            e.printStackTrace();
        }
        int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_UNDEFINED);

        Bitmap rotatedBitmap = do_rotateBitmap(bitmap, orientation);

        // Recycle this bitch
        if(rotatedBitmap != bitmap) {
            bitmap.recycle();
        }

        return rotatedBitmap;
    }

    private Bitmap getImageViewBitmap(Bitmap rotatedBitmap, ImageView imview){
        int targetW = imview.getWidth();
        int targetH = imview.getHeight();
        String s = "w, h = " + targetW + ", " + targetH;
        Log.d(TAG, s);

        Bitmap resizedBitmap = getResizedBitmap(rotatedBitmap, targetW, targetH);
        if(resizedBitmap != rotatedBitmap) rotatedBitmap.recycle();
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

        if(resizedBitmap != bm) bm.recycle();

        return resizedBitmap;
    }

    public static Bitmap do_rotateBitmap(Bitmap bitmap, int orientation) {
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
            if(bmRotated != bitmap) bitmap.recycle();
            return bmRotated;
        }
        catch (OutOfMemoryError e) {
            e.printStackTrace();
            return null;
        }
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
        Log.i(TAG, "File created. Photo path = " + mCurrentPhotoPath);
        return image;
    }



    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.i(TAG, "onActivityResult: " + this);
        if (requestCode == REQUEST_TAKE_PHOTO && resultCode == Activity.RESULT_OK) {
            mPicLoaded = false;
            onWindowFocusChanged(true);
            sendImageToServerAndWaitForResult(mCurrentPhotoPath);
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

    public void onOcrResult(String ocrText) {
        Log.d(TAG, "Received OCR result in EditResultActivity.");
        populateTextEdits(ocrText);
    }


    public void populateTextEdits(String ocrText) {
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
