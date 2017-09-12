package com.punuo.sys.app.xungeng.adapter;

import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.graphics.drawable.AnimationDrawable;
import android.media.MediaPlayer;
import android.os.Handler;
import android.os.Message;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import com.punuo.sys.app.R;
import com.punuo.sys.app.xungeng.audiorecord.MediaPlayerManager;
import com.punuo.sys.app.xungeng.db.DatabaseInfo;
import com.punuo.sys.app.xungeng.file.FileInfo;
import com.punuo.sys.app.xungeng.model.FileType;
import com.punuo.sys.app.xungeng.model.Msg;
import com.punuo.sys.app.xungeng.model.MyFile;
import com.punuo.sys.app.xungeng.sip.SipInfo;
import com.punuo.sys.app.xungeng.ui.ShowLocation;
import com.punuo.sys.app.xungeng.ui.ShowPhoto;
import com.punuo.sys.app.xungeng.ui.SmallVideoPlay;
import com.punuo.sys.app.xungeng.util.FileSizeUtil;
import com.punuo.sys.app.xungeng.util.LogUtil;
import com.punuo.sys.app.xungeng.view.CircleImageView;
import com.tb.emoji.EmojiUtil;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

//import com.punuo.sys.app.xungeng.ui.ShowLocation;

/**
 * Created by acer on 2016/10/9.
 */

public class MsgAdapter extends BaseAdapter {
    public static final String TAG = "MsgAdapter";
    private Context mContext;
    private int resourceId;
    private List<Msg> msgList;
    private SimpleDateFormat format1 = new SimpleDateFormat("HH:mm");
    private SimpleDateFormat format2 = new SimpleDateFormat("M月d日 HH:mm");
    private DownloadListener mDownloadListener;
    private OpenFileListener mOpenFileListener;
    private int lastProgress = -1;
    private int mMinWidth;
    //item的最大宽度
    private int mMaxWidth;
    private ImageView animView;
    public Handler openhandler;

    public MsgAdapter(Context context, int textViewResourceId, List<Msg> objects,
                      DownloadListener downloadListener, OpenFileListener openFileListener) {
        mContext = context;
        msgList = objects;
        resourceId = textViewResourceId;
        mDownloadListener = downloadListener;
        mOpenFileListener = openFileListener;

        //获取屏幕的宽度
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        DisplayMetrics outMetrics = new DisplayMetrics();
        wm.getDefaultDisplay().getMetrics(outMetrics);
        //最大宽度为屏幕宽度的百分之七十
        mMaxWidth = (int) (outMetrics.widthPixels * 0.7f);
        //最大宽度为屏幕宽度的百分之十五
        mMinWidth = (int) (outMetrics.widthPixels * 0.15f);
    }

    @Override
    public int getCount() {
        return msgList.size();
    }

