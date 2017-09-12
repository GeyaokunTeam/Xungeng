package com.punuo.sys.app.xungeng.ui;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AbsListView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.punuo.sys.app.R;
import com.punuo.sys.app.xungeng.adapter.MsgAdapter;
import com.punuo.sys.app.xungeng.audiorecord.AudioRecorderButton;
import com.punuo.sys.app.xungeng.db.DatabaseInfo;
import com.punuo.sys.app.xungeng.file.FileInfo;
import com.punuo.sys.app.xungeng.ftp.FTP;
import com.punuo.sys.app.xungeng.model.Friend;
import com.punuo.sys.app.xungeng.model.Msg;
import com.punuo.sys.app.xungeng.model.MyFile;
import com.punuo.sys.app.xungeng.sip.BodyFactory;
import com.punuo.sys.app.xungeng.sip.SipInfo;
import com.punuo.sys.app.xungeng.sip.SipMessageFactory;
import com.punuo.sys.app.xungeng.sip.SipUser;
import com.punuo.sys.app.xungeng.util.LogUtil;
import com.punuo.sys.app.xungeng.util.MD5Util;
import com.tb.emoji.Emoji;
import com.tb.emoji.EmojiUtil;
import com.tb.emoji.FaceFragment;

import org.zoolu.sip.address.NameAddress;
import org.zoolu.sip.address.SipURL;
import org.zoolu.sip.message.Message;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import butterknife.Bind;
import butterknife.ButterKnife;

import static com.punuo.sys.app.xungeng.ui.Login.closeKeyboard;

/**
 * Created by acer on 2016/11/16.
 */
public class ChatActivity extends MyActivity implements View.OnClickListener, SipUser.MessageListener, FaceFragment.OnEmojiClickListener {

