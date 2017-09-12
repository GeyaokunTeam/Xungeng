package com.punuo.sys.app.xungeng.db;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.punuo.sys.app.xungeng.model.App;
import com.punuo.sys.app.xungeng.model.LastestMsg;
import com.punuo.sys.app.xungeng.model.Msg;
import com.punuo.sys.app.xungeng.model.MyFile;

import java.util.ArrayList;
import java.util.List;


/**
 * Created by chenblue23 on 2016/5/31.
 */
public class SQLiteManager {
    private MyDatabaseHelper dbHelper;
    private SQLiteDatabase db;

    public SQLiteManager(MyDatabaseHelper dbHelper) {
        this.dbHelper = dbHelper;
    }

    /*消息记录*/
    public boolean insertMessage(String msgId, String fromUserId, String toUserId, int time,
                                 int isTimeShow, String content, int type,float audiolen) {
        db = dbHelper.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put("msg_id", msgId);
        contentValues.put("from_userid", fromUserId);
        contentValues.put("to_userid", toUserId);
        contentValues.put("time", time);
        contentValues.put("is_time_show", isTimeShow);
        contentValues.put("content", content);
        contentValues.put("type", type);
        contentValues.put("audiolen", audiolen);
        long i = db.insert("message", null, contentValues);
        return i != -1;
    }

    public List<Msg> queryMessage(String userId1, String userId2, String time, int max) {
        db = dbHelper.getReadableDatabase();
        List<Msg> msgs = new ArrayList<Msg>();
        int i = 0;
        Cursor cursor = db.query("message", new String[]{"msg_id", "from_userid", "to_userid", "time",
                        "is_time_show", "content", "type","audiolen"}, "time < ? and ((from_userid = ? and to_userid = ?) or (from_userid = ? and to_userid = ?))",
                new String[]{time, userId1, userId2, userId2, userId1}, null, null, "time desc");
        if (cursor.moveToFirst()) {
            do {
                Msg msg = new Msg();
                msg.setMsgId(cursor.getString(cursor.getColumnIndex("msg_id")));
                msg.setFromUserId(cursor.getString(cursor.getColumnIndex("from_userid")));
                msg.setToUserId(cursor.getString(cursor.getColumnIndex("to_userid")));
                msg.setTime(cursor.getInt(cursor.getColumnIndex("time")));
                msg.setIsTimeShow(cursor.getInt(cursor.getColumnIndex("is_time_show")));
                msg.setContent(cursor.getString(cursor.getColumnIndex("content")));
                msg.setType(cursor.getInt(cursor.getColumnIndex("type")));
                msg.setRecordtime(cursor.getFloat(cursor.getColumnIndex("audiolen")));
                msgs.add(0, msg);
                i++;
                if (i == max) {
                    break;
                }
            } while (cursor.moveToNext());
        }
        cursor.close();
        return msgs;
    }

    public MyFile queryFile(String fileId) {
        db = dbHelper.getReadableDatabase();
        MyFile myFile = new MyFile();
        Cursor cursor = db.query("file", new String[]{"file_id", "file_name", "file_from", "file_type", "time", "size", "local_path", "ftp_path"
                        , "is_file_transfer_finish", "is_download", "type"}, "file_id = ? ",
                new String[]{fileId}, null, null, null);
        if (cursor.moveToFirst()) {
            do {
                myFile.setFileId(cursor.getString(cursor.getColumnIndex("file_id")));
                myFile.setFileName(cursor.getString(cursor.getColumnIndex("file_name")));
                myFile.setFrom(cursor.getString(cursor.getColumnIndex("file_from")));
                myFile.setFileType(cursor.getString(cursor.getColumnIndex("file_type")));
                myFile.setSize(cursor.getLong(cursor.getColumnIndex("size")));
                myFile.setLocalPath(cursor.getString(cursor.getColumnIndex("local_path")));
                myFile.setFtpPath(cursor.getString(cursor.getColumnIndex("ftp_path")));
                myFile.setIsTransferFinish(cursor.getInt(cursor.getColumnIndex("is_file_transfer_finish")));
                myFile.setIsDownloadFinish(cursor.getInt(cursor.getColumnIndex("is_download")));
                myFile.setTime(cursor.getInt(cursor.getColumnIndex("time")));
                myFile.setType(cursor.getInt(cursor.getColumnIndex("type")));
            } while (cursor.moveToNext());
        }
        cursor.close();
        return myFile;
    }

