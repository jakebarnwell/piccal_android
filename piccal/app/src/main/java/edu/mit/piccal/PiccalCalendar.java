package edu.mit.piccal;

import android.content.Context;
import android.content.Intent;
import android.provider.CalendarContract;
import android.util.Log;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
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

    public Intent addEvent(String unparsedText) {
        // do stuff
        long[] event_time = parseDateTime(unparsedText);

        return null;
    }

    public long[] parseDateTime(String text) {
        // Stores if we located a day, etc.
        boolean day_found = false, month_found = false, year_found = false;
        boolean toTime_found = false, fromTime_found = false;

        String the_month = null, the_day = null, the_year = null;

        // The Matcher object
        Matcher m;

        // Represents (reasonable) possible date-delimiters, including / and -
        String dash = "[-.––_~/\\|\\\\]";
        // Represents any delimiter that isn't a digit/letter
        String delimit = "[^\\w\\d]";
        // Short-hand for delimit*
        String d = delimit + "*";
        d = ".*?"; // hack for now
        // Represent an apostrophe that comes before a year, e.g. '12
        String apostrophe = "['\"`]";
        // Possible endings on any numbers. Optional in all situations.
        String th = "(?:st|nd|rd|th)?";
        // Represent a year token
        String year = "(\\d\\d(\\d\\d)?|" + apostrophe + "\\d\\d)";

        // Array of months, regex form:
        String[] MONTHS = {"jan(uary)?", "feb(r(uary)?)?", "mar(ch)?", "apr(il)?", "may", "june?", "july?",
                "aug(ust)?", "sep(t(ember)?)?", "oct(ober)?", "nov(ember)?", "dec(ember)?"};
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

        String r1 = "(\\d\\d?)" + dash + "(\\d\\d?)" + "(" + dash + "(\\d\\d(\\d\\d)?))?";
        String r2 = "(\\d\\d(\\d\\d)?)" + dash + "(\\d\\d?)" + dash + "(\\d\\d?)";
        String r3 = months_alternated + d + "(\\d\\d?)" + th + "(" + d + "(" + dash + "|through|to)"
                + d + months_alternated + d + "\\d\\d?" + th + ")?" + d + year;
        String r4 = year + d + months_alternated + d
                + "(\\d\\d?)" + th + "(" + d + "(" + dash + "|through|to)"
                + d + months_alternated + d + "\\d\\d?" + th + ")?";
        String r5 = "(\\d\\d?)" + th + d + "(?: |of)?" + d + months_alternated + d + year;
        String r6 = months_alternated + d + "(\\d\\d?)" + th + "(" + d + "(" + dash + "|through|to)"
                + d + months_alternated + d + "\\d\\d?" + th + ")?";
        String r7 = "(\\d\\d?)" + d + "(?: |of)?" + d + months_alternated;

        String s;
        Log.d(L, "start mdy");
        m = Pattern.compile(r1).matcher(text);
        Log.d(L, r1);
        if (m.find()) {
            the_month = m.group(1);
            the_day = m.group(2);
            try {
                the_year = m.group(4);
            } catch (Exception e) {
                the_year = null;
            }

            return dateToLong(the_day, the_month, the_year);
        }

        Log.d(L, "start ymd");
        m = Pattern.compile(r2).matcher(text);
        Log.d(L, r2);
        if (m.find()) {
            Log.d(L, "success");
            the_year = m.group(1);
            the_month = m.group(2);
            the_day = m.group(3);
            return dateToLong(the_day, the_month, the_year);
        }

        Log.d(L, "start 3rd pattern");
        m = Pattern.compile(r3).matcher(text);
        Log.d(L, r3);
        if (m.find()) {
            Log.d(L, "success");
            the_month = m.group(1);
            the_day = m.group(2);
            the_year = m.group(m.groupCount());
            return dateToLong(the_day, the_month, the_year);
        }

        Log.d(L, "start 4th pattern");
        m = Pattern.compile(r4).matcher(text);
        Log.d(L, r4);
        if (m.find()) {
            Log.d(L, "success");
            the_year = m.group(1);
            the_month = m.group(2);
            the_day = m.group(3);
            return dateToLong(the_day, the_month, the_year);
        }

        Log.d(L, "start 5th pattern");
        m = Pattern.compile(r5).matcher(text);
        Log.d(L, r5);
        if (m.find()) {
            Log.d(L, "success");
            the_day = m.group(1);
            the_month = m.group(2);
            the_year = m.group(3);
            return dateToLong(the_day, the_month, the_year);
        }

        Log.d(L, "start 6th pattern");
        m = Pattern.compile(r6).matcher(text);
        Log.d(L, r6);
        if (m.find()) {
            Log.d(L, "success");
            the_year = null;
            the_month = m.group(1);
            the_day = m.group(2);
            return dateToLong(the_day, the_month, the_year);
        }

        Log.d(L, "start 7th pattern");
        m = Pattern.compile(r7).matcher(text);
        Log.d(L, r7);
        if (m.find()) {
            Log.d(L, "success");
            the_day = m.group(1);
            the_month = m.group(2);
            the_year = null;
            return dateToLong(the_day, the_month, the_year);
        }

        return dateToLong(the_day, the_month, the_year);
    }

    public long[] dateToLong(String the_day, String the_month, String the_year) {

        long[] result = {0L,0L};
        if(the_year == null) {
            the_year = "2016";
        }

        String day = getDay(the_day);
        String month = getMonthName(the_month);
        String year = getYear(the_year);

        Log.d(L, "Date: " + month + ":" + day + ":" + year);

        Date date;
        try {
            date = new SimpleDateFormat("MMMMddyyyy", Locale.ENGLISH).parse(the_month + the_day + the_year);
        } catch (ParseException e) {
            date = new Date();
            e.printStackTrace();
        }

        result[0] = date.getTime();
        result[1] = date.getTime() + (1000 * 60 * 60);
        Log.d(L, Long.toString(result[0]));

        return result;
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

    public long[] extractDateInfo(String text) {
        return parseDateTime(text);
    }

    private String getMonthName(String regex_month) {
        String[] MONTHS ={"January","February","March","April","May","June",
                "July","August","September","October","November","December"};

        try {
            int ind = Integer.parseInt(regex_month);
            try {
                return MONTHS[ind];
            } catch(Exception e) {
                ;
            }
        } catch(Exception e) {
            for(String MONTH : MONTHS) {
                if(MONTH.toLowerCase().contains(regex_month)) {
                    return MONTH;
                }
            }
        }

        return null;
    }

    private String getYear(String regex_year) {
        if(regex_year == null) {
            return null;
        }

        if (regex_year.length() == 2) {
            if(regex_year.startsWith("0") || regex_year.startsWith("1")) {
                return "19" + regex_year;
            } else {
                return "20" + regex_year;
            }
        } else if(regex_year.length() == 4) {
            return regex_year;
        } else {
            return null;
        }
    }

    private String getDay(String regex_day) {
        return regex_day;
    }
}