    @Bind(R.id.content_list)
    ListView contentList;
    @Bind(R.id.input_type)
    ImageButton inputType;
    @Bind(R.id.content)
    EditText content;
    @Bind(R.id.recorder_button)
    AudioRecorderButton recorderButton;
    @Bind(R.id.express)
    ImageButton express;
    @Bind(R.id.send)
    Button send;
    @Bind(R.id.addmore)
    ImageButton addmore;
    @Bind(R.id.expresslayout)
    FrameLayout expresslayout;
    @Bind(R.id.addfolder)
    ImageButton addfolder;
    @Bind(R.id.small_video)
    ImageButton smallVideo;
    @Bind(R.id.pic)
    ImageButton pic;
    @Bind(R.id.loc)
    ImageButton loc;
    @Bind(R.id.more)
    RelativeLayout more;
    FaceFragment faceFragment = FaceFragment.Instance();
    @Bind(R.id.back)
    ImageButton back;
    @Bind(R.id.refresh)
    ImageButton refresh;
    @Bind(R.id.title)
    TextView title;
    private Friend friend;
    private String currentFriendId;
    private MsgAdapter msgAdapter;
    private List<Msg> msgList = new ArrayList<>();
    private int inputtype = 0;
    private int anInt = 0;
    private int expInt = 0;
    private static String TAG = "ChatActivity";
    private String sdPath = "/mnt/sdcard/xungeng/download/";
    private int result;
    private Handler handler = new Handler();
    private String filePath;
    private String smallvideopath;
    private boolean istop = false;
    private String picPath;
    private String thumbnailPath;
    private String address;
    private ArrayList<String> picPaths;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivityCollector.addActivity(this);
        setContentView(R.layout.chat);
        ButterKnife.bind(this);
        friend = (Friend) getIntent().getSerializableExtra("friend");
        currentFriendId = friend.getUserId();
        refresh.setVisibility(View.GONE);
        back.setOnClickListener(this);
        send.setOnClickListener(this);
        addfolder.setOnClickListener(this);
        smallVideo.setOnClickListener(this);
        pic.setOnClickListener(this);
        inputType.setOnClickListener(this);
        express.setOnClickListener(this);
        addmore.setOnClickListener(this);
        express.setOnClickListener(this);
        loc.setOnClickListener(this);
        content.addTextChangedListener(watcher);
        title.setText(friend.getRealName());
        msgAdapter = new MsgAdapter(this, R.layout.msg_item, msgList, downloadListener, openFileListener);
        contentList.setAdapter(msgAdapter);
        msgList.clear();
        msgList.addAll(DatabaseInfo.sqLiteManager.queryMessage(SipInfo.userId, friend.getUserId(), "2147483647", 10));
        msgAdapter.notifyDataSetChanged();
        contentList.setSelection(msgList.size());
        SipInfo.sipUser.setMessageListener(this);
        content.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                more.setVisibility(View.GONE);
                content.requestFocus();
                openKeyboard(ChatActivity.this, content);
                express.setImageDrawable(getResources().getDrawable(R.drawable.smile));
                getSupportFragmentManager().beginTransaction().remove(faceFragment).commit();
                anInt = 0;
                expInt = 0;
                return false;
            }
        });
        recorderButton.setFinishRecorderCallBack(new AudioRecorderButton.AudioFinishRecorderCallBack() {
            @Override
            public void onFinish(final float seconds, String filePath) {
                LogUtil.d(TAG, filePath);
                final String content = "[语音消息]";
                final int time = (int) (System.currentTimeMillis() / 1000);
                final int isTimeShow;
                final String id = SipInfo.userId + System.currentTimeMillis();
                final String ftpPath = "/" + SipInfo.userId;
                final File file = new File(filePath);
                final long size = file.length();
                final String md5 = MD5Util.getFileMD5String(file);
                FileInfo fileInfo = new FileInfo(file.getPath(), file.getName(), file.isDirectory());
                final String filetype = getType(fileInfo);
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            new FTP().uploadSingleFile(file, ftpPath, new FTP.UploadProgressListener() {
                                @Override
                                public void onUploadProgress(String currentStep, long uploadSize, File file) {
                                    LogUtil.d(TAG, currentStep);
                                    android.os.Message msg = new android.os.Message();
                                    msg.obj = id;
                                    if (currentStep.equals(FTP.FTP_UPLOAD_SUCCESS)) {
                                        LogUtil.d(TAG, "-----上传成功--");
                                        msg.what = 0x1112;
                                        myhandler.sendMessage(msg);
                                    } else if (currentStep.equals(FTP.FTP_UPLOAD_LOADING)) {
                                        long fize = file.length();
                                        float num = (float) uploadSize / (float) fize;
                                        result = (int) (num * 100);
//                                    Log.d(TAG, "-----上传---" + result + "%");
//                                    Log.d(TAG, "run: " + result);
                                        msg.arg1 = result;
                                        msg.what = 0x1111;
                                        //将消息发送给主线程的Handler
                                        myhandler.sendMessage(msg);
                                    }
                                }
                            });
                        } catch (IOException e) {
                            e.printStackTrace();
                        } finally {
                            SipURL remote = new SipURL(currentFriendId, SipInfo.serverIp, SipInfo.SERVER_PORT);
                            SipInfo.toUser = new NameAddress(friend.getUsername(), remote);
                            SipInfo.sipUser.sendMessage(SipMessageFactory.createNotifyRequest(SipInfo.sipUser, SipInfo.toUser
                                    , SipInfo.user_from, BodyFactory.createFileTransferBody(SipInfo.userId, currentFriendId, id,
                                            file.getName(), filetype, "", "/" + SipInfo.userId + "/" + file.getName(), (long) seconds, md5, 0)));
                        }
                    }
                }).start();
                if (time - DatabaseInfo.sqLiteManager.queryLastTime(SipInfo.userId, friend.getUserId()) > 300) {
                    isTimeShow = 1;
                } else {
                    isTimeShow = 0;
                }
                DatabaseInfo.sqLiteManager.insertMessage(id, SipInfo.userId, currentFriendId,
                        time, isTimeShow, content, 1, seconds);
                DatabaseInfo.sqLiteManager.insertFile(id, file.getName(), SipInfo.username,
                        filetype, time, filePath, ftpPath + "/" + file.getName(), size, md5, 0, 0, 0);
                Msg msg = new Msg();
                msg.setMsgId(id);
                msg.setFromUserId(SipInfo.userId);
                msg.setToUserId(friend.getUserId());
                msg.setTime(time);
                msg.setIsTimeShow(isTimeShow);
                msg.setContent(content);
                msg.setType(1);
                msg.setRecordtime(seconds);
                msgList.add(msg);
                msgAdapter.notifyDataSetChanged();
                contentList.setSelection(msgList.size());
                DatabaseInfo.sqLiteManager.insertLastestMsgTo(0, friend.getUserId(), content, time);
            }
        });

        contentList.setOnScrollListener(new AbsListView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {
                if (istop && scrollState == AbsListView.OnScrollListener.SCROLL_STATE_IDLE) {
                    istop = false;
                    int time = msgList.get(0).getTime();
                    int before = msgList.size();
                    msgList.addAll(0, DatabaseInfo.sqLiteManager.queryMessage(SipInfo.userId, currentFriendId, "" + time, 10));
                    int after = msgList.size();
                    msgAdapter.notifyDataSetChanged();
                    contentList.setSelection(after - before);

                }
            }

            @Override
            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
                if (firstVisibleItem == 0) {
                    istop = true;
                } else {
                    istop = false;
                }
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        msgAdapter.notifyDataSetChanged();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        ActivityCollector.removeActivity(this);
    }

    @Override
    public void onBackPressed() {
        int i = DatabaseInfo.sqLiteManager.queryLastestMsgCount(0, friend.getUserId());
        DatabaseInfo.sqLiteManager.updateLastestMsg(friend.getUserId());
        ActivityCollector.removeActivity(this);
        finish();
    }

    public static void openKeyboard(Context context, View view) {
        InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.showSoftInput(view, InputMethodManager.SHOW_FORCED);
    }

    private TextWatcher watcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            if (s.length()>0){
                send.setVisibility(View.VISIBLE);
                addmore.setVisibility(View.GONE);
            }
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            if (s.length() == 0) {
                send.setVisibility(View.GONE);
                addmore.setVisibility(View.VISIBLE);
            } else if (s.length() > 0) {
                send.setVisibility(View.VISIBLE);
                addmore.setVisibility(View.GONE);
            }
        }

        @Override
        public void afterTextChanged(Editable s) {

        }
    };

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.back:
                DatabaseInfo.sqLiteManager.updateLastestMsg(friend.getUserId());
                closeKeyboard(ChatActivity.this, getWindow().getDecorView());
                ActivityCollector.removeActivity(this);
                finish();
                break;
            case R.id.send:
                final String con = content.getText().toString();
                if (!TextUtils.isEmpty(con)) {
                    final int time = (int) (System.currentTimeMillis() / 1000);
                    final int isTimeShow;
                    final String id = SipInfo.userId + System.currentTimeMillis();
                    SipURL remote = new SipURL(friend.getUserId(), SipInfo.serverIp, SipInfo.SERVER_PORT);
                    SipInfo.toUser = new NameAddress(friend.getUsername(), remote);
                    Message message = SipMessageFactory.createNotifyRequest(SipInfo.sipUser, SipInfo.toUser,
                            SipInfo.user_from, BodyFactory.createMessageBody(id, SipInfo.userId, friend.getUserId(), con, "", 0));
                    SipInfo.sipUser.sendMessage(message);
                    if (time - DatabaseInfo.sqLiteManager.queryLastTime(SipInfo.userId, friend.getUserId()) > 300) {
                        isTimeShow = 1;
                    } else {
                        isTimeShow = 0;
                    }
                    DatabaseInfo.sqLiteManager.insertMessage(id, SipInfo.userId, friend.getUserId(),
                            time, isTimeShow, con, 0, 0);
                    Msg msg = new Msg();
                    msg.setMsgId(id);
                    msg.setFromUserId(SipInfo.userId);
                    msg.setToUserId(friend.getUserId());
                    msg.setTime(time);
                    msg.setIsTimeShow(isTimeShow);
                    msg.setContent(con);
                    msg.setType(0);
                    msgList.add(msg);
                    msgAdapter.notifyDataSetChanged();
                    contentList.setSelection(msgList.size());
                    content.setText("");
                    DatabaseInfo.sqLiteManager.insertLastestMsgTo(0, friend.getUserId(), con, time);
                }
                break;
            case R.id.addfolder:
                Intent intent = new Intent(ChatActivity.this, FileChooserActivity.class);
                if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED))
                    startActivityForResult(intent, 1);
                else
                    Toast.makeText(this, getText(R.string.sdcard_unmonted_hint), Toast.LENGTH_SHORT).show();
                break;
            case R.id.small_video:
                Intent videointent = new Intent(ChatActivity.this, MakeSmallVideo.class);
                startActivityForResult(videointent, 3);
                break;
            case R.id.pic:
                LinearLayout linearLayout = (LinearLayout) getLayoutInflater().inflate(R.layout.popup, null);
                Button camera = (Button) linearLayout.findViewById(R.id.camera);
                Button album= (Button) linearLayout.findViewById(R.id.album);
                final AlertDialog dialog = new AlertDialog.Builder(this)
                        .setView(linearLayout)
                        .create();
                dialog.show();
                camera.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent openCamera = new Intent(ChatActivity.this, MyCamera.class);
                        openCamera.putExtra("type", 1);
                        startActivityForResult(openCamera, 4);
                        dialog.dismiss();
                    }
                });
                album.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent openAlbum = new Intent(ChatActivity.this, AlbumAty.class);
                        openAlbum.putExtra("type", 1);
                        startActivityForResult(openAlbum, 6);
                        dialog.dismiss();
                    }
                });
                break;
            case R.id.input_type:
                express.setImageDrawable(getResources().getDrawable(R.drawable.smile));
                getSupportFragmentManager().beginTransaction().remove(faceFragment).commit();
                if (inputtype % 2 == 0) {
                    inputType.setImageDrawable(getResources().getDrawable(R.drawable.keyboard_c));
                    recorderButton.setVisibility(View.VISIBLE);
                    content.setVisibility(View.GONE);
                    more.setVisibility(View.GONE);
                    content.clearFocus();
                    closeKeyboard(ChatActivity.this, getWindow().getDecorView());
                } else if (inputtype % 2 == 1) {
                    inputType.setImageDrawable(getResources().getDrawable(R.drawable.audio_c));
                    recorderButton.setVisibility(View.GONE);
                    content.setVisibility(View.VISIBLE);
                    content.requestFocus();
                    openKeyboard(ChatActivity.this, content);
                }
                inputtype++;
                anInt = 0;
                expInt = 0;
                break;
            case R.id.addmore:
                recorderButton.setVisibility(View.GONE);
                content.setVisibility(View.VISIBLE);
                express.setImageDrawable(getResources().getDrawable(R.drawable.smile));
                getSupportFragmentManager().beginTransaction().remove(faceFragment).commit();
                if (anInt % 2 == 0) {
                    more.setVisibility(View.VISIBLE);
                    content.clearFocus();
                    inputType.setImageDrawable(getResources().getDrawable(R.drawable.audio_c));
                    closeKeyboard(ChatActivity.this, getWindow().getDecorView());
                    inputtype = 0;
                } else if (anInt % 2 == 1) {
                    more.setVisibility(View.GONE);
                    content.requestFocus();
                    openKeyboard(ChatActivity.this, content);
                }
                anInt++;
                expInt = 0;
                break;
            case R.id.express:
                recorderButton.setVisibility(View.GONE);
                inputType.setImageDrawable(getResources().getDrawable(R.drawable.audio_c));
                content.setVisibility(View.VISIBLE);
                content.clearFocus();
                more.setVisibility(View.GONE);
                anInt = 0;
                inputtype = 0;
                if (expInt % 2 == 0) {
                    express.setImageDrawable(getResources().getDrawable(R.drawable.keyboard_c));
                    getSupportFragmentManager().beginTransaction().add(R.id.expresslayout, faceFragment).commit();
                    closeKeyboard(ChatActivity.this, getWindow().getDecorView());
                } else if (expInt % 2 == 1) {
                    express.setImageDrawable(getResources().getDrawable(R.drawable.smile));
                    openKeyboard(ChatActivity.this, content);
                    getSupportFragmentManager().beginTransaction().remove(faceFragment).commit();
                }
                expInt++;
                break;
            case R.id.loc:
                Intent openSend = new Intent(ChatActivity.this, SendLocation.class);
                startActivityForResult(openSend,5);
                break;
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == 1) {
            if (resultCode == RESULT_OK) {
                filePath = data.getStringExtra("FilePath");
                final String id = SipInfo.userId + System.currentTimeMillis();
                final String content = "[文件]";
                final int time = (int) (System.currentTimeMillis() / 1000);
                final String ftpPath = "/" + SipInfo.userId;
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        final File file = new File(filePath);
                        final long size = file.length();
                        final String md5 = MD5Util.getFileMD5String(file);
                        FileInfo fileInfo = new FileInfo(file.getPath(), file.getName(), file.isDirectory());
                        final String filetype = getType(fileInfo);
                        handler.post(new Runnable() {
                            @Override
                            public void run() {

                                final int isTimeShow;
                                if (time - DatabaseInfo.sqLiteManager.queryLastTime(SipInfo.userId, currentFriendId) > 300) {
                                    isTimeShow = 1;
                                } else {
                                    isTimeShow = 0;
                                }
                                LogUtil.d(TAG, "run: " + filePath);
                                DatabaseInfo.sqLiteManager.insertMessage(id, SipInfo.userId, currentFriendId,
                                        time, isTimeShow, content, 1, 0);
                                DatabaseInfo.sqLiteManager.insertFile(id, file.getName(), SipInfo.username, filetype, time, filePath, ftpPath + "/" + file.getName(), size, md5, 2, 0, 0);
                                DatabaseInfo.sqLiteManager.insertLastestMsgTo(0, currentFriendId, content, time);
                                final Msg msgfile = new Msg();
                                msgfile.setMsgId(id);
                                msgfile.setContent(content);
                                msgfile.setFromUserId(SipInfo.userId);
                                msgfile.setToUserId(currentFriendId);
                                msgfile.setTime(time);
                                msgfile.setIsTimeShow(isTimeShow);
                                msgfile.setType(1);
//                            msgfile.setIsTransferFinish(0);
//                            msgfile.setFilePath(filePath);
                                msgList.add(msgfile);
                                msgAdapter.notifyDataSetChanged(); // 当有新消息时，刷新ListView中的显示
                                contentList.setSelection(msgList.size()); // 将ListView定位到最后一行

                            }
                        });
                        try {
                            new FTP().uploadSingleFile(file, ftpPath, new FTP.UploadProgressListener() {
                                @Override
                                public void onUploadProgress(String currentStep, long uploadSize, File file) {
                                    LogUtil.d(TAG, currentStep);
                                    android.os.Message msg = new android.os.Message();
                                    msg.obj = id;
                                    if (currentStep.equals(FTP.FTP_UPLOAD_SUCCESS)) {
//                                    Log.d(TAG, "-----上传成功--");
                                        msg.what = 0x1112;
                                        myhandler.sendMessage(msg);
                                    } else if (currentStep.equals(FTP.FTP_UPLOAD_LOADING)) {
                                        long fize = file.length();
                                        float num = (float) uploadSize / (float) fize;
                                        result = (int) (num * 100);
//                                    Log.d(TAG, "-----上传---" + result + "%");
//                                    Log.d(TAG, "run: " + result);
                                        msg.arg1 = result;
                                        msg.what = 0x1111;
                                        //将消息发送给主线程的Handler
                                        myhandler.sendMessage(msg);
                                    }
                                }
                            });
                        } catch (IOException e) {
                            e.printStackTrace();
                        } finally {
                            SipURL remote = new SipURL(currentFriendId, SipInfo.serverIp, SipInfo.SERVER_PORT);
                            SipInfo.toUser = new NameAddress(friend.getUsername(), remote);
                            SipInfo.sipUser.sendMessage(SipMessageFactory.createNotifyRequest(SipInfo.sipUser, SipInfo.toUser
                                    , SipInfo.user_from, BodyFactory.createFileTransferBody(SipInfo.userId, currentFriendId, id,
                                            file.getName(), filetype, "", "/" + SipInfo.userId + "/" + file.getName(), size, md5, 2)));
                            DatabaseInfo.sqLiteManager.insertLastestMsgTo(0, friend.getUserId(), "[文件]", time);
                            handler.post(new Runnable() {
                                @Override
                                public void run() {
                                    Toast.makeText(ChatActivity.this, "发送成功", Toast.LENGTH_SHORT).show();
                                }
                            });
                        }
                    }
                }).start();
            }
        }

        if (requestCode == 3) {
            if (resultCode == RESULT_OK) {
                smallvideopath = data.getStringExtra("smallvideopath");
                LogUtil.i(TAG, smallvideopath);
                final String id = SipInfo.userId + System.currentTimeMillis();
                final String content = "小视频";
                final int time = (int) (System.currentTimeMillis() / 1000);
                final String ftpPath = "/" + SipInfo.userId;
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        final File file = new File(smallvideopath);
                        final long size = file.length();
                        final String md5 = MD5Util.getFileMD5String(file);
                        FileInfo fileInfo = new FileInfo(file.getPath(), file.getName(), file.isDirectory());
                        final String filetype = getType(fileInfo);
                        handler.post(new Runnable() {
                            @Override
                            public void run() {

                                final int isTimeShow;
                                if (time - DatabaseInfo.sqLiteManager.queryLastTime(SipInfo.userId, currentFriendId) > 300) {
                                    isTimeShow = 1;
                                } else {
                                    isTimeShow = 0;
                                }
                                LogUtil.d(TAG, "run: " + smallvideopath);
                                DatabaseInfo.sqLiteManager.insertMessage(id, SipInfo.userId, currentFriendId,
                                        time, isTimeShow, content, 1, 0);
                                DatabaseInfo.sqLiteManager.insertFile(id, file.getName(), SipInfo.username, filetype, time, smallvideopath, ftpPath + "/" + file.getName(), size, md5, 1, 0, 0);
                                final Msg msgfile = new Msg();
                                msgfile.setMsgId(id);
                                msgfile.setContent(content);
                                msgfile.setFromUserId(SipInfo.userId);
                                msgfile.setToUserId(currentFriendId);
                                msgfile.setTime(time);
                                msgfile.setIsTimeShow(isTimeShow);
                                msgfile.setType(1);
//                            msgfile.setIsTransferFinish(0);
//                            msgfile.setFilePath(filePath);
                                msgList.add(msgfile);
                                msgAdapter.notifyDataSetChanged(); // 当有新消息时，刷新ListView中的显示
                                contentList.setSelection(msgList.size()); // 将ListView定位到最后一行

                            }
                        });
                        try {
                            new FTP().uploadSingleFile(file, ftpPath, new FTP.UploadProgressListener() {
                                @Override
                                public void onUploadProgress(String currentStep, long uploadSize, File file) {
                                    LogUtil.d(TAG, currentStep);
                                    android.os.Message msg = new android.os.Message();
                                    msg.obj = id;
                                    if (currentStep.equals(FTP.FTP_UPLOAD_SUCCESS)) {
//                                    Log.d(TAG, "-----上传成功--");
                                        msg.what = 0x1112;
                                        myhandler.sendMessage(msg);
                                    } else if (currentStep.equals(FTP.FTP_UPLOAD_LOADING)) {
                                        long fize = file.length();
                                        float num = (float) uploadSize / (float) fize;
                                        result = (int) (num * 100);
//                                    Log.d(TAG, "-----上传---" + result + "%");
//                                    Log.d(TAG, "run: " + result);
                                        msg.arg1 = result;
                                        msg.what = 0x1111;
                                        //将消息发送给主线程的Handler
                                        myhandler.sendMessage(msg);
                                    }
                                }
                            });
                        } catch (IOException e) {
                            e.printStackTrace();
                        } finally {
                            SipURL remote = new SipURL(currentFriendId, SipInfo.serverIp, SipInfo.SERVER_PORT);
                            SipInfo.toUser = new NameAddress(friend.getUsername(), remote);
                            SipInfo.sipUser.sendMessage(SipMessageFactory.createNotifyRequest(SipInfo.sipUser, SipInfo.toUser
                                    , SipInfo.user_from, BodyFactory.createFileTransferBody(SipInfo.userId, currentFriendId, id,
                                            file.getName(), filetype, "", "/" + SipInfo.userId + "/" + file.getName(), size, md5, 1)));
                            DatabaseInfo.sqLiteManager.insertLastestMsgTo(0, friend.getUserId(), "[小视频]", time);
                            handler.post(new Runnable() {
                                @Override
                                public void run() {
                                    Toast.makeText(ChatActivity.this, "发送成功", Toast.LENGTH_SHORT).show();
                                }
                            });
                        }

                    }
                }).start();
            }
        }
        if (requestCode == 4) {
            if (resultCode == RESULT_OK) {
                picPath = data.getStringExtra("picpath");
                thumbnailPath = picPath.replace(getString(R.string.Image), getString(R.string.Thumbnail));
                LogUtil.d(TAG, picPath);
                LogUtil.d(TAG, thumbnailPath);
                final String id = SipInfo.userId + System.currentTimeMillis();
                final String content = "图片";
                final int time = (int) (System.currentTimeMillis() / 1000);
                final String ftpPath = "/" + SipInfo.userId;
                final String thumbnailftpPath = ftpPath + "/Thumbnail";
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        final File file = new File(picPath);
                        final File thumbnailFile = new File(thumbnailPath);
                        final long size = file.length();
                        final String md5 = MD5Util.getFileMD5String(file);
                        FileInfo fileInfo = new FileInfo(file.getPath(), file.getName(), file.isDirectory());
                        final String filetype = getType(fileInfo);
                        handler.post(new Runnable() {
                            @Override
                            public void run() {

                                final int isTimeShow;
                                if (time - DatabaseInfo.sqLiteManager.queryLastTime(SipInfo.userId, currentFriendId) > 300) {
                                    isTimeShow = 1;
                                } else {
                                    isTimeShow = 0;
                                }
                                LogUtil.d(TAG, "run: " + picPath);
                                DatabaseInfo.sqLiteManager.insertMessage(id, SipInfo.userId, currentFriendId,
                                        time, isTimeShow, content, 1, 0);
                                DatabaseInfo.sqLiteManager.insertFile(id, file.getName(), SipInfo.username, filetype, time, picPath, ftpPath + "/" + file.getName(), size, md5, 3, 0, 0);
                                final Msg msgfile = new Msg();
                                msgfile.setMsgId(id);
                                msgfile.setContent(content);
                                msgfile.setFromUserId(SipInfo.userId);
                                msgfile.setToUserId(currentFriendId);
                                msgfile.setTime(time);
                                msgfile.setIsTimeShow(isTimeShow);
                                msgfile.setType(1);
//                            msgfile.setIsTransferFinish(0);
//                            msgfile.setFilePath(filePath);
                                msgList.add(msgfile);
                                msgAdapter.notifyDataSetChanged(); // 当有新消息时，刷新ListView中的显示
                                contentList.setSelection(msgList.size()); // 将ListView定位到最后一行

                            }
                        });
                        try {
                            new FTP().uploadSingleFile(file, ftpPath, new FTP.UploadProgressListener() {
                                @Override
                                public void onUploadProgress(String currentStep, long uploadSize, File file) {
                                    LogUtil.d(TAG, currentStep);
                                    android.os.Message msg = new android.os.Message();
                                    msg.obj = id;
                                    if (currentStep.equals(FTP.FTP_UPLOAD_SUCCESS)) {
//                                    Log.d(TAG, "-----上传成功--");
                                        msg.what = 0x1112;
                                        myhandler.sendMessage(msg);
                                    } else if (currentStep.equals(FTP.FTP_UPLOAD_LOADING)) {
                                        long fize = file.length();
                                        float num = (float) uploadSize / (float) fize;
                                        result = (int) (num * 100);
//                                    Log.d(TAG, "-----上传---" + result + "%");
//                                    Log.d(TAG, "run: " + result);
                                        msg.arg1 = result;
                                        msg.what = 0x1111;
                                        //将消息发送给主线程的Handler
                                        myhandler.sendMessage(msg);
                                    }
                                }
                            });
                            new FTP().uploadSingleFile(thumbnailFile, thumbnailftpPath, new FTP.UploadProgressListener() {
                                @Override
                                public void onUploadProgress(String currentStep, long uploadSize, File file) {
                                }
                            });
                        } catch (IOException e) {
                            e.printStackTrace();
                        } finally {
                            SipURL remote = new SipURL(currentFriendId, SipInfo.serverIp, SipInfo.SERVER_PORT);
                            SipInfo.toUser = new NameAddress(friend.getUsername(), remote);
                            SipInfo.sipUser.sendMessage(SipMessageFactory.createNotifyRequest(SipInfo.sipUser, SipInfo.toUser
                                    , SipInfo.user_from, BodyFactory.createFileTransferBody(SipInfo.userId, currentFriendId, id,
                                            file.getName(), filetype, "", thumbnailftpPath + "/" + file.getName(), size, md5, 3)));
                            DatabaseInfo.sqLiteManager.insertLastestMsgTo(0, friend.getUserId(), "[图片]", time);
                            handler.post(new Runnable() {
                                @Override
                                public void run() {
                                    Toast.makeText(ChatActivity.this, "发送成功", Toast.LENGTH_SHORT).show();
                                }
                            });
                        }
                    }
                }).start();
            }
        }
        if (requestCode==5){
            if (resultCode==RESULT_OK){
                address=data.getStringExtra("location");
                LogUtil.d(TAG,address);
                final int time = (int) (System.currentTimeMillis() / 1000);
                final int isTimeShow;
                final String id = SipInfo.userId + System.currentTimeMillis();
                SipURL remote = new SipURL(friend.getUserId(), SipInfo.serverIp, SipInfo.SERVER_PORT);
                SipInfo.toUser = new NameAddress(friend.getUsername(), remote);
                Message message = SipMessageFactory.createNotifyRequest(SipInfo.sipUser, SipInfo.toUser,
                        SipInfo.user_from, BodyFactory.createMessageBody(id, SipInfo.userId, friend.getUserId(), address, "", 2));
                SipInfo.sipUser.sendMessage(message);
                if (time - DatabaseInfo.sqLiteManager.queryLastTime(SipInfo.userId, friend.getUserId()) > 300) {
                    isTimeShow = 1;
                } else {
                    isTimeShow = 0;
                }
                DatabaseInfo.sqLiteManager.insertMessage(id, SipInfo.userId, friend.getUserId(),
                        time, isTimeShow, address, 2, 0);
                Msg msg = new Msg();
                msg.setMsgId(id);
                msg.setFromUserId(SipInfo.userId);
                msg.setToUserId(friend.getUserId());
                msg.setTime(time);
                msg.setIsTimeShow(isTimeShow);
                msg.setContent(address);
                msg.setType(2);
                msgList.add(msg);
                msgAdapter.notifyDataSetChanged();
                contentList.setSelection(msgList.size());
                content.setText("");
                DatabaseInfo.sqLiteManager.insertLastestMsgTo(0, friend.getUserId(), "[位置信息]", time);
            }
        }
        if (requestCode == 6) {
            if (resultCode == RESULT_OK) {
                picPaths = data.getStringArrayListExtra("picpaths");
                for (final String Path:picPaths) {
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            String thumbnailPath=Path;
                            final String picPath = thumbnailPath.replace(getString(R.string.Thumbnail), getString(R.string.Image));
                            LogUtil.d(TAG, picPath);
                            LogUtil.d(TAG, thumbnailPath);
                            final String id = SipInfo.userId + System.currentTimeMillis();
                            final String content = "图片";
                            final int time = (int) (System.currentTimeMillis() / 1000);
                            final String ftpPath = "/" + SipInfo.userId;
                            final String thumbnailftpPath = ftpPath + "/Thumbnail";
                            final File file = new File(picPath);
                            final File thumbnailFile = new File(thumbnailPath);
                            final long size = file.length();
                            final String md5 = MD5Util.getFileMD5String(file);
                            FileInfo fileInfo = new FileInfo(file.getPath(), file.getName(), file.isDirectory());
                            final String filetype = getType(fileInfo);
                            handler.post(new Runnable() {
                                @Override
                                public void run() {

                                    final int isTimeShow;
                                    if (time - DatabaseInfo.sqLiteManager.queryLastTime(SipInfo.userId, currentFriendId) > 300) {
                                        isTimeShow = 1;
                                    } else {
                                        isTimeShow = 0;
                                    }
                                    LogUtil.d(TAG, "run: " + picPath);
                                    DatabaseInfo.sqLiteManager.insertMessage(id, SipInfo.userId, currentFriendId,
                                            time, isTimeShow, content, 1, 0);
                                    DatabaseInfo.sqLiteManager.insertFile(id, file.getName(), SipInfo.username, filetype, time, picPath, ftpPath + "/" + file.getName(), size, md5, 3, 0, 0);
                                    final Msg msgfile = new Msg();
                                    msgfile.setMsgId(id);
                                    msgfile.setContent(content);
                                    msgfile.setFromUserId(SipInfo.userId);
                                    msgfile.setToUserId(currentFriendId);
                                    msgfile.setTime(time);
                                    msgfile.setIsTimeShow(isTimeShow);
                                    msgfile.setType(1);
//                            msgfile.setIsTransferFinish(0);
//                            msgfile.setFilePath(filePath);
                                    msgList.add(msgfile);
                                    msgAdapter.notifyDataSetChanged(); // 当有新消息时，刷新ListView中的显示
                                    contentList.setSelection(msgList.size()); // 将ListView定位到最后一行

                                }
                            });
                            try {
                                new FTP().uploadSingleFile(file, ftpPath, new FTP.UploadProgressListener() {
                                    @Override
                                    public void onUploadProgress(String currentStep, long uploadSize, File file) {
                                        LogUtil.d(TAG, currentStep);
                                        android.os.Message msg = new android.os.Message();
                                        msg.obj = id;
                                        if (currentStep.equals(FTP.FTP_UPLOAD_SUCCESS)) {
//                                    Log.d(TAG, "-----上传成功--");
                                            msg.what = 0x1112;
                                            myhandler.sendMessage(msg);
                                        } else if (currentStep.equals(FTP.FTP_UPLOAD_LOADING)) {
                                            long fize = file.length();
                                            float num = (float) uploadSize / (float) fize;
                                            result = (int) (num * 100);
//                                    Log.d(TAG, "-----上传---" + result + "%");
//                                    Log.d(TAG, "run: " + result);
                                            msg.arg1 = result;
                                            msg.what = 0x1111;
                                            //将消息发送给主线程的Handler
                                            myhandler.sendMessage(msg);
                                        }
                                    }
                                });
                                new FTP().uploadSingleFile(thumbnailFile, thumbnailftpPath, new FTP.UploadProgressListener() {
                                    @Override
                                    public void onUploadProgress(String currentStep, long uploadSize, File file) {
                                    }
                                });
                            } catch (IOException e) {
                                e.printStackTrace();
                            } finally {
                                SipURL remote = new SipURL(currentFriendId, SipInfo.serverIp, SipInfo.SERVER_PORT);
                                SipInfo.toUser = new NameAddress(friend.getUsername(), remote);
                                SipInfo.sipUser.sendMessage(SipMessageFactory.createNotifyRequest(SipInfo.sipUser, SipInfo.toUser
                                        , SipInfo.user_from, BodyFactory.createFileTransferBody(SipInfo.userId, currentFriendId, id,
                                                file.getName(), filetype, "", thumbnailftpPath + "/" + file.getName(), size, md5, 3)));
                                DatabaseInfo.sqLiteManager.insertLastestMsgTo(0, friend.getUserId(), "[图片]", time);
                                handler.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        Toast.makeText(ChatActivity.this, "发送成功", Toast.LENGTH_SHORT).show();
                                    }
                                });
                            }
                        }
                    }).start();
                }
            }
        }
    }

    private Handler myhandler = new Handler() {
        @Override
        public void handleMessage(android.os.Message message) {
            Msg msg = new Msg();
            msg.setMsgId((String) message.obj);
            int index = msgList.indexOf(msg);
            if (index != -1) {
                if (message.what == 0x1111) {
                    msgList.get(index).setProgress(message.arg1);
                    msgAdapter.notifyDataSetChanged();
                }
                if (message.what == 0x1112) {
//                msgList.get(index).setIsFileTransfer(0);
                    DatabaseInfo.sqLiteManager.updateIsFileTransferFinish(msg.getMsgId(), 1);
                    msgAdapter.notifyDataSetChanged();
                }
            }
        }
    };
    /**
     * 下载监听
     */
    private MsgAdapter.DownloadListener downloadListener = new MsgAdapter.DownloadListener() {
        @Override
        public void onDownload(final String msgId, final String filePath, final String fileName) {
            Thread downlistener = new Thread(new Runnable() {
                @Override
                public void run() {
                    String FilePath = sdPath + SipInfo.username + "/personal/";
                    File file = new File(FilePath + fileName);
                    if (file.exists()) {
                        file.delete();
                        LogUtil.d(TAG, "删除成功");
                    }
                    /**开始下载*/
                    try {
                        /**单文件下载*/
                        new FTP().downloadSingleFile(filePath, FilePath, fileName, new FTP.DownLoadProgressListener() {
                            @Override
                            public void onDownLoadProgress(String currentStep, long downProcess, File file) {
                                LogUtil.d(TAG, currentStep);
                                android.os.Message message = new android.os.Message();
                                message.obj = msgId;
                                if (currentStep.equals(FTP.FTP_DOWN_SUCCESS)) {
                                    LogUtil.d(TAG, "-----下载--successful");
                                    message.what = 0x2222;
                                    dhandler.sendMessage(message);
                                } else if (currentStep.equals(FTP.FTP_DOWN_LOADING)) {
                                    LogUtil.d(TAG, "-----下载---" + downProcess + "%");
                                    message.arg1 = (int) downProcess;
                                    message.what = 0x2221;
                                    dhandler.sendMessage(message);
                                }

                            }
                        });
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
            downlistener.start();
        }
    };
    private Handler dhandler = new Handler() {
        @Override
        public void handleMessage(android.os.Message message) {
            Msg msg = new Msg();
            msg.setMsgId((String) message.obj);
            MyFile myFile = DatabaseInfo.sqLiteManager.queryFile(msg.getMsgId());
            int index = msgList.indexOf(msg);
            if (index != -1) {
                if (message.what == 0x2221) {
                    msgList.get(index).setProgress(message.arg1);
                    msgAdapter.notifyDataSetChanged();
                }
                if (message.what == 0x2222) {
                    DatabaseInfo.sqLiteManager.updateFileDownload(msg.getMsgId(), 1);
                    DatabaseInfo.sqLiteManager.updateLocalPath(msg.getMsgId(), sdPath + SipInfo.username + "/personal/");
                    msgAdapter.notifyDataSetChanged();
                    if (myFile.getType() == 0) {
                        android.os.Message openmessage = new android.os.Message();
                        openmessage.what = 0x1;
                        openmessage.obj = msg.getMsgId();
                        msgAdapter.openhandler.sendMessage(openmessage);
                    }
                }
            }
        }
    };
    /**
     * 打开文件监听
     */
    private MsgAdapter.OpenFileListener openFileListener = new MsgAdapter.OpenFileListener() {
        @Override
        public void OpenFile(File file) {
            Intent intent = new Intent();
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            //设置intent的Action属性
            intent.setAction(Intent.ACTION_VIEW);
            //获取文件file的MIME类型
            String type = getMIMEType(file);
            //设置intent的data和Type属性。
            intent.setDataAndType(/*uri*/Uri.fromFile(file), type);
            //跳转
            startActivity(intent);
        }
    };


    /**
     * 根据文件后缀名获得对应的MIME类型。
     *
     * @param file
     */
    private String getMIMEType(File file) {

        String type = "*/*";
        String fName = file.getName();
        //获取后缀名前的分隔符"."在fName中的位置。
        int dotIndex = fName.lastIndexOf(".");
        if (dotIndex < 0) {
            return type;
        }
        /**获取文件的后缀名*/
        String end = fName.substring(dotIndex, fName.length()).toLowerCase();
        if (end == "") return type;
        /**在MIME和文件类型的匹配表中找到对应的MIME类型。*/
        for (int i = 0; i < MIME_MapTable.length; i++) { //MIME_MapTable??在这里你一定有疑问，这个MIME_MapTable是什么？
            if (end.equals(MIME_MapTable[i][0]))
                type = MIME_MapTable[i][1];
        }
        return type;
    }

    public String getType(FileInfo fileInfo) {
        switch (fileInfo.whichtype()) {
            case DOC:
                return "doc";
            case DOCX:
                return "docx";
            case PPT:
                return "ppt";
            case PPTX:
                return "pptx";
            case UNKNOWN:
                return "unknown";
            case XLS:
                return "xls";
            case XLXS:
                return "xlxs";
            case PDF:
                return "pdf";
            case PNG:
                return "png";
            case TXT:
                return "txt";
            case MP3:
                return "mp3";
            case MP4:
                return "mp4";
            case BMP:
                return "bmp";
            case GIF:
                return "gif";
            case AVI:
                return "avi";
            case WMA:
                return "wma";
            case RAR:
                return "rar";
            case ZIP:
                return "zip";
            case WAV:
                return "wav";
            case JPG:
                return "jpg";
            case NULL:
                return "";
            default:
                return "";
        }
    }

    private final String[][] MIME_MapTable = {
            /**{后缀名，MIME类型}*/
            {".3gp", "video/3gpp"},
            {".apk", "application/vnd.android.package-archive"},
            {".asf", "video/x-ms-asf"},
            {".avi", "video/x-msvideo"},
            {".bin", "application/octet-stream"},
            {".bmp", "image/bmp"},
            {".c", "text/plain"},
            {".class", "application/octet-stream"},
            {".conf", "text/plain"},
            {".cpp", "text/plain"},
            {".doc", "application/msword"},
            {".docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document"},
            {".xls", "application/vnd.ms-excel"},
            {".xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"},
            {".exe", "application/octet-stream"},
            {".gif", "image/gif"},
            {".gtar", "application/x-gtar"},
            {".gz", "application/x-gzip"},
            {".h", "text/plain"},
            {".htm", "text/html"},
            {".html", "text/html"},
            {".jar", "application/java-archive"},
            {".java", "text/plain"},
            {".jpeg", "image/jpeg"},
            {".jpg", "image/jpeg"},
            {".js", "application/x-javascript"},
            {".log", "text/plain"},
            {".m3u", "audio/x-mpegurl"},
            {".m4a", "audio/mp4a-latm"},
            {".m4b", "audio/mp4a-latm"},
            {".m4p", "audio/mp4a-latm"},
            {".m4u", "video/vnd.mpegurl"},
            {".m4v", "video/x-m4v"},
            {".mov", "video/quicktime"},
            {".mp2", "audio/x-mpeg"},
            {".mp3", "audio/x-mpeg"},
            {".mp4", "video/mp4"},
            {".mpc", "application/vnd.mpohun.certificate"},
            {".mpe", "video/mpeg"},
            {".mpeg", "video/mpeg"},
            {".mpg", "video/mpeg"},
            {".mpg4", "video/mp4"},
            {".mpga", "audio/mpeg"},
            {".msg", "application/vnd.ms-outlook"},
            {".ogg", "audio/ogg"},
            {".pdf", "application/pdf"},
            {".png", "image/png"},
            {".pps", "application/vnd.ms-powerpoint"},
            {".ppt", "application/vnd.ms-powerpoint"},
            {".pptx", "application/vnd.openxmlformats-officedocument.presentationml.presentation"},
            {".prop", "text/plain"},
            {".rc", "text/plain"},
            {".rmvb", "audio/x-pn-realaudio"},
            {".rtf", "application/rtf"},
            {".sh", "text/plain"},
            {".tar", "application/x-tar"},
            {".tgz", "application/x-compressed"},
            {".txt", "text/plain"},
            {".wav", "audio/x-wav"},
            {".wma", "audio/x-ms-wma"},
            {".wmv", "audio/x-ms-wmv"},
            {".wps", "application/vnd.ms-works"},
            {".xml", "text/plain"},
            {".z", "application/x-compress"},
            {".zip", "application/x-zip-compressed"},
            {"", "*/*"}
    };

    @Override
    public void onReceivedMessage(final Msg msg) {
        if (msg.getFromUserId().equals(friend.getUserId())) {
            handler.post(new Runnable() {
                @Override
                public void run() {
                    msgList.add(msg);
                    msgAdapter.notifyDataSetChanged(); // 当有新消息时，刷新ListView中的显示
                    contentList.setSelection(msgList.size()); // 将ListView定位到最后一行
                }
            });
        }
    }

    @Override
    public void onEmojiDelete() {
        String text = content.getText().toString();
        if (text.isEmpty()) {
            return;
        }
        if ("]".equals(text.substring(text.length() - 1, text.length()))) {
            int index = text.lastIndexOf("[");
            if (index == -1) {
                int action = KeyEvent.ACTION_DOWN;
                int code = KeyEvent.KEYCODE_DEL;
                KeyEvent event = new KeyEvent(action, code);
                content.onKeyDown(KeyEvent.KEYCODE_DEL, event);
                displayTextView();
                return;
            }
            content.getText().delete(index, text.length());
            displayTextView();
            return;
        }
        int action = KeyEvent.ACTION_DOWN;
        int code = KeyEvent.KEYCODE_DEL;
        KeyEvent event = new KeyEvent(action, code);
        content.onKeyDown(KeyEvent.KEYCODE_DEL, event);
        displayTextView();
    }

    @Override
    public void onEmojiClick(Emoji emoji) {
        if (emoji != null) {
            int index = content.getSelectionStart();
            Editable editable = content.getEditableText();
            if (index < 0) {
                editable.append(emoji.getContent());
            } else {
                editable.insert(index, emoji.getContent());
            }
        }
        displayTextView();
    }

    private void displayTextView() {
        try {
            EmojiUtil.handlerEmojiText(content, content.getText().toString(), this);
            content.setSelection(content.getText().length());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
