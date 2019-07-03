package me.pqpo.smartcropper;

import android.app.Application;

import me.pqpo.smartcropperlib.SmartCropper;

/**
 * Created by qiulinmin@u51.com on 2019/7/3.
 */
public class App extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        // 如果使用机器学习代替 Canny 算子，请初始化 ImageDetector
        SmartCropper.buildImageDetector(this);
    }
}
