package com.punuo.sys.app.xungeng.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.punuo.sys.app.R;
import com.punuo.sys.app.xungeng.audio.PhoneCall;
import com.punuo.sys.app.xungeng.model.Friend;

import java.util.List;

import static com.punuo.sys.app.xungeng.sip.SipInfo.phoneNum;


/**
 * Created by chenblue23 on 2016/5/4.
 */
public class FriendAdapter extends BaseAdapter{
    private Context mContext;
    private List<Friend> list;

    public FriendAdapter(Context mContext, List<Friend> list) {
        this.mContext = mContext;
        this.list = list;
    }

    @Override
    public int getCount() {
        return list.size();
    }

    @Override
    public Object getItem(int position) {
        return list.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        ViewHolder holder = null;
        if (convertView == null) {
            holder = new ViewHolder();
            convertView = LayoutInflater.from(mContext).inflate(R.layout.listfragmentitem, null);
            holder.textView = (TextView) convertView.findViewById(R.id.devName);
            holder.userView = (ImageView) convertView.findViewById(R.id.devIcon);
            holder.checkView = (ImageView) convertView.findViewById(R.id.check);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }
        if (list.get(position).isLive()) {
            holder.userView.setImageResource(R.drawable.icon_online);
            holder.checkView.setVisibility(View.VISIBLE);
            holder.checkView.setImageResource(R.drawable.icon_btncall);
        } else {
            holder.userView.setImageResource(R.drawable.icon_offline);
            holder.checkView.setVisibility(View.GONE);
        }
        holder.textView.setText(list.get(position).getRealName());
        holder.checkView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                phoneNum = list.get(position).getPhoneNum();
                PhoneCall.actionStart(mContext, phoneNum, 1);
            }
        });
        return convertView;
    }

    private static class ViewHolder {
        TextView textView;
        ImageView userView;
        ImageView checkView;
    }
}
