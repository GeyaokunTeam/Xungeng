package com.punuo.sys.app.xungeng.sip;


import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.MediaPlayer;
import android.util.Log;

import com.punuo.sys.app.R;
import com.punuo.sys.app.xungeng.db.DatabaseInfo;
import com.punuo.sys.app.xungeng.ftp.FTP;
import com.punuo.sys.app.xungeng.model.App;
import com.punuo.sys.app.xungeng.model.Device;
import com.punuo.sys.app.xungeng.model.Friend;
import com.punuo.sys.app.xungeng.model.Msg;
import com.punuo.sys.app.xungeng.model.MyFile;
import com.punuo.sys.app.xungeng.model.PointInfo;
import com.punuo.sys.app.xungeng.ui.CheckActivity;
import com.punuo.sys.app.xungeng.util.LogUtil;
import com.punuo.sys.app.xungeng.util.SHA1;
import com.punuo.sys.app.xungeng.video.VideoInfo;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.zoolu.sip.address.NameAddress;
import org.zoolu.sip.address.SipURL;
import org.zoolu.sip.message.Message;
import org.zoolu.sip.provider.SipProvider;
import org.zoolu.sip.provider.Transport;
import org.zoolu.sip.provider.TransportConnId;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import static com.punuo.sys.app.xungeng.sip.SipInfo.loginReplace;

/**
 * Created by chenblue23 on 2016/4/20.
 */
public class SipUser extends SipProvider {
    private Context context;
    public static final String TAG = "SipUser";
    public static final String[] PROTOCOLS = {"udp"};
    //    private MessageListener messageListener;
    private ChangePWDListener changePWDListener;
    private ExecutorService pool = Executors.newFixedThreadPool(3);
    private LoadAppList loadAppList;
    private String sdPath = "/mnt/sdcard/xungeng/download/icon/";
    private MessageListener messageListener;
    private TotalListener totalListener;
    private BottomListener bottomListener;
    private LoginNotifyListener loginNotifyListener;
    private boolean flag;
    MediaPlayer mediaPlayer;

    public SipUser(Context context, String viaAddr, int hostPort) {
        super(viaAddr, hostPort, PROTOCOLS, null);
        this.context = context;
        mediaPlayer = MediaPlayer.create(context, R.raw.msg);
    }

    public TransportConnId sendMessage(Message msg) {
        return sendMessage(msg, SipInfo.serverIp, SipInfo.SERVER_PORT);
    }

