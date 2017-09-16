package com.punuo.sys.app.xungeng.sip;

import android.net.sip.SipAudioCall;
import android.os.Handler;
import android.support.v7.app.AlertDialog;

import com.punuo.sys.app.xungeng.model.App;
import com.punuo.sys.app.xungeng.model.Device;
import com.punuo.sys.app.xungeng.model.Friend;
import com.punuo.sys.app.xungeng.model.LastestMsg;
import com.punuo.sys.app.xungeng.model.PointInfo;
import com.punuo.sys.app.xungeng.movierecord.MovieRecord;
import com.punuo.sys.app.xungeng.service.SipService;
import com.punuo.sys.app.xungeng.ui.MakeSmallVideo;
import com.punuo.sys.app.xungeng.ui.MyCamera;

import org.zoolu.sip.address.NameAddress;
import org.zoolu.sip.message.Message;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by chenblue23 on 2016/4/20.
 */

public class SipInfo {
    public static final String SERVER_NAME = "rvsup";
    public static final String SERVER_ID = "330100000010000090";
    public static final int SERVER_PORT = 6061;
    public static final int SERVER_PORT_DEV = 6060;
    public static final String REGISTER_ID = "330100000010000190";//注册第一步的id，服务器会返回自己的id
    public static String serverIp;
    public static String username;
    public static String userNum;
    public static String userId;
    public static String devId;
    public static String seed;
    public static String salt;
    public static String password;
    public static String phoneNum;
    public static SipUser sipUser;
    public static SipDev sipDev;
    public static String centerPhone = "7001";
    public static NameAddress user_from;
    public static NameAddress user_to;
    public static NameAddress dev_from;
    public static NameAddress dev_to;
    public static NameAddress toDev;
    public static NameAddress toUser;
    public static String devName="";
    public static KeepAlive keepUserAlive;
    public static KeepAlive keepDevAlive;
    public static boolean logined = false;
    public static boolean loginTimeout = true;
    public static boolean dev_logined = false;
    public static boolean dev_loginTimeout = true;
    public static boolean user_heartbeatResponse = false;
    public static boolean dev_heartbeatResponse = false;
    public static boolean queryResponse = false;
    public static boolean inviteResponse = false;
    public static boolean isConnected = false;
    public static boolean isCountExist = true;
    public static int devCount;
    public static boolean decoding = false;
    public static List<Device> devList = new ArrayList<Device>();
    public static Message msg;
    public static Handler notifymedia;
    public static Handler Phone=new Handler();
    public static Handler loginReplace;
    public static int friendCount;
    public static int friendChangeTime;
    public static HashMap<String, List<Friend>> friendList = new HashMap<String, List<Friend>>();
    public static List<Friend> friends = new ArrayList<Friend>();
    public static boolean isAlive = false;
    public static List<App> appList = new ArrayList<>();
    public static int messageCount;
    public static List<LastestMsg> lastestMsgs = new ArrayList<>();
    public static List<PointInfo> pointInfoList = new ArrayList<>();
    public static List<PointInfo> pointInfoListbd09 = new ArrayList<>();
    public static AlertDialog alertDialog;
    public static String version;
    //库区号
    public static int addr_code;
    public static MakeSmallVideo instance;
    public static MyCamera myCamera;
    public static MovieRecord movieRecord;

    public static SipAudioCall lastCall;
    public static SipService sipService;
    public static int[] points;
    public static int lastPoint=-1;
    public static String turn;
}
