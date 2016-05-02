package edu.mit.piccal;

import android.content.ContentUris;
import android.content.Intent;
import android.net.Uri;
import android.provider.CalendarContract;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

public class SuccessActivity extends AppCompatActivity {
    private static final String TAG = "SuccessActivity";

    private long eventId = -1L;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_success);

        if(savedInstanceState == null) {
            Bundle extras = getIntent().getExtras();
            if(extras != null) {
                eventId = extras.getLong("eventId");
            }
        } else {
            eventId = (long) savedInstanceState.getSerializable("eventId");
        }

        Log.d(TAG, "Event ID is: " + eventId);

    }

    public void viewEvent(View view) {
        Uri uri = ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, eventId);
        Intent intent = new Intent(Intent.ACTION_VIEW)
                .setData(uri);
        startActivity(intent);
    }

    public void addAnotherEvent(View view) {
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
    }

    public void quitApp(View view) {
        this.finishAffinity();
    }
}
