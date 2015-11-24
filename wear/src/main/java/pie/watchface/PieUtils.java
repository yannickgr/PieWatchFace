package pie.watchface;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Point;
import android.util.TypedValue;

import java.util.Date;

/**
 * Created by ghans on 11/23/15.
 */
public class PieUtils {

    /**
     * Transform a given date to minutes, removing the difference for AM and PM
     *
     * @param date
     * @return
     */
    public static int getDateInMinutes(Date date) {
        int minutes = date.getMinutes();
        int hours = date.getHours();

        if (hours > 12) {
            hours = hours - 12;
        }
        minutes = minutes + (hours * 60);

        return minutes;
    }

    public static float getDegreesForMinutes(int minutes) {
        return minutes * 0.5f;
    }

    public static float getAngleForDate(Date date, boolean takeArcDrawingOffsetIntoAccount) {
        int minutes = getDateInMinutes(date);
        return getAngleForDate(minutes, takeArcDrawingOffsetIntoAccount);
    }

    private static float getAngleForDate(int minutes, boolean takeArcDrawingOffsetIntoAccount) {
        // get the angle starting from the top (12 o clock)
        float startAngle = getDegreesForMinutes(minutes);

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

    /**
     * For any angle of the circle given, get the coordinates of its point on the circumference
     *
     * @param radius
     * @param angle
     * @param centreX
     * @param centreY
     * @return
     */
    public static Point getPointOnTheCircleCircumference(double radius, double angle, float centreX, float centreY) {
        int y = (int) Math.round(centreY + radius * Math.sin(angle * Math.PI / 180f));
        int x = (int) Math.round(centreX + radius * Math.cos(angle * Math.PI / 180f));

        return new Point(x, y);
    }

    public static float getPixelsForDips(Context context, float dips) {
        Resources r = context.getResources();
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dips, r.getDisplayMetrics());
    }
}
