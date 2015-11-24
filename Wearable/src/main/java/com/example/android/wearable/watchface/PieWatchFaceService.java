package com.example.android.wearable.watchface;

import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.CalendarContract;
import android.support.wearable.provider.WearableCalendarContract;
import android.support.wearable.watchface.CanvasWatchFaceService;
import android.support.wearable.watchface.WatchFaceStyle;
import android.text.format.DateUtils;
import android.text.format.Time;
import android.util.Log;
import android.util.TypedValue;
import android.view.SurfaceHolder;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.List;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

/**
 * Created by Yannick Grossard on 13/04/15.
 */
public class PieWatchFaceService extends CanvasWatchFaceService {
    @Override
    public Engine onCreateEngine() {
        return new Engine();
    }

    private static final long INTERACTIVE_UPDATE_RATE_MS = TimeUnit.SECONDS.toMillis(1);


    private class CalendarEvent {
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
            return getAngleForDate(this.Start, takeArcDrawingOffsetIntoAccount);
        }

        public float getEndAngle(boolean takeArcDrawingOffsetIntoAccount) {
            return getAngleForDate(this.End, takeArcDrawingOffsetIntoAccount);
        }

        public float getDurationInDegrees() {
            return getDegreesForMinutes(((this.End.getHours() * 60) + this.End.getMinutes()) -
                    ((this.Start.getHours() * 60) + this.Start.getMinutes()));
        }
    }

    /**
     * For any angle of the circle given, get the coordinates of its point on the circumference
     *
     * @param radius
     * @param angle
     * @param centreX
     * @param centreY
     * @return
     */
    public Point getPointOnTheCircleCircumference(double radius, double angle, float centreX, float centreY) {
        int y = (int) Math.round(centreY + radius * Math.sin(angle * Math.PI / 180f));
        int x = (int) Math.round(centreX + radius * Math.cos(angle * Math.PI / 180f));

        return new Point(x, y);
    }

    /**
     * Transform a given date to minutes, removing the difference for AM and PM
     *
     * @param date
     * @return
     */
    public int getDateInMinutes(Date date) {
        int minutes = date.getMinutes();
        int hours = date.getHours();

        if (hours > 12) {
            hours = hours - 12;
        }
        minutes = minutes + (hours * 60);

        return minutes;
    }

    private float getDegreesForMinutes(int minutes) {
        return minutes * 0.5f;
    }

    private float getAngleForDate(Date date, boolean takeArcDrawingOffsetIntoAccount) {
        int minutes = getDateInMinutes(date);
        return this.getAngleForDate(minutes, takeArcDrawingOffsetIntoAccount);
    }

    private float getAngleForDate(int minutes, boolean takeArcDrawingOffsetIntoAccount) {
        // get the angle starting from the top (12 o clock)
        float startAngle = this.getDegreesForMinutes(minutes);

        if (takeArcDrawingOffsetIntoAccount) {
            // 0 degrees starts at 180 minutes/3 hours for the canvas/paintbrush (so NOT at noon/12)
            // result: everything below that must get 270 degrees added, everything above that must get 180 minutes subtracted

            if (minutes >= 180) {
                // this is more then 3 hours, subtracting the 90 degree offset
                startAngle -= 90f;
            } else {
                // this is less then 3 hours, rotating to the top (push 270 degrees)
                startAngle += 270f;
            }
        }

        return startAngle;
    }

    public float getPixelsForDips(float dips) {
        Resources r = getResources();
        float px = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dips, r.getDisplayMetrics());

        return px;
    }

    // implement service callback methods
    private class Engine extends CanvasWatchFaceService.Engine {

        static final int MSG_UPDATE_TIME = 0;

        // a time object
        Time mTime;

        // device features
        boolean mLowBitAmbient;

        // graphic objects
        Bitmap mBackgroundBitmap;
        Bitmap mBackgroundScaledBitmap;
        Paint mPiePaint;
        Paint mTextPaint;
        Paint mDialPaint;
        private boolean mBurnInProtection;

        @Override
        public void onCreate(SurfaceHolder holder) {
            super.onCreate(holder);
            // initialize the watch face & configure the system UI
            setWatchFaceStyle(new WatchFaceStyle.Builder(PieWatchFaceService.this)
                    .setCardPeekMode(WatchFaceStyle.PEEK_MODE_SHORT)
                    .setBackgroundVisibility(WatchFaceStyle
                            .BACKGROUND_VISIBILITY_INTERRUPTIVE)
                    .setShowSystemUiTime(false)
                    .build());


            // load the background image
            Resources resources = PieWatchFaceService.this.getResources();
            Drawable backgroundDrawable = resources.getDrawable(R.drawable.bg);
            mBackgroundBitmap = ((BitmapDrawable) backgroundDrawable).getBitmap();

            // initialize the paint brushes
            // the brush used to paint the pie pieces
            mPiePaint = new Paint();
            mPiePaint.setARGB(120, 100, 240, 200);
            mPiePaint.setStrokeWidth(5.0f);
            mPiePaint.setAntiAlias(true);
            mPiePaint.setStrokeCap(Paint.Cap.ROUND);
            mPiePaint.setShadowLayer(10.0f, 0.0f, 2.0f, 0xFF000000);

            // the brush used to paint the text on the pie pieces
            mTextPaint = new Paint();
            mTextPaint.setColor(Color.WHITE);
            mTextPaint.setStrokeWidth(5.0f);
            mTextPaint.setAntiAlias(true);
            mTextPaint.setStrokeCap(Paint.Cap.ROUND);
            mTextPaint.setTextSize(24);

            // the brush used to paint the hour markers and current time marker
            mDialPaint = new Paint();
            mDialPaint.setColor(Color.WHITE);
            mDialPaint.setAlpha(180);
            mDialPaint.setStrokeWidth(6.0f);
            mDialPaint.setAntiAlias(true);
            mDialPaint.setStrokeCap(Paint.Cap.ROUND);
            mDialPaint.setShadowLayer(10.0f, 0.0f, 2.0f, 0xFF000000);

            // allocate an object to hold the time
            mTime = new Time();
        }

        @Override
        public void onPropertiesChanged(Bundle properties) {
            super.onPropertiesChanged(properties);
            /* get device features (burn-in, low-bit ambient) */
            mLowBitAmbient = properties.getBoolean(PROPERTY_LOW_BIT_AMBIENT, false);
            mBurnInProtection = properties.getBoolean(PROPERTY_BURN_IN_PROTECTION,
                    false);
        }

        @Override
        public void onTimeTick() {
            super.onTimeTick();
            /* the time changed */
            invalidate();
        }

        @Override
        public void onAmbientModeChanged(boolean inAmbientMode) {
            super.onAmbientModeChanged(inAmbientMode);
            /* the wearable switched between modes */
            if (mLowBitAmbient) {
                boolean antiAlias = !inAmbientMode;
                mPiePaint.setAntiAlias(antiAlias);
            }
            invalidate();
            updateTimer();
        }

        @Override
        public void onDraw(Canvas canvas, Rect bounds) {
            // draw the watch face

            // update the time
            mTime.setToNow();

            // getting the bounds
            int width = bounds.width();
            int height = bounds.height();

            // draw the background, scaled to fit.
            if (mBackgroundScaledBitmap == null
                    || mBackgroundScaledBitmap.getWidth() != width
                    || mBackgroundScaledBitmap.getHeight() != height) {
                mBackgroundScaledBitmap = Bitmap.createScaledBitmap(mBackgroundBitmap,
                        width, height, true /* filter */);
            }
            canvas.drawBitmap(mBackgroundScaledBitmap, 0, 0, null);

            // Find the center. Ignore the window insets so that, on round watches
            // with a "chin", the watch face is centered on the entire screen, not
            // just the usable portion.
            float centerX = width / 2f;
            float centerY = height / 2f;
            RectF boundsF = new RectF(bounds);
            double radius = width / 2;

            String TAG = "PieApp";

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

            // creating cursor and content resolver
            Cursor cur = null;
            ContentResolver cr = getContentResolver();

            // get the current time to find out the current angle
            long begin = System.currentTimeMillis();
            Date now = new Date(begin);
            int nowMinutes = getDateInMinutes(now);
            float nowAngle = getAngleForDate(now, true);

            // query the upcoming (next 12 hours) calendar events
            Uri.Builder builder =
                    WearableCalendarContract.Instances.CONTENT_URI.buildUpon();
            ContentUris.appendId(builder, begin);
            ContentUris.appendId(builder, begin + (DateUtils.DAY_IN_MILLIS / 2)); // get the next 12 hours
            cur = cr.query(builder.build(), EVENT_PROJECTION, null, null,
                    null); //TODO: CalendarContract.Instances.BEGIN); // selection, selectionArgs, null);

            // looping through the results, creating a CalendarEvent instance for each event
            List<CalendarEvent> events = new ArrayList<CalendarEvent>();
            while (cur.moveToNext()) {
                /* adding dummy events at the moment (see below)
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
                */
            }

            // Adding a bunch of hard-coded dummy events, to not always have to add events manually in the android calendar
            Calendar start = new GregorianCalendar();
            Calendar end = new GregorianCalendar();

            /*start.set(Calendar.HOUR_OF_DAY, 6); start.set(Calendar.MINUTE, 0); start.set(Calendar.SECOND, 0);
            end.set(Calendar.HOUR_OF_DAY, 7); end.set(Calendar.MINUTE, 0); end.set(Calendar.SECOND, 0);
            events.add(new CalendarEvent(1, "Running", start.getTime(), end.getTime(), 0, "Outside", false, Color.BLUE));*/

            start.set(Calendar.HOUR_OF_DAY, 12);
            start.set(Calendar.MINUTE, 0);
            start.set(Calendar.SECOND, 0);
            end.set(Calendar.HOUR_OF_DAY, 13);
            end.set(Calendar.MINUTE, 30);
            end.set(Calendar.SECOND, 0);
            events.add(new CalendarEvent(1, "Lunch", start.getTime(), end.getTime(), 0, "Chipotle", false, Color.parseColor("#009688")));

            start.set(Calendar.HOUR_OF_DAY, 14);
            start.set(Calendar.MINUTE, 0);
            start.set(Calendar.SECOND, 0);
            end.set(Calendar.HOUR_OF_DAY, 15);
            end.set(Calendar.MINUTE, 0);
            end.set(Calendar.SECOND, 0);
            events.add(new CalendarEvent(1, "Conf.call", start.getTime(), end.getTime(), 0, "Room A1", false, Color.parseColor("#2196F3")));

            start.set(Calendar.HOUR_OF_DAY, 15);
            start.set(Calendar.MINUTE, 30);
            start.set(Calendar.SECOND, 0);
            end.set(Calendar.HOUR_OF_DAY, 16);
            end.set(Calendar.MINUTE, 30);
            end.set(Calendar.SECOND, 0);
            events.add(new CalendarEvent(1, "Evaluation", start.getTime(), end.getTime(), 0, "Room B1", false, Color.parseColor("#2196F3")));

            start.set(Calendar.HOUR_OF_DAY, 18);
            start.set(Calendar.MINUTE, 0);
            start.set(Calendar.SECOND, 0);
            end.set(Calendar.HOUR_OF_DAY, 20);
            end.set(Calendar.MINUTE, 0);
            end.set(Calendar.SECOND, 0);
            events.add(new CalendarEvent(1, "Dinner w Amy", start.getTime(), end.getTime(), 0, "La Place", false, Color.parseColor("#009688")));

            start.set(Calendar.HOUR_OF_DAY, 21);
            start.set(Calendar.MINUTE, 0);
            start.set(Calendar.SECOND, 0);
            end.set(Calendar.HOUR_OF_DAY, 23);
            end.set(Calendar.MINUTE, 30);
            end.set(Calendar.SECOND, 0);
            events.add(new CalendarEvent(1, "Skype call", start.getTime(), end.getTime(), 0, "La Place", false, Color.parseColor("#ee6161")));

            // for easy use, the primary positions of the dial
            final int DIAL_12_OCLOCK = 270;
            final int DIAL_3_OCLOCK = 0;
            final int DIAL_3_OCLOCK_ALT = 360;
            final int DIAL_6_OCLOCK = 90;
            final int DIAL_9_OCLOCK = 180;

            // assume the start is the current time
            float baselineAngle = nowAngle;

            // looping through the stored events
            Iterator<CalendarEvent> iterator = events.iterator();
            while (iterator.hasNext()) {
                CalendarEvent event = iterator.next();
                /*Log.d(TAG, "--NEXT EVENT");
                Log.d(TAG, "Title: " + event.Title);
                Log.d(TAG, "Location: " + event.Location);
                Log.d(TAG, "Start: " + event.Start.toString());
                Log.d(TAG, "End: " + event.End.toString());
                Log.d(TAG, "All day: " + event.AllDay);
                Log.d(TAG, "Color: " + event.Color);*/

                if (event.AllDay) {
                    //TODO: These should be added just behind the current time indicator, and be "dragged along" for the rest of the day. The downside is we lose some of our events horizon.
                } else {
                    int startMinutes = getDateInMinutes(event.Start);
                    int endMinutes = getDateInMinutes(event.End);
                    float endAngle = event.getEndAngle(true);
                    float startAngle = event.getStartAngle(true);
                    float duration = event.getDurationInDegrees();

                    if (nowMinutes > startMinutes &&
                            nowMinutes < endMinutes &&
                            baselineAngle == nowAngle) {
                        // we are currently in progress of this event, use the start as baseline
                        baselineAngle = startAngle;
                    }

                    // drawing the pie piece
                    mPiePaint.setColor(event.Color);
                    canvas.drawArc(boundsF, startAngle, duration, true, mPiePaint);

                    // drawing the title inside the pie piece
                    Path path = new Path();
                    Point endPoint = getPointOnTheCircleCircumference(radius, endAngle, centerX, centerY);
                    Point startPoint = getPointOnTheCircleCircumference(radius, startAngle, centerX, centerY);
                    float vOffset = 0;
                    float hOffset = 0;

                    //Log.d(TAG, "Event " + event.Title + ", start angle " + startAngle + " - end angle " + endAngle);
                    if (
                        /*(
                                startAngle > DIAL_12_OCLOCK ||
                                (startAngle >= DIAL_3_OCLOCK && startAngle < DIAL_6_OCLOCK)
                        )
                        &&*/
                            (
                                    endAngle <= DIAL_6_OCLOCK ||
                                            (endAngle <= DIAL_3_OCLOCK_ALT && endAngle > DIAL_12_OCLOCK)
                            )
                            ) {
                        // draw the text at the end of the slice
                        mTextPaint.setTextAlign(Paint.Align.RIGHT);
                        path.moveTo(centerX, centerY);
                        path.lineTo(endPoint.x, endPoint.y);
                        vOffset = getPixelsForDips(-5);
                        hOffset = getPixelsForDips(-5);
                    } else if (
                            /*startAngle > DIAL_6_OCLOCK
                                    &&
                                    startAngle < DIAL_12_OCLOCK
                                    && */
                            endAngle <= DIAL_12_OCLOCK) {
                        // draw the text at the end of the slice
                        mTextPaint.setTextAlign(Paint.Align.LEFT);
                        path.moveTo(endPoint.x, endPoint.y);
                        path.lineTo(centerX, centerY);
                        vOffset = getPixelsForDips(20);
                        hOffset = getPixelsForDips(5);
                    } else {
                        // draw the text at the beginning of the slice
                        //mTitlePaint.setTextAlign(Paint.Align.LEFT);
                        //path.moveTo(startPoint.x, startPoint.y);
                        //path.lineTo(centerX, centerY);
                        Log.d(TAG, "Can't determine pie title placing. Start angle is " + startAngle + " and end angle is " + endAngle);
                        throw new IllegalArgumentException("We do not know where to place the title of this appointment");
                    }

                    canvas.drawTextOnPath(event.Title, path, hOffset, vOffset, mTextPaint);
                }

            } // for each event

            // TODO: draw horizon gradient
            //mPiePaint.setColor(Color.DKGRAY);
            //canvas.drawArc(boundsF, baselineAngle - 30, 30, true, mPiePaint);

            // drawing current time indicator
            Point nowPoint = getPointOnTheCircleCircumference(radius, nowAngle, centerX, centerY);
            canvas.drawLine(centerX, centerY, nowPoint.x, nowPoint.y, mDialPaint);

            // drawing center dot
            canvas.drawCircle(centerX, centerY, getPixelsForDips(5), mDialPaint);

            // drawing hour markers
            float markerLength = getPixelsForDips(10);
            canvas.drawLine(centerX, height, centerX, height - markerLength, mDialPaint);
            canvas.drawLine(centerX, 0, centerX, markerLength, mDialPaint);
            canvas.drawLine(width, centerY, width - markerLength, centerY, mDialPaint);
            canvas.drawLine(0, centerY, markerLength, centerY, mDialPaint);

            if (!isInAmbientMode()) {
                // only draw this stuff in interactive mode.
                //Log.d(TAG, "We are not in ambient mode");
            } else {
                //Log.d(TAG, "We are in ambient mode");
            }
        }

        @Override
        public void onVisibilityChanged(boolean visible) {
            super.onVisibilityChanged(visible);
            /* the watch face became visible or invisible */
            if (visible) {
                registerReceiver();

                // Update time zone in case it changed while we weren't visible.
                mTime.clear(TimeZone.getDefault().getID());
                mTime.setToNow();
            } else {
                unregisterReceiver();
            }

            // Whether the timer should be running depends on whether we're visible and
            // whether we're in ambient mode), so we may need to start or stop the timer
            updateTimer();
        }

        /* handler to update the time once a second in interactive mode */
        final Handler mUpdateTimeHandler = new Handler() {
            @Override
            public void handleMessage(Message message) {
                switch (message.what) {
                    case MSG_UPDATE_TIME:
                        invalidate();
                        if (shouldTimerBeRunning()) {
                            long timeMs = System.currentTimeMillis();
                            long delayMs = INTERACTIVE_UPDATE_RATE_MS
                                    - (timeMs % INTERACTIVE_UPDATE_RATE_MS);
                            mUpdateTimeHandler
                                    .sendEmptyMessageDelayed(MSG_UPDATE_TIME, delayMs);
                        }
                        break;
                }
            }
        };

        /* receiver to update the time zone */
        final BroadcastReceiver mTimeZoneReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                mTime.clear(intent.getStringExtra("time-zone"));
                mTime.setToNow();
            }
        };

        private void updateTimer() {
            mUpdateTimeHandler.removeMessages(MSG_UPDATE_TIME);
            if (shouldTimerBeRunning()) {
                mUpdateTimeHandler.sendEmptyMessage(MSG_UPDATE_TIME);
            }
        }

        private boolean shouldTimerBeRunning() {
            return isVisible() && !isInAmbientMode();
        }

        boolean mRegisteredTimeZoneReceiver = false;

        private void registerReceiver() {
            if (mRegisteredTimeZoneReceiver) {
                return;
            }
            mRegisteredTimeZoneReceiver = true;
            IntentFilter filter = new IntentFilter(Intent.ACTION_TIMEZONE_CHANGED);
            PieWatchFaceService.this.registerReceiver(mTimeZoneReceiver, filter);
        }

        private void unregisterReceiver() {
            if (!mRegisteredTimeZoneReceiver) {
                return;
            }
            mRegisteredTimeZoneReceiver = false;
            PieWatchFaceService.this.unregisterReceiver(mTimeZoneReceiver);
        }
    }
}
