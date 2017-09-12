package com.punuo.sys.app.xungeng.ui;

import android.annotation.TargetApi;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.ViewSwitcher;

import com.punuo.sys.app.R;
import com.punuo.sys.app.xungeng.audio.PhoneCall;
import com.punuo.sys.app.xungeng.ftp.FTP;
import com.punuo.sys.app.xungeng.sip.BodyFactory;
import com.punuo.sys.app.xungeng.sip.SipInfo;
import com.punuo.sys.app.xungeng.sip.SipMessageFactory;
import com.punuo.sys.app.xungeng.util.LogUtil;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import butterknife.Bind;
import butterknife.ButterKnife;

import static android.app.Activity.DEFAULT_KEYS_SEARCH_LOCAL;
import static android.content.ContentValues.TAG;
import static android.content.Context.MODE_PRIVATE;

/**
 * Created by ch on 2016/11/8.
 */

public class MenuFragment extends Fragment implements GestureDetector.OnGestureListener {
    @Bind(R.id.viewSwitcher)
    ViewSwitcher viewSwitcher;
    public static final int NUMBER_PER_SCREEN = 16;
    public static final int XUNGENG = 0;
    public static final int CALLCENTER = 1;
    public static final int CHANGEPWD = 2;
    public static final int GALLER = 3;
    public static final int UPDATE = 4;
    public static final int ADDAPP = 5;
    @Bind(R.id.usernum)
    TextView usernum;
    @Bind(R.id.username)
    TextView username;
    // 记录当前正在显示第几屏的程序
    private int screenNo = -1;
    // 保存程序所占的总屏数
    private int screenCount;
    LayoutInflater myinflater;
    GestureDetector detector;
    final int FLIP_DISTANCE = 50;
    private final BroadcastReceiver mApplicationsReceiver = new ApplicationsIntentReceiver();
    private static ArrayList<MyApplicationInfo> mApplications;
    private GridView mGrid;
    int[] icon = new int[]{
            R.drawable.xungeng,
            R.drawable.icon_call_center,
            R.drawable.change_psd,
            R.drawable.album,
            R.drawable.update

    };
    List<String> titlelist = new ArrayList<>();
    String[] title = new String[]{
            "巡更",
            "呼叫平台",
            "修改密码",
            "相册",
            "软件更新",
            "添加应用"
    };
    FTP ftp;
    String newVersion;
    String version;
    private String sdPath = "/mnt/sdcard/xungeng/download/apk/";
    private AlertDialog dialog;
    @Override
    public View onCreateView(final LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.menufragment, container, false);
        ButterKnife.bind(this, view);
//        Typeface fontFace = Typeface.createFromAsset(getActivity().getAssets(),
//                "fonts/zqc.TTF");
//        usernum.setTypeface(fontFace);
//        username.setTypeface(fontFace);
        usernum.setText(SipInfo.userNum);
        username.setText(SipInfo.username);
        detector = new GestureDetector(getActivity(), this);
        for (int i = 0; i < title.length; i++) {
            titlelist.add(title[i]);
        }
        myinflater = inflater;
        getActivity().setDefaultKeyMode(DEFAULT_KEYS_SEARCH_LOCAL);
        registerIntentReceivers();
        loadApplications(true);
        screenCount = mApplications.size() % NUMBER_PER_SCREEN == 0 ?
                mApplications.size() / NUMBER_PER_SCREEN :
                mApplications.size() / NUMBER_PER_SCREEN + 1;
        viewSwitcher.setFactory(new ViewSwitcher.ViewFactory() {
            @Override
            public View makeView() {
                return mGrid = (GridView) inflater.inflate(R.layout.slidelistview, null);
            }
        });
        next();
        view.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return detector.onTouchEvent(event);
            }
        });
        view.setClickable(true);
        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        ButterKnife.unbind(this);
        unregisterIntentReceivers();
    }
    private SharedPreferences preferences;//巡更库区记忆
    private SharedPreferences.Editor editor;
    private AdapterView.OnItemClickListener onItemClickListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            MyApplicationInfo app = (MyApplicationInfo) parent.getItemAtPosition(position);
            if (app.type == MyApplicationInfo.TYPE_APP) {
                startActivity(app.intent);
            } else {
                switch (titlelist.indexOf(app.title)) {
                    case XUNGENG:
                        preferences=getActivity().getSharedPreferences("kuqu",MODE_PRIVATE);
                        editor=preferences.edit();
                        int lastkuqu=preferences.getInt("addr_code",-1);
                        if (lastkuqu==-1) {
                            final Spinner spinner = new Spinner(getActivity());
                            String[] arr = {"德清", "越州", "舟山", "衢州"};
                            ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(getActivity(), android.R.layout.simple_list_item_1, arr);
                            spinner.setAdapter(arrayAdapter);
                            dialog = new AlertDialog.Builder(getActivity())
                                    .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                        /*请求巡更点信息*/
                                            SipInfo.addr_code = spinner.getSelectedItemPosition() + 1;
                                            SipInfo.pointInfoListbd09.clear();
                                            SipInfo.pointInfoList.clear();
                                            SipInfo.sipUser.sendMessage(SipMessageFactory.createSubscribeRequest(SipInfo.sipUser, SipInfo.user_to,
                                                    SipInfo.user_from, BodyFactory.createPointsQuery(SipInfo.addr_code)));

                                        }
                                    })
                                    .setNegativeButton("取消", null)
                                    .create();
                            dialog.setTitle("请选择库区");
                            dialog.setView(spinner);
                            dialog.show();
                        }else{
                            SipInfo.addr_code = lastkuqu;
                            SipInfo.pointInfoListbd09.clear();
                            SipInfo.pointInfoList.clear();
                            SipInfo.sipUser.sendMessage(SipMessageFactory.createSubscribeRequest(SipInfo.sipUser, SipInfo.user_to,
                                    SipInfo.user_from, BodyFactory.createPointsQuery(SipInfo.addr_code)));
                        }
                        break;
                    case CALLCENTER:
                        PhoneCall.actionStart(getActivity(), SipInfo.centerPhone, 1);
                        break;
                    case CHANGEPWD:
                        Intent changeIntent = new Intent(getActivity(), ChangPWD.class);
                        startActivity(changeIntent);
                        break;
                    case GALLER:
                        Intent gallerIntent = new Intent(getActivity(), AlbumAty.class);
                        startActivity(gallerIntent);
                        break;
                    case UPDATE:
                        ftp = new FTP();
                        PackageManager packageManager = getActivity().getPackageManager();
                        try {
                            PackageInfo pi = packageManager.getPackageInfo(getActivity().getPackageName(), 0);
                            version = pi.versionName;
                        } catch (PackageManager.NameNotFoundException e) {
                            e.printStackTrace();
                        }
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    ftp.openConnect();
                                    ftp.ftpClient.changeWorkingDirectory("/apk/liangku");
                                    String[] file = ftp.ftpClient.listNames();
                                    String[] ss = file[0].split("V");
                                    newVersion = ss[ss.length - 1];
                                    newVersion = newVersion.substring(0, newVersion.lastIndexOf('.'));
                                    LogUtil.d("version", version);
                                    LogUtil.d("newversion", newVersion);
                                    if (!version.equals(newVersion)) {
                                        Message message = new Message();
                                        message.what = 0x1000;
                                        message.obj = file[0];
                                        handler.sendMessage(message);
                                    } else {
                                        handler.post(new Runnable() {
                                            @Override
                                            public void run() {
                                                AlertDialog dialog = new AlertDialog.Builder(getActivity())
                                                        .setTitle("当前为最新版本")
                                                        .setPositiveButton("确定", null)
                                                        .create();
                                                dialog.setCanceledOnTouchOutside(false);
                                                dialog.show();
                                            }
                                        });

                                    }
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            }
                        }).start();
                        break;
                    case ADDAPP:
                        Intent intent = new Intent(getActivity(), AppList.class);
                        startActivity(intent);
                        break;
                }
            }
        }
    };
    private ProgressDialog mydialog;
    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (msg.what == 0x1000) {
                final String apkname = (String) msg.obj;
                AlertDialog dialog = new AlertDialog.Builder(getActivity())
                        .setTitle("有新版本,当前版本为" + version + ",新版本为" + newVersion)
                        .setPositiveButton("下载并安装", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                mydialog = new ProgressDialog(getActivity());
                                mydialog.setTitle("下载进度");
                                mydialog.setMessage("已经下载了");
                                mydialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
                                mydialog.setCancelable(false);
                                mydialog.setIndeterminate(false);
                                mydialog.setMax(100);
                                mydialog.show();
                                new Thread() {
                                    @Override
                                    public void run() {
                                        try {
                                            new FTP().downloadSingleFile("/apk/liangku/"  + apkname, sdPath, apkname, new FTP.DownLoadProgressListener() {
                                                @Override
                                                public void onDownLoadProgress(String currentStep, long downProcess, File file) {
                                                    Log.d(TAG, currentStep);
                                                    if (currentStep.equals(FTP.FTP_DOWN_SUCCESS)) {
                                                        Message message = new Message();
                                                        message.obj = sdPath + apkname;
                                                        message.what = 0x2000;
                                                        mydialog.dismiss();
                                                        handler.sendMessage(message);
                                                    }
                                                    if (currentStep.equals(FTP.FTP_DOWN_LOADING)){
                                                        mydialog.setProgress((int)downProcess);
                                                    }
                                                }
                                            });
                                        } catch (Exception e) {
                                            e.printStackTrace();
                                        }
                                    }
                                }.start();
                            }
                        })
                        .setNegativeButton("取消", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {

                            }
                        }).create();
                dialog.setCanceledOnTouchOutside(false);
                dialog.show();
            } else if (msg.what == 0x2000) {
                String localapk = (String) msg.obj;
                File file = new File(localapk);
                if (file.exists()) {
                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    intent.addCategory(Intent.CATEGORY_DEFAULT);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    intent.setDataAndType(Uri.fromFile(new File(localapk)),
                            "application/vnd.android.package-archive");
                    //注销
                    SipInfo.sipUser.sendMessage(SipMessageFactory.createNotifyRequest(SipInfo.sipUser,SipInfo.user_to,
                            SipInfo.user_from, BodyFactory.createLogoutBody()));
                    SipInfo.sipDev.sendMessage(SipMessageFactory.createNotifyRequest(SipInfo.sipDev, SipInfo.dev_to,
                            SipInfo.dev_from, BodyFactory.createLogoutBody()));
                    ActivityCollector.finishToFirstView();
                    startActivity(intent);
                }
            }
        }
    };
    @Override
    public boolean onDown(MotionEvent e) {
        return true;
    }

    @Override
    public void onShowPress(MotionEvent e) {

    }

    @Override
    public boolean onSingleTapUp(MotionEvent e) {
        return false;
    }

    @Override
    public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
        return false;
    }

    @Override
    public void onLongPress(MotionEvent e) {

    }

    @Override
    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
        if (e1.getX() - e2.getX() > FLIP_DISTANCE) {
            next();
            return true;
        }
        if (e2.getX() - e1.getX() > FLIP_DISTANCE) {
            prev();
            return true;
        }
        return false;
    }

    private class ApplicationsIntentReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String packageName = intent.getDataString().substring(8);
            System.out.println("---------------" + packageName);
            loadApplications(false);
            bindApplications();
        }
    }

    private void registerIntentReceivers() {
        IntentFilter filter;
        filter = new IntentFilter(Intent.ACTION_PACKAGE_ADDED);
        filter.addAction(Intent.ACTION_PACKAGE_REMOVED);
        filter.addAction(Intent.ACTION_PACKAGE_CHANGED);
        filter.addDataScheme("package");
        getActivity().registerReceiver(mApplicationsReceiver, filter);
    }

    private void unregisterIntentReceivers() {
        if (mApplicationsReceiver != null) {
            getActivity().unregisterReceiver(mApplicationsReceiver);
        }
    }

    /**
     * Loads the list of installed applications in mApplications.
     */
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private void loadApplications(boolean isLaunching) {
        if (isLaunching && mApplications != null) {
            return;
        }
        PackageManager manager = getActivity().getPackageManager();
        Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
        mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);
        final List<ResolveInfo> apps = manager.queryIntentActivities(mainIntent, 0);
        Collections.sort(apps, new ResolveInfo.DisplayNameComparator(manager));
        if (mApplications == null) {
            mApplications = new ArrayList<>();
        }
        mApplications.clear();
        for (int i = 0; i < icon.length; i++) {
            MyApplicationInfo application = new MyApplicationInfo();
            application.type = MyApplicationInfo.TYPE_BUTTON;
            application.title = title[i];
            application.icon = getActivity().getDrawable(icon[i]);
            mApplications.add(application);
        }
        if (apps != null) {
            int count = apps.size();
            for (int i = 0; i < count; i++) {
                MyApplicationInfo application = new MyApplicationInfo();
                ResolveInfo info = apps.get(i);
                if (info.loadLabel(manager).equals("设置")||(info.activityInfo.applicationInfo.packageName.contains("com.punuo.sys.app")&&!info.activityInfo.applicationInfo.packageName.contains("com.punuo.sys.app.xungeng"))) {
                    application.type = MyApplicationInfo.TYPE_APP;
                    application.title = info.loadLabel(manager);
                    application.packageName = info.activityInfo.applicationInfo.packageName;
                    application.setActivity(new ComponentName(
                                    info.activityInfo.applicationInfo.packageName,
                                    info.activityInfo.name),
                            Intent.FLAG_ACTIVITY_NEW_TASK
                                    | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
                    if (info.loadLabel(manager).equals("设置")) {
                        application.icon = getActivity().getDrawable(R.drawable.setting);
                    } else {
                        application.icon = info.activityInfo.loadIcon(manager);
                    }
                    mApplications.add(application);
                }
            }
        }
        MyApplicationInfo applicationInfo = new MyApplicationInfo();
        applicationInfo.type = MyApplicationInfo.TYPE_BUTTON;
        applicationInfo.title = "添加应用";
        applicationInfo.icon = getActivity().getDrawable(R.drawable.icon_add);
        mApplications.add(applicationInfo);
    }

    /**
     * Creates a new appplications adapter for the grid view and registers it.
     */
    private void bindApplications() {
        loadApplications(true);
        screenCount = mApplications.size() % NUMBER_PER_SCREEN == 0 ?
                mApplications.size() / NUMBER_PER_SCREEN :
                mApplications.size() / NUMBER_PER_SCREEN + 1;
        viewSwitcher.removeAllViews();
        viewSwitcher.setFactory(new ViewSwitcher.ViewFactory() {
            @Override
            public View makeView() {
                return myinflater.inflate(R.layout.slidelistview, null);
            }
        });
        screenNo = -1;
        next();
    }

    public void next() {
        if (screenNo < screenCount - 1) {
            screenNo++;
            viewSwitcher.setInAnimation(getActivity(), R.anim.slide_in_right);
            viewSwitcher.setOutAnimation(getActivity(), R.anim.slide_out_left);
            mGrid = (GridView) viewSwitcher.getNextView();
            mGrid.setAdapter(adapter);
            mGrid.setOnItemClickListener(onItemClickListener);
            viewSwitcher.showNext();
        }
    }

    public void prev() {
        if (screenNo > 0) {
            screenNo--;
            viewSwitcher.setInAnimation(getActivity(), android.R.anim.slide_in_left);
            viewSwitcher.setOutAnimation(getActivity(), android.R.anim.slide_out_right);
            mGrid = (GridView) viewSwitcher.getNextView();
            mGrid.setAdapter(adapter);
            mGrid.setOnItemClickListener(onItemClickListener);
            viewSwitcher.showPrevious();
        }
    }

    private BaseAdapter adapter = new BaseAdapter() {
        @Override
        public int getCount() {
            // 如果已经到了最后一屏，且应用程序的数量不能整除NUMBER_PER_SCREEN
            if (screenNo == screenCount - 1
                    && mApplications.size() % NUMBER_PER_SCREEN != 0) {
                // 最后一屏显示的程序数为应用程序的数量对NUMBER_PER_SCREEN求余
                return mApplications.size() % NUMBER_PER_SCREEN;
            }
            // 否则每屏显示的程序数量为NUMBER_PER_SCREEN
            return NUMBER_PER_SCREEN;
        }

        @Override
        public MyApplicationInfo getItem(int position) {
            // 根据screenNo计算第position个列表项的数据
            return mApplications.get(screenNo * NUMBER_PER_SCREEN + position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view = convertView;
            if (convertView == null) {
                view = myinflater.inflate(R.layout.labelicon, null);
            }
            // 获取R.layout.labelicon布局文件中的ImageView组件，并为之设置图标
            ImageView imageView = (ImageView)
                    view.findViewById(R.id.imageview);
            imageView.setImageDrawable(getItem(position).icon);
            // 获取R.layout.labelicon布局文件中的TextView组件，并为之设置文本
            TextView textView = (TextView)
                    view.findViewById(R.id.textview);
            textView.setText(getItem(position).title);
            return view;
        }
    };
}
