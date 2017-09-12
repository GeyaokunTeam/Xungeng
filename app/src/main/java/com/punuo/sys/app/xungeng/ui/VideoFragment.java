package com.punuo.sys.app.xungeng.ui;

import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;

import com.punuo.sys.app.R;
import com.punuo.sys.app.xungeng.adapter.DevAdapter;
import com.punuo.sys.app.xungeng.model.Constant;
import com.punuo.sys.app.xungeng.sip.BodyFactory;
import com.punuo.sys.app.xungeng.sip.SipInfo;
import com.punuo.sys.app.xungeng.sip.SipMessageFactory;
import com.punuo.sys.app.xungeng.view.CustomProgressDialog;

import java.util.Collections;

import butterknife.Bind;
import butterknife.ButterKnife;

/**
 * Created by ch on 2016/11/8.
 */
public class VideoFragment extends Fragment implements View.OnClickListener {

    DevAdapter devAdapter;
    @Bind(R.id.video_list)
    ListView videoList;
//    @Bind(R.id.btnCall)
//    ImageButton btnPlay;
    @Bind(R.id.back)
    ImageButton back;
//    @Bind(R.id.refresh)
//    ImageButton refresh;
    @Bind(R.id.title)
    TextView title;

    private int lastPosition = -1;
    public static Handler handler = new Handler();
    private String TAG = "Video";
    private CustomProgressDialog inviting;
    private Main main;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.videofragment, container, false);
        view.setClickable(true);
        ButterKnife.bind(this, view);
        main = (Main) getActivity();
        title.setText("视频浏览("+SipInfo.devName+")");
//        btnPlay.setOnClickListener(this);
//        btnPlay.setImageResource(R.drawable.btn_play);
        back.setOnClickListener(this);
//        refresh.setOnClickListener(this);
//        Collections.sort(SipInfo.devList);
//        devAdapter = new DevAdapter(getActivity(), SipInfo.devList);
//        videoList.setAdapter(devAdapter);
//        videoList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
//            @Override
//            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
//                Device device = SipInfo.devList.get(position);
//                if (device.isLive()) {
//                    if (device.isSelect()) {
//                        device.setSelect(false);
//                        lastPosition = -1;
//                    } else {
//                        device.setSelect(true);
//                        if (lastPosition >= 0) {
//                            SipInfo.devList.get(lastPosition).setSelect(false);
//                        }
//                        lastPosition = position;
//                    }
//                    devAdapter.notifyDataSetChanged();
//                } else {
//                    Toast.makeText(getActivity(), "该设备不在线", Toast.LENGTH_SHORT).show();
//                }
//            }
//        });
        SipInfo.devCount = 0;
        SipInfo.devList.clear();
        /*请求设备列表*/
        SipInfo.sipUser.sendMessage(SipMessageFactory.createSubscribeRequest(
                SipInfo.sipUser, SipInfo.user_to, SipInfo.user_from, BodyFactory.createDevsQueryBody()));
        return view;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.back:
                main.setButtonType(Constant.MENU);
                break;