    public TransportConnId sendMessage(final Message msg, final String destAddr, final int destPort) {
        LogUtil.i(TAG, "<----------send sip message---------->");
        LogUtil.i(TAG, msg.toString());
        TransportConnId id = null;
        try {
            id = pool.submit(new Callable<TransportConnId>() {
                public TransportConnId call() {
                    return sendMessage(msg, "udp", destAddr, destPort, 0);
                }
            }).get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
        return id;
    }

    public synchronized void onReceivedMessage(Transport transport, Message msg) {
        LogUtil.i(TAG, "<----------received sip message---------->");
        LogUtil.i(TAG, msg.toString());
        int port = msg.getRemotePort();
        if (port == SipInfo.SERVER_PORT) {
            Log.e(TAG, "onReceivedMessage: " + port);
            String body = msg.getBody();
            if (msg.isRequest()) {// 请求消息
                if (!requestParse(msg)) {
                    int requestType = bodyParse(body);
                    switch (requestType) {
                        case 4:
                            SipInfo.sipUser.sendMessage(SipMessageFactory.createResponse(msg, 200, "OK", ""));
                            break;
                    }
                }
            } else { // 响应消息
                int code = msg.getStatusLine().getCode();
                if (code == 200) {
                    if (!responseParse(msg)) {
                        bodyParse(body);
                    }
                } else if (code == 401) {
                    SipInfo.loginTimeout = false;
                    SipInfo.isCountExist = true;
                } else if (code == 402) {
                    SipInfo.isCountExist = false;
                }
            }
        }
    }

    private int bodyParse(String body) {
        if (body != null) {
            StringReader sr = new StringReader(body);
            InputSource is = new InputSource(sr);
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder;
            Document document;
            try {
                builder = factory.newDocumentBuilder();
                document = builder.parse(is);
                Element root = document.getDocumentElement();
                final String type = root.getTagName();
                switch (type) {
                    case "negotiate_response":/*注册第一步响应*/
                        Element seedElement = (Element) root.getElementsByTagName("seed").item(0);
                        Element userIdElement = (Element) root.getElementsByTagName("user_id").item(0);
                        if (userIdElement != null) {
                            Element saltElement = (Element) root.getElementsByTagName("salt").item(0);
                            Element phoneNumElement = (Element) root.getElementsByTagName("phone_num").item(0);
                            Element realNameElement = (Element) root.getElementsByTagName("real_name").item(0);
                            SipInfo.userId = userIdElement.getFirstChild().getNodeValue();
                            SipInfo.username = realNameElement.getFirstChild().getNodeValue();
                            SipURL local = new SipURL(SipInfo.userId, SipInfo.serverIp, SipInfo.SERVER_PORT);
                            SipInfo.user_from.setAddress(local);
                            SipInfo.phoneNum = phoneNumElement.getFirstChild().getNodeValue();
                            SipInfo.seed = seedElement.getFirstChild().getNodeValue();
                            SipInfo.salt = saltElement.getFirstChild().getNodeValue();
                            LogUtil.i(TAG, "收到用户注册第一步响应");
                            SHA1 sha1 = SHA1.getInstance();
                            String password = sha1.hashData(SipInfo.salt + SipInfo.password);
                            password = sha1.hashData(SipInfo.seed + password);
                            Message register = SipMessageFactory.createRegisterRequest(
                                    SipInfo.sipUser, SipInfo.user_to, SipInfo.user_from,
                                    BodyFactory.createRegisterBody(password));
                            SipInfo.sipUser.sendMessage(register);
                        } else {
                            SipInfo.isAlive = false;
                            LogUtil.e(TAG, "掉线");
                            SipInfo.logined = false;
                            SipURL local = new SipURL(SipInfo.REGISTER_ID, SipInfo.serverIp, SipInfo.SERVER_PORT);
                            NameAddress from = new NameAddress(SipInfo.userNum, local);
                            Message register = SipMessageFactory.createRegisterRequest(
                                    SipInfo.sipUser, SipInfo.user_to, from);
                            SipInfo.sipUser.sendMessage(register);
                        }
                        return 1;
                    case "login_response":
                        if (SipInfo.logined) {
                            SipInfo.user_heartbeatResponse = true;
                            LogUtil.i(TAG, "收到用户心跳回复");
                            SipInfo.isAlive = true;
                            LogUtil.i(TAG, "用户在线!");
                        } else {
                            SipInfo.logined = true;
                            SipInfo.isCountExist = true;
                            SipInfo.loginTimeout = false;
                            LogUtil.i(TAG, "用户注册成功");
                            /*请求好友列表*/
                            SipInfo.friendCount = 0;
                            SipInfo.friendChangeTime = 0;
                            SipInfo.friendList.clear();
                            SipInfo.friends.clear();
                            SipInfo.sipUser.sendMessage(SipMessageFactory.createSubscribeRequest(SipInfo.sipUser,
                                    SipInfo.user_to, SipInfo.user_from, BodyFactory.createFriendsQueryBody(0, 0)));


                        }
                        return 2;
                    case "dev_count"://设备数量
                        SipInfo.devCount = Integer.parseInt(root.getFirstChild().getNodeValue());
                        LogUtil.d(TAG, "设备总数：" + SipInfo.devCount);
                        return 3;
                    case "dev_notify"://设备列表
                        Element devsElement = (Element) root.getElementsByTagName("devs").item(0);
                        Element loginElement = (Element) root.getElementsByTagName("login").item(0);
                        if (devsElement != null) {
                            NodeList devs = devsElement.getElementsByTagName("dev");
                            for (int i = 0; i < devs.getLength(); i++) {
                                Device device = new Device();
                                Element devElement = (Element) devs.item(i);
                                Element devIdElement = (Element) devElement.getElementsByTagName("devid").item(0);
                                Element nameElement = (Element) devElement.getElementsByTagName("name").item(0);
                                Element phoneElement = (Element) devElement.getElementsByTagName("phone").item(0);
                                Element devTypeElement = (Element) devElement.getElementsByTagName("dev_type").item(0);
                                Element liveElement = (Element) devElement.getElementsByTagName("live").item(0);
                                String name = nameElement.getFirstChild().getNodeValue();
                                device.setDevId(devIdElement.getFirstChild().getNodeValue());
                                device.setName(name);
//                                device.setPhoneNum(phoneElement.getFirstChild().getNodeValue());
                                device.setDevType(devTypeElement.getFirstChild().getNodeValue());
                                if (liveElement.getFirstChild().getNodeValue().equals("1")) {
                                    device.setLive(true);
                                } else {
                                    device.setLive(false);
                                }
                                if (!device.getDevId().equals(SipInfo.devId)) {
                                    SipInfo.devList.add(device);
                                } else {
                                    SipInfo.devName = name;
                                }
                            }
                            if (loginNotifyListener != null) {
                                loginNotifyListener.onDevNotify();
                            }
                            LogUtil.d(TAG, "当前设备数：" + SipInfo.devList.size());
                            return 4;
                        } else if (loginElement != null) {
                            Element devIdElement = (Element) loginElement.getElementsByTagName("devid").item(0);
                            Element liveElement = (Element) loginElement.getElementsByTagName("live").item(0);
                            String devid = devIdElement.getFirstChild().getNodeValue();
                            if (!devid.equals(SipInfo.devId)) {
                                Device device = new Device();
                                device.setDevId(devid);
                                int index = SipInfo.devList.indexOf(device);
                                if (index != -1) {
                                    if (liveElement.getFirstChild().getNodeValue().equals("1")) {
                                        SipInfo.devList.get(index).setLive(true);
                                    } else {
                                        SipInfo.devList.get(index).setLive(false);
                                    }
                                }
                                if (loginNotifyListener != null) {
                                    loginNotifyListener.onDevNotify();
                                }
                            }
                            return 5;
                        }
                    case "query_response":
                        Element resolutionElement = (Element) root.getElementsByTagName("resolution").item(0);
                        switch (resolutionElement.getFirstChild().getNodeValue()) {
                            case "QCIF":
                                VideoInfo.width = 176;
                                VideoInfo.height = 144;
                                VideoInfo.videoType = 1;
                                break;
                            case "CIF":
                                VideoInfo.width = 352;
                                VideoInfo.height = 288;
                                VideoInfo.videoType = 2;
                                break;
                            case "QCIF_MOBILE_SOFT":
                                VideoInfo.width = 176;
                                VideoInfo.height = 144;
                                VideoInfo.videoType = 3;
                                break;
                            case "CIF_MOBILE_SOFT":
                                VideoInfo.width = 320;
                                VideoInfo.height = 240;
                                VideoInfo.videoType = 4;
                                break;
                            default:
                                break;
                        }
                        SipInfo.queryResponse = true;
                        return 6;
                    case "media":
                        Element peerElement = (Element) root.getElementsByTagName("peer").item(0);
                        Element magicElement = (Element) root.getElementsByTagName("magic").item(0);
                        String peer = peerElement.getFirstChild().getNodeValue();
                        VideoInfo.rtpIp = peer.substring(0, peer.indexOf("UDP")).trim();
                        VideoInfo.rtpPort = Integer.parseInt(peer.substring(peer.indexOf("UDP") + 3).trim());
                        String magic = magicElement.getFirstChild().getNodeValue();
                        VideoInfo.magic = new byte[magic.length() / 2 + magic.length() % 2];
                        for (int i = 0; i < VideoInfo.magic.length; i++) {
                            try {
                                VideoInfo.magic[i] = (byte) (0xff & Integer.parseInt(magic.substring(i * 2, i * 2 + 2), 16));
                            } catch (NumberFormatException e) {
                                e.printStackTrace();
                            }
                        }
                        SipInfo.inviteResponse = true;
                        return 7;
                    case "user_notify":
                        Element userloginElement = (Element) root.getElementsByTagName("login").item(0);
                        Element useridElement = (Element) userloginElement.getElementsByTagName("userid").item(0);
                        Element liveElement = (Element) userloginElement.getElementsByTagName("live").item(0);
                        String userid = useridElement.getFirstChild().getNodeValue();
                        if (!userid.equals(SipInfo.userId)) {
                            Friend friend = new Friend();
                            friend.setUserId(userid);
                            int index = SipInfo.friends.indexOf(friend);
                            int index2 = -1;
                            if (index != -1) {
                                if (liveElement.getFirstChild().getNodeValue().equals("True")) {
                                    SipInfo.friends.get(index).setLive(true);
                                } else {
                                    SipInfo.friends.get(index).setLive(false);
                                }
                                index2 = SipInfo.friendList.get(SipInfo.friends.get(index).getUnit()).indexOf(friend);
                            }
                            if (index2 != -1) {
                                if (liveElement.getFirstChild().getNodeValue().equals("True")) {
                                    SipInfo.friendList.get(SipInfo.friends.get(index).getUnit()).get(index2).setLive(true);
                                } else {
                                    SipInfo.friendList.get(SipInfo.friends.get(index).getUnit()).get(index2).setLive(false);
                                }
                            }
                            if (loginNotifyListener != null) {
                                loginNotifyListener.onUserNotify();
                            }
                        }
                        return 8;
                    case "session_notify":
                        if (SipInfo.instance != null) {
                            SipInfo.instance.finish();
                            SipInfo.instance = null;
                        }
                        if (SipInfo.myCamera != null) {
                            SipInfo.myCamera.finish();
                            SipInfo.myCamera = null;
                        }
                        if (SipInfo.movieRecord != null) {
                            SipInfo.movieRecord.onBackPressed();
                            SipInfo.movieRecord = null;
                        }
                        if (loginReplace != null) {
                            loginReplace.sendEmptyMessage(0x1111);
                        }
                        return 9;
                }
            } catch (ParserConfigurationException e) {
                e.printStackTrace();
            } catch (SAXException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (Exception e) {
                e.printStackTrace();
            }

        } else

        {
            LogUtil.d(TAG, "body is null");
        }

        return -1;
    }

    private boolean requestParse(final Message msg) {
        String body = msg.getBody();
        if (body != null) {
            StringReader sr = new StringReader(body);
            InputSource is = new InputSource(sr);
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder;
            Document document;
            try {
                builder = factory.newDocumentBuilder();
                document = builder.parse(is);
                Element root = document.getDocumentElement();
                final String type = root.getTagName();
                switch (type) {
                    case "friends": {
                        NodeList friends = root.getElementsByTagName("friend");
                        for (int i = 0; i < friends.getLength(); i++) {
                            Friend friend = new Friend();
                            Element friendElement = (Element) friends.item(i);
                            Element userIdElement = (Element) friendElement.getElementsByTagName("userid").item(0);
                            Element usernameElement = (Element) friendElement.getElementsByTagName("username").item(0);
                            Element phoneElement = (Element) friendElement.getElementsByTagName("phone_num").item(0);
                            Element liveElement = (Element) friendElement.getElementsByTagName("live").item(0);
                            Element telElement = (Element) friendElement.getElementsByTagName("tel_num").item(0);
                            Element realnamement = (Element) friendElement.getElementsByTagName("real_name").item(0);
                            Element unitment = (Element) friendElement.getElementsByTagName("unit").item(0);
                            friend.setUserId(userIdElement.getFirstChild().getNodeValue());
                            friend.setUsername(usernameElement.getFirstChild().getNodeValue());
                            friend.setPhoneNum(phoneElement.getFirstChild().getNodeValue());
                            friend.setRealName(realnamement.getFirstChild().getNodeValue());
                            String tel = telElement.getFirstChild().getNodeValue();
                            if (!tel.equals("None")) {
                                friend.setTelNum(tel);
                            }
                            switch (unitment.getFirstChild().getNodeValue()) {
                                case "1":
                                    friend.setUnit("德清");
                                    break;
                                case "2":
                                    friend.setUnit("越州");
                                    break;
                                case "3":
                                    friend.setUnit("舟山");
                                    break;
                                case "4":
                                    friend.setUnit("衢州");
                                    break;
                                default:
                                    friend.setUnit(unitment.getFirstChild().getNodeValue());
                                    break;
                            }

                            if (liveElement.getFirstChild().getNodeValue().equals("1")) {
                                friend.setLive(true);
                            } else {
                                friend.setLive(false);
                            }
                            if (!SipInfo.friendList.containsKey(friend.getUnit())) {
                                SipInfo.friendList.put(friend.getUnit(), new ArrayList<Friend>());
                            }
                            SipInfo.friends.add(friend);
                            SipInfo.friendList.get(friend.getUnit()).add(friend);
                        }
                        Message message = SipMessageFactory.createResponse(msg, 200, "OK", BodyFactory.createFriendsResBody(200));
                        SipInfo.sipUser.sendMessage(message);
                        if (loginNotifyListener != null) {
                            loginNotifyListener.onUserNotify();
                        }
                        return true;
                    }
                    case "message": {
                        Element idElement = (Element) root.getElementsByTagName("id").item(0);
                        Element fromElement = (Element) root.getElementsByTagName("from").item(0);
                        Element toElement = (Element) root.getElementsByTagName("to").item(0);
                        Element contentElement = (Element) root.getElementsByTagName("content").item(0);
                        Element timeElement = (Element) root.getElementsByTagName("time").item(0);
                        Element typeElement = (Element) root.getElementsByTagName("type").item(0);
                        final String id = idElement.getFirstChild().getNodeValue();
                        final String content = contentElement.getFirstChild().getNodeValue();
                        final int time = Integer.parseInt(timeElement.getFirstChild().getNodeValue());
                        final String fromUserId = fromElement.getFirstChild().getNodeValue();
                        final String toUserId = toElement.getFirstChild().getNodeValue();
                        final int msgtype = Integer.parseInt(typeElement.getFirstChild().getNodeValue());
                        final int isTimeShow;
                        if (time - DatabaseInfo.sqLiteManager.queryLastTime(fromUserId, toUserId) > 300) {
                            isTimeShow = 1;
                        } else {
                            isTimeShow = 0;
                        }
                        DatabaseInfo.sqLiteManager.insertMessage(id, fromUserId, toUserId, time,
                                isTimeShow, content, msgtype, 0);
                        Message message = SipMessageFactory.createResponse(msg, 200, "OK", BodyFactory.createMessageResBody(id, 200));
                        SipInfo.sipUser.sendMessage(message);
                        if (msgtype == 2) {
                            DatabaseInfo.sqLiteManager.insertLastestMsgFrom(0, fromUserId, "[位置信息]", time);
                        } else {
                            DatabaseInfo.sqLiteManager.insertLastestMsgFrom(0, fromUserId, content, time);
                        }
                        Msg friendMsg = new Msg();
                        friendMsg.setMsgId(id);
                        friendMsg.setFromUserId(fromUserId);
                        friendMsg.setToUserId(toUserId);
                        friendMsg.setTime(time);
                        friendMsg.setIsTimeShow(isTimeShow);
                        friendMsg.setContent(content);
                        friendMsg.setType(msgtype);
                        if (totalListener != null) {
                            totalListener.onReceivedTotalMessage(friendMsg);
                        }
                        if (bottomListener != null) {
                            bottomListener.onReceivedBottomMessage(friendMsg);
                        }

                        if (messageListener != null) {
                            messageListener.onReceivedMessage(friendMsg);
                        }
                        if (!mediaPlayer.isPlaying()) {
                            mediaPlayer.start();
                        }

                        return true;
                    }
                    case "filetransfer": {
                        Element fromElement = (Element) root.getElementsByTagName("from").item(0);
                        Element toElement = (Element) root.getElementsByTagName("to").item(0);
                        Element idElement = (Element) root.getElementsByTagName("id").item(0);
                        Element nameElement = (Element) root.getElementsByTagName("name").item(0);
                        Element fileTypeElement = (Element) root.getElementsByTagName("filetype").item(0);
                        Element timeElement = (Element) root.getElementsByTagName("time").item(0);
                        Element pathElement = (Element) root.getElementsByTagName("path").item(0);
                        Element sizeElement = (Element) root.getElementsByTagName("size").item(0);
                        Element md5Element = (Element) root.getElementsByTagName("md5").item(0);
                        Element typeElement = (Element) root.getElementsByTagName("type").item(0);
                        final String fromUserId = fromElement.getFirstChild().getNodeValue();
                        final String toUserId = toElement.getFirstChild().getNodeValue();
                        final String id = idElement.getFirstChild().getNodeValue();
                        final String name = nameElement.getFirstChild().getNodeValue();
                        final String fileType = fileTypeElement.getFirstChild().getNodeValue();
                        final int time = Integer.parseInt(timeElement.getFirstChild().getNodeValue());
                        final String ftppath = pathElement.getFirstChild().getNodeValue();
                        final String size = sizeElement.getFirstChild().getNodeValue();
                        final String md5 = md5Element.getFirstChild().getNodeValue();
                        final int typeInt = Integer.parseInt(typeElement.getFirstChild().getNodeValue());
                        final int isTimeShow;
                        if (time - DatabaseInfo.sqLiteManager.queryLastTime(fromUserId, toUserId) > 300) {
                            isTimeShow = 1;
                        } else {
                            isTimeShow = 0;
                        }
                        String content = "[文件]";
                        if (typeInt == 1) {
                            content = "[小视频]";
                        } else if (typeInt == 2) {
                            content = "[文件]";
                        } else if (typeInt == 3) {
                            content = "[图片]";
                            new Thread() {
                                @Override
                                public void run() {
                                    String thumnailpath = ftppath.replace(context.getString(R.string.Image), context.getString(R.string.Thumbnail));
                                    try {
                                        new FTP().downloadSingleFile(thumnailpath, "/mnt/sdcard/xungeng/Files/Camera/Thumbnail/", name, new FTP.DownLoadProgressListener() {
                                            @Override
                                            public void onDownLoadProgress(String currentStep, long downProcess, File file) {
                                                if (currentStep.equals(FTP.FTP_DOWN_SUCCESS)) {
                                                    DatabaseInfo.sqLiteManager.insertMessage(id, fromUserId, toUserId, time,
                                                            isTimeShow, "[图片]", 1, 0);
                                                    DatabaseInfo.sqLiteManager.insertFile(id, name, fromUserId, fileType, time, null,
                                                            ftppath, Long.parseLong(size), md5, typeInt, 0, 0);
                                                    Message message = SipMessageFactory.createResponse(msg, 200, "OK",
                                                            BodyFactory.createFileTransferResBody(id, 200));
                                                    SipInfo.sipUser.sendMessage(message);
                                                    DatabaseInfo.sqLiteManager.insertLastestMsgFrom(0, fromUserId, "[图片]", time);
                                                    final Msg friendMsg = new Msg();
                                                    friendMsg.setMsgId(id);
                                                    friendMsg.setFromUserId(fromUserId);
                                                    friendMsg.setToUserId(toUserId);
                                                    friendMsg.setTime(time);
                                                    friendMsg.setIsTimeShow(isTimeShow);
                                                    friendMsg.setContent("[图片]");
                                                    friendMsg.setType(1);

                                                    if (messageListener != null) {
                                                        messageListener.onReceivedMessage(friendMsg);
                                                    }
                                                    if (bottomListener != null) {
                                                        bottomListener.onReceivedBottomMessage(friendMsg);
                                                    }
                                                    if (totalListener != null) {
                                                        totalListener.onReceivedTotalMessage(friendMsg);
                                                    }
                                                }
                                            }
                                        });
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    }
                                }
                            }.start();
                        } else if (typeInt == 0) {
                            content = "[语音消息]";
                        } else if (typeInt == 5) {
                            content = "[位置]";
                        }
                        if (typeInt != 3) {
                            DatabaseInfo.sqLiteManager.insertMessage(id, fromUserId, toUserId, time,
                                    isTimeShow, content, 1, 0);
                            DatabaseInfo.sqLiteManager.insertFile(id, name, fromUserId, fileType, time, null,
                                    ftppath, Long.parseLong(size), md5, typeInt, 0, 0);
                            Message message = SipMessageFactory.createResponse(msg, 200, "OK",
                                    BodyFactory.createFileTransferResBody(id, 200));
                            SipInfo.sipUser.sendMessage(message);
                            DatabaseInfo.sqLiteManager.insertLastestMsgFrom(0, fromUserId, content, time);
                            final Msg friendMsg = new Msg();
                            friendMsg.setMsgId(id);
                            friendMsg.setFromUserId(fromUserId);
                            friendMsg.setToUserId(toUserId);
                            friendMsg.setTime(time);
                            friendMsg.setIsTimeShow(isTimeShow);
                            friendMsg.setContent(content);
                            friendMsg.setType(1);

                            if (messageListener != null) {
                                messageListener.onReceivedMessage(friendMsg);
                            }
                            if (bottomListener != null) {
                                bottomListener.onReceivedBottomMessage(friendMsg);
                            }
                            if (totalListener != null) {
                                totalListener.onReceivedTotalMessage(friendMsg);
                            }
                            if (!mediaPlayer.isPlaying()) {
                                mediaPlayer.start();
                            }
                        }
                        return true;
                    }
                    default:
                        return false;
                }
            } catch (ParserConfigurationException e) {
                e.printStackTrace();
            } catch (SAXException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (Exception e) {
                e.printStackTrace();
            }


        } else {
            LogUtil.d(TAG, "body is null");
            return true;
        }
        return false;
    }

    private boolean responseParse(Message msg) {
        String body = msg.getBody();
        if (body != null) {
            StringReader sr = new StringReader(body);
            InputSource is = new InputSource(sr);
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder;
            Document document;
            try {
                builder = factory.newDocumentBuilder();
                document = builder.parse(is);
                Element root = document.getDocumentElement();
                String type = root.getTagName();
                Element codeElement;
                String code;
                switch (type) {
                    case "points_query":
                        codeElement = (Element) root.getElementsByTagName("code").item(0);
                        code = codeElement.getFirstChild().getNodeValue();
                        if (code.equals("200")) {
                            Element pointsElement = (Element) root.getElementsByTagName("points").item(0);
                            NodeList points = pointsElement.getElementsByTagName("point");
                            if (points.getLength() > 0) {
                                for (int i = 0; i < points.getLength(); i++) {
                                    Element pointElement = (Element) points.item(i);
                                    Element idELement = (Element) pointElement.getElementsByTagName("point_id").item(0);
                                    Element nameELement = (Element) pointElement.getElementsByTagName("name").item(0);
                                    Element langELement = (Element) pointElement.getElementsByTagName("lang").item(0);
                                    Element latELement = (Element) pointElement.getElementsByTagName("lat").item(0);
                                    int id = Integer.parseInt(idELement.getFirstChild().getNodeValue());
                                    String name = nameELement.getFirstChild().getNodeValue();
                                    double lang = Double.parseDouble(langELement.getFirstChild().getNodeValue());
                                    double lat = Double.parseDouble(latELement.getFirstChild().getNodeValue());
                                    double pointGcj02[] = bd09_To_Gcj02(lat, lang);
                                    //Gcj02坐标
                                    PointInfo pointInfoGcj02 = new PointInfo();
                                    pointInfoGcj02.setId(id);
                                    pointInfoGcj02.setName(name);
                                    pointInfoGcj02.setLang(pointGcj02[1]);
                                    pointInfoGcj02.setLat(pointGcj02[0]);
                                    pointInfoGcj02.setCheck(false);
                                    PointInfo pointInfobd09 = new PointInfo();
                                    pointInfobd09.setId(id);
                                    pointInfobd09.setName(name);
                                    pointInfobd09.setLang(lang);
                                    pointInfobd09.setLat(lat);
                                    pointInfobd09.setCheck(false);
                                    SipInfo.pointInfoList.add(pointInfoGcj02);
                                    SipInfo.pointInfoListbd09.add(pointInfobd09);
                                }
                                Intent xunGeng = new Intent(context, CheckActivity.class);
                                xunGeng.putExtra("addr_code", SipInfo.addr_code);
                                context.startActivity(xunGeng);
                                SharedPreferences preferences=context.getSharedPreferences("kuqu",Context.MODE_PRIVATE);
                                SharedPreferences.Editor e=preferences.edit();
                                e.putInt("addr_code",SipInfo.addr_code);
                                e.apply();
                            }
                        }
                        break;
                    case "friends_query":
                        codeElement = (Element) root.getElementsByTagName("code").item(0);
                        code = codeElement.getFirstChild().getNodeValue();
                        if (code.equals("200")) {
                            Element numElement = (Element) root.getElementsByTagName("num").item(0);
                            SipInfo.friendCount = Integer.parseInt(numElement.getFirstChild().getNodeValue());
                        }
                        return true;
                    case "app_query": {
                        codeElement = (Element) root.getElementsByTagName("code").item(0);
                        code = codeElement.getFirstChild().getNodeValue();
                        if (code.equals("200")) {
                            Element appsElement = (Element) root.getElementsByTagName("apps").item(0);
                            NodeList apps = appsElement.getElementsByTagName("app");
                            if (apps.getLength() > 0) {
                                for (int i = 0; i < apps.getLength(); i++) {
                                    Element appElement = (Element) apps.item(i);
                                    Element appidElement = (Element) appElement.getElementsByTagName("appid").item(0);
                                    Element appnameElement = (Element) appElement.getElementsByTagName("appname").item(0);
                                    Element sizeElement = (Element) appElement.getElementsByTagName("size").item(0);
                                    Element urlElement = (Element) appElement.getElementsByTagName("url").item(0);
                                    Element iconurlElement = (Element) appElement.getElementsByTagName("iconurl").item(0);
                                    Element descElement = (Element) appElement.getElementsByTagName("desc").item(0);
                                    Element nameElement = (Element) appElement.getElementsByTagName("name").item(0);
                                    Element iconnameElement = (Element) appElement.getElementsByTagName("iconname").item(0);
                                    String appid = appidElement.getFirstChild().getNodeValue();
                                    String appname = appnameElement.getFirstChild().getNodeValue();
                                    long size = Long.parseLong(sizeElement.getFirstChild().getNodeValue());
                                    String url = urlElement.getFirstChild().getNodeValue();
                                    String iconurl = iconurlElement.getFirstChild().getNodeValue();
                                    String desc = descElement.getFirstChild().getNodeValue();
                                    String name = nameElement.getFirstChild().getNodeValue();
                                    String iconname = iconnameElement.getFirstChild().getNodeValue();
                                    App app = new App();
                                    app.setAppid(appid);
                                    app.setAppname(appname);
                                    app.setSize(size);
                                    app.setUrl(url);
                                    app.setIconUrl(iconurl);
                                    app.setDesc(desc);
                                    app.setName(name);
                                    app.setIconname(iconname);
                                    SipInfo.appList.add(app);
                                    DatabaseInfo.sqLiteManager.insertAppInfo(appid, appname, size, null, 0);
                                }
                                for (int i = 0; i < SipInfo.appList.size(); i++) {
                                    final int finalI = i;
                                    Thread thread = new Thread() {
                                        @Override
                                        public void run() {
                                            File file = new File(sdPath + SipInfo.appList.get(finalI).getIconname());
                                            if (file.exists()) {

                                            } else {
                                                try {
                                                    new FTP().downloadSingleFile(SipInfo.appList.get(finalI).getIconUrl(),
                                                            sdPath, SipInfo.appList.get(finalI).getIconname(), new FTP.DownLoadProgressListener() {
                                                                @Override
                                                                public void onDownLoadProgress(String currentStep, long downProcess, File file) {
                                                                    Log.d(TAG, currentStep);
                                                                }

                                                            });
                                                } catch (Exception e) {
                                                    e.printStackTrace();
                                                }
                                            }
                                        }
                                    };
                                    thread.start();
                                }
                            }
                        }
                        return true;
                    }
                    case "user_gps_update_response":
                        break;
                    case "update_password_response":
                        Element resultElement = (Element) root.getElementsByTagName("result").item(0);
                        String result = resultElement.getFirstChild().getNodeValue();
                        if (result.equals("0")) {
                            if (changePWDListener != null) {
                                changePWDListener.onChangePWD(1);
                            }
                        } else {
                            if (changePWDListener != null) {
                                changePWDListener.onChangePWD(0);
                            }
                        }
                        return true;
                    default:
                        return false;
                }
            } catch (ParserConfigurationException e) {
                e.printStackTrace();
            } catch (SAXException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (Exception e) {
                e.printStackTrace();
            }


        } else {
            LogUtil.d(TAG, "body is null");
            return true;
        }
        return false;
    }

    public void setMessageListener(MessageListener messageListener) {
        this.messageListener = messageListener;
    }

    public interface MessageListener {
        void onReceivedMessage(Msg msg);
    }

    public interface TotalListener {
//        void onReceivedTotalNotice(Notice notice);

        void onReceivedTotalMessage(Msg msg);

        void onReceivedTotalFileshare(MyFile myfile);
    }

    public void setTotalListener(TotalListener totalListener) {
        this.totalListener = totalListener;
    }

    public interface BottomListener {
//        void onReceivedBottomNotice(Notice notice);

        void onReceivedBottomMessage(Msg msg);

        void onReceivedBottomFileshare(MyFile myfile);
    }

    public void setBottomListener(BottomListener bottomListener) {
        this.bottomListener = bottomListener;
    }

    public void setChangePWDListener(ChangePWDListener changePWDListener) {
        this.changePWDListener = changePWDListener;
    }

    public interface ChangePWDListener {
        void onChangePWD(int i);
    }

    public void setLoadingAppList(LoadAppList loadAppList) {
        this.loadAppList = loadAppList;
    }

    public interface LoadAppList {
        void Onload();
    }

    public void setLoginNotifyListener(LoginNotifyListener loginNotifyListener) {
        this.loginNotifyListener = loginNotifyListener;
    }

    public interface LoginNotifyListener {
        void onDevNotify();

        void onUserNotify();
    }

    /**
     * * 火星坐标系 (GCJ-02) 与百度坐标系 (BD-09) 的转换算法 * * 将 BD-09 坐标转换成GCJ-02 坐标 * * @param
     * bd_lat * @param bd_lon * @return
     */
    private static double x_pi = 3.14159265358979324 * 3000.0 / 180.0;

    private static double[] bd09_To_Gcj02(double lat, double lon) {
        double x = lon - 0.0065, y = lat - 0.006;
        double z = Math.sqrt(x * x + y * y) - 0.00002 * Math.sin(y * x_pi);
        double theta = Math.atan2(y, x) - 0.000003 * Math.cos(x * x_pi);
        double tempLon = z * Math.cos(theta);
        double tempLat = z * Math.sin(theta);
        double[] gps = {tempLat, tempLon};
        return gps;
    }
}
