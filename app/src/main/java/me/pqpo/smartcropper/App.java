package me.pqpo.smartcropper;

import android.app.Application;

import me.pqpo.smartcropperlib.SmartCropper;

public class App extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        // 如果使用机器学习代替 Canny 算子，请初始化 ImageDetector
        SmartCropper.buildImageDetector(this);
    }
}
