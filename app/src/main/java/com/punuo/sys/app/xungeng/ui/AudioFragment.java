package com.punuo.sys.app.xungeng.ui;

import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.punuo.sys.app.R;
import com.punuo.sys.app.xungeng.audio.DialFragment;
import com.punuo.sys.app.xungeng.audio.FriendListFragment;
import com.punuo.sys.app.xungeng.audio.PhoneCall;
import com.punuo.sys.app.xungeng.model.Constant;
import com.punuo.sys.app.xungeng.sip.SipInfo;

import butterknife.Bind;
import butterknife.ButterKnife;


/**
 * Created by acer on 2016/11/9.
 */

public class AudioFragment extends Fragment implements View.OnClickListener {
    @Bind(R.id.back)
    ImageButton back;
    @Bind(R.id.btnCall)
    ImageButton btnCall;
//    @Bind(R.id.refresh)
//    ImageButton refresh;
    @Bind(R.id.title)
    TextView title;
    @Bind(R.id.tv1)
    TextView tv1;
    @Bind(R.id.tv2)
    TextView tv2;
    @Bind(R.id.iv1)
    ImageView iv1;
    @Bind(R.id.iv2)
    ImageView iv2;
    Main main;
    private FriendListFragment friendListFragment;
    private DialFragment dialFragment;
    private FragmentManager fragmentManager;
    private Handler handler = new Handler();
    private String TAG = "AudioFragment";

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.audiofragment, container, false);
        ButterKnife.bind(this, view);
        main = (Main) getActivity();
        view.setClickable(true);
        title.setText("语音电话(" + SipInfo.phoneNum + ")");
        back.setOnClickListener(this);
        tv1.setOnClickListener(this);
        tv2.setOnClickListener(this);
        btnCall.setOnClickListener(this);
//        refresh.setOnClickListener(this);
        initView();
        friendListFragment = new FriendListFragment();
        dialFragment = new DialFragment();
        fragmentManager = getActivity().getSupportFragmentManager();
        fragmentManager.beginTransaction()
                .add(R.id.frame, friendListFragment)
                .commit();
        return view;
    }

    private void initView() {
        Drawable dr1 = getResources().getDrawable(R.drawable.icon_list);
        Drawable dr2 = getResources().getDrawable(R.drawable.icon_phone);
        DisplayMetrics dm = getResources().getDisplayMetrics();
        float density = dm.density;        // 屏幕密度（像素比例：0.75/1.0/1.5/2.0/3.0）
        int len = (int) (19 * density);
        dr1.setBounds(0, 0, len, len);
        dr2.setBounds(0, 0, len, len);
        tv1.setCompoundDrawables(dr1, null, null, null);
        tv2.setCompoundDrawables(dr2, null, null, null);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        ButterKnife.unbind(this);
//        int lastPosition = friendListFragment.getLastPosition();
//        if (lastPosition >= 0) {
//            SipInfo.friends.get(lastPosition).setSelect(false);
//        }
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
//                final List<Friend> friends = SipInfo.friends;
//                SipInfo.friendCount = -1;
//                SipInfo.friends = new ArrayList<>();
//                SipInfo.friendList.clear();
//                SipInfo.sipUser.sendMessage(SipMessageFactory.createSubscribeRequest(
//                        SipInfo.sipUser, SipInfo.user_to, SipInfo.user_from, BodyFactory.createFriendsQueryBody(0, 0)));
//                new Thread() {
//                    @Override
//                    public void run() {
//                        try {
//                            for (int i = 0; i < 4; i++) {
//                                sleep(1000);
//                                if (SipInfo.friends.size() == SipInfo.friendCount) {
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
//                                    if (SipInfo.friends.size() == SipInfo.friendCount) {
//                                        friendListFragment.notifyFriendListChanged();
//                                        LogUtil.d(TAG, "列表刷新成功");
//                                    } else {
//                                        SipInfo.friends = friends;
//                                    }
//                                    refresh.setClickable(true);
//                                }
//                            });
//                        }
//                    }
//                }.start();
//                break;
            case R.id.tv1:
                if (iv1.getVisibility() == View.INVISIBLE && iv2.getVisibility() == View.VISIBLE) {
                    iv1.setVisibility(View.VISIBLE);
                    iv2.setVisibility(View.INVISIBLE);
//                    refresh.setVisibility(View.VISIBLE);
                    btnCall.setVisibility(View.GONE);
                    fragmentManager.beginTransaction()
                            .replace(R.id.frame, friendListFragment)
                            .commit();
                }
                break;
            case R.id.tv2:
                if (iv1.getVisibility() == View.VISIBLE && iv2.getVisibility() == View.INVISIBLE) {
                    iv1.setVisibility(View.INVISIBLE);
                    iv2.setVisibility(View.VISIBLE);
//                    refresh.setVisibility(View.GONE);
                    btnCall.setVisibility(View.VISIBLE);
                    fragmentManager.beginTransaction()
                            .replace(R.id.frame, dialFragment)
                            .commit();
                }
                break;
            case R.id.btnCall:
                String phoneNum;
                if (iv1.getVisibility() == View.VISIBLE && iv2.getVisibility() == View.INVISIBLE) {
//                    int lastPosition = friendListFragment.getLastPosition();
//                    if (lastPosition == -1) {
//                        Toast.makeText(getActivity(), "请选择在线设备", Toast.LENGTH_SHORT).show();
//                    } else {
//                        phoneNum = SipInfo.friends.get(lastPosition).getPhoneNum();
//                        PhoneCall.actionStart(getActivity(), phoneNum, 1);
//                    }
                } else if (iv1.getVisibility() == View.INVISIBLE && iv2.getVisibility() == View.VISIBLE) {
                    phoneNum = dialFragment.getPhoneNum();
                    if (TextUtils.isEmpty(phoneNum)) {
                        Toast.makeText(getActivity(), "请输入号码", Toast.LENGTH_SHORT).show();
                    } else {
                        PhoneCall.actionStart(getActivity(), phoneNum, 1);
                    }
                }
                break;
        }
    }
    public void userNotify(){
        handler.post(new Runnable() {
            @Override
            public void run() {
                friendListFragment.notifyFriendListChanged();
            }
        });
    }
}
