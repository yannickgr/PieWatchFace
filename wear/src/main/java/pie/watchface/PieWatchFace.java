package pie.watchface;

import android.content.Context;
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
import android.support.annotation.NonNull;
import android.util.Log;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

/**
 * Created by ghans on 11/24/15.
 */
public class PieWatchFace {

    private static final String TAG = PieWatchFace.class.getSimpleName();
    private Context mContext;

    // graphic objects
    Bitmap mBackgroundBitmap;
    Bitmap mBackgroundScaledBitmap;
    Paint mPiePaint;
    Paint mTextPaint;
    Paint mDialPaint;

    @SuppressWarnings("unused")
    private boolean mLowBitAmbientMode;

    @SuppressWarnings("unused")
    private boolean mBurnInProtectionMode;

    public PieWatchFace(Context context) {
        mContext = context;

        // load the background image
        Drawable backgroundDrawable = context.getResources().getDrawable(R.drawable.bg, null);
        if (backgroundDrawable != null)
            mBackgroundBitmap = ((BitmapDrawable) backgroundDrawable).getBitmap();

        readyPaintBrushes();
    }

    public void draw(Canvas canvas, Rect bounds, Rect peekCardBounds, boolean ambientMode) {
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

        if (ambientMode && (mLowBitAmbientMode || mBurnInProtectionMode)) {
//            canvas.drawColor(Color.BLACK);
            Log.i(TAG, "low-bit ambient mode");
        } else if (ambientMode) {
//            canvas.drawBitmap(mGrayBackgroundBitmap, 0, 0, mBackgroundPaint);
            Log.i(TAG, "normal ambient mode");
        } else {
//            canvas.drawBitmap(mBackgroundBitmap, 0, 0, mBackgroundPaint);
            Log.i(TAG, "normal mode");
        }

        float centerX = width / 2f;
        float centerY = height / 2f;
        RectF boundsF = new RectF(bounds);
        double radius = width / 2;

        // get the current time to find out the current angle
        Date now = new Date();
        int nowMinutes = PieUtils.getDateInMinutes(now);
        float nowAngle = PieUtils.getAngleForDate(now, true);

        if (!ambientMode)
            drawCalEvents(canvas, centerX, centerY, boundsF, radius, nowMinutes, nowAngle);

        // drawing current time indicator
        Point nowPoint = PieUtils.getPointOnTheCircleCircumference(radius, nowAngle, centerX, centerY);
        canvas.drawLine(centerX, centerY, nowPoint.x, nowPoint.y, mDialPaint);

        // drawing center dot
        canvas.drawCircle(centerX, centerY, PieUtils.getPixelsForDips(mContext, 5), mDialPaint);

        // drawing hour markers
        float markerLength = PieUtils.getPixelsForDips(mContext, 10);
        canvas.drawLine(centerX, height, centerX, height - markerLength, mDialPaint);
        canvas.drawLine(centerX, 0, centerX, markerLength, mDialPaint);
        canvas.drawLine(width, centerY, width - markerLength, centerY, mDialPaint);
        canvas.drawLine(0, centerY, markerLength, centerY, mDialPaint);

        if (ambientMode) {
            Paint bgPaint = new Paint();
            bgPaint.setColor(Color.BLACK);
            canvas.drawRect(peekCardBounds, bgPaint);

            Paint linePaint = new Paint();
            linePaint.setColor(Color.WHITE);
            canvas.drawLine(peekCardBounds.left, peekCardBounds.top, peekCardBounds.right, peekCardBounds.top, linePaint);
        }
    }

    private void drawCalEvents(Canvas canvas, float centerX, float centerY, RectF boundsF, double radius, int nowMinutes, float nowAngle) {
        List<CalendarEvent> events = getCalendarEvents();


        // for easy use, the primary positions of the dial
        final int DIAL_12_OCLOCK = 270;
        final int DIAL_3_OCLOCK = 0;
        final int DIAL_3_OCLOCK_ALT = 360;
        final int DIAL_6_OCLOCK = 90;
        final int DIAL_9_OCLOCK = 180;

        // assume the start is the current time
        float baselineAngle = nowAngle;

        // looping through the stored events
        for (CalendarEvent event : events) {
//                Log.i(TAG, event.toString());

            if (event.AllDay) {
                //TODO: These should be added just behind the current time indicator, and be "dragged along" for the rest of the day. The downside is we lose some of our events horizon.
            } else {
                int startMinutes = PieUtils.getDateInMinutes(event.Start);
                int endMinutes = PieUtils.getDateInMinutes(event.End);
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
                Point endPoint = PieUtils.getPointOnTheCircleCircumference(radius, endAngle, centerX, centerY);
                Point startPoint = PieUtils.getPointOnTheCircleCircumference(radius, startAngle, centerX, centerY);

                float vOffset;
                float hOffset;

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
                    vOffset = PieUtils.getPixelsForDips(mContext, -5);
                    hOffset = PieUtils.getPixelsForDips(mContext, -5);
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
                    vOffset = PieUtils.getPixelsForDips(mContext, 20);
                    hOffset = PieUtils.getPixelsForDips(mContext, 5);
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
    }

    private void readyPaintBrushes() {
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
    }

    @NonNull
    private List<CalendarEvent> getCalendarEvents() {
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

        start.set(Calendar.HOUR_OF_DAY, 6);
        start.set(Calendar.MINUTE, 0);
        start.set(Calendar.SECOND, 0);
        end.set(Calendar.HOUR_OF_DAY, 7);
        end.set(Calendar.MINUTE, 0);
        end.set(Calendar.SECOND, 0);
        events.add(new CalendarEvent(1, "Running", start.getTime(), end.getTime(), 0, "Outside", false, Color.BLUE));

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
        return events;
    }

    public void setLowBitAmbientMode(boolean lowBitAmbientMode) {
        this.mLowBitAmbientMode = lowBitAmbientMode;
    }

    public void setBurnInProtectionMode(boolean burnInProtectionMode) {
        this.mBurnInProtectionMode = burnInProtectionMode;
    }
}
