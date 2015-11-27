package pie.watchface;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.SweepGradient;
import android.graphics.Typeface;

import java.util.Date;

/**
 * Created by ghans on 11/24/15.
 */
public class PieWatchFace {

    public static final String TAG = PieWatchFace.class.getSimpleName();


    private Context mContext;

    @SuppressWarnings("unused")
    private boolean mLowBitAmbientMode;

    @SuppressWarnings("unused")
    private boolean mBurnInProtectionMode;

    private Bitmap mBackgroundImg;

    // all paint brushes
    private Paint mPiePaint;
    private Paint mTextPaint;
    private Paint mDialPaint;
    private Paint mDotPaint;
    private Paint mHorizonPaint;
    private Paint mTimeLeftTextPaint;

    // watchface variables calculated on every draw() call
    private Canvas mCanvas;
    private Rect mWatchFaceBounds;
    private boolean mAmbientMode;
    private float mCurrentAngle;
    private PointF mWatchFaceCenter;


    public PieWatchFace(Context context) {
        this.mContext = context;
        createPaintBrushes();
    }

    public void draw(Canvas canvas, Rect watchFaceBounds, Rect peekCardBounds, boolean ambientMode) {
        // TODO: account for screen bounds, otherwise, events get drawn outside the screen view

        /**
         * ----- LAYERS ( drawing in reverse order ) ----- *
         * 0. Ambient mode Peek card overlay
         * 1. Hour markers and center dot plus time arm (basic clock)
         * 2. Events (all day, then normal events)
         * 3. BG Image
         */

        this.mCanvas = canvas;
        this.mWatchFaceBounds = watchFaceBounds;
        this.mAmbientMode = ambientMode;
        this.mCurrentAngle = PieUtils.getAngleForDate(new Date(), true);
        this.mWatchFaceCenter = new PointF(mWatchFaceBounds.exactCenterX(), mWatchFaceBounds.exactCenterY());


        drawBgImage();

        drawEvents();

        drawHorizon();

        drawBasicClock();

        drawPeekCardBounds(peekCardBounds);
    }

    private void drawBasicClock() {

        int width = mWatchFaceBounds.width();
        int height = mWatchFaceBounds.height();

        double radius = width / 2;

        // drawing current time indicator
        Point nowPoint = PieUtils.getPointOnTheCircleCircumference(radius, mCurrentAngle, mWatchFaceCenter.x, mWatchFaceCenter.y);
        mCanvas.drawLine(mWatchFaceCenter.x, mWatchFaceCenter.y, nowPoint.x, nowPoint.y, mDialPaint);

        // drawing center dot
        mCanvas.drawCircle(mWatchFaceCenter.x, mWatchFaceCenter.y, PieUtils.getPixelsForDips(mContext, 5), mDotPaint);

        // drawing hour markers
        float markerLength = PieUtils.getPixelsForDips(mContext, 10);
        mCanvas.drawLine(mWatchFaceCenter.x, height, mWatchFaceCenter.x, height - markerLength, mDialPaint);
        mCanvas.drawLine(mWatchFaceCenter.x, 0, mWatchFaceCenter.x, markerLength, mDialPaint);
        mCanvas.drawLine(width, mWatchFaceCenter.y, width - markerLength, mWatchFaceCenter.y, mDialPaint);
        mCanvas.drawLine(0, mWatchFaceCenter.y, markerLength, mWatchFaceCenter.y, mDialPaint);
    }

    private void drawPeekCardBounds(Rect peekCardBounds) {
        if (mAmbientMode) {
            Paint bgPaint = new Paint();
            bgPaint.setColor(Color.BLACK);
            mCanvas.drawRect(peekCardBounds, bgPaint);

            Paint linePaint = new Paint();
            linePaint.setColor(Color.WHITE);
            mCanvas.drawLine(peekCardBounds.left, peekCardBounds.top, peekCardBounds.right, peekCardBounds.top, linePaint);
        }
    }

