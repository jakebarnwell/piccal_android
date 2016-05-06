package edu.mit.piccal;



import android.util.Log;

import java.util.*;
import com.joestelmach.natty.*;


public class InformationExtractor {

    private static final String TAG = "InformationExtractor";

    public static String location(String ocrText) {
        String[] split_text = ocrText.split("\\s+");

        String locText = "";
        if(split_text.length >= 3) {
            StringBuilder strBuilder = new StringBuilder();
            for (int i = split_text.length - 3; i < split_text.length; i++) {
                strBuilder.append(split_text[i] + " ");
            }
            locText = strBuilder.toString();
        }

        return locText;
    }

    public static String title(String ocrText) {
        String[] split_text = ocrText.split("\\s+");

        String titleText = ocrText;
        if(split_text.length >= 4) {
            StringBuilder strBuilder = new StringBuilder();
            for (int i = 0; i < 4; i++) {
                strBuilder.append(split_text[i] + " ");
            }
            titleText = strBuilder.toString();
        }

        return titleText;
    }

    public static String description(String ocrText) {
        return ocrText;
    }

    public static Calendar[] startEnd(String ocrString) {
        Calendar[] start_end = {null, null};
        Parser parser = new Parser();

        //parse out possible start and end dates as DateGroup objects
        List<DateGroup> dateGroups = parser.parse(ocrString);

        // for each possible date group
        for(int i = 0; i < dateGroups.size(); i++) {

            DateGroup group = dateGroups.get(i);

            // get string value
            String matchingValue = group.getText();

            // if too short, then ignore it and move to next possibility in for loop
            if (matchingValue.length() < 6){
                continue;
            }
            //else continue processing

            // extract start and end dates from group object as list of Date objects
            List<Date> dates = group.getDates();

            // get first date in the list
            Date startDate = dates.get(0);

            // initialis
            Date endDate = new Date(startDate.getYear(),startDate.getMonth(),startDate.getDate());

            // if there is more that one date in the group, the set the end date as the second object.
            if (dates.size() > 1){
                endDate = dates.get(1);
            }
            // else, set end date as start date + 1 hour if the group only had the one start date
            else {
                endDate.setHours(startDate.getHours() + 1);
                endDate.setMinutes(startDate.getMinutes());
                endDate.setSeconds(startDate.getSeconds());
            }

            Date currentDate = new Date();

            // if start date is in the future
            if (currentDate.getMinutes()-startDate.getMinutes() < 2) {

                //go through all other valid date groups and check if they are better
                while(++i< dateGroups.size() && dateGroups.get(i).getText().length() < 6);

                if (i < dateGroups.size()) {

                    // get new date group
                    DateGroup newGroup = dateGroups.get(i);
                    // get dates from group
                    List<Date> newDates = newGroup.getDates();
                    // set start date
                    startDate.setHours(newDates.get(0).getHours());
                    startDate.setMinutes(newDates.get(0).getMinutes());
                    startDate.setSeconds(newDates.get(0).getSeconds());

                    // if there are more than one dates, set the new end date to the second date
                    if (newDates.size() > 1) {
                        Log.d(TAG, "Second date found.");
                        endDate.setHours(newDates.get(1).getHours());
                        endDate.setMinutes(newDates.get(1).getMinutes());
                        endDate.setSeconds(newDates.get(1).getSeconds());
                    }
                    // else, set it as the start date + 1 hour
                    else {
                        Log.d(TAG, "No second date found. Defaulting to start + 1 hour.");
                        endDate.setHours(startDate.getHours()+1);
                        endDate.setMinutes(startDate.getMinutes());
                        endDate.setSeconds(startDate.getSeconds());
                    }
                }
            }

            Calendar start_cal = Calendar.getInstance(); start_cal.setTime(startDate);
            Calendar end_cal = Calendar.getInstance(); end_cal.setTime(endDate);

            start_end[0] = start_cal;
            start_end[1] = end_cal;

            break;
        }
        return start_end;
    }

}