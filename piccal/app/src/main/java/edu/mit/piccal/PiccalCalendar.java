package edu.mit.piccal;

import android.content.Context;
import android.content.Intent;
import android.provider.CalendarContract;
import android.util.Log;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by Jake on 4/20/2016.
 */
public class PiccalCalendar {

    Context context;
    private static final String L = "PiccalCalendar";

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

    public Intent addEvent(String unparsedText) {
        // do stuff
        long[] event_time = parseDateTime(unparsedText);

        return null;
    }

    public long[] parseDateTime(String text) {
        // Stores if we located a day, etc.
        boolean day_found = false, month_found = false, year_found = false;
        boolean toTime_found = false, fromTime_found = false;

        // The Matcher object
        Matcher m;

        // Represents (reasonable) possible date-delimiters, including / and -
        String dash = "[-.––_~/\\|\\\\]";
        // Represents any delimiter that isn't a digit/letter
        String delimit = "[^\\w\\d]";
        // Short-hand for delimit*
        String d = delimit + "*";
        // Represent an apostrophe that comes before a year, e.g. '12
        String apostrophe = "['\"`]";
        // Possible endings on any numbers. Optional in all situations.
        String th = "(st|nd|rd|th)?";
        // Represent a year token
        String year = "\\d\\d(\\d\\d)?|" + apostrophe + "\\d\\d";

        // Array of months, regex form:
        String[] MONTHS = {"jan(uary)?","feb(r(uary)?)?","mar(ch)?","apr(il)?","may","june?","july?",
                "aug(ust)?","sep(t(ember)?)?","oct(ober)?","nov(ember)?","dec(ember)?"};
        // Alternation of regex months, i.e. "jan(uary)?|feb(r(uary)?)?|..."
        String months_alternated = "(" + alternateThese(MONTHS) + ")";

        /*
          We do a series of different parses, looking for date information each
          time. If one fails, we go on to the next. They are ordered most-information
          to least-information, and easiest to hardest.

          1. mm/dd/yy and similar (this will always be parsed as mm/dd/yy, not dd/mm/yy,
             because this is 'Murica!)
          2. yy/mm/dd and similar.
          3. "nov(ember)? 23(-31)?, 2012" and similar
          4. "2012 nov(ember) 23(-31)?" and similar
          5. "12th of nov(ember)?, 2012" and similar
          6. "nov(ember)? 23(-31)?" and similar
          7. "12th of november" and similar
         */

        String r1 = "\\d\\d?" + dash + "\\d\\d?" + "(" + dash + "\\d\\d(\\d\\d)?)?";
        String r2 = "\\d\\d(\\d\\d)?" + dash + "\\d\\d?" + dash + "\\d\\d?";
        String r3 = months_alternated + d + "\\d\\d?" + th + "(" + d + "(" + dash + "|through|to)"
                + d + months_alternated + d + "\\d\\d?" + th + ")?" + d + year;
        String r4 = year + d + months_alternated + d
                + "\\d\\d?" + th + "(" + d + "(" + dash + "|through|to)"
                + d + months_alternated + d + "\\d\\d?" + th + ")?";
        String r5 = "\\d\\d?" + th + d + "( |of)?" + d + months_alternated + d + year;
        String r6 = months_alternated + d + "\\d\\d?" + th + "(" + d + "(" + dash + "|through|to)"
                + d + months_alternated + d + "\\d\\d?" + th + ")?";
        String r7 = "\\d\\d?" + d + "( |of)?" + d + months_alternated;

        Log.d(L, "start mdy");
        m = Pattern.compile(r1).matcher(text);
        Log.d(L, r1);
        Log.d(L, Boolean.toString(m.find()));

        Log.d(L, "start ymd");
        m = Pattern.compile(r2).matcher(text);
        Log.d(L, r2);
        Log.d(L, Boolean.toString(m.find()));

        Log.d(L, "start 3rd pattern");
        m = Pattern.compile(r3).matcher(text);
        Log.d(L, r3);
        Log.d(L, Boolean.toString(m.find()));

        Log.d(L, "start 4th pattern");
        m = Pattern.compile(r4).matcher(text);
        Log.d(L, r4);
        Log.d(L, Boolean.toString(m.find()));

        Log.d(L, "start 5th pattern");
        m = Pattern.compile(r5).matcher(text);
        Log.d(L, r5);
        Log.d(L, Boolean.toString(m.find()));

        Log.d(L, "start 6th pattern");
        m = Pattern.compile(r6).matcher(text);
        Log.d(L, r6);
        Log.d(L, Boolean.toString(m.find()));

        Log.d(L, "start 7th pattern");
        m = Pattern.compile(r7).matcher(text);
        Log.d(L, r7);
        Log.d(L, Boolean.toString(m.find()));

        return null;
    }

    private String alternateThese(String[] strings) {
        String bar = "|";
        String result = "";
        for(String s : strings) {
            result += (s + bar);
        }
        result = result.substring(0, result.length() - bar.length());

        return result;
    }
}
