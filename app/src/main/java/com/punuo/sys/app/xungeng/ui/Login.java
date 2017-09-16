package com.punuo.sys.app.xungeng.ui;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TableLayout;
import android.widget.Toast;

import com.punuo.sys.app.R;
import com.punuo.sys.app.xungeng.db.DatabaseInfo;
import com.punuo.sys.app.xungeng.db.MyDatabaseHelper;
import com.punuo.sys.app.xungeng.db.SQLiteManager;
import com.punuo.sys.app.xungeng.sip.BodyFactory;
import com.punuo.sys.app.xungeng.sip.KeepAlive;
import com.punuo.sys.app.xungeng.sip.SipDev;
import com.punuo.sys.app.xungeng.sip.SipInfo;
import com.punuo.sys.app.xungeng.sip.SipMessageFactory;
import com.punuo.sys.app.xungeng.sip.SipUser;
import com.punuo.sys.app.xungeng.util.LogUtil;
import com.punuo.sys.app.xungeng.view.CustomProgressDialog;

import org.zoolu.sip.address.NameAddress;
import org.zoolu.sip.address.SipURL;
import org.zoolu.sip.message.Message;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Properties;
import java.util.Random;

import butterknife.Bind;
import butterknife.ButterKnife;

import static com.punuo.sys.app.xungeng.sip.SipInfo.alertDialog;
import static com.punuo.sys.app.xungeng.sip.SipInfo.centerPhone;
import static com.punuo.sys.app.xungeng.sip.SipInfo.devId;
import static com.punuo.sys.app.xungeng.sip.SipInfo.serverIp;
import static com.punuo.sys.app.xungeng.sip.SipInfo.version;


public class Login extends AppCompatActivity implements View.OnClickListener {
    //用户名
    @Bind(R.id.username)
    EditText username;
    //密码
    @Bind(R.id.password)
    EditText password;
    //登录按钮
    @Bind(R.id.login)
    Button login;
    //主界面布局
    @Bind(R.id.root)
    LinearLayout root;
    //网络设置
    @Bind(R.id.setting)
    Button setting;
    //修改配置
    @Bind(R.id.changedevId)
    Button changedevId;
    //环形进度条
    private CustomProgressDialog registering;

