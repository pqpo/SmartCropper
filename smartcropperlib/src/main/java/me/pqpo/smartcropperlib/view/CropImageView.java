package me.pqpo.smartcropperlib.view;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Xfermode;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.ImageView;

import me.pqpo.smartcropperlib.SmartCropper;

/**
 * Created by qiulinmin on 8/2/17.
 */
public class CropImageView extends ImageView {

    private static final int TOUCH_POINT_CATCH_DISTANCE = 12;
    private static final int POINT_RADIUS = 10;

    private float[] matrixValue = new float[9];
    private Paint mPointPaint;
    private Paint mLinePaint;
    private Paint mMaskPaint;
    private Paint mGuideLinePaint;
    private float scaleX, scaleY;
    private int actW, actH, actLeft, actTop;
    private Xfermode maskXfermode = new PorterDuffXfermode(PorterDuff.Mode.DST_OUT);
    private Path mPointLinePath = new Path();
    private float density;
    private Point mDraggingPoint = null;

    Point[] mCropPoints;
    int mMaskAlpha = 86;
    boolean mShowGuideLine = true;
    int mLineColor = 0xFF00FFFF;
    int mLineWidth = 1;

    public CropImageView(Context context) {
        this(context, null);
    }

    public CropImageView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CropImageView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        ScaleType scaleType = getScaleType();
        if (scaleType == ScaleType.FIT_END || scaleType == ScaleType.FIT_START || scaleType == ScaleType.MATRIX) {
            throw new RuntimeException("Image in CropImageView must be in center");
        }
        density = getResources().getDisplayMetrics().density;
        initPaints();
    }

    public void setCropPoints(Point[] cropPoints) {
        if (cropPoints == null || cropPoints.length != 4) {
            throw new IllegalArgumentException("The length of cropPoints must be 4 , and sort by leftTop, rightTop, rightBottom, leftBottom");
        }
        this.mCropPoints = cropPoints;
        invalidate();
    }

    public Point[] getCropPoints() {
        return mCropPoints;
    }

    public void setMaskAlpha(int mMaskAlpha) {
        mMaskAlpha = Math.min(Math.max(0, mMaskAlpha), 255);
        this.mMaskAlpha = mMaskAlpha;
        invalidate();
    }

    public void setShowGuideLine(boolean showGuideLine) {
        this.mShowGuideLine = showGuideLine;
        invalidate();
    }

    public void setLineColor(int lineColor) {
        this.mLineColor = lineColor;
        invalidate();
    }

    public void setLineWidth(int lineWidth) {
        this.mLineWidth = lineWidth;
        invalidate();
    }

    public Bitmap crop() {
        return crop(mCropPoints);
    }

    public Bitmap crop(Point[] points) {
        if (points == null || points.length != 4) {
            return null;
        }
        Bitmap bmp = getBitmap();
        return bmp == null ? null : SmartCropper.crop(bmp, points);
    }

