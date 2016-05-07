package edu.mit.piccal;

import android.app.Activity;
import android.app.ProgressDialog;
import android.app.TimePickerDialog;
import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
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
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.TimePicker;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class EditResultActivity extends AppCompatActivity {
    private static final String TAG = "piccal_log";

    private static final int REQUEST_TAKE_PHOTO = 1;
    private static final int ADD_CALENDAR_EVENT = 2;

    // Photo path from MainActivity
    String mCurrentPhotoPath;

    private ImageView mPopupImageView;
    private boolean mPicLoaded;

    private boolean popupShowing = false;

    // Stores the event ID (of the event added to the calendar) so we can access it later
    private long calendarEventId;

    // Stores current from and to times of the event
    private static final int FROM = 0, TO = 1;
    private Calendar[] mEventTime = {null, null};

    // Set-time listeners for FROM and TO time pickers:
    private final TimePickerDialog.OnTimeSetListener[] TIME_SET_LISTENER
            = {makeTimeSetListener(FROM), makeTimeSetListener(TO)};

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
        findViewById(R.id.iv_poster_thumbnail).setOnTouchListener(new View.OnTouchListener() {
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
                            findViewById(R.id.btn_add2Calendar).setVisibility(View.INVISIBLE);
                            findViewById(R.id.btn_retry).setVisibility(View.INVISIBLE);
                            popupShowing = true;
                        }
                        break;
                    case MotionEvent.ACTION_UP:
                        if (popupShowing) {
                            mPopupImageView.setVisibility(View.INVISIBLE);
                            findViewById(R.id.btn_add2Calendar).setVisibility(View.VISIBLE);
                            findViewById(R.id.btn_retry).setVisibility(View.VISIBLE);
                            popupShowing = false;
                        }
                        break;
                    default:
                        break;
                }
                return true;
            }
        });



        // Set initial from and to times
        mEventTime[FROM] = Calendar.getInstance();
        mEventTime[TO] = Calendar.getInstance();
        mEventTime[TO].add(Calendar.HOUR_OF_DAY, 1);

        // Populate Time fields with the initially-created times
        populateTimeText(FROM, mEventTime[FROM]);
        populateTimeText(TO, mEventTime[TO]);

        // Set GridLayout listeners for time pickers:
        GridLayout grid = (GridLayout) findViewById(R.id.gridLayout);
        int index_setFromTime = 7;
        int index_setToTime = 9;

        grid.getChildAt(index_setFromTime).setOnClickListener(new View.OnClickListener(){
            public void onClick(View view) {
                Log.d(TAG, "Editing TO time.");
                TimePickerDialog tpd = makeTimePickerDialog(mEventTime[FROM], TIME_SET_LISTENER[FROM]);
                tpd.show();
            }
        });
        grid.getChildAt(index_setToTime).setOnClickListener(new View.OnClickListener(){
            public void onClick(View view) {
                Log.d(TAG, "Editing TO time.");
                TimePickerDialog tpd = makeTimePickerDialog(mEventTime[TO], TIME_SET_LISTENER[TO]);
                tpd.show();
            }
        });

        // Initialize DatePicker with initial date as well as its onSet listener:
        Calendar date = mEventTime[FROM];
        final int year = date.get(Calendar.YEAR), month = date.get(Calendar.MONTH),
                day = date.get(Calendar.DATE);
        DatePicker.OnDateChangedListener dateListener = new DatePicker.OnDateChangedListener() {
            @Override
            public void onDateChanged(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                mEventTime[FROM].set(year, monthOfYear, dayOfMonth);
                mEventTime[TO].set(year, monthOfYear, dayOfMonth);
            }
        };
        ((DatePicker)findViewById(R.id.datePicker)).init(year, month, day, dateListener);

        // Send the image to the server for processing
        sendImageToServerAndWaitForResult(mCurrentPhotoPath);
    }

    private String populateTimeText(final int which, Calendar time) {
        Log.d(TAG, "Setting time " + which + " to: " + time.toString());
        int editTextId;
        if(which == FROM) {
            editTextId = R.id.editTextFrom;
        } else if(which == TO) {
            editTextId = R.id.editTextTo;
        } else {
            throw new IllegalArgumentException("Illegal value for 'which'. Should be one of FROM or TO.");
        }

        SimpleDateFormat formatter = new SimpleDateFormat("hh:mm a", Locale.US);
        String text = formatter.format(time.getTime());
        ((EditText)findViewById(editTextId)).setText(text);
        return text;
    }

    public void sendImageToServerAndWaitForResult(String path) {
        ProgressDialog progDialog = new ProgressDialog(EditResultActivity.this);
        progDialog.setMessage("Please Wait. Analyzing image...");
        progDialog.setIndeterminate(false);
        progDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        progDialog.setCancelable(true);
        ServerCommunicator server = new ServerCommunicator(this, progDialog);
        server.send(path);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);

        // Set the popup image view and thumbnail image view to store the photo we just took
        if(!mPicLoaded) {
            Bitmap bitmap = getBitmap(mCurrentPhotoPath);
            Bitmap rotatedBitmap = getRotatedBitmap(bitmap, mCurrentPhotoPath);
            if(bitmap != rotatedBitmap) bitmap.recycle();
            Bitmap imviewBitmap = getImageViewBitmap(rotatedBitmap, mPopupImageView);
            if(rotatedBitmap != imviewBitmap) rotatedBitmap.recycle();
            mPopupImageView.setImageBitmap(imviewBitmap);

            ImageView posterThumbnail = (ImageView)findViewById(R.id.iv_poster_thumbnail);
            Bitmap bitmap2 = getBitmap(mCurrentPhotoPath);
            Bitmap rotatedBitmap2 = getRotatedBitmap(bitmap2, mCurrentPhotoPath);
            if(bitmap2 != rotatedBitmap2) bitmap2.recycle();
            int rbw = rotatedBitmap2.getWidth(), rbh = rotatedBitmap2.getHeight();
            int ptw = posterThumbnail.getWidth(), pth = posterThumbnail.getHeight();
            double ratio;
            if(pth < ptw) {
                ratio = 1.0 * rbh / pth;
            } else {
                ratio = 1.0 * rbw / ptw;
            }
            int targetW = (int)(rbw / ratio), targetH = (int)(rbh / ratio);
            String s = "thumbnail target w, h = " + targetW + ", " + targetH; Log.d(TAG, s);
            Bitmap resizedBitmap = getResizedBitmap(rotatedBitmap2, targetW, targetH);
            if(resizedBitmap != rotatedBitmap2) rotatedBitmap2.recycle();
            posterThumbnail.setImageBitmap(resizedBitmap);

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

        long startTime = mEventTime[FROM].getTimeInMillis();
        long endTime = mEventTime[TO].getTimeInMillis();

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
                successIntent.putExtra("eventId", calendarEventId);
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
        Event event = new Event(ocrText);

        mEventTime[FROM] = event.getSmartStart();
        mEventTime[TO] = event.getSmartEnd();

        populateTimeText(FROM, mEventTime[FROM]);
        populateTimeText(TO, mEventTime[TO]);

        setDatePicker((DatePicker)findViewById(R.id.datePicker), mEventTime[FROM]);

        ((EditText)findViewById(R.id.editTextTitle)).setText(event.title);
        ((EditText)findViewById(R.id.editTextLocation)).setText(event.location);
        ((EditText)findViewById(R.id.editTextDescription)).setText(event.description);
    }

    private void setDatePicker(DatePicker dp, Calendar c) {
        int year = c.get(Calendar.YEAR);
        int month = c.get(Calendar.MONTH);
        int day = c.get(Calendar.DATE);
        dp.updateDate(year, month, day);
    }

    private TimePickerDialog.OnTimeSetListener makeTimeSetListener(final int which) {
        TimePickerDialog.OnTimeSetListener listener = new TimePickerDialog.OnTimeSetListener() {
            @Override
            public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
                Calendar time = mEventTime[which];
                time.set(Calendar.HOUR_OF_DAY, hourOfDay);
                time.set(Calendar.MINUTE, minute);
                populateTimeText(which, time);
            }
        };

        return listener;
    }

    private TimePickerDialog makeTimePickerDialog(Calendar time, TimePickerDialog.OnTimeSetListener listener) {
        int hour = time.get(Calendar.HOUR_OF_DAY);
        int min = time.get(Calendar.MINUTE);
        TimePickerDialog tpd = new TimePickerDialog(this, listener, hour, min, false);

        return tpd;
    }



}