    public int queryLastTime(String userId1, String userId2) {
        db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery("select * from message where (from_userid = ? and to_userid = ?) or (from_userid = ? and to_userid = ?) order by time desc",
                new String[]{userId1, userId2, userId2, userId1});
        if (cursor.moveToFirst()) {
            int time = cursor.getInt(cursor.getColumnIndex("time"));
            cursor.close();
            return time;
        } else {
            cursor.close();
            return -1;
        }
    }

    public boolean insertAppInfo(String appId, String appname, long size, String localPath, int isDownload) {
        db = dbHelper.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put("app_id", appId);
        contentValues.put("app_name", appname);
        contentValues.put("app_size", size);
        contentValues.put("Local_path", localPath);
        contentValues.put("is_download", isDownload);
        long i=-1;
        if (!isAppExist(appId)) {
             i = db.insert("app", null, contentValues);
        }
        return i != -1;
    }

    public App queryApp(String appId) {
        db = dbHelper.getReadableDatabase();
        App app = new App();
        Cursor cursor = db.query("app", new String[]{"app_id", "app_name", "app_size", "local_path", "is_download"}, "app_id = ? ",
                new String[]{appId}, null, null, null);
        if (cursor.moveToFirst()) {
            do {
                app.setAppid(cursor.getString(cursor.getColumnIndex("app_id")));
                app.setAppname(cursor.getString(cursor.getColumnIndex("app_name")));
                app.setSize(cursor.getLong(cursor.getColumnIndex("app_size")));
                app.setLocalPath(cursor.getString(cursor.getColumnIndex("local_path")));
                app.setIsDownload(cursor.getInt(cursor.getColumnIndex("is_download")));
            } while (cursor.moveToNext());
        }
        cursor.close();
        return app;
    }

    public void updateMessageTime(String msgId, int time) {
        db = dbHelper.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put("time", time);
        db.update("message", contentValues, "msg_id = ?", new String[]{msgId});
    }

    public void updateFileTime(String fileId, int time) {
        db = dbHelper.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put("time", time);
        db.update("file", contentValues, "file_id = ?", new String[]{fileId});
    }

    public void updateIsFileTransferFinish(String fileId, int status) {
        db = dbHelper.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put("is_file_transfer_finish", status);
        db.update("file", contentValues, "file_id = ?", new String[]{fileId});
    }

    public void updateFileDownload(String fileId, int state) {
        db = dbHelper.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put("is_download", state);
        db.update("file", contentValues, "file_id=?", new String[]{fileId});
    }

    public void updateLocalPath(String fileId, String localPath) {
        db = dbHelper.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put("local_path", localPath);
        db.update("file", contentValues, "file_id=?", new String[]{fileId});
    }

    public void updateAppDownload(String appId, int state) {
        db = dbHelper.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put("is_download", state);
        db.update("app", contentValues, "app_id=?", new String[]{appId});
    }

    public void updateAppLocalPath(String appId, String localPath) {
        db = dbHelper.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put("local_path", localPath);
        db.update("app", contentValues, "app_id=?", new String[]{appId});
    }

    public void deleteMessageById(String msgId) {
        db = dbHelper.getWritableDatabase();
        db.delete("message", "msg_id = ?", new String[]{msgId});
    }

