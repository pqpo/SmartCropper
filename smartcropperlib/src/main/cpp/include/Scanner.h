//
// Created by qiulinmin on 8/1/17.
//

#ifndef CROPPER_DOC_SCANNER_H
#define CROPPER_DOC_SCANNER_H

#include <opencv2/opencv.hpp>

namespace scanner{

    class Scanner {
    public:
        int resizeThreshold = 500;

        Scanner(cv::Mat& bitmap, bool canny);
        virtual ~Scanner();
        std::vector<cv::Point> scanPoint();
    private:
        cv::Mat srcBitmap;
        float resizeScale = 1.0f;

        bool canny = true;

        bool isHisEqual = false;

        cv::Mat resizeImage();

        cv::Mat preprocessedImage(cv::Mat &image, int cannyValue, int blurValue);

        cv::Point choosePoint(cv::Point center, std::vector<cv::Point> &points, int type);

        std::vector<cv::Point> selectPoints(std::vector<cv::Point> points);

        std::vector<cv::Point> sortPointClockwise(std::vector<cv::Point> vector);

        long long pointSideLine(cv::Point& lineP1, cv::Point& lineP2, cv::Point& point);
    };

}

#endif //CROPPER_DOC_SCANNER_H
