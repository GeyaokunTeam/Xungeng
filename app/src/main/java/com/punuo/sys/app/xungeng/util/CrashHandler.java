package com.punuo.sys.app.xungeng.util;


import android.content.Context;
import android.util.Log;

/**
 * Author chzjy
 * Date 2016/12/19.
 */

public class CrashHandler implements Thread.UncaughtExceptionHandler {
    //单例引用，这里我们做成单例的，因为我们一个应用程序里面只需要一个UncaughtExceptionHandler实例
    private static CrashHandler instance;

    private CrashHandler() {
    }

    //同步方法，以免单例多线程环境下出现异常
    public synchronized static CrashHandler getInstance() {
        if (instance == null) {
            instance = new CrashHandler();
        }
        return instance;
    }
    //初始化，把当前对象设置成UncaughtExceptionHandler处理器
    public void init(Context context){
        Thread.setDefaultUncaughtExceptionHandler(this);
    }
    //当有未处理的异常发生时，调用此方法
    @Override
    public void uncaughtException(Thread thread, Throwable ex) {
        Log.e("error", "uncaughtException, thread: " + thread
                + " name: " + thread.getName() + " id: " + thread.getId() + "exception: "
                + ex);
        String threadName = thread.getName();
    }
}
