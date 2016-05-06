package edu.mit.piccal;

import java.util.Calendar;
import java.util.Date;

public class Event {

    protected Calendar start;
    protected Calendar end;
    protected String location;
    protected String title;
    protected String description;

    public Event() {
        start = Calendar.getInstance();
        end = Calendar.getInstance();
        location = new String();
        title = new String();
        description = new String();
    }

    public Calendar getSmartStart() {
        return start;
    }

    public Calendar getSmartEnd() {
        if(sameTime(start, end)) {
            Calendar smart_end = (Calendar)start.clone();
            smart_end.add(Calendar.HOUR, 1);
            return smart_end;
        } else {
            return end;
        }
    }

    private boolean sameTime(Calendar c1, Calendar c2) {
        long withinOneMinute = 2 * (1000 * 60);
        return c1.getTimeInMillis() - c2.getTimeInMillis() < withinOneMinute;
    }

}