    private void drawEvents() {
        if (mAmbientMode)
            return;

        RectF floatingPointBounds = new RectF(mWatchFaceBounds);

        int nowMinutes = PieUtils.getDateInMinutes(new Date());

        for (CalendarEvent event : CalendarEvent.allEvents()) {
            mPiePaint.setColor(event.Color);

            float nowAngle = PieUtils.getAngleForMinutes(nowMinutes);
            int startMinutes = PieUtils.getDateInMinutes(event.Start);
            int endMinutes = PieUtils.getDateInMinutes(event.End);

            // draw piece background
            if (nowMinutes > startMinutes && nowMinutes < endMinutes) {
                // we are on this event
                mCanvas.drawArc(floatingPointBounds, nowAngle, event.durationInDegrees - (nowAngle - event.startAngle), true, mPiePaint);
            } else {
                // normal future event
                mCanvas.drawArc(floatingPointBounds, event.startAngle, event.durationInDegrees, true, mPiePaint);
            }

            // draw event text
            double radius = mWatchFaceBounds.width() / 2;
            Point endPoint = PieUtils.getPointOnTheCircleCircumference(radius, event.endAngle, mWatchFaceCenter.x, mWatchFaceCenter.y);
            Point startPoint = PieUtils.getPointOnTheCircleCircumference(radius, event.startAngle, mWatchFaceCenter.x, mWatchFaceCenter.y);

            // title text
            Path eventTitlePath = new Path();
            float titleTextVOffset;
            float titleTextHOffset;
            Point edgePoint;

            if (event.drawTitleOnStartingEdge) {
                edgePoint = startPoint;

                if ((event.startAngle >= 270 && event.startAngle <= 360) || (event.startAngle >= 0 && event.startAngle < 90)) {
                    // drawing text on the starting edge when you're on the first half of circle
                    mTextPaint.setTextAlign(Paint.Align.RIGHT);
                    eventTitlePath.moveTo(mWatchFaceCenter.x, mWatchFaceCenter.y);
                    eventTitlePath.lineTo(edgePoint.x, edgePoint.y);
                    titleTextVOffset = PieUtils.getPixelsForDips(mContext, 15);
                    titleTextHOffset = PieUtils.getPixelsForDips(mContext, -5);
                } else {
                    // drawing text on the starting edge when you're on the second half of circle
                    mTextPaint.setTextAlign(Paint.Align.LEFT);
                    eventTitlePath.moveTo(edgePoint.x, edgePoint.y);
                    eventTitlePath.lineTo(mWatchFaceCenter.x, mWatchFaceCenter.y);
                    titleTextVOffset = PieUtils.getPixelsForDips(mContext, -5);
                    titleTextHOffset = PieUtils.getPixelsForDips(mContext, 5);
                }
            } else {
                edgePoint = endPoint;

                if (event.endAngle >= 90 && event.endAngle < 270) {
                    // drawing text on the ending edge when you're on the second half of circle
                    eventTitlePath.moveTo(edgePoint.x, edgePoint.y);
                    eventTitlePath.lineTo(mWatchFaceCenter.x, mWatchFaceCenter.y);
                    mTextPaint.setTextAlign(Paint.Align.LEFT);
                    titleTextVOffset = PieUtils.getPixelsForDips(mContext, 15);
                    titleTextHOffset = PieUtils.getPixelsForDips(mContext, 5);
                } else {
                    // drawing text on the ending edge when you're on the first half of circle
                    eventTitlePath.moveTo(mWatchFaceCenter.x, mWatchFaceCenter.y);
                    eventTitlePath.lineTo(edgePoint.x, edgePoint.y);
                    mTextPaint.setTextAlign(Paint.Align.RIGHT);
                    titleTextVOffset = PieUtils.getPixelsForDips(mContext, -5);
                    titleTextHOffset = PieUtils.getPixelsForDips(mContext, -5);
                }
            }

            int[] colors = {Color.WHITE, event.Color, Color.TRANSPARENT};
            float[] positions = {0.6f, 0.8f, 0.8f};
            LinearGradient textGradient = new LinearGradient(edgePoint.x, edgePoint.y, mWatchFaceCenter.x, mWatchFaceCenter.y, colors, positions, Shader.TileMode.MIRROR);
            mTextPaint.setShader(textGradient);

            mCanvas.drawTextOnPath(event.Title, eventTitlePath, titleTextHOffset, titleTextVOffset, mTextPaint);
        }


//        double radius = mWatchFaceBounds.width() / 2;
//
//        int nowMinutes = PieUtils.getDateInMinutes(new Date());
//
//        // assume the start is the current time
//        float baselineAngle = mCurrentAngle;
//
//        // looping through the stored events
//        for (CalendarEvent event : events) {
//
//            //TODO: for all day events, these should be added just behind the current time indicator, and be "dragged along" for the rest of the day. The downside is we lose some of our events horizon.
//
//            // draw background piece
//            mPiePaint.setColor(event.Color);
//            mCanvas.drawArc(floatingPointBounds, event.startAngle, event.durationInDegrees, true, mPiePaint);
//
//
//            /*if (nowMinutes > startMinutes &&
//                    nowMinutes < endMinutes &&
//                    baselineAngle == mCurrentAngle) {
//                // we are currently in progress of this event, use the start as baseline
//                baselineAngle = startAngle;
//
//                // cut the duration short
//                duration = duration - (mCurrentAngle - startAngle);
//
//                // drawing the pie piece from now for the rest of the duration
//                mCanvas.drawArc(boundsF, mCurrentAngle, duration, true, mPiePaint);
//            } else {
//                // drawing the pie piece fully
//            }*/
//
//            /*// drawing the title inside the pie piece
//            int MIN_DEG_FOR_TITLE = 12;
//
//            // draw Text
//            if (duration >= MIN_DEG_FOR_TITLE) {
//                Path path = new Path();
//                Path timePath = new Path();
//
//                Point endPoint = PieUtils.getPointOnTheCircleCircumference(radius, endAngle, mWatchFaceCenter.x, mWatchFaceCenter.y);
//                Point startPoint = PieUtils.getPointOnTheCircleCircumference(radius, startAngle, mWatchFaceCenter.x, mWatchFaceCenter.y);
//
//                float titleTextVOffset;
//                float titleTextHOffset;
//                float timeTextVOffset;
//                float timeTextHOffset;
//
//                //Log.d(TAG, "Event " + event.Title + ", start angle " + startAngle + " - end angle " + endAngle);
//                if (endAngle <= POS_DIAL_6_OCLOCK
//                        || (endAngle <= POS_DIAL_3_OCLOCK_ALT && endAngle > POS_DIAL_12_OCLOCK)) {
//                    // draw the text at the end of the slice
//                    mTextPaint.setTextAlign(Paint.Align.RIGHT);
//                    path.moveTo(mWatchFaceCenter.x, mWatchFaceCenter.y);
//                    path.lineTo(endPoint.x, endPoint.y);
//                    titleTextVOffset = PieUtils.getPixelsForDips(mContext, -5);
//                    titleTextHOffset = PieUtils.getPixelsForDips(mContext, -5);
//
//                    mTimeLeftTextPaint.setTextAlign(Paint.Align.RIGHT);
//
//                    timePath.moveTo(mWatchFaceCenter.x, mWatchFaceCenter.y);
//                    timePath.lineTo(startPoint.x, startPoint.y);
//
//                    timeTextVOffset = PieUtils.getPixelsForDips(mContext, 13);
//                    timeTextHOffset = PieUtils.getPixelsForDips(mContext, -5);
//
//                } else if (endAngle <= POS_DIAL_12_OCLOCK) {
//
//                    // draw the text at the end of the slice
//                    mTextPaint.setTextAlign(Paint.Align.LEFT);
//                    path.moveTo(endPoint.x, endPoint.y);
//                    path.lineTo(mWatchFaceCenter.x, mWatchFaceCenter.y);
//                    titleTextVOffset = PieUtils.getPixelsForDips(mContext, 20);
//                    titleTextHOffset = PieUtils.getPixelsForDips(mContext, 5);
//
//                    mTimeLeftTextPaint.setTextAlign(Paint.Align.LEFT);
//
//                    timePath.moveTo(startPoint.x, startPoint.y);
//                    timePath.lineTo(mWatchFaceCenter.x, mWatchFaceCenter.y);
//
//                    timeTextVOffset = PieUtils.getPixelsForDips(mContext, -5);
//                    timeTextHOffset = PieUtils.getPixelsForDips(mContext, 5);
//
//                } else {
//                    // draw the text at the beginning of the slice
//                    //mTitlePaint.setTextAlign(Paint.Align.LEFT);
//                    //path.moveTo(startPoint.x, startPoint.y);
//                    //path.lineTo(centerX, centerY);
//                    Log.d(TAG, "Can't determine pie title placing. Start angle is " + startAngle + " and end angle is " + endAngle);
//
//                    // FIXME: 11/26/15 it would be much better to skip drawing event rather than crashing the whole app
//                    throw new IllegalArgumentException("We do not know where to place the title of this appointment");
//                }
//
//                mCanvas.drawTextOnPath(event.Title, path, titleTextHOffset, titleTextVOffset, mTextPaint);
//
//                // TODO: 11/26/15 stop drawing when event has started, aka 'in 0min'
//                // TODO: 11/26/15 only draw this for next upcoming event
//                mCanvas.drawTextOnPath("in 38min", timePath
//                        , timeTextHOffset
//                        , timeTextVOffset
//                        , mTimeLeftTextPaint);
//
//            }*/
//
//        } // for each event
    }

