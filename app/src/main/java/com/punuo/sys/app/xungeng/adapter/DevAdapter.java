package com.punuo.sys.app.xungeng.adapter;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.punuo.sys.app.xungeng.model.Device;
import com.punuo.sys.app.R;
import com.punuo.sys.app.xungeng.sip.BodyFactory;
import com.punuo.sys.app.xungeng.sip.SipInfo;
import com.punuo.sys.app.xungeng.sip.SipMessageFactory;
import com.punuo.sys.app.xungeng.util.LogUtil;
import com.punuo.sys.app.xungeng.video.RtpVideo;
import com.punuo.sys.app.xungeng.video.SendActivePacket;
import com.punuo.sys.app.xungeng.video.VideoInfo;
import com.punuo.sys.app.xungeng.video.VideoPlay;
import com.punuo.sys.app.xungeng.view.CustomProgressDialog;

import org.zoolu.sip.address.NameAddress;
import org.zoolu.sip.address.SipURL;
import org.zoolu.sip.message.Message;

import java.net.SocketException;
import java.util.List;

import static com.punuo.sys.app.xungeng.ui.VideoFragment.handler;

/**
 * Created by chenblue23 on 2016/5/4.
 */
public class DevAdapter extends BaseAdapter{
    private Context mContext;
    private List<Device> list;
    private CustomProgressDialog inviting;
    public DevAdapter(Context mContext, List<Device> list) {
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
            holder.checkView.setImageResource(R.drawable.btn_play);
        } else {
            holder.userView.setImageResource(R.drawable.icon_offline);
            holder.checkView.setVisibility(View.GONE);
        }
        holder.textView.setText(list.get(position).getName());
        holder.checkView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String devId = SipInfo.devList.get(position).getDevId();
                devId = devId.substring(0, devId.length() - 4).concat("0160");//设备id后4位替换成0160
                String devName = SipInfo.devList.get(position).getName();
                final String devType = SipInfo.devList.get(position).getDevType();
                SipURL sipURL = new SipURL(devId, SipInfo.serverIp, SipInfo.SERVER_PORT);
                SipInfo.toDev = new NameAddress(devName, sipURL);
                SipInfo.queryResponse = false;
                SipInfo.inviteResponse = false;
                inviting = new CustomProgressDialog(mContext);
                inviting.setCancelable(false);
                inviting.setCanceledOnTouchOutside(false);
                inviting.show();
                new Thread() {
                    @Override
                    public void run() {
                        try {
                            Message query = SipMessageFactory.createOptionsRequest(SipInfo.sipUser, SipInfo.toDev,
                                    SipInfo.user_from, BodyFactory.createQueryBody(devType));
                            outer:
                            for (int i = 0; i < 3; i++) {
                                SipInfo.sipUser.sendMessage(query);
                                for (int j = 0; j < 20; j++) {
                                    sleep(100);
                                    if (SipInfo.queryResponse) {
                                        break outer;
                                    }
                                }
                                if (SipInfo.queryResponse) {
                                    break;
                                }
                            }
                            if (SipInfo.queryResponse) {
                                Message invite = SipMessageFactory.createInviteRequest(SipInfo.sipUser,
                                        SipInfo.toDev, SipInfo.user_from, BodyFactory.createMediaBody());
                                outer2:
                                for (int i = 0; i < 3; i++) {
                                    SipInfo.sipUser.sendMessage(invite);
                                    for (int j = 0; j < 20; j++) {
                                        sleep(100);
                                        if (SipInfo.inviteResponse) {
                                            break outer2;
                                        }
                                    }
                                    if (SipInfo.inviteResponse) {
                                        break;
                                    }
                                }
                            }
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        } finally {
                            inviting.dismiss();
                            if (SipInfo.queryResponse && SipInfo.inviteResponse) {
                                LogUtil.d("DevAdapter", "视频请求成功");
                                SipInfo.decoding = true;
                                try {
                                    VideoInfo.rtpVideo = new RtpVideo(VideoInfo.rtpIp, VideoInfo.rtpPort);
                                    VideoInfo.sendActivePacket = new SendActivePacket();
                                    VideoInfo.sendActivePacket.startThread();
                                    mContext.startActivity(new Intent(mContext, VideoPlay.class));
                                } catch (SocketException e) {
                                    e.printStackTrace();
                                }
                            } else {
                                LogUtil.d("DevAdapter", "视频请求失败");
                                handler.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        new AlertDialog.Builder(mContext)
                                                .setTitle("视频请求失败！")
                                                .setMessage("请重新尝试")
                                                .setPositiveButton("确定", null).show();
                                    }
                                });
                            }
                        }
                    }
                }.start();
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
