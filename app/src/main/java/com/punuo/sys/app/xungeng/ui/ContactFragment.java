package com.punuo.sys.app.xungeng.ui;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ExpandableListView;
import android.widget.ImageButton;
import android.widget.TextView;

import com.punuo.sys.app.R;
import com.punuo.sys.app.xungeng.adapter.ContactAdapter;
import com.punuo.sys.app.xungeng.model.Constant;
import com.punuo.sys.app.xungeng.model.Friend;
import com.punuo.sys.app.xungeng.sip.SipInfo;

import java.util.Set;

import butterknife.Bind;
import butterknife.ButterKnife;

import static com.punuo.sys.app.xungeng.sip.SipInfo.friendList;


/**
 * Created by acer on 2016/11/9.
 */

public class ContactFragment extends Fragment implements View.OnClickListener {
    @Bind(R.id.contact_list)
    ExpandableListView contactList;
    @Bind(R.id.back)
    ImageButton back;
//    @Bind(R.id.refresh)
//    ImageButton refresh;
    @Bind(R.id.title)
    TextView title;
    Main main;
    private ContactAdapter contactAdapter;
    private Handler handler = new Handler();
    private String TAG="ContactFragment";
    private int[] groupState;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.contactfragment, container, false);
        ButterKnife.bind(this, view);
        main = (Main) getActivity();
        Set keyname = friendList.keySet();
        groupState = new int[keyname.size()];
        contactAdapter = new ContactAdapter(getActivity(), SipInfo.friendList);
        contactList.setAdapter(contactAdapter);
        back.setOnClickListener(this);
//        refresh.setOnClickListener(this);
        view.setClickable(true);
        title.setText("联系人");
        contactList.setOnChildClickListener(new ExpandableListView.OnChildClickListener() {
            @Override
            public boolean onChildClick(ExpandableListView parent, View v, int groupPosition, int childPosition, long id) {
                Friend friend=(Friend)contactAdapter.getChild(groupPosition, childPosition);
                Intent intent=new Intent(getActivity(),ChatActivity.class);
                Bundle bundle=new Bundle();
                bundle.putSerializable("friend",friend);
                intent.putExtras(bundle);
                startActivity(intent);
                return false;
            }
        });
        contactList.setOnGroupClickListener(new ExpandableListView.OnGroupClickListener() {
            @Override
            public boolean onGroupClick(ExpandableListView parent, View v, int groupPosition, long id) {
                if (groupState[groupPosition] == 0) {
                    groupState[groupPosition] = 1;
                } else {
                    groupState[groupPosition] = 0;
                }
                return false;
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
    public void notifyFriendListChanged() {
        handler.post(new Runnable() {
            @Override
            public void run() {
                contactAdapter = new ContactAdapter(getActivity(), friendList);
                contactList.setAdapter(contactAdapter);
                for (int i = 0; i < groupState.length; i++) {
                    if (groupState[i] == 1) {
                        contactList.expandGroup(i);
                    }
                }
            }
        });
    }
}
