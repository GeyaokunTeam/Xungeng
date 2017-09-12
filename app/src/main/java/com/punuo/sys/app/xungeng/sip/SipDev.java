package com.punuo.sys.app.xungeng.sip;

import android.content.Context;
import android.content.Intent;
import android.os.PowerManager;
import android.util.Log;

import com.punuo.sys.app.xungeng.groupvoice.GroupInfo;
import com.punuo.sys.app.xungeng.groupvoice.GroupKeepAlive;
import com.punuo.sys.app.xungeng.groupvoice.GroupUdpThread;
import com.punuo.sys.app.xungeng.groupvoice.RtpAudio;
import com.punuo.sys.app.xungeng.service.PTTService;
import com.punuo.sys.app.xungeng.util.LogUtil;
import com.punuo.sys.app.xungeng.video.VideoInfo;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.zoolu.sip.address.SipURL;
import org.zoolu.sip.message.Message;
import org.zoolu.sip.provider.SipProvider;
import org.zoolu.sip.provider.Transport;
import org.zoolu.sip.provider.TransportConnId;

import java.io.IOException;
import java.io.StringReader;
import java.net.SocketException;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import static com.punuo.sys.app.xungeng.sip.SipInfo.serverIp;

/**
 * Created by acer on 2016/9/8.
 */
public class SipDev extends SipProvider {
    public static final String TAG = "SipDev";
    public static final String[] PROTOCOLS = {"udp"};
    private Context context;
    private ExecutorService pool = Executors.newFixedThreadPool(3);

    public SipDev(Context context,String viaAddr, int hostPort) {
        super(viaAddr, hostPort, PROTOCOLS, null);
        this.context=context;
    }

    public TransportConnId sendMessage(Message msg) {
        return sendMessage(msg, serverIp, SipInfo.SERVER_PORT_DEV);
    }