    @Override
    public Object getItem(int position) {
        return msgList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        final ViewHolder viewHolder;
        final Msg msg = msgList.get(position);
        if (convertView == null) {
            viewHolder = new ViewHolder();
            convertView = LayoutInflater.from(mContext).inflate(resourceId, null);
            viewHolder.leftLayout = (LinearLayout) convertView.findViewById
                    (R.id.left_layout);
            viewHolder.rightLayout = (LinearLayout) convertView.findViewById
                    (R.id.right_layout);
            viewHolder.leftMsg = (TextView) convertView.findViewById(R.id.left_msg);
            viewHolder.time = (TextView) convertView.findViewById(R.id.time);
            viewHolder.rightMsg = (TextView) convertView.findViewById(R.id.right_msg);
            viewHolder.circleImageView = (CircleImageView) convertView.findViewById(R.id.chatto);

            viewHolder.progressBar = (ProgressBar) convertView.findViewById(R.id.progressBar);
            viewHolder.fileIcon = (ImageView) convertView.findViewById(R.id.file_icon);
            viewHolder.fileName = (TextView) convertView.findViewById(R.id.file_name);
            viewHolder.fileSize = (TextView) convertView.findViewById(R.id.file_size);
            viewHolder.status = (TextView) convertView.findViewById(R.id.status);
            viewHolder.file_layout = (LinearLayout) convertView.findViewById(R.id.file_layout);
            viewHolder.fileshow = (LinearLayout) convertView.findViewById(R.id.file_show);
            viewHolder.videoview = (RelativeLayout) convertView.findViewById(R.id.videoview);
            viewHolder.iconplay = (ImageButton) convertView.findViewById(R.id.icon_play);
            viewHolder.pic = (ImageView) convertView.findViewById(R.id.pic);
            viewHolder.locationLayout = (RelativeLayout) convertView.findViewById(R.id.location_layout);
            viewHolder.locAdress = (TextView) convertView.findViewById(R.id.loc_adress);

            viewHolder.recprogressBar = (ProgressBar) convertView.findViewById(R.id.rec_progressBar);
            viewHolder.recfileIcon = (ImageView) convertView.findViewById(R.id.rec_file_icon);
            viewHolder.recfileName = (TextView) convertView.findViewById(R.id.rec_file_name);
            viewHolder.recfileSize = (TextView) convertView.findViewById(R.id.rec_file_size);
            viewHolder.recstatus = (TextView) convertView.findViewById(R.id.rec_status);
            viewHolder.recfile_layout = (LinearLayout) convertView.findViewById(R.id.rec_file_layout);
            viewHolder.recfileshow = (LinearLayout) convertView.findViewById(R.id.rec_file_show);
            viewHolder.recvideoview = (RelativeLayout) convertView.findViewById(R.id.recvideoview);
            viewHolder.reciconplay = (ImageButton) convertView.findViewById(R.id.rec_icon_play);
            viewHolder.recpic = (ImageView) convertView.findViewById(R.id.recpic);
            viewHolder.reclocationLayout = (RelativeLayout) convertView.findViewById(R.id.rec_location_layout);
            viewHolder.reclocAdress = (TextView) convertView.findViewById(R.id.rec_loc_adress);

            viewHolder.seconds = (TextView) convertView.findViewById(R.id.id_recoder_time);
            viewHolder.length = convertView.findViewById(R.id.id_recoder_length);
            viewHolder.recseconds = (TextView) convertView.findViewById(R.id.id_rec_recoder_time);
            viewHolder.reclength = convertView.findViewById(R.id.id_rec_recoder_length);
            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
            VideoView videoView = (VideoView) viewHolder.videoview.getTag();
            if (videoView != null) {
                viewHolder.videoview.removeView(videoView);
            }
        }

        if (msg.getToUserId().equals(SipInfo.userId)) {
            // 如果是收到的消息，则显示左边的消息布局，将右边的消息布局隐藏
            viewHolder.leftLayout.setVisibility(View.VISIBLE);
            viewHolder.rightLayout.setVisibility(View.GONE);
//            viewHolder.leftMsg.setText(msg.getContent());
            try {
                EmojiUtil.handlerEmojiText(viewHolder.leftMsg, msg.getContent(), mContext);
            } catch (IOException e) {
                e.printStackTrace();
            }
            viewHolder.reclength.setVisibility(View.GONE);
            viewHolder.recseconds.setVisibility(View.GONE);
            viewHolder.recfile_layout.setVisibility(View.GONE);
            viewHolder.recprogressBar.setVisibility(View.GONE);
            viewHolder.leftMsg.setVisibility(View.GONE);
            viewHolder.recfileshow.setVisibility(View.GONE);
            viewHolder.recvideoview.setVisibility(View.GONE);
            viewHolder.recpic.setVisibility(View.GONE);
            viewHolder.reclocationLayout.setVisibility(View.GONE);
            if (msg.getType() == 0) {
                viewHolder.leftMsg.setVisibility(View.VISIBLE);
            }else if (msg.getType()==2){
                viewHolder.recfile_layout.setVisibility(View.VISIBLE);
                viewHolder.reclocationLayout.setVisibility(View.VISIBLE);
                viewHolder.reclocAdress.setText(msg.getContent());
            }else if (msg.getType() == 1) {
                MyFile myFile = DatabaseInfo.sqLiteManager.queryFile(msg.getMsgId());
                viewHolder.recfile_layout.setVisibility(View.VISIBLE);
                if (myFile.getType() == 2) {
                    viewHolder.recfileshow.setVisibility(View.VISIBLE);
                    viewHolder.recfileName.setText(myFile.getFileName());
                    viewHolder.recfileSize.setText(FileSizeUtil.FormetFileSize(myFile.getSize()));
                    setIcon(viewHolder, myFile.getFileType());
                    if (myFile.getIsDownloadFinish() == 0) {
                        viewHolder.recstatus.setText("未下载");
                    } else if (myFile.getIsDownloadFinish() == 1) {
                        viewHolder.recstatus.setText("已下载");
                    } else {
                        viewHolder.recstatus.setText("正在下载");
                        viewHolder.recprogressBar.setVisibility(View.VISIBLE);
                        viewHolder.recprogressBar.setProgress(msg.getProgress());
                    }
                } else if (myFile.getType() == 1) {
                    viewHolder.reciconplay.setVisibility(View.VISIBLE);
                    viewHolder.recvideoview.setVisibility(View.VISIBLE);
                    if (myFile.getIsDownloadFinish() == 0) {

                    } else if (myFile.getIsDownloadFinish() == 1) {

                        viewHolder.recprogressBar.setVisibility(View.GONE);
                    } else {

                        viewHolder.recprogressBar.setVisibility(View.VISIBLE);
                        viewHolder.recprogressBar.setProgress(msg.getProgress());
                    }
                } else if (myFile.getType() == 0) {
                    viewHolder.recprogressBar.setVisibility(View.GONE);
                    viewHolder.reclength.setVisibility(View.VISIBLE);
                    viewHolder.recseconds.setVisibility(View.VISIBLE);
                    viewHolder.recseconds.setText(myFile.getSize() + "\"");
                    ViewGroup.LayoutParams lp = viewHolder.reclength.getLayoutParams();
                    lp.width = (int) (mMinWidth + (mMaxWidth / 60f) * myFile.getSize());
                } else if (myFile.getType() == 3) {
                    viewHolder.recpic.setVisibility(View.VISIBLE);
                    viewHolder.recprogressBar.setVisibility(View.GONE);
                    String path="/mnt/sdcard/xungeng/Files/Camera/Thumbnail/" + myFile.getFileName();
                    if (new File(path).exists()) {
                        viewHolder.recpic.setImageBitmap(BitmapFactory.decodeFile(path));
                    }else{
                        viewHolder.recpic.setImageDrawable(mContext.getDrawable(R.drawable.ic_error));
                    }
                }
            }
        } else if (msg.getFromUserId().equals(SipInfo.userId)) {
            // 如果是发出的消息，则显示右边的消息布局，将左边的消息布局隐藏
            viewHolder.rightLayout.setVisibility(View.VISIBLE);
            viewHolder.leftLayout.setVisibility(View.GONE);
            viewHolder.length.setVisibility(View.GONE);
            viewHolder.seconds.setVisibility(View.GONE);
            viewHolder.file_layout.setVisibility(View.GONE);
            viewHolder.progressBar.setVisibility(View.GONE);
            viewHolder.rightMsg.setVisibility(View.GONE);
            viewHolder.fileshow.setVisibility(View.GONE);
            viewHolder.videoview.setVisibility(View.GONE);
            viewHolder.pic.setVisibility(View.GONE);
            viewHolder.locationLayout.setVisibility(View.GONE);
            try {
                EmojiUtil.handlerEmojiText(viewHolder.rightMsg, msg.getContent(), mContext);
            } catch (IOException e) {
                e.printStackTrace();
            }
            if (msg.getType() == 0) {
                viewHolder.rightMsg.setVisibility(View.VISIBLE);
            } else if (msg.getType() == 2) {
                viewHolder.file_layout.setVisibility(View.VISIBLE);
                viewHolder.locationLayout.setVisibility(View.VISIBLE);
                viewHolder.locAdress.setText(msg.getContent());
            } else if (msg.getType() == 1) {
                final MyFile myFile = DatabaseInfo.sqLiteManager.queryFile(msg.getMsgId());
                viewHolder.file_layout.setVisibility(View.VISIBLE);
                viewHolder.progressBar.setVisibility(View.VISIBLE);
                if (myFile.getType() == 2) {
                    viewHolder.fileshow.setVisibility(View.VISIBLE);
                    if (myFile != null) {
                        viewHolder.fileName.setText(myFile.getFileName());
                        viewHolder.fileSize.setText(FileSizeUtil.FormetFileSize(myFile.getSize()));
                        setIcon(viewHolder, new FileInfo(myFile.getLocalPath(), myFile.getFileName(), false).whichtype());
                        if (myFile.getIsTransferFinish() == 0) {
                            if (lastProgress != -1) {
                                if (lastProgress != msg.getProgress()) {
                                    viewHolder.progressBar.setProgress(msg.getProgress());
                                } else {
                                    viewHolder.progressBar.setProgress(lastProgress);
                                }
                            }
                            lastProgress = msg.getProgress();
                            viewHolder.status.setText("发送中");
                        } else {
                            viewHolder.progressBar.setVisibility(View.GONE);
                            viewHolder.status.setText("已发送");
                            DatabaseInfo.sqLiteManager.updateIsFileTransferFinish(msg.getMsgId(), 1);
                        }
                    }
                } else if (myFile.getType() == 1) {
                    viewHolder.iconplay.setVisibility(View.VISIBLE);
                    viewHolder.videoview.setVisibility(View.VISIBLE);
                    if (myFile.getIsTransferFinish() == 0) {
                        if (lastProgress != -1) {
                            if (lastProgress != msg.getProgress()) {
                                viewHolder.progressBar.setProgress(msg.getProgress());
                            } else {
                                viewHolder.progressBar.setProgress(lastProgress);
                            }
                        }
                        lastProgress = msg.getProgress();
                    } else {
                        viewHolder.progressBar.setVisibility(View.GONE);
                        DatabaseInfo.sqLiteManager.updateIsFileTransferFinish(msg.getMsgId(), 1);

                    }
                } else if (myFile.getType() == 0) {
                    viewHolder.progressBar.setVisibility(View.GONE);
                    viewHolder.length.setVisibility(View.VISIBLE);
                    viewHolder.seconds.setVisibility(View.VISIBLE);
                    viewHolder.seconds.setText(Math.round(msg.getRecordtime()) + "\"");
                    ViewGroup.LayoutParams lp = viewHolder.length.getLayoutParams();
                    lp.width = (int) (mMinWidth + (mMaxWidth / 60f) * msg.getRecordtime());
                } else if (myFile.getType() == 3) {
                    viewHolder.pic.setVisibility(View.VISIBLE);
                    if (myFile.getIsTransferFinish() == 0) {
                        if (lastProgress != -1) {
                            if (lastProgress != msg.getProgress()) {
                                viewHolder.progressBar.setProgress(msg.getProgress());
                            } else {
                                viewHolder.progressBar.setProgress(lastProgress);
                            }
                        }
                        lastProgress = msg.getProgress();
                    } else {
                        viewHolder.progressBar.setVisibility(View.GONE);
                        DatabaseInfo.sqLiteManager.updateIsFileTransferFinish(msg.getMsgId(), 1);
                    }
                    if (new File(myFile.getLocalPath()).exists()) {
                        String thumbnailpath = myFile.getLocalPath().replace(mContext.getString(R.string.Image), mContext.getString(R.string.Thumbnail));
                        viewHolder.pic.setImageBitmap(BitmapFactory.decodeFile(thumbnailpath));
                    }else{
                        viewHolder.pic.setImageDrawable(mContext.getDrawable(R.drawable.ic_error));
                    }
                }
            }
        }
        if (msg.getIsTimeShow() == 1) {
            Date date = new Date(msg.getTime() * 1000L);
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(date);
            Calendar calendar1 = Calendar.getInstance();
            if (calendar.get(Calendar.YEAR) == calendar1.get(Calendar.YEAR) &&
                    calendar.get(Calendar.MONTH) == calendar1.get(Calendar.MONTH) &&
                    calendar.get(Calendar.DATE) == calendar1.get(Calendar.DATE)) {
                viewHolder.time.setText(format1.format(date));
            } else {
                viewHolder.time.setText(format2.format(date));
            }
            viewHolder.time.setVisibility(View.VISIBLE);
        } else {
            viewHolder.time.setVisibility(View.GONE);
        }
        viewHolder.iconplay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                final MyFile myFile = DatabaseInfo.sqLiteManager.queryFile(msg.getMsgId());
                final VideoView sendvideoView = new VideoView(mContext);
                sendvideoView.setVideoPath(myFile.getLocalPath());
                RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(180, 320);
                viewHolder.videoview.addView(sendvideoView, 0, params);
                viewHolder.videoview.setTag(sendvideoView);
                viewHolder.iconplay.setVisibility(View.GONE);
                sendvideoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                    @Override
                    public void onPrepared(MediaPlayer mp) {
                        mp.setVolume(0f, 0f);
                        mp.start();
                        sendvideoView.start();
                    }
                });
                sendvideoView.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                    @Override
                    public void onCompletion(MediaPlayer mp) {
                        viewHolder.videoview.removeView(sendvideoView);
                        viewHolder.iconplay.setVisibility(View.VISIBLE);
                    }
                });


            }
        });
        final View finalConvertView = convertView;
        viewHolder.file_layout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (msg.getType() == 1) {
                    final MyFile myFile = DatabaseInfo.sqLiteManager.queryFile(msg.getMsgId());
                    final File file = new File(myFile.getLocalPath());
                    LogUtil.d(TAG, file.getPath());
                    if (myFile.getType() == 2) {
                        if (file.exists()) {
                            mOpenFileListener.OpenFile(file);
                        } else {
                            Toast.makeText(mContext, "文件已删除", Toast.LENGTH_SHORT).show();
                        }
                    } else if (myFile.getType() == 1) {
                        Intent intent = new Intent(mContext, SmallVideoPlay.class);
                        intent.putExtra("videopath", myFile.getLocalPath());
                        mContext.startActivity(intent);
                    } else if (myFile.getType() == 0) {
                        if (animView != null) {
                            animView.setBackgroundResource(R.drawable.adj);
                            animView = null;
                        }
                        animView = (ImageView) finalConvertView.findViewById(R.id.id_recoder_anim);
                        animView.setBackgroundResource(R.drawable.play_anim);
                        AnimationDrawable animation = (AnimationDrawable) animView.getBackground();
                        animation.start();
                        // 播放录音
                        MediaPlayerManager.playSound(file.getPath(), new MediaPlayer.OnCompletionListener() {

                            public void onCompletion(MediaPlayer mp) {
                                //播放完成后修改图片
                                animView.setBackgroundResource(R.drawable.adj);
                            }
                        });
                    } else if (myFile.getType() == 3) {
                        Intent openpic = new Intent(mContext, ShowPhoto.class);
                        openpic.putExtra("path", myFile.getLocalPath());
                        openpic.putExtra("type",0);
                        mContext.startActivity(openpic);
                    }
                }else if (msg.getType()==2){
                    Intent showloc=new Intent(mContext, ShowLocation.class);
                    showloc.putExtra("location",msg.getContent());
                    mContext.startActivity(showloc);
                }
            }
        });
        viewHolder.reciconplay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final MyFile myFile = DatabaseInfo.sqLiteManager.queryFile(msg.getMsgId());
                final File file = new File(myFile.getLocalPath() + myFile.getFileName());
                if (myFile.getIsDownloadFinish() == 0) {
                    DatabaseInfo.sqLiteManager.updateFileDownload(msg.getMsgId(), 2);
                    mDownloadListener.onDownload(msg.getMsgId(), myFile.getFtpPath(), myFile.getFileName());
                }
                if (myFile.getIsDownloadFinish() == 1) {
                    final VideoView sendvideoView = new VideoView(mContext);
                    sendvideoView.setVideoPath(myFile.getLocalPath() + "/" + myFile.getFileName());
                    RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(180, 320);
                    viewHolder.recvideoview.addView(sendvideoView, 0, params);
                    viewHolder.recvideoview.setTag(sendvideoView);
                    viewHolder.reciconplay.setVisibility(View.GONE);
                    sendvideoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                        @Override
                        public void onPrepared(MediaPlayer mp) {
                            mp.setVolume(0f, 0f);
                            mp.start();
                            sendvideoView.start();
                        }
                    });
                    sendvideoView.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                        @Override
                        public void onCompletion(MediaPlayer mp) {
                            viewHolder.recvideoview.removeView(sendvideoView);
                            viewHolder.reciconplay.setVisibility(View.VISIBLE);
                        }
                    });
                }
            }
        });
        viewHolder.recfile_layout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (msg.getType() == 1) {
                    final MyFile myFile = DatabaseInfo.sqLiteManager.queryFile(msg.getMsgId());
                    final File file = new File(myFile.getLocalPath() + myFile.getFileName());
                    if (myFile.getType()!=3) {
                        if (myFile.getIsDownloadFinish() == 0) {
                            DatabaseInfo.sqLiteManager.updateFileDownload(msg.getMsgId(), 2);
                            mDownloadListener.onDownload(msg.getMsgId(), myFile.getFtpPath(), myFile.getFileName());
                            playAnim(finalConvertView);
                        }
                        if (myFile.getIsDownloadFinish() == 1) {
                            if (file.exists()) {
                                if (myFile.getType() != 0) {
                                    Intent intent = new Intent(mContext, SmallVideoPlay.class);
                                    intent.putExtra("videopath", file.getAbsolutePath());
                                    mContext.startActivity(intent);
                                } else {
                                    playAnim(finalConvertView);
                                    // 播放录音
                                    MediaPlayerManager.playSound(file.getPath(), new MediaPlayer.OnCompletionListener() {

                                        public void onCompletion(MediaPlayer mp) {
                                            //播放完成后修改图片
                                            animView.setBackgroundResource(R.drawable.rec_adj);
                                        }
                                    });
                                }
                            } else {
                                Toast.makeText(mContext, "文件已删除", Toast.LENGTH_SHORT).show();
                            }
                        }
                    }else{
                        Intent openpic = new Intent(mContext, ShowPhoto.class);
                        openpic.putExtra("path", "/mnt/sdcard/xungeng/Files/Camera/Thumbnail/" + myFile.getFileName());
                        openpic.putExtra("type",1);
                        openpic.putExtra("ftppath",myFile.getFtpPath());
                        openpic.putExtra("msgid",myFile.getFileId());
                        mContext.startActivity(openpic);
                    }
                }else if (msg.getType()==2){
                    Intent showloc=new Intent(mContext, ShowLocation.class);
                    showloc.putExtra("location",msg.getContent());
                    mContext.startActivity(showloc);
                }
            }
        });
        openhandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {

                // 播放录音
                MyFile myFile = DatabaseInfo.sqLiteManager.queryFile((String) msg.obj);
                File file = new File(myFile.getLocalPath() + myFile.getFileName());
                MediaPlayerManager.playSound(file.getPath(), new MediaPlayer.OnCompletionListener() {

                    public void onCompletion(MediaPlayer mp) {
                        //播放完成后修改图片
                        animView.setBackgroundResource(R.drawable.rec_adj);
                    }
                });
            }
        };
        return convertView;
    }

    private void playAnim(View finalConvertView) {
        if (animView != null) {
            animView.setBackgroundResource(R.drawable.rec_adj);
            animView = null;
        }
        animView = (ImageView) finalConvertView.findViewById(R.id.id_rec_recoder_anim);
        animView.setBackgroundResource(R.drawable.rec_play_anim);
        AnimationDrawable animation = (AnimationDrawable) animView.getBackground();
        animation.start();
    }


    class ViewHolder {
        LinearLayout leftLayout;
        LinearLayout rightLayout;
        LinearLayout file_layout;
        LinearLayout recfile_layout;
        TextView leftMsg;
        TextView time;
        TextView rightMsg;
        CircleImageView circleImageView;

        ProgressBar progressBar;
        ImageView fileIcon;
        TextView fileName;
        TextView fileSize;
        TextView status;
        LinearLayout fileshow;
        RelativeLayout videoview;
        ImageButton iconplay;
        ImageView pic;
        RelativeLayout locationLayout;
        TextView locAdress;

        ProgressBar recprogressBar;
        ImageView recfileIcon;
        TextView recfileName;
        TextView recfileSize;
        TextView recstatus;
        LinearLayout recfileshow;
        RelativeLayout recvideoview;
        ImageButton reciconplay;
        ImageView recpic;
        RelativeLayout reclocationLayout;
        TextView reclocAdress;


        TextView seconds;
        View length;
        TextView recseconds;
        View reclength;
    }

    public void setIcon(ViewHolder viewHolder, FileType fileType) {
        switch (fileType) {
            case DOC:
                viewHolder.fileIcon.setImageResource(R.drawable.doc);
                break;
            case DOCX:
                viewHolder.fileIcon.setImageResource(R.drawable.docx);
                break;
            case PPT:
                viewHolder.fileIcon.setImageResource(R.drawable.ppt);
                break;
            case PPTX:
                viewHolder.fileIcon.setImageResource(R.drawable.pptx);
                break;
            case UNKNOWN:
                viewHolder.fileIcon.setImageResource(R.drawable.unknown);
                break;
            case XLS:
                viewHolder.fileIcon.setImageResource(R.drawable.xls);
                break;
            case XLXS:
                viewHolder.fileIcon.setImageResource(R.drawable.xlxs);
                break;
            case PDF:
                viewHolder.fileIcon.setImageResource(R.drawable.pdf);
                break;
            case PNG:
                viewHolder.fileIcon.setImageResource(R.drawable.png);
                break;
            case TXT:
                viewHolder.fileIcon.setImageResource(R.drawable.txt);
                break;
            case MP3:
                viewHolder.fileIcon.setImageResource(R.drawable.mp3);
                break;
            case MP4:
                viewHolder.fileIcon.setImageResource(R.drawable.mp4);
                break;
            case BMP:
                viewHolder.fileIcon.setImageResource(R.drawable.bmp);
                break;
            case GIF:
                viewHolder.fileIcon.setImageResource(R.drawable.gif);
                break;
            case AVI:
                viewHolder.fileIcon.setImageResource(R.drawable.avi);
                break;
            case WMA:
                viewHolder.fileIcon.setImageResource(R.drawable.wma);
                break;
            case RAR:
                viewHolder.fileIcon.setImageResource(R.drawable.rar);
                break;
            case ZIP:
                viewHolder.fileIcon.setImageResource(R.drawable.zip);
                break;
            case WAV:
                viewHolder.fileIcon.setImageResource(R.drawable.wav);
                break;
            case JPG:
                viewHolder.fileIcon.setImageResource(R.drawable.jpg);
            case NULL:
                break;
            default:
                break;
        }
    }

    public void setIcon(ViewHolder viewHolder, String fileType) {
        switch (fileType) {
            case "doc":
                viewHolder.recfileIcon.setImageResource(R.drawable.doc);
                break;
            case "docx":
                viewHolder.recfileIcon.setImageResource(R.drawable.docx);
                break;
            case "ppt":
                viewHolder.recfileIcon.setImageResource(R.drawable.ppt);
                break;
            case "pptx":
                viewHolder.recfileIcon.setImageResource(R.drawable.pptx);
                break;
            case "unknown":
                viewHolder.recfileIcon.setImageResource(R.drawable.unknown);
                break;
            case "xls":
                viewHolder.recfileIcon.setImageResource(R.drawable.xls);
                break;
            case "xlxs":
                viewHolder.recfileIcon.setImageResource(R.drawable.xlxs);
                break;
            case "pdf":
                viewHolder.recfileIcon.setImageResource(R.drawable.pdf);
                break;
            case "png":
                viewHolder.recfileIcon.setImageResource(R.drawable.png);
                break;
            case "txt":
                viewHolder.recfileIcon.setImageResource(R.drawable.txt);
                break;
            case "mp3":
                viewHolder.recfileIcon.setImageResource(R.drawable.mp3);
                break;
            case "mp4":
                viewHolder.recfileIcon.setImageResource(R.drawable.mp4);
                break;
            case "bmp":
                viewHolder.recfileIcon.setImageResource(R.drawable.bmp);
                break;
            case "gif":
                viewHolder.recfileIcon.setImageResource(R.drawable.gif);
                break;
            case "avi":
                viewHolder.recfileIcon.setImageResource(R.drawable.avi);
                break;
            case "wma":
                viewHolder.recfileIcon.setImageResource(R.drawable.wma);
                break;
            case "rar":
                viewHolder.recfileIcon.setImageResource(R.drawable.rar);
                break;
            case "zip":
                viewHolder.recfileIcon.setImageResource(R.drawable.zip);
                break;
            case "wav":
                viewHolder.recfileIcon.setImageResource(R.drawable.wav);
                break;
            case "jpg":
                viewHolder.recfileIcon.setImageResource(R.drawable.jpg);
                break;
            case "null":
                break;
            default:
                viewHolder.recfileIcon.setImageResource(R.drawable.unknown);
                break;
        }

    }

    public interface DownloadListener {
        public void onDownload(String msgId, String filePath, String fileName);
    }

    public interface OpenFileListener {
        public void OpenFile(File file);
    }


}