//            case refresh:
//                refresh.setClickable(false);
//                RotateAnimation animation = new RotateAnimation(0, 360, Animation.RELATIVE_TO_SELF, 0.5f,
//                        Animation.RELATIVE_TO_SELF, 0.5f);
//                animation.setDuration(1000);//设定转一圈的时间
//                animation.setRepeatCount(Animation.INFINITE);//设定无限循环
//                refresh.startAnimation(animation);
//                final List<Device> deviceList = SipInfo.devList;
//                SipInfo.devList = new ArrayList<>();
//                SipInfo.sipUser.sendMessage(SipMessageFactory.createSubscribeRequest(
//                        SipInfo.sipUser, SipInfo.user_to, SipInfo.user_from, BodyFactory.createDevsQueryBody()));
//                new Thread() {
//                    @Override
//                    public void run() {
//                        try {
//                            for (int i = 0; i < 4; i++) {
//                                sleep(1000);
//                                if (SipInfo.devList.size() == SipInfo.devCount - 1) {
//                                    break;
//                                }
//                            }
//                        } catch (InterruptedException e) {
//                            e.printStackTrace();
//                        } finally {
//                            handler.post(new Runnable() {
//                                @Override
//                                public void run() {
//                                    refresh.clearAnimation();
//                                    if (SipInfo.devList.size() == SipInfo.devCount - 1) {
//                                        lastPosition = -1;
//                                        Collections.sort(SipInfo.devList);
//                                        devAdapter = new DevAdapter(getActivity(), SipInfo.devList);
//                                        videoList.setAdapter(devAdapter);
//                                        LogUtil.d(TAG, "列表刷新成功");
//                                        Toast.makeText(getActivity(), "列表刷新成功", Toast.LENGTH_SHORT).show();
//                                    } else {
//                                        SipInfo.devList = deviceList;
//                                        Toast.makeText(getActivity(), "列表刷新失败,请重试", Toast.LENGTH_SHORT).show();
//                                    }
//                                    refresh.setClickable(true);
//                                }
//                            });
//                        }
//                    }
//                }.start();
//                break;
//            case R.id.btnCall:
//                if (lastPosition == -1) {
//                    Toast.makeText(getActivity(), "请选择在线设备", Toast.LENGTH_SHORT).show();
//                } else {
//                    String devId = SipInfo.devList.get(lastPosition).getDevId();
//                    devId = devId.substring(0, devId.length() - 4).concat("0160");//设备id后4位替换成0160
//                    String devName = SipInfo.devList.get(lastPosition).getName();
//                    final String devType = SipInfo.devList.get(lastPosition).getDevType();
//                    SipURL sipURL = new SipURL(devId, SipInfo.serverIp, SipInfo.SERVER_PORT);
//                    SipInfo.toDev = new NameAddress(devName, sipURL);
//                    SipInfo.queryResponse = false;
//                    SipInfo.inviteResponse = false;
//                    inviting = new CustomProgressDialog(getActivity());
//                    inviting.setCancelable(false);
//                    inviting.setCanceledOnTouchOutside(false);
//                    inviting.show();
//                    new Thread() {
//                        @Override
//                        public void run() {
//                            try {
//                                Message query = SipMessageFactory.createOptionsRequest(SipInfo.sipUser, SipInfo.toDev,
//                                        SipInfo.user_from, BodyFactory.createQueryBody(devType));
//                                outer:
//                                for (int i = 0; i < 3; i++) {
//                                    SipInfo.sipUser.sendMessage(query);
//                                    for (int j = 0; j < 20; j++) {
//                                        sleep(100);
//                                        if (SipInfo.queryResponse) {
//                                            break outer;
//                                        }
//                                    }
//                                    if (SipInfo.queryResponse) {
//                                        break;
//                                    }
//                                }
//                                if (SipInfo.queryResponse) {
//                                    Message invite = SipMessageFactory.createInviteRequest(SipInfo.sipUser,
//                                            SipInfo.toDev, SipInfo.user_from, BodyFactory.createMediaBody());
//                                    outer2:
//                                    for (int i = 0; i < 3; i++) {
//                                        SipInfo.sipUser.sendMessage(invite);
//                                        for (int j = 0; j < 20; j++) {
//                                            sleep(100);
//                                            if (SipInfo.inviteResponse) {
//                                                break outer2;
//                                            }
//                                        }
//                                        if (SipInfo.inviteResponse) {
//                                            break;
//                                        }
//                                    }
//                                }
//                            } catch (InterruptedException e) {
//                                e.printStackTrace();
//                            } finally {
//                                inviting.dismiss();
//                                if (SipInfo.queryResponse && SipInfo.inviteResponse) {
//                                    LogUtil.d(TAG, "视频请求成功");
//                                    SipInfo.decoding = true;
//                                    try {
//                                        VideoInfo.rtpVideo = new RtpVideo(VideoInfo.rtpIp, VideoInfo.rtpPort);
//                                        VideoInfo.sendActivePacket = new SendActivePacket();
//                                        VideoInfo.sendActivePacket.startThread();
//                                        startActivity(new Intent(getActivity(), VideoPlay.class));
//                                    } catch (SocketException e) {
//                                        e.printStackTrace();
//                                    }
//                                } else {
//                                    LogUtil.d(TAG, "视频请求失败");
//                                    handler.post(new Runnable() {
//                                        @Override
//                                        public void run() {
//                                            new AlertDialog.Builder(getActivity())
//                                                    .setTitle("视频请求失败！")
//                                                    .setMessage("请重新尝试")
//                                                    .setPositiveButton("确定", null).show();
//                                        }
//                                    });
//                                }
//                            }
//                        }
//                    }.start();
//                }
//                break;
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        ButterKnife.unbind(this);
//        if (lastPosition != -1) {
//            SipInfo.devList.get(lastPosition).setSelect(false);
//        }
    }
    public void devNotify() {
        handler.post(new Runnable() {
            @Override
            public void run() {
                Collections.sort(SipInfo.devList);
                devAdapter = new DevAdapter(getActivity(), SipInfo.devList);
                videoList.setAdapter(devAdapter);
                devAdapter.notifyDataSetChanged();
            }
        });
    }
}