    public void drawBgImage() {
        if (mBackgroundImg == null)
            mBackgroundImg = Bitmap.createScaledBitmap(
                    BitmapFactory.decodeResource(mContext.getResources(), R.drawable.bg)
                    , mWatchFaceBounds.width()
                    , mWatchFaceBounds.height()
                    , true /* filter */
            );

        mCanvas.drawBitmap(mBackgroundImg, 0, 0, null);
    }

    private void drawHorizon() {
        // drawing horizon separator
        int horizonSeparatorLength = 40;
        int[] colors = {Color.TRANSPARENT, Color.BLACK};
        float[] positions = {(mCurrentAngle - horizonSeparatorLength) / 360f, mCurrentAngle / 360f};
        SweepGradient horizonGradient = new SweepGradient(mWatchFaceCenter.x, mWatchFaceCenter.y, colors, positions);
        mHorizonPaint.setShader(horizonGradient);

        mCanvas.drawArc(new RectF(mWatchFaceBounds)
                , mCurrentAngle - horizonSeparatorLength
                , horizonSeparatorLength
                , true
                , mHorizonPaint);
    }

    private void createPaintBrushes() {
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
        Typeface plain = Typeface.create("sans-serif-condensed", Typeface.NORMAL);
        mTextPaint.setTypeface(plain);

        // the brush used to paint the time left till next event piece
        mTimeLeftTextPaint = new Paint(mTextPaint);
        mTimeLeftTextPaint.setTextSize(19);
        mTimeLeftTextPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.ITALIC));

        // the brush used to paint the hour markers and current time marker
        mDialPaint = new Paint();
        mDialPaint.setColor(Color.WHITE);
        mDialPaint.setAlpha(180);
        mDialPaint.setStrokeWidth(6.0f);
        mDialPaint.setAntiAlias(true);
        mDialPaint.setStrokeCap(Paint.Cap.ROUND);
        mDialPaint.setShadowLayer(10.0f, 0.0f, 2.0f, 0xFF000000);

        // the brush used to paint the hour markers and current time marker
        mDotPaint = new Paint();
        mDotPaint.setColor(Color.parseColor("#C9C9C9"));
        mDotPaint.setAlpha(255);
        mDotPaint.setStrokeWidth(6.0f);
        mDotPaint.setAntiAlias(true);
        mDotPaint.setStrokeCap(Paint.Cap.ROUND);
        mDotPaint.setShadowLayer(10.0f, 0.0f, 2.0f, 0xFF000000);

        // the brush used to paint the horizon separator
        mHorizonPaint = new Paint();
        mHorizonPaint.setColor(Color.BLACK);
        mHorizonPaint.setStrokeWidth(5.0f);
        mHorizonPaint.setAntiAlias(true);
        mHorizonPaint.setStrokeCap(Paint.Cap.ROUND);
    }

    public void setLowBitAmbientMode(boolean lowBitAmbientMode) {
        this.mLowBitAmbientMode = lowBitAmbientMode;
    }


    //
    // MARK: property setters
    //

    public void setBurnInProtectionMode(boolean burnInProtectionMode) {
        this.mBurnInProtectionMode = burnInProtectionMode;
    }

    // for easy use, the primary positions of the dial
    public enum Pos {
        DIAL_12_OCLOCK(270),
        DIAL_3_OCLOCK(0),
        DIAL_3_OCLOCK_ALT(360),
        DIAL_6_OCLOCK(90),
        DIAL_9_OCLOCK(180);

        final int nativeInt;

        Pos(int nativeInt) {
            this.nativeInt = nativeInt;
        }
    }

}
