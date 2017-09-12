package com.punuo.sys.app.xungeng.sip;

/**
 * Created by chenblue23 on 2016/4/20.
 */
public class BodyFactory {
    public static String createRegisterBody(String password) {
        StringBuilder body = new StringBuilder(
                "<?xml version=\"1.0\"?>\r\n<login_request>\r\n<password>");
        body.append(password);
        body.append("</password>\r\n</login_request>\r\n");
        return body.toString();
    }

    public static String createHeartbeatBody() {
        String body = new String(
                "<?xml version=\"1.0\"?>\r\n<heartbeat_request></heartbeat_request>\r\n");
        return body;
    }

    public static String createLogoutBody() {
        StringBuilder body = new StringBuilder();
        body.append("<?xml version=\"1.0\"?>\r\n" +
                "<logout></logout>\r\n");
        return body.toString();
    }

    public static String createDevsQueryBody() {
        String body = new String("<?xml version=\"1.0\"?>\r\n<devs_query></devs_query>\r\n");
        return body;
    }

    public static String createAppsQueryBody() {
        String body = new String("<?xml version=\"1.0\"?>\r\n<apps_query></apps_query>\r\n");
        return body;
    }

    public static String createQueryBody(String devType) {
        StringBuilder body = new StringBuilder();
        body.append("<?xml version=\"1.0\"?>\r\n<query>\r\n<variable>MediaInfo_Video</variable>\r\n<dev_type>");
        body.append(devType);
        body.append("</dev_type>\r\n</query>\r\n");
        return body.toString();
    }

    public static String createMediaBody() {
        StringBuilder body = new StringBuilder();
        body.append("<?xml version=\"1.0\"?>\r\n");
        body.append("<media>\r\n<resolution>CIF</resolution>\r\n");
        body.append("<video>H.264</video>\r\n");
        body.append("<audio>G.722</audio>\r\n");
        body.append("<kbps>800</kbps>\r\n");
        body.append("<self>192.168.1.129 UDP 5200</self>\r\n");
        body.append("<mode>active</mode>\r\n");
        body.append("<magic>01234567890123456789012345678901</magic>\r\n");
        body.append("<dev_type>2</dev_type>\r\n</media>\r\n");
        return body.toString();
    }

    public static String cretePasswordChange(String password_old, String password_new) {
        StringBuilder body = new StringBuilder();
        body.append("<?xml version=\"1.0\"?>\r\n<password_change>\r\n<password_old>");
        body.append(password_old);
        body.append("</password_old>\r\n<password_new>");
        body.append(password_new);
        body.append("</password_new>\r\n</password_change>\r\n");
        return body.toString();
    }

    public static String createOptionsBody() {
        StringBuilder body = new StringBuilder();
        body.append("<?xml version=\"1.0\"?>\r\n");
        body.append("<query_response>\r\n<variable>MediaInfo_Video</variable>\r\n");
        body.append("<result>0</result>\r\n");
        body.append("<video>H.264</video>\r\n");
        body.append("<resolution>CIF_MOBILE_SOFT</resolution>\r\n");
        body.append("<framerate>25</framerate>\r\n");
        body.append("<bitrate>256</bitrate>\r\n");
        body.append("<bright>51</bright>\r\n");
        body.append("<contrast>49</contrast>\r\n");
        body.append("<saturation>50</saturation>\r\n</query_response>\r\n");
        return body.toString();
    }

    public static String createMediaResponseBody(String resolution) {
        StringBuilder body = new StringBuilder();
        body.append("<?xml version=\"1.0\"?>\r\n");
        body.append("<media>\r\n<resolution>");
        body.append(resolution);
        body.append("</resolution>\r\n");
        body.append("<video>H.264</video>\r\n");
        body.append("<audio>G.722</audio>\r\n");
        body.append("<kbps>800</kbps>\r\n");
        body.append("<self>192.168.1.129 UDP 5000</self>\r\n");
        body.append("<mode>active</mode>\r\n");
        body.append("<magic>01234567890123456789012345678901</magic>\r\n");
        body.append("<dev_type>2</dev_type>\r\n</media>\r\n");
        return body.toString();
    }

    /* 好友查询*/
    public static String createFriendsQueryBody(int num, int time) {
        StringBuilder body = new StringBuilder();
        body.append("<?xml version=\"1.0\"?>\r\n");
        body.append("<friends_query>\r\n<num>");
        body.append(num);
        body.append("</num>\r\n<time>");
        body.append(time);
        body.append("</time>\r\n</friends_query>\r\n");
        return body.toString();
    }

