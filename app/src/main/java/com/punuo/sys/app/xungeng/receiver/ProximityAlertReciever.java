package com.punuo.sys.app.xungeng.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Vibrator;
import android.widget.Toast;

import com.punuo.sys.app.xungeng.sip.BodyFactory;
import com.punuo.sys.app.xungeng.sip.SipInfo;
import com.punuo.sys.app.xungeng.sip.SipMessageFactory;
import com.punuo.sys.app.xungeng.util.MyToast;

import static android.content.Context.VIBRATOR_SERVICE;
import static com.punuo.sys.app.xungeng.sip.SipInfo.pointInfoListbd09;
import static com.punuo.sys.app.xungeng.sip.SipInfo.userId;

public class ProximityAlertReciever extends BroadcastReceiver {
    String GEOFENCE_BROADCAST_ACTION = "com.punuo.sys.app.broadcast";

    public ProximityAlertReciever() {
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        Vibrator vibrator = (Vibrator) context.getSystemService(VIBRATOR_SERVICE);
        if (intent.getAction().equals(GEOFENCE_BROADCAST_ACTION)) {
            int status = intent.getIntExtra("status", 0);
            final int id = intent.getIntExtra("id", 0);
            if (status == 1) {
                //进图围栏区域
                MyToast.show(context, "进入点"+pointInfoListbd09.get(id-1).getName(), Toast.LENGTH_SHORT);
                vibrator.vibrate(1000);
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        for (int i=0;i<3;i++){
//                            SipInfo.sipUser.sendMessage(SipMessageFactory.createNotifyRequest(SipInfo.sipUser, SipInfo.user_to, SipInfo.user_from,
//                                    BodyFactory.creatGPSInfoBody(userId, SipInfo.addr_code, pointInfoListbd09.get(id - 1).getLang(), pointInfoListbd09.get(id - 1).getLat(), System.currentTimeMillis() / 1000, id, 0)));
                            try {
                                Thread.sleep(5000);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }).start();
            } else if (status == 2) {
                // 离开围栏区域
            }
        }

    }
}
