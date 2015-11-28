package pie.watchface;

import android.graphics.Canvas;
import android.graphics.Rect;
import android.os.Bundle;
import android.support.wearable.watchface.CanvasWatchFaceService;
import android.support.wearable.watchface.WatchFaceStyle;
import android.util.Log;
import android.view.SurfaceHolder;

/**
 * Created by Yannick Grossard on 13/04/15.
 */
public class PieWatchFaceService extends CanvasWatchFaceService {

    public static final String TAG = PieWatchFaceService.class.getSimpleName();

    @Override
    public PieWatchFaceEngine onCreateEngine() {
        Log.i(TAG, "onCreateEngine()");
        return new PieWatchFaceEngine();
    }


    // implement service callback methods
    class PieWatchFaceEngine extends CanvasWatchFaceService.Engine {

        public final String TAG = PieWatchFaceEngine.class.getSimpleName();

        private PieWatchFace mWatchFace;

        @Override
        public void onCreate(SurfaceHolder holder) {
            super.onCreate(holder);

            // initialize the watch face & configure the system UI
            setWatchFaceStyle(new WatchFaceStyle.Builder(PieWatchFaceService.this)
                    .setCardPeekMode(WatchFaceStyle.PEEK_MODE_SHORT)
                    .setBackgroundVisibility(WatchFaceStyle.BACKGROUND_VISIBILITY_INTERRUPTIVE)
                    .setShowSystemUiTime(false)
                    .build());

            mWatchFace = new PieWatchFace(PieWatchFaceService.this);
        }

        @Override
        public void onPropertiesChanged(Bundle properties) {
            super.onPropertiesChanged(properties);

            // get device features (burn-in, low-bit ambient)
            mWatchFace.setLowBitAmbientMode(properties.getBoolean(PROPERTY_LOW_BIT_AMBIENT, false));
            mWatchFace.setBurnInProtectionMode(properties.getBoolean(PROPERTY_BURN_IN_PROTECTION, false));
        }

        @Override
        public void onTimeTick() {
            super.onTimeTick();
            invalidate();
        }

        @Override
        public void onAmbientModeChanged(boolean inAmbientMode) {
            super.onAmbientModeChanged(inAmbientMode);

            Log.i(TAG, "onAmbientModeChanged(), inAmbientMode: " + inAmbientMode);

            invalidate();
        }

        @Override
        public void onDraw(Canvas canvas, Rect bounds) {
            super.onDraw(canvas, bounds);

            Log.i(TAG, "onDraw() canvas draw, bounds: " + bounds);

            mWatchFace.draw(canvas, bounds, getPeekCardPosition(), isInAmbientMode());
        }

        @Override
        public void onPeekCardPositionUpdate(Rect rect) {
            super.onPeekCardPositionUpdate(rect);
            // if the clock is sitting in ambient mode, and a peek card notification pops up
            // the peek card background isn't drawn properly, so here we catch the update and
            // invalidate the current frame right away to redraw
            invalidate();
        }

        @Override
        public void onVisibilityChanged(boolean visible) {
            super.onVisibilityChanged(visible);


            Log.i(TAG, "onVisibilityChanged(), visible:" + visible);

            /* the watch face became visible or invisible */
            invalidate();
        }

    }
}
