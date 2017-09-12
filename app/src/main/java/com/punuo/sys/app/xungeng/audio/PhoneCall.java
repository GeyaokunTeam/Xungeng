package com.punuo.sys.app.xungeng.audio;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.net.sip.SipException;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.SystemClock;
import android.os.Vibrator;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.Chronometer;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.punuo.sys.app.R;
import com.punuo.sys.app.xungeng.service.SipService;
import com.punuo.sys.app.xungeng.ui.ActivityCollector;
import com.punuo.sys.app.xungeng.ui.MyActivity;
import com.punuo.sys.app.xungeng.util.LogUtil;

import java.io.IOException;

import butterknife.Bind;
import butterknife.ButterKnife;

import static com.punuo.sys.app.xungeng.sip.SipInfo.lastCall;
import static com.punuo.sys.app.xungeng.sip.SipInfo.sipService;

public class PhoneCall extends MyActivity implements SipService.AudioCallListener {
    @Bind(R.id.phoneNum)
    TextView phoneNum;
    @Bind(R.id.status)
    TextView status;
    @Bind(R.id.time)
    Chronometer time;
    @Bind(R.id.speakerMode)
    Button speakerMode;
    @Bind(R.id.layout1)
    LinearLayout layout1;
    @Bind(R.id.accept)
    ImageButton accept;
    @Bind(R.id.decline)
    ImageButton decline;
    @Bind(R.id.layout2)
    RelativeLayout layout2;
    @Bind(R.id.endCall)
    ImageButton endCall;
    private MediaPlayer mMediaPlayer;
    private MediaPlayer mediaPlayer_ring;
    private AudioManager audioManager;
    private BroadcastReceiver mReceiver;
    private long[] pattern = new long[]{400, 800, 1200};
    private Vibrator vibrator;
    private int type;
    private boolean isSpeakerMode = false;
    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (msg.what == 200) {
                status.setVisibility(View.INVISIBLE);
                time.setVisibility(View.VISIBLE);
                time.setBase(SystemClock.elapsedRealtime());
                time.start();
                setVolumeControlStream(AudioManager.STREAM_VOICE_CALL);
                if (mediaPlayer_ring != null) {
                    mediaPlayer_ring.stop();
                }
            }
        }
    };

    private SipService.MyBinder serviceBinder;
    private ServiceConnection conn = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            serviceBinder = (SipService.MyBinder) service;
            serviceBinder.setAudioCallListener(PhoneCall.this);
            if (type == 1) {
                serviceBinder.makeAudioCall(phoneNum.getText().toString());
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
        }
    };

    public static void actionStart(Context context, String name, int type) {
        Intent intent = new Intent(context, PhoneCall.class);
        intent.putExtra("name", name);
        intent.putExtra("type", type);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ActivityCollector.addActivity(this);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
        setContentView(R.layout.activity_phone_call);
        ButterKnife.bind(this);
        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        vibrator = (Vibrator) getSystemService(Service.VIBRATOR_SERVICE);
        mMediaPlayer = MediaPlayer.create(PhoneCall.this, getSystemDefultRingtoneUri());
        mediaPlayer_ring = MediaPlayer.create(PhoneCall.this, R.raw.ring);
        Intent intent = getIntent();
        phoneNum.setText(intent.getStringExtra("name"));
        type = intent.getIntExtra("type", 0);
        bindService(new Intent(this, SipService.class), conn, BIND_AUTO_CREATE);
        if (type == 1) {
            status.setText("正在呼叫…");
            layout2.setVisibility(View.INVISIBLE);
            setVolumeControlStream(AudioManager.STREAM_MUSIC);
            audioManager.setMode(AudioManager.MODE_IN_COMMUNICATION);
            callring();
        } else if (type == 2) {
            status.setText("来电");
            layout1.setVisibility(View.INVISIBLE);
            endCall.setVisibility(View.INVISIBLE);
            IntentFilter filter = new IntentFilter("android.media.RINGER_MODE_CHANGED");
            mReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    if (type == 2) {
                        ring();
                    }
                }

            };
            registerReceiver(mReceiver, filter);
            audioManager.setMode(AudioManager.MODE_NORMAL);
            setVolumeControlStream(AudioManager.STREAM_MUSIC);
        }

        speakerMode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isSpeakerMode) {
                    speakerMode.setBackgroundResource(R.drawable.btn_no_speaker);
                    serviceBinder.setSpeakerMode(false);
                    isSpeakerMode = false;
                } else {
                    speakerMode.setBackgroundResource(R.drawable.btn_speaker);
                    serviceBinder.setSpeakerMode(true);
                    isSpeakerMode = true;
                }
            }
        });
        accept.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                serviceBinder.startAudio();
                layout1.setVisibility(View.VISIBLE);
                layout2.setVisibility(View.INVISIBLE);
                status.setVisibility(View.INVISIBLE);
                time.setVisibility(View.VISIBLE);
                time.setBase(SystemClock.elapsedRealtime());
                time.start();
                endCall.setVisibility(View.VISIBLE);
                if (mMediaPlayer != null) {
                    mMediaPlayer.stop();
                }
                vibrator.cancel();
                unregisterReceiver(mReceiver);
                type = 3;
                setVolumeControlStream(AudioManager.STREAM_VOICE_CALL);
                if (lastCall!=null) {
                    try {
                        lastCall.holdCall(0);
                    } catch (SipException e) {
                        e.printStackTrace();
                    }
                }
            }
        });

        decline.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                serviceBinder.endCall();
                if (lastCall!=null){
                    sipService.setCall(lastCall);
                    try {
                        lastCall.continueCall(0);
                    } catch (SipException e) {
                        e.printStackTrace();
                    }
                }
                finish();
            }
        });

        endCall.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                serviceBinder.endCall();
                if (lastCall!=null){
                    sipService.setCall(lastCall);
                    try {
                        lastCall.continueCall(0);
                    } catch (SipException e) {
                        e.printStackTrace();
                    }
                }
                finish();
            }
        });

        LogUtil.d("SipS", "onCreate");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        //异地登录，如果正在通话，就断开当前通话
        if (serviceBinder.getCall().isInCall()){
            serviceBinder.endCall();
        }
        ActivityCollector.removeActivity(this);
        serviceBinder.setSpeakerMode(false);
        unbindService(conn);
        if (type == 2) {
            if (mMediaPlayer != null) {
                mMediaPlayer.stop();
            }
            vibrator.cancel();
            unregisterReceiver(mReceiver);
        }
        if (type == 1) {
            if (mediaPlayer_ring != null) {
                mediaPlayer_ring.stop();
            }
            audioManager.setMode(AudioManager.MODE_NORMAL);
        }
    }

    @Override
    public void onBackPressed() {
    }

    @Override
    public void startCall() {
        handler.sendEmptyMessage(200);
    }

    @Override
    public void endCall() {
        if (lastCall!=null){
            sipService.setCall(lastCall);
        }
        finish();
    }

    public void ring() {

        switch (audioManager.getRingerMode()) {
            case AudioManager.RINGER_MODE_NORMAL:

                mMediaPlayer.setLooping(true);
                try {
                    mMediaPlayer.prepare();
                } catch (IllegalStateException | IOException e) {
                    e.printStackTrace();
                }
                mMediaPlayer.start();
                break;

            case AudioManager.RINGER_MODE_SILENT:
                vibrator.cancel();
                mMediaPlayer.stop();
                break;

            case AudioManager.RINGER_MODE_VIBRATE:
                vibrator.vibrate(pattern, 0);
                mMediaPlayer.stop();
                break;


        }
    }

    public void callring() {

        mediaPlayer_ring.setLooping(true);
        try {
            mediaPlayer_ring.prepare();
        } catch (IllegalStateException | IOException e) {
            e.printStackTrace();
        }
        mediaPlayer_ring.start();
    }

    //获取系统默认铃声的Uri
    private Uri getSystemDefultRingtoneUri() {
        return RingtoneManager.getActualDefaultRingtoneUri(this,
                RingtoneManager.TYPE_RINGTONE);
    }
}
