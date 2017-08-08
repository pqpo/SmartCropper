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

        Scanner(cv::Mat& bitmap);
        virtual ~Scanner();
        std::vector<cv::Point> scanPoint();
    private:
        cv::Mat srcBitmap;
        float resizeScale = 1.0f;

        cv::Mat resizeImage();

        cv::Mat preprocessImage(cv::Mat& image);

        std::vector<cv::Point> selectPoints(std::vector<cv::Point> points, int selectTimes);

        std::vector<cv::Point> sortPointClockwise(std::vector<cv::Point> vector);

        long long pointSideLine(cv::Point& lineP1, cv::Point& lineP2, cv::Point& point);
    };

}

#endif //CROPPER_DOC_SCANNER_H
