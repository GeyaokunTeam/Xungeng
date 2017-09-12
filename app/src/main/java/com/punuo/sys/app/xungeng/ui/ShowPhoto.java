package com.punuo.sys.app.xungeng.ui;

import android.content.Intent;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.widget.ImageView;

import com.punuo.sys.app.R;
import com.punuo.sys.app.xungeng.db.DatabaseInfo;
import com.punuo.sys.app.xungeng.ftp.FTP;
import com.punuo.sys.app.xungeng.util.LogUtil;
import com.punuo.sys.app.xungeng.view.CustomProgressDialog;

import java.io.File;

import butterknife.Bind;
import butterknife.ButterKnife;

/**
 * Created by acer on 2016/11/11.
 */
public class ShowPhoto extends MyActivity {
    @Bind(R.id.photo)
    ImageView photo;
    private String mPhotoPath;
    private int type;
    private CustomProgressDialog dialog;
    String ftppath;
    private String localPath;
    private String msgid;
    private Handler handler=new Handler();
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.showphoto);
        ButterKnife.bind(this);
        Intent intent=getIntent();
        mPhotoPath=intent.getStringExtra("path");
        type=intent.getIntExtra("type",0);
        switch (type){
            case 0:
                if (new File(mPhotoPath).exists()) {
                    photo.setImageBitmap(BitmapFactory.decodeFile(mPhotoPath));
                }else{
                    photo.setImageDrawable(getDrawable(R.drawable.ic_error));
                }
                break;
            case 1:
                final File file=new File(mPhotoPath);
                if (file.exists()) {
                    ftppath = intent.getStringExtra("ftppath");
                    ftppath = ftppath.replace("/Thumbnail/", "/");
                    LogUtil.d("111", ftppath);
                    msgid = intent.getStringExtra("msgid");
                    localPath = "/mnt/sdcard/xungeng/Files/Camera/Image/";
                    final String localphotoPath = localPath + file.getName();
                    if (!new File(localphotoPath).exists()) {
                        photo.setImageBitmap(BitmapFactory.decodeFile(mPhotoPath));
                        dialog = new CustomProgressDialog(this);
                        dialog.setCanceledOnTouchOutside(false);
                        dialog.setCancelable(false);
                        dialog.show();
                        new Thread() {
                            @Override
                            public void run() {
                                try {
                                    new FTP().downloadSingleFile(ftppath, localPath, file.getName(), new FTP.DownLoadProgressListener() {
                                        @Override
                                        public void onDownLoadProgress(String currentStep, long downProcess, File file) {
                                            if (currentStep.equals(FTP.FTP_DOWN_SUCCESS)) {
                                                DatabaseInfo.sqLiteManager.updateFileDownload(msgid, 1);
                                                DatabaseInfo.sqLiteManager.updateLocalPath(msgid, localphotoPath);
                                            }
                                        }
                                    });
                                } catch (Exception e) {
                                    e.printStackTrace();
                                } finally {
                                    dialog.dismiss();
                                    handler.post(new Runnable() {
                                        @Override
                                        public void run() {
                                            photo.setImageBitmap(BitmapFactory.decodeFile(localphotoPath));
                                        }
                                    });

                                }
                            }
                        }.start();
                    } else {
                        photo.setImageBitmap(BitmapFactory.decodeFile(localphotoPath));
                    }
                }else{
                    photo.setImageDrawable(getDrawable(R.drawable.ic_error));
                }
                break;
        }

    }

}
