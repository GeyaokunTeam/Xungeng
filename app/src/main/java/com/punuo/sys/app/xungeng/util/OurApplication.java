package com.punuo.sys.app.xungeng.util;

import android.app.Application;

/**
 * Author chzjy
 * Date 2016/12/19.
 */

public class OurApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        CrashHandler handler = CrashHandler.getInstance();
        //在Appliction里面设置我们的异常处理器为UncaughtExceptionHandler处理器
        handler.init(getApplicationContext());
    }
}
