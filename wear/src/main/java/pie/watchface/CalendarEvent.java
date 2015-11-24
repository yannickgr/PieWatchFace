package pie.watchface;

import java.util.Date;

/**
 * Created by ghans on 11/23/15.
 */
public class CalendarEvent {

    public long CalendarId;
    public String Title;
    public Date Start;
    public Date End;
    public int Duration;
    public String Location;
    public boolean AllDay;
    public int Color;

    /**
     * This class contains the info we need on a calendar event, combined with some helpers classes
     *
     * @param calendarId
     * @param title
     * @param start
     * @param end
     * @param duration
     * @param location
     * @param allDay
     * @param color
     */
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
    }

    public float getStartAngle(boolean takeArcDrawingOffsetIntoAccount) {
        return PieUtils.getAngleForDate(this.Start, takeArcDrawingOffsetIntoAccount);
    }

    public float getEndAngle(boolean takeArcDrawingOffsetIntoAccount) {
        return PieUtils.getAngleForDate(this.End, takeArcDrawingOffsetIntoAccount);
    }

    public float getDurationInDegrees() {
        return PieUtils.getDegreesForMinutes(((this.End.getHours() * 60) + this.End.getMinutes()) -
                ((this.Start.getHours() * 60) + this.Start.getMinutes()));
    }
}