    /*文件列表*/
    public boolean insertFile(String fileId, String fileName, String from, String fileType, int time, String localpath, String ftppath,
                              long size, String md5, int type, int istransferfinish, int isdownloadfinish) {
        db = dbHelper.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put("file_id", fileId);
        contentValues.put("file_name", fileName);
        contentValues.put("file_from", from);
        contentValues.put("file_type", fileType);
        contentValues.put("time", time);
        contentValues.put("local_path", localpath);
        contentValues.put("ftp_path", ftppath);
        contentValues.put("size", size);
        contentValues.put("md5", md5);
        contentValues.put("type", type);
        contentValues.put("is_file_transfer_finish", istransferfinish);
        contentValues.put("is_download", isdownloadfinish);
        long i = db.insert("file", null, contentValues);
        return i != -1;
    }

    /* 用户信息*/
    public boolean insertUser(String userId, int friendCount, int changeTime) {
        if (isUserExist(userId)) {
            deleteUserById(userId);
        }
        db = dbHelper.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put("userid", userId);
        contentValues.put("friend_count", friendCount);
        contentValues.put("change_time", changeTime);
        long i = db.insert("user", null, contentValues);
        return i != -1;
    }

    public boolean isUserExist(String userId) {
        db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery("select * from user where userid = ?",
                new String[]{userId});
        if (cursor.moveToFirst()) {
            cursor.close();
            return true;
        } else {
            cursor.close();
            return false;
        }
    }

    public boolean isFileExist(String fileId) {
        db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery("select * from file where file_id = ?",
                new String[]{fileId});
        if (cursor.moveToFirst()) {
            cursor.close();
            return true;
        } else {
            cursor.close();
            return false;
        }
    }
    public boolean isAppExist(String appId) {
        db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery("select * from app where app_id = ?",
                new String[]{appId});
        if (cursor.moveToFirst()) {
            cursor.close();
            return true;
        } else {
            cursor.close();
            return false;
        }
    }
    public void deleteUserById(String userId) {
        db = dbHelper.getWritableDatabase();
        db.delete("user", "userid = ?", new String[]{userId});
    }

    public int queryFriendCount(String userId) {
        db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query("user", new String[]{"friend_count"}, "userid = ?",
                new String[]{userId}, null, null, null);
        if (cursor.moveToFirst()) {
            int count = cursor.getInt(cursor.getColumnIndex("friend_count"));
            cursor.close();
            return count;
        } else {
            cursor.close();
            return 0;
        }
    }

    public int queryChangeTime(String userId) {
        db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query("user", new String[]{"change_time"}, "userid = ?",
                new String[]{userId}, null, null, null);
        if (cursor.moveToFirst()) {
            int time = cursor.getInt(cursor.getColumnIndex("change_time"));
            cursor.close();
            return time;
        } else {
            cursor.close();
            return 0;
        }
    }

    public boolean insertLastestMsg(int type, String groupid, int groupmsgtype, int lastesttime) {
        db = dbHelper.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put("type", type);
        contentValues.put("fri_or_gro_id", groupid);
        contentValues.put("groupmsgtype", groupmsgtype);
        contentValues.put("lastesttime", lastesttime);
        long i = -1;
        if (!islastestMsgExist(type, groupid)) {
            i = db.insert("recentmessage", null, contentValues);
        } else {
            updateLastestMsg(groupid, groupmsgtype, lastesttime);
        }
        return i != -1;
    }

    /**
     * 储存接收到的消息到最近消息表中
     */
    public boolean insertLastestMsgFrom(int type, String friendid, String lastestmsg, int lastesttime) {
        db = dbHelper.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put("type", type);
        contentValues.put("fri_or_gro_id", friendid);
        contentValues.put("lastestmsg", lastestmsg);
        contentValues.put("lastesttime", lastesttime);
        long i = -1;
        if (!islastestMsgExist(type, friendid)) {
            contentValues.put("newmsgcount", 1);
            i = db.insert("recentmessage", null, contentValues);
        } else {
            int count = queryLastestMsgCount(type, friendid);
            count += 1;
            updateLastestMsg(friendid, lastestmsg, lastesttime, count);
            i = 0;
        }
        return i != -1;
    }

