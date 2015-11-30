package pie.watchface;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.provider.CalendarContract;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.wearable.provider.WearableCalendarContract;
import android.text.format.DateUtils;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

import pie.watchface.PieWatchFace.Pos;

/**
 * Created by ghans on 11/23/15.
 */
public class CalendarEvent {

    private static final String TAG = CalendarEvent.class.getSimpleName();

    // holds the next upcoming event
    @Nullable
    public static CalendarEvent nextEvent;

    public final boolean drawTitleOnStartingEdge;
    public final float startAngle;
    public final float endAngle;
    public final float durationInDegrees;
    public long _id;
    public String title;
    public Date startDate;
    public Date endDate;
    public int duration;
    public String location;
    public final boolean isAllDay;
    public int displayColor;

    public CalendarEvent(long calendarId,
                         String title, Date start, Date end, int duration,
                         String location, boolean allDay,
                         int color) {
        this._id = calendarId;
        this.title = title;
        this.startDate = start;
        this.endDate = end;
        this.duration = duration;
        this.location = location;
        this.isAllDay = allDay;
        this.displayColor = color;

        this.startAngle = PieUtils.getAngleForDate(this.startDate, true);
        this.endAngle = PieUtils.getAngleForDate(this.endDate, true);
        this.durationInDegrees = PieUtils.getDegreesForMinutes(((this.endDate.getHours() * 60) + this.endDate.getMinutes()) -
                ((this.startDate.getHours() * 60) + this.startDate.getMinutes()));

        this.drawTitleOnStartingEdge = (this.endAngle > Pos.DIAL_6_OCLOCK.nativeInt)
                && ((this.endAngle > Pos.DIAL_3_OCLOCK_ALT.nativeInt) || (this.endAngle <= Pos.DIAL_12_OCLOCK.nativeInt))
                && (this.startAngle >= Pos.DIAL_6_OCLOCK.nativeInt);
    }

    public String getInTimeString() {
        int mins = Math.abs(PieUtils.getDateInMinutes(this.startDate)) - PieUtils.getDateInMinutes(new Date());
        if (mins > 60) return "in " + (mins / 60) + "h";
        else return "in " + mins + "m";
    }

    @NonNull
    public static List<CalendarEvent> allEvents(Context context) {
        List<CalendarEvent> events = new ArrayList<>();

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

        // creating cursor and content resolver
        ContentResolver cr = context.getContentResolver();
        Cursor cur;// query the upcoming (next 12 hours) calendar events
        Uri.Builder builder = WearableCalendarContract.Instances.CONTENT_URI.buildUpon();
        ContentUris.appendId(builder, System.currentTimeMillis());
        ContentUris.appendId(builder, System.currentTimeMillis() + (DateUtils.DAY_IN_MILLIS / 2)); // get the next 12 hours

        cur = cr.query(builder.build(), EVENT_PROJECTION, null, null, null);

        if (cur == null) return events;

        // looping through the results, creating a CalendarEvent instance for each event
        while (cur.moveToNext()) {
            if (cur.getString(PROJECTION_TITLE_INDEX).contains("BOL"))
                continue;

            CalendarEvent event = new CalendarEvent(
                    cur.getLong(PROJECTION_ID_INDEX),
                    cur.getString(PROJECTION_TITLE_INDEX),
                    new Date(cur.getLong(PROJECTION_START_INDEX)),
                    new Date(cur.getLong(PROJECTION_END_INDEX)),
                    0,
                    cur.getString(PROJECTION_LOCATION_INDEX),
                    cur.getInt(PROJECTION_ALLDAY_INDEX) != 0,
                    0xffcd3737
            );
//            events.add(event);
        }
        events.addAll(getHardCodedEvents(true));

        cur.close();

        // sort events by time
        // ideally this would be done in the query builder, but that doesnt seem to be working
        Collections.sort(events, new Comparator<CalendarEvent>() {
            @Override
            public int compare(CalendarEvent lhs, CalendarEvent rhs) {
                return lhs.startDate.compareTo(rhs.startDate);
            }
        });

        if (events.isEmpty())
            return events;


        // Update Next Event variable with the next event
        int nowMinutes = PieUtils.getDateInMinutes(new Date());
        int startMinutes = PieUtils.getDateInMinutes(events.get(0).startDate);
        int endMinutes = PieUtils.getDateInMinutes(events.get(0).endDate);

        if (nowMinutes >= startMinutes && nowMinutes < endMinutes) {
            // if we are in middle of an ongoing event, then next event is located at position 2 in the array
            if (events.size() >= 2)
                nextEvent = events.get(1); // this will always be true
        } else {
            // else the next event is in first position of the array
            if (events.size() >= 1)
                nextEvent = events.get(0);
        }

        return events;
    }


