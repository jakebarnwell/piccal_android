package edu.mit.piccal;

import java.util.Date;

public class Event {

    Date start;
    Date end;
    String location;
    String title;
    String description;

    public Event() {
        start = new Date();
        end = new Date();
        location = new String();
        title = new String();
        description = new String();
    }

}