    public TransportConnId sendMessage(final Message msg, final String destAddr, final int destPort) {
        LogUtil.d(TAG, "<----------send sip message---------->");
        LogUtil.d(TAG, msg.toString());
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
        LogUtil.d(TAG, "<----------received sip message---------->");
        LogUtil.d(TAG, msg.toString());
        int port = msg.getRemotePort();
        if (port == SipInfo.SERVER_PORT_DEV) {
            Log.e(TAG, "onReceivedMessage: " + port);
            String body = msg.getBody();
            if (msg.isRequest()) {// 请求消息
                if (!requestParse(msg)) {
                    int requestType = bodyParse(body);
                    switch (requestType) {
                        case 3:
                            SipInfo.sipDev.sendMessage(SipMessageFactory.createResponse(msg, 200, "OK", ""));
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
                    SipInfo.dev_loginTimeout = false;
                } else if (code == 402) {
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
                String type = root.getTagName();
                switch (type) {
                    case "negotiate_response"://注册第一步响应
                        Element seedElement = (Element) root.getElementsByTagName("seed").item(0);
                        SipURL local = new SipURL(SipInfo.devId, serverIp, SipInfo.SERVER_PORT_DEV);
                        SipInfo.dev_from.setAddress(local);
                        LogUtil.d(TAG, "收到设备注册第一步响应");
                        String password = "123456";
                        Message register = SipMessageFactory.createRegisterRequest(
                                SipInfo.sipDev, SipInfo.dev_to, SipInfo.dev_from,
                                BodyFactory.createRegisterBody(/*随便输*/password));
                        SipInfo.sipDev.sendMessage(register);
                        return 0;
                    case "login_response"://注册成功响应，心跳回复
                        if (SipInfo.dev_logined) {
                            SipInfo.dev_heartbeatResponse = true;
                            LogUtil.d(TAG, "设备收到心跳回复");
                        } else {
                            PowerManager powerManager= (PowerManager) context.getSystemService(Context.POWER_SERVICE);
                            GroupInfo.wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, context.getClass().getCanonicalName());
                            GroupInfo.wakeLock.acquire();
                            /**开启心跳发送*/
//                            context.startService(new Intent(context, KeepAliveService.class));
                            SipInfo.dev_logined = true;
                            SipInfo.dev_loginTimeout = false;
                            LogUtil.d(TAG, "设备注册成功");
                            /*群组呼叫组别查询*/
                            SipInfo.sipDev.sendMessage(SipMessageFactory.createSubscribeRequest(SipInfo.sipDev,
                                    SipInfo.dev_to, SipInfo.dev_from, BodyFactory.createGroupSubscribeBody(SipInfo.devId)));
                        }
                        return 1;
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
        }
        return -1;
    }

    private boolean requestParse(Message msg) {
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
                switch (type) {
                    case "query":
                        if (SipInfo.instance!=null) {
                            SipInfo.instance.finish();
                            SipInfo.instance=null;
                        }
                        if (SipInfo.myCamera!=null) {
                            SipInfo.myCamera.finish();
                            SipInfo.myCamera=null;
                        }
                        if (SipInfo.movieRecord!=null){
                            SipInfo.movieRecord.onBackPressed();
                            SipInfo.movieRecord=null;
                        }
                        Message message = SipMessageFactory.createResponse(msg, 200, "OK",
                                BodyFactory.createOptionsBody());
                        SipInfo.sipDev.sendMessage(message);
                        return true;
                    case "media":
                        Element peerElement = (Element) root.getElementsByTagName("peer").item(0);
                        Element magicElement = (Element) root.getElementsByTagName("magic").item(0);
                        String peer = peerElement.getFirstChild().getNodeValue();
                        String magic = magicElement.getFirstChild().getNodeValue();
                        VideoInfo.media_info_ip = peer.substring(0, peer.indexOf("UDP")).trim();
                        VideoInfo.media_info_port = Integer.parseInt(peer.substring(peer.indexOf("UDP") + 3).trim());
                        VideoInfo.media_info_magic = new byte[magic.length() / 2 + magic.length() % 2];
                        for (int i = 0; i < VideoInfo.media_info_magic.length; i++) {
                            try {
                                VideoInfo.media_info_magic[i] = (byte) (0xff & Integer.parseInt(magic.substring(i * 2, i * 2 + 2), 16));
                            } catch (NumberFormatException e) {
                                e.printStackTrace();
                            }
                        }
                        SipInfo.msg = msg;
                        SipInfo.notifymedia.sendEmptyMessage(0x1111);
                        return true;
                    case "recvaddr":
                        VideoInfo.endView = true;
                        SipInfo.sipDev.sendMessage(SipMessageFactory.createResponse(msg, 200, "Ok", ""));
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
                switch (type) {
                    case "subscribe_grouppn_response":
                        Element codeElement = (Element) root.getElementsByTagName("code").item(0);
                        String code = codeElement.getFirstChild().getNodeValue();
                        if (code.equals("200")) {
                            Element groupNumElement = (Element) root.getElementsByTagName("group_num").item(0);
                            Element peerElement = (Element) root.getElementsByTagName("peer").item(0);
                            Element levelElement = (Element) root.getElementsByTagName("level").item(0);
                            Element phoneElement=(Element) root.getElementsByTagName("phone").item(0);
                            Element nameElement=(Element) root.getElementsByTagName("name").item(0);
                            GroupInfo.groupNum = groupNumElement.getFirstChild().getNodeValue();
                            String peer = peerElement.getFirstChild().getNodeValue();
                            GroupInfo.ip = peer.substring(0, peer.indexOf("UDP")).trim();
                            GroupInfo.port = Integer.parseInt(peer.substring(peer.indexOf("UDP") + 3).trim());
                            GroupInfo.level = levelElement.getFirstChild().getNodeValue();
                            String name = nameElement.getFirstChild().getNodeValue();
                            SipInfo.devName=name;
//                            SipInfo.phoneNum=phoneElement.getFirstChild().getNodeValue();
                            Thread groupVoice = new Thread(new Runnable() {
                                @Override
                                public void run() {
                                    try {
                                        GroupInfo.rtpAudio = new RtpAudio(serverIp, GroupInfo.port);
                                        GroupInfo.groupUdpThread = new GroupUdpThread(serverIp, GroupInfo.port);
                                        GroupInfo.groupUdpThread.startThread();
                                        GroupInfo.groupKeepAlive = new GroupKeepAlive();
                                        GroupInfo.groupKeepAlive.startThread();
                                        Intent PTTIntent = new Intent(context, PTTService.class);
                                        context.startService(PTTIntent);
                                    } catch (SocketException e) {
                                        e.printStackTrace();
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }
                                }
                            }, "groupVoice");
                            groupVoice.start();
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
}