    private Handler handler = new Handler();
    //错误次数
    private int error_time = 0;
    //上一个用户账号
    private String last_num;
    private String TAG = "Login";
    //配置文件的路径
    private String propertiesPath = "/mnt/sdcard/config.properties";
    //对话框
    AlertDialog dialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivityCollector.addActivity(this);
        setContentView(R.layout.activity_login);
        ButterKnife.bind(this);
        //判断网络是否连接
        isNetworkreachable();
        login.setOnClickListener(this);
        setting.setOnClickListener(this);
        changedevId.setOnClickListener(this);
        root.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                root.requestFocus();
                username.clearFocus();
                password.clearFocus();
                closeKeyboard(Login.this, getWindow().getDecorView());
                return false;
            }
        });
        //读取配置文件
        loadProperties();
    }

    /**
     * 读取配置文件
     */
    private void loadProperties() {
        PackageManager packageManager = getPackageManager();
        try {
            PackageInfo pi = packageManager.getPackageInfo(getPackageName(), 0);
            version = pi.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        Properties properties;
        if (new File(propertiesPath).exists()) {
            properties = loadConfig(this, propertiesPath);
            if (properties != null) {
                SipInfo.serverIp = properties.getProperty("serverip");
                SipInfo.devId = properties.getProperty("devId");
                SipInfo.centerPhone = properties.getProperty("centerPhone");
            }
            if (SipInfo.serverIp == null || SipInfo.devId == null || SipInfo.centerPhone == null) {
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (dialog == null || !dialog.isShowing()) {
                            dialog = new AlertDialog.Builder(Login.this)
                                    .setPositiveButton("配置", new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            dialog.dismiss();
                                            changeProperties();
                                        }
                                    })
                                    .create();
                            dialog.setTitle("配置文件不存在,请配置!");
                            dialog.setCanceledOnTouchOutside(false);
                            dialog.setCancelable(false);
                            dialog.show();
                        }
                    }
                });
            }
        } else {
            handler.post(new Runnable() {
                @Override
                public void run() {
                    if (dialog == null || !dialog.isShowing()) {
                        dialog = new AlertDialog.Builder(Login.this)
                                .setPositiveButton("配置", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        dialog.dismiss();
                                        changeProperties();
                                    }
                                })
                                .create();
                        dialog.setTitle("配置文件不存在,请配置!");
                        dialog.setCanceledOnTouchOutside(false);
                        dialog.setCancelable(false);
                        dialog.show();
                    }
                }
            });
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        LogUtil.d(TAG, "onResume");
        //重新读取读取配置文件
        loadProperties();
        isNetworkreachable();
        username.setText("");
        password.setText("");
        root.requestFocus();
        username.clearFocus();
        password.clearFocus();
        closeKeyboard(Login.this, getWindow().getDecorView());
    }

    /**
     * 判断网络是否连接
     *
     * @return
     */
    public boolean isNetworkreachable() {
        ConnectivityManager manager = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        NetworkInfo info = manager.getActiveNetworkInfo();
        if (info == null) {
            SipInfo.isConnected = false;
        } else {
            SipInfo.isConnected = info.getState() == NetworkInfo.State.CONNECTED;
        }
        return SipInfo.isConnected;
    }

    /**
     * 关闭键盘
     *
     * @param context
     * @param view
     */
    public static void closeKeyboard(Context context, View view) {
        InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }

    /**
     * 屏蔽后退键
     */
    @Override
    public void onBackPressed() {

    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.login:
                if (SipInfo.isConnected) {
                    final String num = username.getText().toString();
                    String passwd = password.getText().toString();
                    if (TextUtils.isEmpty(num)) {
                        Toast.makeText(this, "账号不能为空", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (TextUtils.isEmpty(passwd)) {
                        Toast.makeText(this, "密码不能为空", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    //调出修改配置
                    if (num.equals("0000") && passwd.equals("0")) {
                        changeProperties();
                        return;
                    }
                    if (last_num != null) {
                        if (!num.equals(last_num)) {
                            error_time = 0;
                        }
                    }
                    SipInfo.logined = false;
                    SipInfo.loginTimeout = true;
                    SipInfo.userNum = num;
                    SipInfo.password = passwd;
                    SipURL local = new SipURL(SipInfo.REGISTER_ID, SipInfo.serverIp, SipInfo.SERVER_PORT);
                    SipURL remote = new SipURL(SipInfo.SERVER_ID, SipInfo.serverIp, SipInfo.SERVER_PORT);
                    SipInfo.user_from = new NameAddress(SipInfo.userNum, local);
                    SipInfo.user_to = new NameAddress(SipInfo.SERVER_NAME, remote);
                    SipInfo.dev_logined = false;
                    SipInfo.dev_loginTimeout = true;
                    SipURL local_dev = new SipURL(SipInfo.devId, SipInfo.serverIp, SipInfo.SERVER_PORT_DEV);
                    SipURL remote_dev = new SipURL(SipInfo.SERVER_ID, SipInfo.serverIp, SipInfo.SERVER_PORT_DEV);
                    SipInfo.dev_from = new NameAddress(SipInfo.devId, local_dev);
                    SipInfo.dev_to = new NameAddress(SipInfo.SERVER_NAME, remote_dev);
                    registering = new CustomProgressDialog(Login.this);
                    registering.setCancelable(false);//禁止取消
                    registering.setCanceledOnTouchOutside(false);//禁止点击对话框以外的地方取消对话框
                    registering.show();
                    new Thread() {
                        @Override
                        public void run() {
                            try {
                                int hostPort = new Random().nextInt(5000) + 2000;
                                SipInfo.sipUser = new SipUser(Login.this, null, hostPort);//无网络时在主线程操作会报异常
                                //用户注册第一步,发送RegisterRequest信息
                                Message register = SipMessageFactory.createRegisterRequest(
                                        SipInfo.sipUser, SipInfo.user_to, SipInfo.user_from);
                                SipInfo.sipUser.sendMessage(register);
                                sleep(1000);
                                for (int i = 0; i < 2; i++) {
                                    if (!SipInfo.loginTimeout || !SipInfo.isCountExist) {
                                        //若超时或者用户不存在,跳出
                                        break;
                                    }
                                    //若无响应重发
                                    SipInfo.sipUser.sendMessage(register);
                                    sleep(1000);
                                }
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            } finally {
                                if (!SipInfo.isCountExist) {
                                    /**账号不存在提示*/
                                    registering.dismiss();
                                    handler.post(new Runnable() {
                                        @Override
                                        public void run() {
                                            AlertDialog dialog = new AlertDialog.Builder(Login.this)
                                                    .setTitle("不存在该账号")
                                                    .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                                                        @Override
                                                        public void onClick(DialogInterface dialog, int which) {

                                                        }
                                                    })
                                                    .create();
                                            dialog.show();
                                            dialog.setCanceledOnTouchOutside(false);
                                        }
                                    });
                                } else if (!SipInfo.logined) {
                                    registering.dismiss();
                                    if (2 - error_time > 0) {
                                        handler.post(new Runnable() {
                                            @Override
                                            public void run() {
                                                AlertDialog dialog = new AlertDialog.Builder(Login.this)
                                                        .setTitle("密码输入错误/还有" + (2 - error_time) + "次输入机会")
                                                        .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                                                            @Override
                                                            public void onClick(DialogInterface dialog, int which) {

                                                            }
                                                        })
                                                        .create();
                                                dialog.show();
                                                dialog.setCanceledOnTouchOutside(false);
                                                error_time++;
                                                last_num = num;
                                            }
                                        });
                                    } else {
                                        handler.post(new Runnable() {
                                            @Override
                                            public void run() {
                                                AlertDialog dialog = new AlertDialog.Builder(Login.this)
                                                        .setTitle("由于密码输入错误过多,该账号已被冻结")
                                                        .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                                                            @Override
                                                            public void onClick(DialogInterface dialog, int which) {

                                                            }
                                                        })
                                                        .create();
                                                dialog.show();
                                                dialog.setCanceledOnTouchOutside(false);
                                                Toast.makeText(getApplicationContext(), "该账号已被冻结", Toast.LENGTH_SHORT).show();
                                            }
                                        });
                                    }
                                } else {
                                    LogUtil.i(TAG, "用户验证登录成功!");
                                    //开启用户保活心跳包
                                    SipInfo.keepUserAlive = new KeepAlive();
                                    //用户类型为0
                                    SipInfo.keepUserAlive.setType(0);
                                    SipInfo.keepUserAlive.startThread();
                                    //数据库名
                                    String dbPath = SipInfo.userId + ".db";
//                                    deleteDatabase(dbPath);
                                    MyDatabaseHelper myDatabaseHelper = new MyDatabaseHelper(Login.this, dbPath, null, 7);
                                    DatabaseInfo.sqLiteManager = new SQLiteManager(myDatabaseHelper);
                                    //请求app列表时先清除applist
                                    SipInfo.appList.clear();
                                    //请求平台上的app列表
                                    SipInfo.sipUser.sendMessage(SipMessageFactory.createSubscribeRequest(SipInfo.sipUser,
                                            SipInfo.user_to, SipInfo.user_from, BodyFactory.createAppsQueryBody()));

                                    new Thread() {
                                        @Override
                                        public void run() {
                                            try {
                                                int hostPort = new Random().nextInt(5000) + 2000;
                                                SipInfo.sipDev = new SipDev(Login.this, null, hostPort);//无网络时在主线程操作会报异常
                                                //设备注册第一步
                                                Message register = SipMessageFactory.createRegisterRequest(
                                                        SipInfo.sipDev, SipInfo.dev_to, SipInfo.dev_from);
                                                for (int i = 0; i < 3; i++) {
                                                    SipInfo.sipDev.sendMessage(register);
                                                    sleep(2000);
                                                    if (!SipInfo.dev_loginTimeout) {
                                                        break;
                                                    }
                                                }
                                            } catch (InterruptedException e) {
                                                e.printStackTrace();
                                            } finally {
                                                registering.dismiss();
                                                if (SipInfo.dev_logined) {
                                                    Log.i(TAG, "设备注册成功!");
                                                    Log.d(TAG, "设备心跳包发送!");
                                                    //跳转界面
                                                    startActivity(new Intent(Login.this, Main.class));
                                                    //开启设备保活心跳包
                                                    SipInfo.keepDevAlive = new KeepAlive();
                                                    //设备类型为1
                                                    SipInfo.keepDevAlive.setType(1);
                                                    SipInfo.keepDevAlive.startThread();
                                                } else {
                                                    Log.e(TAG, "设备注册失败!");
                                                }
                                            }
                                        }
                                    }.start();
                                }
                            }
                        }
                    }.start();
                } else {
                    /**无网络提示*/
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            AlertDialog dialog = new AlertDialog.Builder(Login.this)
                                    .setTitle("当前无网络,请检查网络连接")
                                    .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            Intent mIntent = new Intent(Settings.ACTION_SETTINGS);
                                            startActivity(mIntent);
                                        }
                                    })
                                    .create();
                            dialog.show();
                            dialog.setCanceledOnTouchOutside(false);
                        }
                    });
                }
                break;
            case R.id.setting:
                Intent mIntent = new Intent(Settings.ACTION_SETTINGS);
                startActivity(mIntent);
                break;
            case R.id.changedevId:
                changeProperties();
                break;
        }

    }

    private void changeProperties() {
        handler.post(new Runnable() {
            @Override
            public void run() {
                TableLayout changeSetting = (TableLayout) getLayoutInflater().inflate(R.layout.change_setting, null);
                final EditText serverIP = (EditText) changeSetting.findViewById(R.id.serverip);
                final EditText devID = (EditText) changeSetting.findViewById(R.id.devid);
                final EditText centerPhonenum = (EditText) changeSetting.findViewById(R.id.centerphonenum);
                serverIP.setText(serverIp);
                devID.setText(devId);
                centerPhonenum.setText(centerPhone);
                alertDialog = new android.support.v7.app.AlertDialog.Builder(Login.this).setTitle("参数配置:")
                        .setView(changeSetting)
                        .setNegativeButton("取消", null)
                        .setPositiveButton("修改", null)
                        .create();
                alertDialog.setCanceledOnTouchOutside(false);
                alertDialog.setCancelable(false);
                alertDialog.show();
                alertDialog.getButton(android.support.v7.app.AlertDialog.BUTTON_POSITIVE)
                        .setOnClickListener(
                                new View.OnClickListener() {
                                    @Override
                                    public void onClick(View v) {
                                        if (TextUtils.isEmpty(serverIP.getText())) {
                                            Toast.makeText(Login.this, "服务器ip不能为空", Toast.LENGTH_SHORT).show();
                                        } else if (TextUtils.isEmpty(devID.getText())) {
                                            Toast.makeText(Login.this, "设备号不能为空", Toast.LENGTH_SHORT).show();
                                        } else if (TextUtils.isEmpty(centerPhonenum.getText())) {
                                            Toast.makeText(Login.this, "中心号码不能为空", Toast.LENGTH_SHORT).show();
                                        } else {

                                            Properties properties = loadConfig(Login.this, propertiesPath);
                                            if (properties != null) {
                                                properties.put("serverip", serverIP.getText().toString().trim());
                                                properties.put("devId", devID.getText().toString().trim());
                                                properties.put("centerPhone", centerPhonenum.getText().toString().trim());
                                            } else {
                                                properties = new Properties();
                                                properties.put("serverip", serverIP.getText().toString().trim());
                                                properties.put("devId", devID.getText().toString().trim());
                                                properties.put("centerPhone", centerPhonenum.getText().toString().trim());
                                            }
                                            serverIp = serverIP.getText().toString().trim();
                                            devId = devID.getText().toString().trim();
                                            centerPhone = centerPhonenum.getText().toString().trim();
                                            saveConfig(Login.this, propertiesPath, properties);
                                            alertDialog.dismiss();
                                        }
                                    }

                                }

                        );
            }
        });
    }

    /**
     * 保存配置文件
     * <p>
     * Title: saveConfig
     * <p>
     * <p>
     * Description:
     * </p>
     *
     * @param context
     * @param file
     * @param properties
     * @return
     */
    public static boolean saveConfig(Context context, String file,
                                     Properties properties) {
        try {
            File fil = new File(file);
            if (!fil.exists())
                fil.createNewFile();
            FileOutputStream s = new FileOutputStream(fil);
            properties.store(s, "");
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    /**
     * 读取配置文件
     * <p>
     * Title: loadConfig
     * <p>
     * <p>
     * Description:
     * </p>
     *
     * @param context
     * @param file
     * @return
     */
    public static Properties loadConfig(Context context, String file) {
        Properties properties = new Properties();
        try {
            FileInputStream s = new FileInputStream(file);
            properties.load(s);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        return properties;
    }
}
