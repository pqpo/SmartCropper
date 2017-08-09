package me.pqpo.smartcropperlib;

import android.graphics.Bitmap;
import android.graphics.Point;

import java.util.ArrayList;

import me.pqpo.smartcropperlib.utils.CropUtils;

/**
 * Created by qiulinmin on 8/1/17.
 */

public class SmartCropper {

    /**
     *  输入图片扫描边框顶点
     * @param srcBmp 扫描图片
     * @return 返回顶点数组，以 左上，右上，右下，左下排序
     */
    public static Point[] scan(Bitmap srcBmp) {
        if (srcBmp == null) {
            throw new IllegalArgumentException("srcBmp cannot be null");
        }
        Point[] outPoints = new Point[4];
        nativeScan(srcBmp, outPoints);
        return outPoints;
    }

    /**
     * 裁剪图片
     * @param srcBmp 待裁剪图片
     * @param cropPoints 裁剪区域顶点，顶点坐标以图片大小为准
     * @return 返回裁剪后的图片
     */
    public static Bitmap crop(Bitmap srcBmp, Point[] cropPoints) {
        if (srcBmp == null || cropPoints == null) {
            throw new IllegalArgumentException("srcBmp and cropPoints cannot be null");
        }
        if (cropPoints.length != 4) {
            throw new IllegalArgumentException("The length of cropPoints must be 4 , and sort by leftTop, rightTop, rightBottom, leftBottom");
        }
        Point leftTop = cropPoints[0];
        Point rightTop = cropPoints[1];
        Point rightBottom = cropPoints[2];
        Point leftBottom = cropPoints[3];

        int cropWidth = (int) ((CropUtils.getPointsDistance(leftTop, rightTop)
                + CropUtils.getPointsDistance(leftBottom, rightBottom))/2);
        int cropHeight = (int) ((CropUtils.getPointsDistance(leftTop, leftBottom)
                + CropUtils.getPointsDistance(rightTop, rightBottom))/2);

        Bitmap cropBitmap = Bitmap.createBitmap(cropWidth, cropHeight, Bitmap.Config.ARGB_8888);
        SmartCropper.nativeCrop(srcBmp, cropPoints, cropBitmap);
        return cropBitmap;
    }



    private static native void nativeScan(Bitmap srcBitmap, Point[] outPoints);

    private static native void nativeCrop(Bitmap srcBitmap, Point[] points, Bitmap outBitmap);

    static {
        System.loadLibrary("smart_cropper");
    }

}
