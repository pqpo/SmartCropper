package me.pqpo.smartcropperlib.utils;

import android.graphics.Point;

/**
 * Created by qiulinmin on 8/3/17.
 */

public class CropUtils {

    public static double getPointsDistance(Point p1, Point p2) {
        return Math.sqrt(Math.pow(p1.x - p2.x, 2) + Math.pow(p1.y - p2.y, 2));
    }

}
