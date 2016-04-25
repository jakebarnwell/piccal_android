package edu.mit.piccal;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.AssetManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.provider.CalendarContract;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;



//import com.google.api.client.auth.oauth2.Credential;
//import com.google.api.client.http.HttpTransport;
//import com.google.api.client.json.JsonFactory;
//import com.google.api.client.json.jackson2.JacksonFactory;
//import com.google.appengine.repackaged.com.google.api.client.extensions.appengine.http.UrlFetchTransport;
//import com.google.api.services.calendar.Calendar;
//import com.google.api.services.calendar.model.Event;
//import com.google.appengine.api.users.UserServiceFactory;

public class MainActivity extends AppCompatActivity implements OnClickListener {
    public static final String PACKAGE_NAME = "edu.mit.piccal";
    private static final String TAG = "piccal_log";

    private Button mTakePhoto;

    String mCurrentPhotoPath;

    static final int REQUEST_TAKE_PHOTO = 1;
    File photoFile = null;


    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mTakePhoto = (Button) findViewById(R.id.take_photo);
        mTakePhoto.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        switch (id) {
            case R.id.take_photo:
                dispatchTakePictureIntent();
                break;
        }
    }

    public void click_addTestToCal(View view) {
        PiccalCalendar cal = new PiccalCalendar(this);
        String title = "Test Event", time_date = "Apr 22 10:30am", loc = "Killian Court";
        String descr = "This is a test event for the Piccal android app.";
        long startTime = System.currentTimeMillis();
        long endTime = System.currentTimeMillis() + (1000 * 3600); // adds 1 hour
//        Intent dispatchedIntent = addEvent(title, startTime, endTime, descr, loc);
        Intent dispatchedIntent = cal.addEvent("unparsed 3/12/14 january");
    }

    public Intent addEvent(String title, long start_time, long end_time, String descr, String loc) {
        Intent intent = new Intent(Intent.ACTION_INSERT)
                .setData(CalendarContract.Events.CONTENT_URI)
                .putExtra(CalendarContract.Events.TITLE, title)
                .putExtra(CalendarContract.Events.EVENT_LOCATION, loc)
                .putExtra(CalendarContract.Events.DESCRIPTION, descr)
                .putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, start_time)
                .putExtra(CalendarContract.EXTRA_EVENT_END_TIME, end_time);

        if (intent.resolveActivity(this.getPackageManager()) != null) {
            this.startActivity(intent);
        }

        return intent;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.i(TAG, "onActivityResult: " + this);
        if (requestCode == REQUEST_TAKE_PHOTO && resultCode == Activity.RESULT_OK) {
            Intent intent = new Intent(this, EditResultActivity.class);
            intent.putExtra("PHOTO_PATH", mCurrentPhotoPath);
            startActivity(intent);
            //setPic();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        Log.i(TAG, "onResume: " + this);
    }

    @Override
    protected void onPause() {
        // TODO Auto-generated method stub
        super.onPause();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        // TODO Auto-generated method stub
        super.onConfigurationChanged(newConfig);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        // TODO Auto-generated method stub
        super.onSaveInstanceState(outState);
        Log.i(TAG, "onSaveInstanceState");
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
}