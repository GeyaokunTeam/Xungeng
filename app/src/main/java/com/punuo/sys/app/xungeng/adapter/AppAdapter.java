package com.punuo.sys.app.xungeng.adapter;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.punuo.sys.app.R;
import com.punuo.sys.app.xungeng.db.DatabaseInfo;
import com.punuo.sys.app.xungeng.model.App;
import com.punuo.sys.app.xungeng.sip.SipInfo;
import com.punuo.sys.app.xungeng.util.FileSizeUtil;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;

import butterknife.Bind;
import butterknife.ButterKnife;

/**
 * Created by acer on 2016/10/24.
 */

public class AppAdapter extends BaseAdapter {
    private Context context;
    private String sdPath = "/mnt/sdcard/xungeng/download/icon/";
    private DownloadListener mDownloadListener;
    private OpenFileListener mOpenFileListener;
    private int dprogress;
    private int lastdprogress;

    public AppAdapter(Context context, DownloadListener downloadListener, OpenFileListener openFileListener) {
        this.context = context;
        mDownloadListener=downloadListener;
        mOpenFileListener=openFileListener;
    }

    @Override
    public int getCount() {
        return SipInfo.appList.size();
    }

    @Override
    public Object getItem(int position) {
        return SipInfo.appList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder viewHolder = null;
        final App app = SipInfo.appList.get(position);
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.appitem, null);
            viewHolder = new ViewHolder(convertView);
            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }
        dprogress = app.getProgress();
        final String appid=app.getAppid();
        final App myApp= DatabaseInfo.sqLiteManager.queryApp(appid);
        viewHolder.appIcon.setImageBitmap(getLoacalBitmap(sdPath + app.getIconname()));
        viewHolder.appName.setText(app.getAppname());
        viewHolder.appSize.setText(FileSizeUtil.FormetFileSize(app.getSize()));
        viewHolder.appDesc.setText(app.getDesc());
        if (myApp.getLocalPath()==null){
            viewHolder.download.setText("下载");
        }else{
            viewHolder.download.setText("安装");
        }
        if (myApp.getIsDownload() == 2) {
            viewHolder.download.setText("下载中");
            viewHolder.progress.setVisibility(View.VISIBLE);
//            if (dprogress != 0) {
//                viewHolder.progress.setProgress(dprogress);
//                lastdprogress = dprogress;
//            } else {
//                viewHolder.progress.setProgress(lastdprogress);
//            }
            viewHolder.progress.setProgress(dprogress);
        } else if (myApp.getIsDownload() == 1) {
            viewHolder.progress.setVisibility(View.GONE);
        }
        final ViewHolder finalViewHolder = viewHolder;
        viewHolder.download.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switch (finalViewHolder.download.getText().toString()) {
                    case "下载":
                        DatabaseInfo.sqLiteManager.updateAppDownload(appid, 2);
                        finalViewHolder.progress.setVisibility(View.VISIBLE);
                        finalViewHolder.progress.setProgress(0);
                        mDownloadListener.onDownload(app.getAppid(), app.getUrl(),app.getName());
                        break;
                    case "安装":
                        File file=new File(myApp.getLocalPath());
                        if (file.exists()) {
                            mOpenFileListener.OpenFile(file);
                        }else{
                            Toast.makeText(context,"安装包不存在",Toast.LENGTH_SHORT).show();
                            DatabaseInfo.sqLiteManager.updateAppLocalPath(app.getAppid(), null);
                            notifyDataSetChanged();

                        }
                        break;
                }
            }
        });
        return convertView;
    }

    static Bitmap getLoacalBitmap(String url) {
        try {
            FileInputStream fis = new FileInputStream(url);
            return BitmapFactory.decodeStream(fis);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return null;
        }
    }

    static class ViewHolder {
        @Bind(R.id.app_icon)
        ImageView appIcon;
        @Bind(R.id.app_name)
        TextView appName;
        @Bind(R.id.app_size)
        TextView appSize;
        @Bind(R.id.app_desc)
        TextView appDesc;
        @Bind(R.id.download)
        Button download;
        @Bind(R.id.progress)
        ProgressBar progress;

        ViewHolder(View view) {
            ButterKnife.bind(this, view);
        }
    }
    public interface DownloadListener {
        public void onDownload(String appId, String appPath, String appName);
    }
    public interface OpenFileListener {
        public void OpenFile(File file);
    }
}
