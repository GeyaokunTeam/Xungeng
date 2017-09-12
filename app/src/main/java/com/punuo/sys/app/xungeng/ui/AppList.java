package com.punuo.sys.app.xungeng.ui;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ListView;

import com.punuo.sys.app.R;
import com.punuo.sys.app.xungeng.adapter.AppAdapter;
import com.punuo.sys.app.xungeng.db.DatabaseInfo;
import com.punuo.sys.app.xungeng.ftp.FTP;
import com.punuo.sys.app.xungeng.model.App;
import com.punuo.sys.app.xungeng.sip.SipInfo;

import java.io.File;

import butterknife.Bind;
import butterknife.ButterKnife;

/**
 * Created by acer on 2016/10/24.
 */

public class AppList extends MyActivity {
    @Bind(R.id.applist)
    ListView applist;

    @Bind(R.id.back)
    ImageButton back;
    public static AppAdapter appAdapter;
    private String sdPath = "/mnt/sdcard/xungeng/download/apk/";
    private String TAG = "AppList";
    private AppAdapter.DownloadListener downloadListener = new AppAdapter.DownloadListener() {
        @Override
        public void onDownload(final String appId, final String appPath, final String appName) {

            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        //单文件下载
                        //文件ftp路径是地址加文件名
                        new FTP().downloadSingleFile(appPath, sdPath, appName, new FTP.DownLoadProgressListener() {
                            @Override
                            public void onDownLoadProgress(String currentStep, long downProcess, File file) {
                                Log.d(TAG, currentStep);
                                Message message = new Message();
                                message.obj = appId;
                                if (currentStep.equals(FTP.FTP_DOWN_SUCCESS)) {
                                    Log.d(TAG, "-----下载--successful");
                                    message.what = 0x3332;
                                    dhandler.sendMessage(message);
                                } else if (currentStep.equals(FTP.FTP_DOWN_LOADING)) {
                                    Log.d(TAG, "-----下载---" + downProcess + "%");
                                    message.arg1 = (int) downProcess;
                                    message.what = 0x3331;
                                    dhandler.sendMessage(message);
                                }
                            }
                        });

                    } catch (Exception e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    } finally {
                        DatabaseInfo.sqLiteManager.updateAppDownload(appId, 1);
                    }

                }
            }).start();
        }
    };
    private AppAdapter.OpenFileListener openFileListener = new AppAdapter.OpenFileListener() {
        @Override
        public void OpenFile(File file) {
            Intent intent = new Intent();
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            //设置intent的Action属性
            intent.setAction(Intent.ACTION_VIEW);
            //获取文件file的MIME类型
            String type = getMIMEType(file);
            //设置intent的data和Type属性。
            intent.setDataAndType(/*uri*/Uri.fromFile(file), type);
            //跳转
            startActivity(intent);
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.applist);
        ButterKnife.bind(this);
        appAdapter = new AppAdapter(AppList.this, downloadListener, openFileListener);
        applist.setAdapter(appAdapter);
        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

    private Handler dhandler = new Handler() {
        @Override
        public void handleMessage(Message message) {
            App app = new App();
            app.setAppid((String) message.obj);
            int index = SipInfo.appList.indexOf(app);
            if (index != -1) {
                if (message.what == 0x3331) {
                    SipInfo.appList.get(index).setProgress(message.arg1);
                    appAdapter.notifyDataSetChanged();
                }
                if (message.what == 0x3332) {
                    DatabaseInfo.sqLiteManager.updateAppDownload(app.getAppid(), 1);
                    DatabaseInfo.sqLiteManager.updateAppLocalPath(app.getAppid(), sdPath + SipInfo.appList.get(index).getName());
                    appAdapter.notifyDataSetChanged();
                    SipInfo.appList.get(index).setProgress(0);
                }
            }
        }
    };

    private String getMIMEType(File file) {

        String type = "*/*";
        String fName = file.getName();
        //获取后缀名前的分隔符"."在fName中的位置。
        int dotIndex = fName.lastIndexOf(".");
        if (dotIndex < 0) {
            return type;
        }
    /* 获取文件的后缀名*/
        String end = fName.substring(dotIndex, fName.length()).toLowerCase();
        if (end == "") return type;
        //在MIME和文件类型的匹配表中找到对应的MIME类型。
        for (int i = 0; i < MIME_MapTable.length; i++) { //MIME_MapTable??在这里你一定有疑问，这个MIME_MapTable是什么？
            if (end.equals(MIME_MapTable[i][0]))
                type = MIME_MapTable[i][1];
        }
        return type;
    }

    private final String[][] MIME_MapTable = {
            //{后缀名，MIME类型}
            {".3gp", "video/3gpp"},
            {".apk", "application/vnd.android.package-archive"},
            {".asf", "video/x-ms-asf"},
            {".avi", "video/x-msvideo"},
            {".bin", "application/octet-stream"},
            {".bmp", "image/bmp"},
            {".c", "text/plain"},
            {".class", "application/octet-stream"},
            {".conf", "text/plain"},
            {".cpp", "text/plain"},
            {".doc", "application/msword"},
            {".docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document"},
            {".xls", "application/vnd.ms-excel"},
            {".xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"},
            {".exe", "application/octet-stream"},
            {".gif", "image/gif"},
            {".gtar", "application/x-gtar"},
            {".gz", "application/x-gzip"},
            {".h", "text/plain"},
            {".htm", "text/html"},
            {".html", "text/html"},
            {".jar", "application/java-archive"},
            {".java", "text/plain"},
            {".jpeg", "image/jpeg"},
            {".jpg", "image/jpeg"},
            {".js", "application/x-javascript"},
            {".log", "text/plain"},
            {".m3u", "audio/x-mpegurl"},
            {".m4a", "audio/mp4a-latm"},
            {".m4b", "audio/mp4a-latm"},
            {".m4p", "audio/mp4a-latm"},
            {".m4u", "video/vnd.mpegurl"},
            {".m4v", "video/x-m4v"},
            {".mov", "video/quicktime"},
            {".mp2", "audio/x-mpeg"},
            {".mp3", "audio/x-mpeg"},
            {".mp4", "video/mp4"},
            {".mpc", "application/vnd.mpohun.certificate"},
            {".mpe", "video/mpeg"},
            {".mpeg", "video/mpeg"},
            {".mpg", "video/mpeg"},
            {".mpg4", "video/mp4"},
            {".mpga", "audio/mpeg"},
            {".msg", "application/vnd.ms-outlook"},
            {".ogg", "audio/ogg"},
            {".pdf", "application/pdf"},
            {".png", "image/png"},
            {".pps", "application/vnd.ms-powerpoint"},
            {".ppt", "application/vnd.ms-powerpoint"},
            {".pptx", "application/vnd.openxmlformats-officedocument.presentationml.presentation"},
            {".prop", "text/plain"},
            {".rc", "text/plain"},
            {".rmvb", "audio/x-pn-realaudio"},
            {".rtf", "application/rtf"},
            {".sh", "text/plain"},
            {".tar", "application/x-tar"},
            {".tgz", "application/x-compressed"},
            {".txt", "text/plain"},
            {".wav", "audio/x-wav"},
            {".wma", "audio/x-ms-wma"},
            {".wmv", "audio/x-ms-wmv"},
            {".wps", "application/vnd.ms-works"},
            {".xml", "text/plain"},
            {".z", "application/x-compress"},
            {".zip", "application/x-zip-compressed"},
            {"", "*/*"}
    };

}
