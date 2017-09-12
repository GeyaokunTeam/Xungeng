package com.punuo.sys.app.xungeng.ui;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;

import com.punuo.sys.app.R;
import com.punuo.sys.app.xungeng.adapter.MessageAdapter;
import com.punuo.sys.app.xungeng.model.Constant;
import com.punuo.sys.app.xungeng.model.Friend;
import com.punuo.sys.app.xungeng.model.LastestMsg;
import com.punuo.sys.app.xungeng.model.Msg;
import com.punuo.sys.app.xungeng.model.MyFile;
import com.punuo.sys.app.xungeng.sip.SipInfo;
import com.punuo.sys.app.xungeng.sip.SipUser;

import butterknife.Bind;
import butterknife.ButterKnife;

import static com.punuo.sys.app.xungeng.db.DatabaseInfo.sqLiteManager;

/**
 * Created by acer on 2016/9/28.
 */

public class MessageFragment extends Fragment implements View.OnClickListener, SipUser.TotalListener {
    @Bind(R.id.message_list)
    ListView messageList;
    @Bind(R.id.back)
    ImageButton back;
    @Bind(R.id.refresh)
    ImageButton refresh;
    @Bind(R.id.title)
    TextView title;
    private View view;
    private MessageAdapter messageAdapter;
    private Handler handler = new Handler();
    private Main main;
    @Override
    public void onResume() {
        super.onResume();
        SipInfo.lastestMsgs = sqLiteManager.queryLastestMsg();
        messageAdapter.notifyDataSetChanged();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.messagefragment, container, false);
        ButterKnife.bind(this, view);
        main=(Main)getActivity();
        title.setText("消息");
        refresh.setVisibility(View.GONE);
        back.setOnClickListener(this);
        SipInfo.sipUser.setTotalListener(this);
        /** 防止点击穿透，底层的fragment响应上层点击触摸事件 */
        view.setClickable(true);
        SipInfo.lastestMsgs = sqLiteManager.queryLastestMsg();
        messageAdapter = new MessageAdapter(getActivity());
        messageList.setAdapter(messageAdapter);
        messageList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                LastestMsg lastestMsgselected = SipInfo.lastestMsgs.get(position);
                Bundle bundle = new Bundle();
                if (lastestMsgselected.getType() == 0) {
                    Friend friend = new Friend();
                    friend.setUserId(lastestMsgselected.getId());
                    int index = SipInfo.friends.indexOf(friend);
                    if (index != -1) {
                        Friend f = SipInfo.friends.get(index);
                        Intent intent = new Intent(getActivity(), ChatActivity.class);
                        bundle.putSerializable("friend", f);
                        intent.putExtras(bundle);
                        startActivity(intent);
                    }
                } else {
//                    Group group = new Group();
//                    group.setGroupid(lastestMsgselected.getId());
//                    int index = SipInfo.groupList.indexOf(group);
//                    if (index != -1) {
//                        Intent intent = new Intent(getActivity(), GroupActivity.class);
//                        intent.putExtra("position", index);
//                        startActivity(intent);
//                    }
                }
            }
        });
        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        ButterKnife.unbind(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.back:
                main.setButtonType(Constant.MENU);
                break;
        }
    }

//    @Override
//    public void onReceivedTotalNotice(Notice notice) {
//        String id = notice.getGroupId();
//        handler.post(new Runnable() {
//            @Override
//            public void run() {
//                SipInfo.lastestMsgs = sqLiteManager.queryLastestMsg();
//                messageAdapter.notifyDataSetChanged();
//            }
//        });
//    }

    @Override
    public void onReceivedTotalMessage(Msg msg) {
        handler.post(new Runnable() {
            @Override
            public void run() {
                SipInfo.lastestMsgs = sqLiteManager.queryLastestMsg();
                messageAdapter.notifyDataSetChanged();
            }
        });
    }

    @Override
    public void onReceivedTotalFileshare(MyFile myfile) {

    }
}
