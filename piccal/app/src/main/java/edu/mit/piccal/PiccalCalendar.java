package edu.mit.piccal;

import android.content.Context;
import android.content.Intent;
import android.provider.CalendarContract;

/**
 * Created by Jake on 4/20/2016.
 */
public class PiccalCalendar {

    Context context;

    public PiccalCalendar(Context ctx) {
        context = ctx;
    }

    public Intent addEvent(String title, String time_date, String descr, String loc) {
        // We should actually parse the time from the time_date string but for now
        //  just make something up

        long startTime = System.currentTimeMillis();
        long endTime = System.currentTimeMillis() + (1000 * 3600); // adds 1 hour

        Intent intent = new Intent(Intent.ACTION_INSERT)
                .setData(CalendarContract.Events.CONTENT_URI)
                .putExtra(CalendarContract.Events.TITLE, title)
                .putExtra(CalendarContract.Events.EVENT_LOCATION, loc)
                .putExtra(CalendarContract.Events.DESCRIPTION, descr)
                .putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, startTime)
                .putExtra(CalendarContract.EXTRA_EVENT_END_TIME, endTime);

        if (intent.resolveActivity(context.getPackageManager()) != null) {
            context.startActivity(intent);
        }

        return intent;
    }
}