//    public boolean canRightCrop() {
//        Point lt = mCropPoints[0];
//        Point rt = mCropPoints[1];
//        Point rb = mCropPoints[2];
//        Point lb = mCropPoints[3];
//        return (pointSideLine(lt, rb, lb) * pointSideLine(lt, rb, rt) < 0) && (pointSideLine(lb, rt, lt) * pointSideLine(lb, rt, rb) < 0);
//    }
//
//    private int pointSideLine(Point lineP1, Point lineP2, Point point) {
//        int x1 = lineP1.x;
//        int y1 = lineP1.y;
//        int x2 = lineP2.x;
//        int y2 = lineP2.y;
//        int x = point.x;
//        int y = point.y;
//        return ((y1 - y2)*x + (x2 - x1)*y + x1 * y2 - x2 * y1);
//    }

    public Bitmap getBitmap() {
        Bitmap bmp = null;
        Drawable drawable = getDrawable();
        if (drawable instanceof BitmapDrawable) {
            bmp = ((BitmapDrawable)drawable).getBitmap();
        }
        return bmp;
    }

    private void initPaints() {
        mPointPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mPointPaint.setColor(mLineColor);
        mPointPaint.setStrokeWidth(dp2px(mLineWidth));
        mPointPaint.setStyle(Paint.Style.STROKE);

        mLinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mLinePaint.setColor(mLineColor);
        mLinePaint.setStrokeWidth(dp2px(mLineWidth));
        mLinePaint.setStyle(Paint.Style.STROKE);

        mMaskPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mMaskPaint.setColor(Color.BLACK);
        mMaskPaint.setStyle(Paint.Style.FILL);

        mGuideLinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mGuideLinePaint.setColor(Color.WHITE);
        mGuideLinePaint.setStyle(Paint.Style.FILL);
        mGuideLinePaint.setStrokeWidth(1);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        getDrawablePosition();
        onDrawCropPoint(canvas);
    }

    protected void onDrawCropPoint(Canvas canvas) {
        onDrawMask(canvas);
        onDrawGuideLine(canvas);
        onDrawPoints(canvas);
        onDrawLines(canvas);
    }

    protected void onDrawGuideLine(Canvas canvas) {
        if (!mShowGuideLine) {
            return;
        }
        int widthStep = actW / 3;
        int heightStep = actH / 3;
        canvas.drawLine(actLeft + widthStep, actTop, actLeft + widthStep, actTop + actH, mGuideLinePaint);
        canvas.drawLine(actLeft + widthStep * 2, actTop, actLeft + widthStep * 2, actTop + actH, mGuideLinePaint);
        canvas.drawLine(actLeft, actTop + heightStep, actLeft + actW, actTop + heightStep, mGuideLinePaint);
        canvas.drawLine(actLeft, actTop + heightStep * 2, actLeft + actW, actTop + heightStep * 2, mGuideLinePaint);
    }

    protected void onDrawMask(Canvas canvas) {
        if (mMaskAlpha <= 0) {
            return;
        }
        Path path = resetPointPath();
        if (path != null) {
            int sc = canvas.saveLayer(actLeft, actTop, actLeft + actW, actTop + actH, mMaskPaint, Canvas.ALL_SAVE_FLAG);
            mMaskPaint.setAlpha(mMaskAlpha);
            canvas.drawRect(actLeft, actTop, actLeft + actW, actTop + actH, mMaskPaint);
            mMaskPaint.setXfermode(maskXfermode);
            mMaskPaint.setAlpha(255);
            canvas.drawPath(path, mMaskPaint);
            mMaskPaint.setXfermode(null);
            canvas.restoreToCount(sc);
        }
    }

    private Path resetPointPath() {
        if (mCropPoints == null || mCropPoints.length != 4) {
            return null;
        }
        mPointLinePath.reset();
        Point lt = mCropPoints[0];
        Point rt = mCropPoints[1];
        Point rb = mCropPoints[2];
        Point lb = mCropPoints[3];
        mPointLinePath.moveTo(getViewPointX(lt),getViewPointY(lt));
        mPointLinePath.lineTo(getViewPointX(rt),getViewPointY(rt));
        mPointLinePath.lineTo(getViewPointX(rb),getViewPointY(rb));
        mPointLinePath.lineTo(getViewPointX(lb),getViewPointY(lb));
        mPointLinePath.close();
        return mPointLinePath;
    }

    private void getDrawablePosition() {
        Drawable drawable = getDrawable();
        if (drawable != null) {
            getImageMatrix().getValues(matrixValue);
            scaleX = matrixValue[Matrix.MSCALE_X];
            scaleY = matrixValue[Matrix.MSCALE_Y];
            int origW = drawable.getIntrinsicWidth();
            int origH = drawable.getIntrinsicHeight();
            actW = Math.round(origW * scaleX);
            actH = Math.round(origH * scaleY);
            actLeft = (getWidth() - actW) / 2;
            actTop = (getHeight() - actH) / 2;
        }
    }

    protected void onDrawLines(Canvas canvas) {
        Path path = resetPointPath();
        if (path != null) {
            canvas.drawPath(path, mLinePaint);
        }
    }

    protected void onDrawPoints(Canvas canvas) {
        if (mCropPoints == null) {
            return;
        }
        for (Point point : mCropPoints) {
            canvas.drawCircle(getViewPointX(point), getViewPointY(point), dp2px(POINT_RADIUS), mPointPaint);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int action = event.getAction();
        boolean handle = true;
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                mDraggingPoint = getNearbyPoint(event);
                if (mDraggingPoint == null) {
                    handle = false;
                }
                break;
            case MotionEvent.ACTION_MOVE:
                toImagePointSize(mDraggingPoint, event);
                invalidate();
                break;
            case MotionEvent.ACTION_UP:
                mDraggingPoint = null;
                break;
        }
        return handle || super.onTouchEvent(event);
    }

    private Point getNearbyPoint(MotionEvent event) {
        if (mCropPoints == null || mCropPoints.length == 0) {
            return null;
        }
        float x = event.getX();
        float y = event.getY();
        for (Point p : mCropPoints) {
            int px = getViewPointX(p);
            int py = getViewPointY(p);
            double distance =  Math.sqrt(Math.pow(x - px, 2) + Math.pow(y - py, 2));
            if (distance < dp2px(TOUCH_POINT_CATCH_DISTANCE)) {
                return p;
            }
        }
        return null;
    }

    private void toImagePointSize(Point dragPoint, MotionEvent event) {
        if (dragPoint == null) {
            return;
        }
        float x = Math.min(Math.max(event.getX(), actLeft), actLeft + actW);
        float y = Math.min(Math.max(event.getY(), actTop), actTop + actH);
        dragPoint.x = (int) ((x - actLeft) / scaleX);
        dragPoint.y = (int) ((y - actTop) / scaleY);
    }

    private int getViewPointX(Point point){
        return (int) (point.x * scaleX + actLeft);
    }

    private int getViewPointY(Point point){
        return (int) (point.y * scaleY + actTop);
    }

    private int dp2px(float dp) {
        return (int) (dp * density + 0.5f);
    }

}
