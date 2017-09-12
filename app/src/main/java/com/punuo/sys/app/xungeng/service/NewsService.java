package com.punuo.sys.app.xungeng.service;

import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;

import com.punuo.sys.app.xungeng.sip.BodyFactory;
import com.punuo.sys.app.xungeng.sip.SipInfo;
import com.punuo.sys.app.xungeng.sip.SipMessageFactory;
import com.punuo.sys.app.xungeng.util.LogUtil;
import com.punuo.sys.app.xungeng.video.H264Sending;

/**
 * Created by ch on 2016/11/14.
 */

public class NewsService extends Service {
    private String TAG="NewsService";
    @Override
    public void onCreate() {
        super.onCreate();
        LogUtil.i(TAG,"News Service 开启");
        SipInfo.notifymedia = new Handler() {
            @Override
            public void handleMessage(android.os.Message msg) {
                System.out.println("收到了 视频邀请");
                Intent intent = new Intent(NewsService.this, H264Sending.class);
                SipInfo.inviteResponse = true;
                SipInfo.sipDev.sendMessage(SipMessageFactory.createResponse(SipInfo.msg, 200, "OK", BodyFactory.createMediaResponseBody("CIF_MOBILE_SOFT")));
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                super.handleMessage(msg);
            }
        };
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
