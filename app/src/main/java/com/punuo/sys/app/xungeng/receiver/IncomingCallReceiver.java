package com.punuo.sys.app.xungeng.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.sip.SipAudioCall;
import android.net.sip.SipException;
import android.util.Log;

import com.punuo.sys.app.xungeng.audio.PhoneCall;
import com.punuo.sys.app.xungeng.service.SipService;

import static com.punuo.sys.app.xungeng.sip.SipInfo.lastCall;
import static com.punuo.sys.app.xungeng.sip.SipInfo.sipService;


public class IncomingCallReceiver extends BroadcastReceiver {
    public IncomingCallReceiver() {
    }
    String TAG = "IncomingCallReceiver";
    @Override
    public void onReceive(Context context, Intent intent) {
        SipAudioCall incomingCall = null;
        sipService = (SipService) context;
        lastCall = sipService.getCall();
        SipAudioCall.Listener listener = new SipAudioCall.Listener() {

            @Override
            public void onCallEnded(SipAudioCall call) {
                super.onCallEnded(call);
                Log.i(TAG, "电话挂断");
                if (call.equals(sipService.getCall())) {
                    if (sipService.getAudioCallListener() != null) {
                        sipService.getAudioCallListener().endCall();
                    }
                }
            }
        };

        try {
            lastCall = sipService.getCall();
            incomingCall = sipService.getManager().takeAudioCall(intent, listener);
            if (lastCall!=null&&lastCall.isInCall()) {
                incomingCall.endCall();
            } else {
                sipService.setCall(incomingCall);
                PhoneCall.actionStart(context, incomingCall.getPeerProfile().getUserName(), 2);
            }
        } catch (SipException e) {
            e.printStackTrace();
        }

    }


}
