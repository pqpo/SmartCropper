#include <Scanner.h>
#include <omp.h>

using namespace scanner;
using namespace cv;
using namespace std;

static bool sortByArea(const vector<Point> &v1, const vector<Point> &v2) {
    return fabs(contourArea(Mat(v1))) > fabs(contourArea(Mat(v2)));
}

Scanner::Scanner(cv::Mat& bitmap, bool canny) {
    srcBitmap = bitmap;
    Scanner::canny = canny;
}

Scanner::~Scanner() {
}

vector<Point> Scanner::scanPoint() {
    vector<Point> result;
    int cannyValue[] = {100, 150, 300};
    int blurValue[] = {3, 7, 11, 15};

    // Thu nhỏ ảnh
    Mat image = resizeImage();

    // Biến để lưu kết quả tạm thời
    vector<Point> tempResult;
    bool foundResult = false;

#pragma omp parallel for collapse(2)
    for (int i = 0; i < 3; i++) {
        for (int j = 0; j < 4; j++) {
            if (foundResult) continue;

            // Tiền xử lý ảnh
            Mat scanImage = preprocessedImage(image, cannyValue[i], blurValue[j]);
            vector<vector<Point>> contours;

            // Tìm contour với phương pháp tối ưu hơn
            findContours(scanImage, contours, RETR_EXTERNAL, CHAIN_APPROX_SIMPLE);

            if (!contours.empty()) {
                // Sắp xếp theo diện tích
                std::sort(contours.begin(), contours.end(), sortByArea);
                vector<Point> contour = contours[0];

                // Tối ưu hóa việc tính chu vi và đa giác xấp xỉ
                double arc = arcLength(contour, true);
                vector<Point> outDP;
                approxPolyDP(Mat(contour), outDP, 0.02 * arc, true);

                // Lọc điểm với khoảng cách tối thiểu
                vector<Point> selectedPoints = selectPoints(outDP);

                if (selectedPoints.size() == 4) {
                    // Tính toán vùng chọn nhanh hơn
                    Rect bounds = boundingRect(selectedPoints);
                    int selectArea = bounds.area();
                    int imageArea = scanImage.cols * scanImage.rows;

                    if (selectArea >= (imageArea / 20)) {
#pragma omp critical
                        {
                            if (!foundResult) {
                                tempResult = selectedPoints;
                                for (Point &p : tempResult) {
                                    p.x *= resizeScale;
                                    p.y *= resizeScale;
                                }
                                tempResult = sortPointClockwise(tempResult);
                                foundResult = true;
                            }
                        }
                    }
                }
            }
        }
    }

    if (foundResult) {
        return tempResult;
    }

    if (!isHisEqual) {
        isHisEqual = true;
        return scanPoint();
    }

    if (result.size() != 4) {
        result = {
                Point2f(0, 0),
                Point2f(image.cols, 0),
                Point2f(image.cols, image.rows),
                Point2f(0, image.rows)
        };
    }

    for (Point &p : result) {
        p.x *= resizeScale;
        p.y *= resizeScale;
    }

    return sortPointClockwise(result);
}

Mat Scanner::resizeImage() {
    int width = srcBitmap.cols;
    int height = srcBitmap.rows;
    int maxSize = std::max(width, height);

    if (maxSize > resizeThreshold) {
        resizeScale = 1.0f * maxSize / resizeThreshold;
        width = static_cast<int>(width / resizeScale);
        height = static_cast<int>(height / resizeScale);
        Size size(width, height);
        Mat resizedBitmap(size, CV_8UC3);
        resize(srcBitmap, resizedBitmap, size, 0, 0, INTER_AREA);
        return resizedBitmap;
    }
    return srcBitmap;
}

Mat Scanner::preprocessedImage(Mat &image, int cannyValue, int blurValue) {
    Mat grayMat;
    cvtColor(image, grayMat, COLOR_BGR2GRAY);

    if (!canny) {
        return grayMat;
    }

    if (isHisEqual) {
        equalizeHist(grayMat, grayMat);
    }

    // Tối ưu hóa Gaussian Blur
    Mat blurMat;
    GaussianBlur(grayMat, blurMat, Size(blurValue, blurValue), 0);

    // Tối ưu hóa Canny edge detection
    Mat cannyMat;
    Canny(blurMat, cannyMat, 50, cannyValue, 3, true);

    // Sử dụng Otsu's thresholding
    Mat thresholdMat;
    threshold(cannyMat, thresholdMat, 0, 255, THRESH_OTSU);

    return thresholdMat;
}

vector<Point> Scanner::selectPoints(vector<Point> points) {
    if (points.size() > 4) {
        // Tối ưu hóa việc tìm min/max
        int minX = points[0].x, maxX = points[0].x;
        int minY = points[0].y, maxY = points[0].y;

#pragma omp parallel for reduction(min:minX,minY) reduction(max:maxX,maxY)
        for (int i = 1; i < points.size(); i++) {
            minX = std::min(minX, points[i].x);
            maxX = std::max(maxX, points[i].x);
            minY = std::min(minY, points[i].y);
            maxY = std::max(maxY, points[i].y);
        }

        Point center((minX + maxX) / 2, (minY + maxY) / 2);
        vector<Point> result;
        result.reserve(4);

        // Tối ưu hóa việc chọn điểm
        for (int type = 0; type < 4; type++) {
            Point p = choosePoint(center, points, type);
            if (p.x != 0 || p.y != 0) {
                result.push_back(p);
            }
        }

        return result;
    }
    return points;
}

Point Scanner::choosePoint(Point center, std::vector<cv::Point> &points, int type) {
    int index = -1;
    int minDis = 0;

#pragma omp parallel for reduction(max:minDis)
    for (int i = 0; i < points.size(); i++) {
        bool inQuadrant = false;
        switch(type) {
            case 0: inQuadrant = points[i].x < center.x && points[i].y < center.y; break;
            case 1: inQuadrant = points[i].x < center.x && points[i].y > center.y; break;
            case 2: inQuadrant = points[i].x > center.x && points[i].y < center.y; break;
            case 3: inQuadrant = points[i].x > center.x && points[i].y > center.y; break;
        }

        if (inQuadrant) {
            int dis = static_cast<int>(sqrt(pow((points[i].x - center.x), 2) + pow((points[i].y - center.y), 2)));
            if (dis > minDis) {
                minDis = dis;
                index = i;
            }
        }
    }

    return index >= 0 ? points[index] : Point(0, 0);
}

vector<Point> Scanner::sortPointClockwise(vector<Point> points) {
    if (points.size() != 4) return points;

    // Tìm điểm trung tâm
    Point center(0, 0);
    for (const Point &p : points) {
        center.x += p.x;
        center.y += p.y;
    }
    center.x /= 4;
    center.y /= 4;

    // Sắp xếp các điểm theo góc
    std::sort(points.begin(), points.end(), [center](const Point &a, const Point &b) {
        return atan2(a.y - center.y, a.x - center.x) < atan2(b.y - center.y, b.x - center.x);
    });

    return points;
}