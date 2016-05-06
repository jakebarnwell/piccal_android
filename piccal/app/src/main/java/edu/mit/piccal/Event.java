package edu.mit.piccal;

import java.util.Calendar;

public class Event {

    protected Calendar start;
    protected Calendar end;
    protected String location;
    protected String title;
    protected String description;

    /**
     * Standard constructor for a default event. Constructs an empty event whose start date
     * is now and end date is now plus 1 hour.
     */
    public Event() {
        start = Calendar.getInstance();
        end = Calendar.getInstance(); end.add(Calendar.HOUR, 1);
        location = new String();
        title = new String();
        description = new String();
    }

    /**
     * Constructs an event from an OCR string.
     * @param ocr_text the text recognized from the OCR system
     */
    public Event(String ocr_text) {
        this();
        extractInfo(ocr_text);
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
        long withinOneMinute = (long)(2 * (1000 * 60));
        return c1.getTimeInMillis() - c2.getTimeInMillis() < withinOneMinute;
    }

    public void extractInfo(String ocr_text) {
        Calendar[] start_end = InformationExtractor.startEnd(ocr_text);
        if(start_end[0] != null) {
            start = start_end[0];
        }
        if(start_end[1] != null) {
            end = start_end[1];
        }
        location = InformationExtractor.location(ocr_text);
        title = InformationExtractor.title(ocr_text);
        description = InformationExtractor.description(ocr_text);
    }

}