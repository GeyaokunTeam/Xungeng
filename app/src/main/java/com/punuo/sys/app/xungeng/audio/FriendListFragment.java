package com.punuo.sys.app.xungeng.audio;


import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.ListFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import com.punuo.sys.app.R;
import com.punuo.sys.app.xungeng.adapter.FriendAdapter;

import java.util.Collections;

import static com.punuo.sys.app.xungeng.sip.SipInfo.friends;


/**
 * A simple {@link Fragment} subclass.
 */
public class FriendListFragment extends ListFragment {
    private FriendAdapter friendAdapter;
    private int lastPosition = -1;

    public FriendListFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate( Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Collections.sort(friends);
        friendAdapter = new FriendAdapter(getActivity(), friends);
        setListAdapter(friendAdapter);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_dev_list, container, false);
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);
//        Friend friend = friends.get(position);
//        if (friend.isLive()) {
//            if (friend.isSelect()) {
//                friend.setSelect(false);
//                lastPosition = -1;
//            } else {
//                friend.setSelect(true);
//                if (lastPosition >= 0) {
//                    friends.get(lastPosition).setSelect(false);
//                }
//                lastPosition = position;
//            }
//            friendAdapter.notifyDataSetChanged();
//        } else {
//            Toast.makeText(getActivity().getApplicationContext(), "该设备不在线", Toast.LENGTH_SHORT).show();
//        }
    }

    public int getLastPosition() {
        return lastPosition;
    }

    public void notifyFriendListChanged() {
//        lastPosition = -1;
        Collections.sort(friends);
        friendAdapter = new FriendAdapter(getActivity(), friends);
        setListAdapter(friendAdapter);
    }
}
