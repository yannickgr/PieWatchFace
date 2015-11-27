package pie.watchface;

import android.support.annotation.NonNull;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

import pie.watchface.PieWatchFace.Pos;

/**
 * Created by ghans on 11/23/15.
 */
public class CalendarEvent {

    private static final String TAG = CalendarEvent.class.getSimpleName();
    public final boolean drawTitleOnStartingEdge;
    public final float startAngle;
    public final float endAngle;
    public final float durationInDegrees;
    public long CalendarId;
    public String Title;
    public Date Start;
    public Date End;
    public int Duration;
    public String Location;
    public boolean AllDay;
    public int Color;

    public CalendarEvent(long calendarId,
                         String title, Date start, Date end, int duration,
                         String location, boolean allDay,
                         int color) {
        this.CalendarId = calendarId;
        this.Title = title;
        this.Start = start;
        this.End = end;
        this.Duration = duration;
        this.Location = location;
        this.AllDay = allDay;
        this.Color = color;

        this.startAngle = PieUtils.getAngleForDate(this.Start, true);
        this.endAngle = PieUtils.getAngleForDate(this.End, true);
        this.durationInDegrees = PieUtils.getDegreesForMinutes(((this.End.getHours() * 60) + this.End.getMinutes()) -
                ((this.Start.getHours() * 60) + this.Start.getMinutes()));

        this.drawTitleOnStartingEdge = (this.endAngle > Pos.DIAL_6_OCLOCK.nativeInt)
                && ((this.endAngle > Pos.DIAL_3_OCLOCK_ALT.nativeInt) || (this.endAngle <= Pos.DIAL_12_OCLOCK.nativeInt))
                && (this.startAngle != Pos.DIAL_6_OCLOCK.nativeInt);
    }