    /**
     * 储存发送的消息到最近消息表中
     */
    public boolean insertLastestMsgTo(int type, String friendid, String lastestmsg, int lastesttime) {
        db = dbHelper.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put("type", type);
        contentValues.put("fri_or_gro_id", friendid);
        contentValues.put("lastestmsg", lastestmsg);
        contentValues.put("lastesttime", lastesttime);
        contentValues.put("newmsgcount", 0);
        long i = -1;
        if (!islastestMsgExist(type, friendid)) {
            i = db.insert("recentmessage", null, contentValues);
        } else {
            updateLastestMsg(friendid, lastestmsg, lastesttime, 0);
            i = 0;
        }
        return i != -1;
    }

    public boolean islastestMsgExist(int type, String id) {
        db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery("select * from recentmessage where (type = ? and fri_or_gro_id = ?)",
                new String[]{String.valueOf(type), id});
        if (cursor.moveToFirst()) {
            cursor.close();
            return true;
        } else {
            cursor.close();
            return false;
        }
    }

    public List<LastestMsg> queryLastestMsg() {
        db = dbHelper.getReadableDatabase();
        List<LastestMsg> lastestMsgs = new ArrayList<>();
        Cursor cursor = db.query("recentmessage", new String[]{"type", "fri_or_gro_id", "lastestmsg", "lastesttime", "groupmsgtype", "newmsgcount"}, null,
                null, null, null, "lastesttime desc");
        if (cursor.moveToFirst()) {
            do {
                LastestMsg lastestMsg = new LastestMsg();
                lastestMsg.setType(cursor.getInt(cursor.getColumnIndex("type")));
                lastestMsg.setId(cursor.getString(cursor.getColumnIndex("fri_or_gro_id")));
                lastestMsg.setGroupmsgtype(cursor.getInt(cursor.getColumnIndex("groupmsgtype")));
                lastestMsg.setLastestmsg(cursor.getString(cursor.getColumnIndex("lastestmsg")));
                lastestMsg.setLastesttime(cursor.getInt(cursor.getColumnIndex("lastesttime")));
                lastestMsg.setNewMsgCount(cursor.getInt(cursor.getColumnIndex("newmsgcount")));
                lastestMsgs.add(lastestMsg);
            } while (cursor.moveToNext());
        }
        cursor.close();
        return lastestMsgs;
    }

    public int queryLastestMsgCount(int type, String id) {
        db = dbHelper.getReadableDatabase();
        int lastestMsgCount = 0;
        Cursor cursor = db.rawQuery("select newmsgcount from recentmessage where (type = ? and fri_or_gro_id = ?)",
                new String[]{String.valueOf(type), id});
        if (cursor.moveToFirst()) {
            do {
                lastestMsgCount = cursor.getInt(cursor.getColumnIndex("newmsgcount"));
            } while (cursor.moveToNext());
        }
        cursor.close();
        return lastestMsgCount;
    }

    public void updateLastestMsg(String friendid, String lastestmsg, int lastesttime, int newmsgcount) {
        db = dbHelper.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put("lastestmsg", lastestmsg);
        contentValues.put("lastesttime", lastesttime);
        contentValues.put("newmsgcount", newmsgcount);
        db.update("recentmessage", contentValues, "fri_or_gro_id = ?", new String[]{friendid});
    }

    public void updateLastestMsg(String friendid) {
        db = dbHelper.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put("newmsgcount", 0);
        db.update("recentmessage", contentValues, "fri_or_gro_id = ?", new String[]{friendid});
    }

    public void updateLastestMsg(String groupid, int groupmsgtype, int lastesttime) {
        db = dbHelper.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put("groupmsgtype", groupmsgtype);
        contentValues.put("lastesttime", lastesttime);
        db.update("recentmessage", contentValues, "fri_or_gro_id = ?", new String[]{groupid});
    }

    public void updateLastestMsg(String groupid, int groupmsgtype) {
        db = dbHelper.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put("groupmsgtype", groupmsgtype);
        db.update("recentmessage", contentValues, "fri_or_gro_id = ?", new String[]{groupid});
    }
}
