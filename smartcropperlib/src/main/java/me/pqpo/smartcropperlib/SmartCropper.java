package me.pqpo.smartcropperlib;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Point;

import java.io.IOException;

import me.pqpo.smartcropperlib.utils.CropUtils;

/**
 * Created by qiulinmin on 8/1/17.
 */

public class SmartCropper {

    private static ImageDetector sImageDetector = null;

    public static void buildImageDetector(Context context) {
        SmartCropper.buildImageDetector(context, null);
    }

    public static void buildImageDetector(Context context, String modelFile) {
        try {
            sImageDetector = new ImageDetector(context, modelFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     *  输入图片扫描边框顶点
     * @param srcBmp 扫描图片
     * @return 返回顶点数组，以 左上，右上，右下，左下排序
     */
    public static Point[] scan(Bitmap srcBmp) {
        if (srcBmp == null) {
            throw new IllegalArgumentException("srcBmp cannot be null");
        }
        if (sImageDetector != null) {
            Bitmap bitmap = sImageDetector.detectImage(srcBmp);
            if (bitmap != null) {
                srcBmp = Bitmap.createScaledBitmap(bitmap, srcBmp.getWidth(), srcBmp.getHeight(), false);
            }
        }
        Point[] outPoints = new Point[4];
        nativeScan(srcBmp, outPoints, sImageDetector == null);
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

        //纠正4个点的位置，将距离(0,0)最近的点作为cropPoints[0]
        double minDistance = Integer.MAX_VALUE;
        int index = -1;
        int length = cropPoints.length;
        for (int i = 0;i < length;i++) {
            double distance2 = CropUtils.getPointsDistance(cropPoints[i],new Point(0,0));
            if(minDistance > distance2){
                minDistance = distance2;
                index = i;
            }
        }
        if(index > 0){
            Point[] resultCropPoint = new Point[length];
            for (int i = 0;i<cropPoints.length;i++) {
                resultCropPoint[i] = cropPoints[(index + i) >= length ? (index+i) - length : (index+i)];
            }
            cropPoints = resultCropPoint.clone();
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



    private static native void nativeScan(Bitmap srcBitmap, Point[] outPoints, boolean canny);

    private static native void nativeCrop(Bitmap srcBitmap, Point[] points, Bitmap outBitmap);

    static {
        System.loadLibrary("smart_cropper");
    }

}