    public static String createFriendsResBody(int code) {
        StringBuilder body = new StringBuilder();
        body.append("<?xml version=\"1.0\"?>\r\n");
        body.append("<friends>\r\n<code>");
        body.append(code);
        body.append("</code>\r\n</friends>\r\n");
        return body.toString();
    }

    /*文字聊天*/
    public static String createMessageBody(String id, String from, String to, String content, String time, int type) {
        StringBuilder body = new StringBuilder();
        body.append("<?xml version=\"1.0\"?>\r\n");
        body.append("<message>\r\n<id>");
        body.append(id);
        body.append("</id>\r\n<from>");
        body.append(from);
        body.append("</from>\r\n<to>");
        body.append(to);
        body.append("</to>\r\n<content>");
        body.append(content);
        body.append("</content>\r\n<time>");
        body.append(time);
        body.append("</time>\r\n<type>");
        body.append(type);
        body.append("</type>\r\n</message>\r\n");
        return body.toString();
    }

    public static String createMessageResBody(String id, int code) {
        StringBuilder body = new StringBuilder();
        body.append("<?xml version=\"1.0\"?>\r\n");
        body.append("<message>\r\n<id>");
        body.append(id);
        body.append("</id>\r\n<code>");
        body.append(code);
        body.append("</code>\r\n</message>\r\n");
        return body.toString();
    }

    public static String createGetHistoryBody() {
        return new String("<?xml version=\"1.0\"?>\r\n<offline_query></offline_query>\r\n");
    }

    public static String createGroupSubscribeBody(String dev_id) {
        StringBuilder body = new StringBuilder();
        body.append("<?xml version=\"1.0\"?>\r\n<subscribe_grouppn>\r\n<dev_id>\r\n");
        body.append(dev_id);
        body.append("</dev_id>\r\n</subscribe_grouppn>\r\n");
        return body.toString();
    }

    public static String createFileTransferBody(String from, String to, String id, String name,
                                                String fileType, String time, String path, long size,
                                                String md5, int type) {
        StringBuilder body = new StringBuilder();
        body.append("<?xml version=\"1.0\"?>\r\n");
        body.append("<filetransfer>\r\n<from>");
        body.append(from);
        body.append("</from>\r\n<to>");
        body.append(to);
        body.append("</to>\r\n<id>");
        body.append(id);
        body.append("</id>\r\n<name>");
        body.append(name);
        body.append("</name>\r\n<filetype>");
        body.append(fileType);
        body.append("</filetype>\r\n<time>");
        body.append(time);
        body.append("</time>\r\n<path>");
        body.append(path);
        body.append("</path>\r\n<size>");
        body.append(size);
        body.append("</size>\r\n<md5>");
        body.append(md5);
        body.append("</md5>\r\n<type>");
        body.append(type);
        body.append("</type>\r\n</filetransfer>\r\n");
        return body.toString();
    }

    public static String createFileTransferResBody(String id, int code) {
        StringBuilder body = new StringBuilder();
        body.append("<?xml version=\"1.0\"?>\r\n");
        body.append("<filetransfer>\r\n<id>");
        body.append(id);
        body.append("</id>\r\n<code>");
        body.append(code);
        body.append("</code>\r\n</filetransfer>\r\n");
        return body.toString();
    }

    public static String creatGPSInfoBody(String userId, int addr_code, double lang, double lat, long gpsutc, int pointid,int direction) {
        StringBuilder body = new StringBuilder();
        body.append("<?xml version=\"1.0\"?>\r\n");
        body.append("<gps_info>\r\n<userid>");
        body.append(userId);
        body.append("</userid>\r\n<addr_code>");
        body.append(addr_code);
        body.append("</addr_code>\r\n<lang>");
        body.append(lang);
        body.append("</lang>\r\n<lat>");
        body.append(lat);
        body.append("</lat>\r\n<gpsutc>");
        body.append(gpsutc);
        body.append("</gpsutc>\r\n<pointid>");
        body.append(pointid);
        body.append("</pointid>\r\n<direction>");
        body.append(direction);
        body.append("</direction>\r\n</gps_info>\r\n");
        return body.toString();
    }

    public static String createPointsQuery(int addr_code) {
        StringBuilder body = new StringBuilder();
        body.append("<?xml version=\"1.0\"?>\r\n");
        body.append("<points_query>\r\n<addr_code>");
        body.append(addr_code);
        body.append("</addr_code>\r\n</points_query>\r\n");
        return body.toString();
    }
}
