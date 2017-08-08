//
// Created by qiulinmin on 8/1/17.
//
#include <Scanner.h>

using namespace scanner;
using namespace cv;

static bool sortByArea(const vector<Point> &v1, const vector<Point> &v2) {
    double v1Area = fabs(contourArea(Mat(v1)));
    double v2Area = fabs(contourArea(Mat(v2)));
    return v1Area > v2Area;
}

Scanner::Scanner(cv::Mat& bitmap) {
    srcBitmap = bitmap;
}

Scanner::~Scanner() {
}

vector<Point> Scanner::scanPoint() {
    Mat image = resizeImage();
    Mat scanImage = preprocessImage(image);
    vector<vector<Point>> contours;
    findContours(scanImage, contours, RETR_EXTERNAL, CHAIN_APPROX_NONE);
    std::sort(contours.begin(), contours.end(), sortByArea);
    vector<Point> result;
    if (contours.size() > 0) {
        vector<Point> contour = contours[0];
        double arc = arcLength(contour, true);
        vector<Point> outDP;
        approxPolyDP(Mat(contour), outDP, 0.02*arc, true);
        vector<Point> selectedPoints = selectPoints(outDP, 1);
        if (selectedPoints.size() != 4) {
            RotatedRect rect = minAreaRect(contour);
            Point2f p[4];
            rect.points(p);
            result.push_back(p[0]);
            result.push_back(p[1]);
            result.push_back(p[2]);
            result.push_back(p[3]);
        } else {
            result = selectedPoints;
        }
        for(Point &p : result) {
            p.x *= resizeScale;
            p.y *= resizeScale;
        }
    }
    return sortPointClockwise(result);
}

Mat Scanner::resizeImage() {
    int width = srcBitmap.cols;
    int height = srcBitmap.rows;
    int maxSize = width > height? width : height;
    if (maxSize > resizeThreshold) {
        resizeScale = 1.0f * maxSize / resizeThreshold;
        width = static_cast<int>(width / resizeScale);
        height = static_cast<int>(height / resizeScale);
        Size size(width, height);
        Mat resizedBitmap(size, CV_8UC3);
        resize(srcBitmap, resizedBitmap, size);
        return resizedBitmap;
    }
    return srcBitmap;
}

Mat Scanner::preprocessImage(Mat& image) {
    Mat grayMat;
    cvtColor(image, grayMat, CV_BGR2GRAY);
    Mat blurMat;
    GaussianBlur(grayMat, blurMat, Size(5,5), 0);
    Mat cannyMat;
    Canny(blurMat, cannyMat, 0, 5);
    Mat thresholdMat;
    threshold(cannyMat, thresholdMat, 0, 255, CV_THRESH_OTSU);
    return cannyMat;
}

vector<Point> Scanner::selectPoints(vector<Point> points, int selectTimes) {
    if (points.size() > 4) {
        double arc = arcLength(points, true);
        vector<Point>::iterator itor = points.begin();
        while (itor != points.end()) {
            if (points.size() == 4) {
                return points;
            }
            Point& p = *itor;
            if (itor != points.begin()) {
                Point& lastP = *(itor - 1);
                double pointLength = sqrt(pow((p.x-lastP.x),2) + pow((p.y-lastP.y),2));
                if(pointLength < arc * 0.01 * selectTimes && points.size() > 4) {
                    itor = points.erase(itor);
                    continue;
                }
            }
            itor++;
        }
        if (points.size() > 4) {
            return selectPoints(points, selectTimes + 1);
        }
    }
    return points;
}

vector<Point> Scanner::sortPointClockwise(vector<Point> points) {
    if (points.size() != 4) {
        return points;
    }

    Point unFoundPoint;
    vector<Point> result = {unFoundPoint, unFoundPoint, unFoundPoint, unFoundPoint};

    long minDistance = -1;
    for(Point &point : points) {
        long distance = point.x * point.x + point.y * point.y;
        if(minDistance == -1 || distance < minDistance) {
            result[0] = point;
            minDistance = distance;
        }
    }
    if (result[0] != unFoundPoint) {
        Point &leftTop = result[0];
        points.erase(std::remove(points.begin(), points.end(), leftTop));
        if ((pointSideLine(leftTop, points[0], points[1]) * pointSideLine(leftTop, points[0], points[2])) < 0) {
            result[2] = points[0];
        } else if ((pointSideLine(leftTop, points[1], points[0]) * pointSideLine(leftTop, points[1], points[2])) < 0) {
            result[2] = points[1];
        } else if ((pointSideLine(leftTop, points[2], points[0]) * pointSideLine(leftTop, points[2], points[1])) < 0) {
            result[2] = points[2];
        }
    }
    if (result[0] != unFoundPoint && result[2] != unFoundPoint) {
        Point &leftTop = result[0];
        Point &rightBottom = result[2];
        points.erase(std::remove(points.begin(), points.end(), rightBottom));
        if (pointSideLine(leftTop, rightBottom, points[0]) > 0) {
            result[1] = points[0];
            result[3] = points[1];
        } else {
            result[1] = points[1];
            result[3] = points[0];
        }
    }

    if (result[0] != unFoundPoint && result[1] != unFoundPoint && result[2] != unFoundPoint && result[3] != unFoundPoint) {
        return result;
    }

    return points;
}

long long Scanner::pointSideLine(Point &lineP1, Point &lineP2, Point &point) {
    long x1 = lineP1.x;
    long y1 = lineP1.y;
    long x2 = lineP2.x;
    long y2 = lineP2.y;
    long x = point.x;
    long y = point.y;
    return (x - x1)*(y2 - y1) - (y - y1)*(x2 - x1);
}