    public static List<CalendarEvent> getHardCodedEvents(boolean add12Hours) {
        List<CalendarEvent> events = new ArrayList<>();

        // Adding a bunch of hard-coded dummy events, to not always have to add events manually in the android calendar
        Calendar start = new GregorianCalendar();
        Calendar end = new GregorianCalendar();

        start.set(Calendar.HOUR_OF_DAY, 6 + (add12Hours ? 12 : 0));
        start.set(Calendar.MINUTE, 0);
        start.set(Calendar.SECOND, 0);
        end.set(Calendar.HOUR_OF_DAY, 7 + (add12Hours ? 12 : 0));
        end.set(Calendar.MINUTE, 30);
        end.set(Calendar.SECOND, 0);
        events.add(new CalendarEvent(1, "Running", start.getTime(), end.getTime(), 0, "Outside", false, Color.parseColor("#ee6161")));

        start.set(Calendar.HOUR_OF_DAY, 0 + (add12Hours ? 12 : 0));
        start.set(Calendar.MINUTE, 15);
        start.set(Calendar.SECOND, 0);
        end.set(Calendar.HOUR_OF_DAY, 1 + (add12Hours ? 12 : 0));
        end.set(Calendar.MINUTE, 30);
        end.set(Calendar.SECOND, 0);
        events.add(new CalendarEvent(1, "Lunch at this restaurant", start.getTime(), end.getTime(), 0, "Chipotle", false, Color.parseColor("#009688")));

        start.set(Calendar.HOUR_OF_DAY, 2 + (add12Hours ? 12 : 0));
        start.set(Calendar.MINUTE, 0);
        start.set(Calendar.SECOND, 0);
        end.set(Calendar.HOUR_OF_DAY, 3 + (add12Hours ? 12 : 0));
        end.set(Calendar.MINUTE, 15);
        end.set(Calendar.SECOND, 0);
        events.add(new CalendarEvent(1, "Conference call about something", start.getTime(), end.getTime(), 0, "Room A1", false, Color.parseColor("#2196F3")));

        start.set(Calendar.HOUR_OF_DAY, 3 + (add12Hours ? 12 : 0));
        start.set(Calendar.MINUTE, 55);
        start.set(Calendar.SECOND, 0);
        end.set(Calendar.HOUR_OF_DAY, 5 + (add12Hours ? 12 : 0));
        end.set(Calendar.MINUTE, 30);
        end.set(Calendar.SECOND, 0);
        events.add(new CalendarEvent(1, "Exams Evaluation tonight", start.getTime(), end.getTime(), 0, "Room B1", false, Color.parseColor("#2196F3")));

        start.set(Calendar.HOUR_OF_DAY, 8 + (add12Hours ? 12 : 0));
        start.set(Calendar.MINUTE, 15);
        start.set(Calendar.SECOND, 0);
        end.set(Calendar.HOUR_OF_DAY, 9 + (add12Hours ? 12 : 0));
        end.set(Calendar.MINUTE, 30);
        end.set(Calendar.SECOND, 0);
        events.add(new CalendarEvent(1, "Dinner with Amy and John", start.getTime(), end.getTime(), 0, "La Place", false, Color.parseColor("#009688")));

        start.set(Calendar.HOUR_OF_DAY, 10 + (add12Hours ? 12 : 0));
        start.set(Calendar.MINUTE, 0);
        start.set(Calendar.SECOND, 0);
        end.set(Calendar.HOUR_OF_DAY, 11 + (add12Hours ? 12 : 0));
        end.set(Calendar.MINUTE, 30);
        end.set(Calendar.SECOND, 0);
        events.add(new CalendarEvent(1, "Skype call with people on MARS", start.getTime(), end.getTime(), 0, "La Place", false, Color.parseColor("#ee6161")));

        return events;
    }

    @Override
    public String toString() {
        return "title: " + this.title
                + " start: " + this.startDate.toString()
                + " end: " + this.endDate.toString()
                + " all day: " + this.isAllDay
                + " startAngle: " + this.startAngle
                + " endAngle: " + this.endAngle
                + " degrees duration: " + this.durationInDegrees;
    }
}