    @NonNull
    public static List<CalendarEvent> allEvents() {
        List<CalendarEvent> events = new ArrayList<>();

            /*// creating cursor and content resolver
            ContentResolver cr = getContentResolver();

            // the indices for the projection array above.
            final int PROJECTION_ID_INDEX = 0;
            final int PROJECTION_ACCOUNT_NAME_INDEX = 1;
            final int PROJECTION_DISPLAY_NAME_INDEX = 2;
            final int PROJECTION_OWNER_ACCOUNT_INDEX = 3;
            final int PROJECTION_TITLE_INDEX = 4;
            final int PROJECTION_DISPLAY_COLOR_INDEX = 5;
            final int PROJECTION_START_INDEX = 6;
            final int PROJECTION_END_INDEX = 7;
            final int PROJECTION_DURATION_INDEX = 8;
            final int PROJECTION_LOCATION_INDEX = 9;
            final int PROJECTION_ALLDAY_INDEX = 10;
            final int PROJECTION_DISPLAYCOLOR_INDEX = 11;

            // getting calendar data
            // This load should move to a separate async function
            // Projection array. Creating indices for this array instead of doing
            // dynamic lookups improves performance.
            final String[] EVENT_PROJECTION = new String[]{
                    CalendarContract.Calendars._ID,                     // 0
                    CalendarContract.Calendars.ACCOUNT_NAME,            // 1
                    CalendarContract.Calendars.CALENDAR_DISPLAY_NAME,   // 2
                    CalendarContract.Calendars.OWNER_ACCOUNT,           // 3
                    CalendarContract.Events.TITLE,                      // 4
                    CalendarContract.Events.DISPLAY_COLOR,              // 5
                    CalendarContract.Events.DTSTART,                    // 6
                    CalendarContract.Events.DTEND,                      // 7
                    CalendarContract.Events.DURATION,                   // 8
                    CalendarContract.Events.EVENT_LOCATION,             // 9
                    CalendarContract.Events.ALL_DAY,                    // 10
                    CalendarContract.Events.DISPLAY_COLOR,              // 11

            };

            Cursor cur;// query the upcoming (next 12 hours) calendar events
            Uri.Builder builder = WearableCalendarContract.Instances.CONTENT_URI.buildUpon();
            ContentUris.appendId(builder, begin);
            ContentUris.appendId(builder, begin + (DateUtils.DAY_IN_MILLIS / 2)); // get the next 12 hours
            cur = cr.query(builder.build(), EVENT_PROJECTION, null, null,
                    null); //TODO: CalendarContract.Instances.BEGIN); // selection, selectionArgs, null);

            // looping through the results, creating a CalendarEvent instance for each event
            while (cur.moveToNext()) {
                *//* adding dummy events at the moment (see below)
                events.add(new CalendarEvent(
                        cur.getLong(PROJECTION_ID_INDEX),
                        cur.getString(PROJECTION_TITLE_INDEX),
                        new Date(cur.getLong(PROJECTION_START_INDEX)),
                        new Date(cur.getLong(PROJECTION_END_INDEX)),
                        0,
                        cur.getString(PROJECTION_LOCATION_INDEX),
                        cur.getInt(PROJECTION_ALLDAY_INDEX) != 0,
                        cur.getInt(PROJECTION_DISPLAYCOLOR_INDEX)
                ));
                *//*
            }*/

        // Adding a bunch of hard-coded dummy events, to not always have to add events manually in the android calendar
        Calendar start = new GregorianCalendar();
        Calendar end = new GregorianCalendar();

        /*start.set(Calendar.HOUR_OF_DAY, 6);
        start.set(Calendar.MINUTE, 0);
        start.set(Calendar.SECOND, 0);
        end.set(Calendar.HOUR_OF_DAY, 7);
        end.set(Calendar.MINUTE, 0);
        end.set(Calendar.SECOND, 0);
        events.add(new CalendarEvent(1, "Running", start.getTime(), end.getTime(), 0, "Outside", false, android.graphics.Color.BLUE));*/

        start.set(Calendar.HOUR_OF_DAY, 12);
        start.set(Calendar.MINUTE, 10);
        start.set(Calendar.SECOND, 0);
        end.set(Calendar.HOUR_OF_DAY, 13);
        end.set(Calendar.MINUTE, 30);
        end.set(Calendar.SECOND, 0);
        events.add(new CalendarEvent(1, "Lunch at this restaurant", start.getTime(), end.getTime(), 0, "Chipotle", false, android.graphics.Color.parseColor("#009688")));

        start.set(Calendar.HOUR_OF_DAY, 14);
        start.set(Calendar.MINUTE, 0);
        start.set(Calendar.SECOND, 0);
        end.set(Calendar.HOUR_OF_DAY, 15);
        end.set(Calendar.MINUTE, 0);
        end.set(Calendar.SECOND, 0);
        events.add(new CalendarEvent(1, "Conference call about something", start.getTime(), end.getTime(), 0, "Room A1", false, android.graphics.Color.parseColor("#2196F3")));

        start.set(Calendar.HOUR_OF_DAY, 15);
        start.set(Calendar.MINUTE, 30);
        start.set(Calendar.SECOND, 0);
        end.set(Calendar.HOUR_OF_DAY, 16);
        end.set(Calendar.MINUTE, 30);
        end.set(Calendar.SECOND, 0);
        events.add(new CalendarEvent(1, "Exams Evaluation tonight", start.getTime(), end.getTime(), 0, "Room B1", false, android.graphics.Color.parseColor("#2196F3")));

        start.set(Calendar.HOUR_OF_DAY, 18);
        start.set(Calendar.MINUTE, 0);
        start.set(Calendar.SECOND, 0);
        end.set(Calendar.HOUR_OF_DAY, 20);
        end.set(Calendar.MINUTE, 0);
        end.set(Calendar.SECOND, 0);
        events.add(new CalendarEvent(1, "Dinner with Amy and John", start.getTime(), end.getTime(), 0, "La Place", false, android.graphics.Color.parseColor("#009688")));

        start.set(Calendar.HOUR_OF_DAY, 21);
        start.set(Calendar.MINUTE, 0);
        start.set(Calendar.SECOND, 0);
        end.set(Calendar.HOUR_OF_DAY, 22);
        end.set(Calendar.MINUTE, 30);
        end.set(Calendar.SECOND, 0);
        events.add(new CalendarEvent(1, "Skype call with people on MARS", start.getTime(), end.getTime(), 0, "La Place", false, android.graphics.Color.parseColor("#ee6161")));
        return events;
    }

    @Override
    public String toString() {
        return "title: " + this.Title
                + " start: " + this.Start.toString()
                + " end: " + this.End.toString()
                + " all day: " + this.AllDay
                + " startAngle: " + this.startAngle
                + " endAngle: " + this.endAngle;
    }
}
