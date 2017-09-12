package com.punuo.sys.app.xungeng.service;

import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.sip.SipAudioCall;
import android.net.sip.SipException;
import android.net.sip.SipManager;
import android.net.sip.SipProfile;
import android.net.sip.SipRegistrationListener;
import android.os.Binder;
import android.os.IBinder;
import android.widget.Toast;

import com.punuo.sys.app.xungeng.receiver.IncomingCallReceiver;
import com.punuo.sys.app.xungeng.sip.SipInfo;
import com.punuo.sys.app.xungeng.util.LogUtil;

import java.text.ParseException;

import static com.punuo.sys.app.xungeng.sip.SipInfo.Phone;

public class SipService extends Service {
    public static final String TAG = "SipService";
    public static final String ACTION_INCOMING_CALL = "com.punuo.punuoapp.intent.action.INCOMING_CALL";
    private SipManager manager;
    private SipProfile localSipProfile;
    private IncomingCallReceiver callReceiver;
    private SipAudioCall call;
    private boolean registrated = false;
    private AudioCallListener audioCallListener;
    private MediaPlayer mediaPlayer_ring;
    private AudioManager audioManager;

    public SipService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        return new MyBinder();
    }

    @Override
    public void onCreate() {
        super.onCreate();
        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        initializeManager();
        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_INCOMING_CALL);
        callReceiver = new IncomingCallReceiver();
        registerReceiver(callReceiver, filter);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        LogUtil.d("sipService", "destroy");
        unregister();
        closeLocalProfile();
        if (callReceiver != null) {
            unregisterReceiver(callReceiver);
        }
    }

    public class MyBinder extends Binder {
        public void makeAudioCall(String phoneNum) {
            SipAudioCall.Listener listener = new SipAudioCall.Listener() {
                @Override
                public void onCallEstablished(SipAudioCall call) {
                    super.onCallEstablished(call);
                    call.startAudio();
                    if (call.isMuted()) {
                        call.toggleMute();
                    }
                    LogUtil.d(TAG, "通话建立");
                    if (audioCallListener != null) {
                        audioCallListener.startCall();
                    }
                }

                @Override
                public void onCalling(SipAudioCall call) {
                    super.onCalling(call);
                    LogUtil.d(TAG, "正在拨打中");
                }

                @Override
                public void onCallEnded(SipAudioCall call) {
                    super.onCallEnded(call);
                    LogUtil.d(TAG, "通话结束");
                    if (audioCallListener != null) {
                        audioCallListener.endCall();
                    }
                }

                @Override
                public void onCallBusy(SipAudioCall call) {
                    super.onCallBusy(call);
                    LogUtil.d(TAG, "拨打的用户忙");
                    if (audioCallListener != null) {
                        audioCallListener.endCall();
                    }
                    Phone.post(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(SipService.this, "你拨打的用户正在通话中，请稍后再拨！", Toast.LENGTH_LONG).show();
                        }
                    });
                }


                @Override
                public void onError(SipAudioCall call, int errorCode, String errorMessage) {
                    super.onError(call, errorCode, errorMessage);
                    LogUtil.d(TAG, "呼叫超时");
                    if (audioCallListener != null) {
                        audioCallListener.endCall();
                    }
                    Phone.post(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(SipService.this, "你拨打的用户不在线，请稍后再拨！", Toast.LENGTH_LONG).show();
                        }
                    });
                }
            };
            try {
                SipProfile.Builder builder = new SipProfile.Builder(phoneNum, SipInfo.serverIp);
                builder.setPort(5000);
                SipProfile peerProfile = builder.build();
                call = manager.makeAudioCall(localSipProfile, peerProfile, listener, 30);
            } catch (ParseException e) {
                e.printStackTrace();
            } catch (SipException e) {
                e.printStackTrace();
            }

        }

        public void endCall() {
            try {
                call.endCall();
            } catch (SipException e) {
                e.printStackTrace();
            }
        }

        public void startAudio() {
            try {
                call.answerCall(30);
            } catch (SipException e) {
                e.printStackTrace();
            }
            call.startAudio();
            if (call.isMuted()) {
                call.toggleMute();
            }
        }

        public void setSpeakerMode(boolean speakerMode) {
            call.setSpeakerMode(speakerMode);
        }

        public void setAudioCallListener(AudioCallListener listener) {
            audioCallListener = listener;
        }

        public boolean getRegistrated() {
            return registrated;
        }

        public SipAudioCall getCall() {
            return call;
        }
        public void close() {
            unregister();
            closeLocalProfile();
        }

    }

    public void initializeManager() {
        if (manager == null) {
            manager = SipManager.newInstance(this);
        }
        initializeLocalProfile();
    }

    public void initializeLocalProfile() {
        if (manager == null) {
            return;
        }
        if (localSipProfile != null) {
            closeLocalProfile();
        }
        try {
            SipProfile.Builder builder = new SipProfile.Builder(SipInfo.phoneNum, SipInfo.serverIp);
            builder.setPort(5000);
            localSipProfile = builder.build();
            Intent i = new Intent();
            i.setAction(ACTION_INCOMING_CALL);
            PendingIntent pi = PendingIntent.getBroadcast(this, 0, i, Intent.FILL_IN_DATA);
            SipRegistrationListener listener = new SipRegistrationListener() {

                @Override
                public void onRegistrationFailed(String localProfileUri, int errorCode,
                                                 String errorMessage) {
                    LogUtil.d(TAG, "注册失败");
                    LogUtil.d(TAG, errorCode + "");
                    LogUtil.d(TAG, errorMessage);
                }

                @Override
                public void onRegistrationDone(String localProfileUri, long expiryTime) {
                    LogUtil.d(TAG, "注册成功");
                    registrated = true;
                }

                @Override
                public void onRegistering(String localProfileUri) {

                }
            };
            manager.open(localSipProfile, pi, null);
            manager.setRegistrationListener(localSipProfile.getUriString(), listener);

        } catch (ParseException e) {
            e.printStackTrace();
        } catch (SipException e) {
            e.printStackTrace();
        }
    }

    public void closeLocalProfile() {
        if (manager == null) {
            return;
        }
        if (localSipProfile != null) {
            try {
                manager.close(localSipProfile.getUriString());
            } catch (SipException e) {
                e.printStackTrace();
            }
        }
    }

    public void unregister() {
        if (manager != null) {
            try {
                manager.unregister(localSipProfile, null);
            } catch (SipException e) {
                e.printStackTrace();
            }
        }
    }

    public SipManager getManager() {
        return manager;
    }

    public void setManager(SipManager manager) {
        this.manager = manager;
    }

    public SipAudioCall getCall() {
        return call;
    }

    public void setCall(SipAudioCall call) {
        this.call = call;
    }

    public AudioCallListener getAudioCallListener() {
        return audioCallListener;
    }

    public interface AudioCallListener {
        void startCall();

        void endCall();
    }

}
