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
//                    lastP.x = (p.x + lastP.x)/2;
//                    lastP.y = (p.y + lastP.y)/2;
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
    if (points.size() == 0) {
        return points;
    }
    Point *leftTop  = nullptr;
    Point *rightBottom = nullptr;

    Point *leftBottom = nullptr;
    Point *rightTop = nullptr;

    int minDistance = -1;
    int maxDistance = -1;
    for(Point &point : points) {
        int distance = point.x * point.x + point.y * point.y;
        if(minDistance == -1 || distance < minDistance) {
            leftTop = &point;
            minDistance = distance;
        }
        if(maxDistance == -1 || distance > maxDistance) {
            rightBottom = &point;
            maxDistance = distance;
        }
    }

    if (leftTop != nullptr && rightBottom != nullptr) {
        int x1 = (*leftTop).x;
        int y1 = (*leftTop).y;
        int x2 = (*rightBottom).x;
        int y2 = (*rightBottom).y;

        for(Point &point : points) {
            if (&point == leftTop || &point == rightBottom) {
                continue;
            }
            int x = point.x;
            int y = point.y;
            if (((y1 - y2)*x + (x2 - x1)*y + x1 * y2 - x2 * y1) > 0) {
                leftBottom = &point;
            } else {
                rightTop = &point;
            }
        }
    }

    if (leftTop != nullptr && rightBottom != nullptr && leftBottom != nullptr && rightTop != nullptr) {
        return {*leftTop, *rightTop, *rightBottom, *leftBottom};
    }

    return points;
}



