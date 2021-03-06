package pie.watchface;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
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
import java.util.List;

/**
 * Created by ghans on 11/24/15.
 */
public class PieWatchFace {

    public static final String TAG = PieWatchFace.class.getSimpleName();

    public static final int MIN_DEG_FOR_TITLE = 15; // min degrees required for the title to be displayed on the pie piece

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
    private List<CalendarEvent> mAllCalendarEvents;


    public PieWatchFace(Context context) {
        this.mContext = context;
        createPaintBrushes();
        fetchCalendarEvents();
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
            Paint paint = new Paint();
            paint.setColor(Color.BLACK);
            mCanvas.drawRect(peekCardBounds, paint);

            paint.setColor(Color.WHITE);
            mCanvas.drawLine(peekCardBounds.left, peekCardBounds.top, peekCardBounds.right, peekCardBounds.top, paint);
        }
    }

    private void drawEvents() {

        RectF floatingPointBounds = new RectF(mWatchFaceBounds);

        int nowMinutes = PieUtils.getDateInMinutes(new Date());
        float nowAngle = PieUtils.getAngleForMinutes(nowMinutes);
        double radius = mWatchFaceBounds.width() / 2;

        for (int i = 0; i < mAllCalendarEvents.size(); i++) {
            CalendarEvent event = mAllCalendarEvents.get(i);
            mPiePaint.setColor(event.displayColor);


            int startMinutes = PieUtils.getDateInMinutes(event.startDate);
            int endMinutes = PieUtils.getDateInMinutes(event.endDate);
            float eventDuration = event.durationInDegrees;

            if (i > 0 && event.startDate.compareTo(mAllCalendarEvents.get(i - 1).startDate) == 0) {
                startMinutes += 20;
            }

            float eventStartAngle = PieUtils.getAngleForMinutes(startMinutes);

            // start and end points for the piece
            Point startPoint;
            Point endPoint = PieUtils.getPointOnTheCircleCircumference(radius, event.endAngle, mWatchFaceCenter.x, mWatchFaceCenter.y);

            // draw piece background
            if (nowMinutes > startMinutes && nowMinutes < endMinutes) {
                // we are on this event
                eventStartAngle = nowAngle;
                eventDuration = PieUtils.getDegreesForMinutes(endMinutes - nowMinutes);
                startPoint = PieUtils.getPointOnTheCircleCircumference(radius, nowAngle, mWatchFaceCenter.x, mWatchFaceCenter.y);
            } else {
                // normal future event
                startPoint = PieUtils.getPointOnTheCircleCircumference(radius, eventStartAngle, mWatchFaceCenter.x, mWatchFaceCenter.y);
            }

            if (!mAmbientMode)
                mCanvas.drawArc(floatingPointBounds, eventStartAngle, eventDuration, true, mPiePaint);

            // calculate initial text gradient (subject to change, later down the road)

            // variable fading for text when pie piece gets small
            float fading_threshold = 0.8f;
            fading_threshold = Math.min(fading_threshold, (fading_threshold / 30) * eventDuration);
            float color_threshold = 0.6f;
            color_threshold = Math.min(color_threshold, (color_threshold / 30) * eventDuration);

            if (mAmbientMode) {
                color_threshold = 0.7f;
                fading_threshold = 0.9f;
            }

            float[] positions = {color_threshold, fading_threshold, fading_threshold};
            int[] colors = {Color.WHITE, mAmbientMode ? Color.TRANSPARENT : event.displayColor, Color.TRANSPARENT};
            LinearGradient textGradient;

            // title text
            Path eventTitlePath = new Path();
            float titleTextVOffset;
            float titleTextHOffset;
            Point edgePoint;

            // time path
            Path eventTimePath = new Path();
            float timePathVOffset;
            float timePathHOffset;

            if (event.drawTitleOnStartingEdge) {
                edgePoint = startPoint;

                textGradient = new LinearGradient(edgePoint.x, edgePoint.y, mWatchFaceCenter.x, mWatchFaceCenter.y, colors, positions, Shader.TileMode.MIRROR);

                if ((event.startAngle >= 270 && event.startAngle <= 360) || (event.startAngle >= 0 && event.startAngle < 90)) {
                    // drawing text on the starting edge when you're in the first half of circle
                    mTextPaint.setTextAlign(Paint.Align.RIGHT);
                    eventTitlePath.moveTo(mWatchFaceCenter.x, mWatchFaceCenter.y);
                    eventTitlePath.lineTo(edgePoint.x, edgePoint.y);
                    titleTextVOffset = PieUtils.getPixelsForDips(mContext, 15);
                    titleTextHOffset = PieUtils.getPixelsForDips(mContext, -5);

                    float measureText = mTextPaint.measureText(event.title);
                    if (measureText > 170) {
                        mTextPaint.setTextAlign(Paint.Align.LEFT);
                        titleTextHOffset = PieUtils.getPixelsForDips(mContext, 28);

                        // invert the text gradient
                        positions = new float[]{0.8f, 1.f, 1.f};
                        textGradient = new LinearGradient(mWatchFaceCenter.x, mWatchFaceCenter.y, edgePoint.x, edgePoint.y, colors, positions, Shader.TileMode.MIRROR);
                    }

                    // drawing time text on the ending edge when you're in the first half of circle
                    mTimeLeftTextPaint.setTextAlign(Paint.Align.RIGHT);
                    eventTimePath.moveTo(mWatchFaceCenter.x, mWatchFaceCenter.y);
                    eventTimePath.lineTo(endPoint.x, endPoint.y);

                    timePathVOffset = PieUtils.getPixelsForDips(mContext, -5);
                    timePathHOffset = PieUtils.getPixelsForDips(mContext, -5);
                } else {
                    // drawing text on the starting edge when you're in the second half of circle
                    mTextPaint.setTextAlign(Paint.Align.LEFT);
                    eventTitlePath.moveTo(edgePoint.x, edgePoint.y);
                    eventTitlePath.lineTo(mWatchFaceCenter.x, mWatchFaceCenter.y);
                    titleTextVOffset = PieUtils.getPixelsForDips(mContext, -5);
                    titleTextHOffset = PieUtils.getPixelsForDips(mContext, 5);

                    // drawing time text on the ending edge when you're in the second half of circle
                    mTimeLeftTextPaint.setTextAlign(Paint.Align.LEFT);
                    eventTimePath.moveTo(endPoint.x, endPoint.y);
                    eventTimePath.lineTo(mWatchFaceCenter.x, mWatchFaceCenter.y);

                    timePathVOffset = PieUtils.getPixelsForDips(mContext, 15);
                    timePathHOffset = PieUtils.getPixelsForDips(mContext, 7);
                }
            } else {
                edgePoint = endPoint;

                textGradient = new LinearGradient(edgePoint.x, edgePoint.y, mWatchFaceCenter.x, mWatchFaceCenter.y, colors, positions, Shader.TileMode.MIRROR);

                if (event.endAngle >= 90 && event.endAngle < 270) {
                    // drawing text on the ending edge when you're in the second half of circle
                    eventTitlePath.moveTo(edgePoint.x, edgePoint.y);
                    eventTitlePath.lineTo(mWatchFaceCenter.x, mWatchFaceCenter.y);
                    mTextPaint.setTextAlign(Paint.Align.LEFT);
                    titleTextVOffset = PieUtils.getPixelsForDips(mContext, 15);
                    titleTextHOffset = PieUtils.getPixelsForDips(mContext, 5);

                    // drawing time text on the starting edge when you're in the second half of circle
                    mTimeLeftTextPaint.setTextAlign(Paint.Align.RIGHT);
                    eventTimePath.moveTo(mWatchFaceCenter.x, mWatchFaceCenter.y);
                    eventTimePath.lineTo(startPoint.x, startPoint.y);

                    timePathVOffset = PieUtils.getPixelsForDips(mContext, 15);
                    timePathHOffset = PieUtils.getPixelsForDips(mContext, 5);
                } else {
                    // drawing text on the ending edge when you're in the first half of circle
                    eventTitlePath.moveTo(mWatchFaceCenter.x, mWatchFaceCenter.y);
                    eventTitlePath.lineTo(edgePoint.x, edgePoint.y);
                    mTextPaint.setTextAlign(Paint.Align.RIGHT);
                    titleTextVOffset = PieUtils.getPixelsForDips(mContext, -5);
                    titleTextHOffset = PieUtils.getPixelsForDips(mContext, -5);

                    float measureText = mTextPaint.measureText(event.title);
                    if (measureText > 170) {
                        mTextPaint.setTextAlign(Paint.Align.LEFT);
                        titleTextHOffset = PieUtils.getPixelsForDips(mContext, 28);

                        // invert the text gradient
                        positions = new float[]{0.8f, 1.f, 1.f};
                        textGradient = new LinearGradient(mWatchFaceCenter.x, mWatchFaceCenter.y, edgePoint.x, edgePoint.y, colors, positions, Shader.TileMode.MIRROR);
                    }

                    // drawing time text on the starting edge when you're in the first half of circle
                    mTimeLeftTextPaint.setTextAlign(Paint.Align.RIGHT);
                    eventTimePath.moveTo(mWatchFaceCenter.x, mWatchFaceCenter.y);
                    eventTimePath.lineTo(startPoint.x, startPoint.y);

                    timePathVOffset = PieUtils.getPixelsForDips(mContext, 15);
                    timePathHOffset = PieUtils.getPixelsForDips(mContext, -7);
                }
            }

            mTextPaint.setShader(textGradient);

            if (mAmbientMode || eventDuration > MIN_DEG_FOR_TITLE)
                mCanvas.drawTextOnPath(event.title, eventTitlePath, titleTextHOffset, titleTextVOffset, mTextPaint);

            CalendarEvent nxt = CalendarEvent.nextEvent;
            boolean canDrawNextEventInTime = nxt != null && nxt.equals(event);
            if (!mAmbientMode && canDrawNextEventInTime && eventDuration > (MIN_DEG_FOR_TITLE * 2))
                mCanvas.drawTextOnPath(event.getInTimeString(), eventTimePath
                        , timePathHOffset
                        , timePathVOffset
                        , mTimeLeftTextPaint);
        }
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
        if (mAmbientMode)
            return;

        int horizonSeparatorLength = 40;
        int[] colors = {Color.TRANSPARENT, Color.BLACK};

        // find out at what angle the gradient needs to start
        float startAngle = (mCurrentAngle - horizonSeparatorLength);

        // if we the start is negative, it means we'll have to "go around"
        if (startAngle < 0) startAngle += 360;

        // the gradient starts at the beginning of the sweep, but has to end at the defined horizon separator length
        float[] positions = {0, horizonSeparatorLength / 360f};
        SweepGradient horizonGradient = new SweepGradient(mWatchFaceCenter.x, mWatchFaceCenter.y, colors, positions);

        // the sweep gradient doesn't like drawing starting from a value before the 0 point (3 o clock) to after,
        // that's why we need a "local" matrix for the gradient, which we can first rotate to always have our start be 0
        Matrix gradientMatrix = new Matrix();

        // we'll rotate one degree back, because for some reason there is a weird striped border at the start,
        // which we do not want to see.
        float rotateAngle = startAngle - 1;
        if (rotateAngle < 0) rotateAngle += 360;

        // do the actual rotate and assign the matrix to the gradient
        gradientMatrix.preRotate(rotateAngle, mWatchFaceCenter.x, mWatchFaceCenter.y);
        horizonGradient.setLocalMatrix(gradientMatrix);

        // assign the gradient shader to the horizon
        mHorizonPaint.setShader(horizonGradient);

        // draw the horizon arc
        mCanvas.drawArc(new RectF(mWatchFaceBounds)
                , startAngle
                , horizonSeparatorLength
                , true
                , mHorizonPaint);
    }

    /**
     * Updates the calendar events, usually should be called once every minute
     */
    public void fetchCalendarEvents() {
        this.mAllCalendarEvents = CalendarEvent.allEvents(mContext);
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
