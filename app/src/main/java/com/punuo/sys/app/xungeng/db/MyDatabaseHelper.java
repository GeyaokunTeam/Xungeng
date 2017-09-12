package com.punuo.sys.app.xungeng.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Created by chenblue23 on 2016/5/31.
 */
public class MyDatabaseHelper extends SQLiteOpenHelper {
    private Context context;
    public static final String CREATE_MESSAGE = "create table message (id integer primary key autoincrement, "
            + "msg_id varchar(45), from_userid varchar(32), to_userid varchar(32), time integer, "
            + "is_time_show integer,content varchar(2000),type integer,audiolen float)";
    public static final String CREATE_FILE = "create table file (id integer primary key autoincrement, "
            + "file_id varchar(45), file_name varchar(45),file_from varchar(32), time integer, size integer, "
            + "local_path varchar(80),ftp_path varchar(80), md5 varchar(45), file_type varchar(10), type integer," +
            "is_file_transfer_finish integer,is_download integer)";
    public static final String CREATE_USERS = "create table user (id integer primary key autoincrement, "
            + "userid varchar(32), friend_count integer, change_time integer)";
    public static final String CREATE_RECENT_MESSAGE = "create table recentmessage (id integer primary key autoincrement,"
            + "type integer,fri_or_gro_id varchar(32),lastestmsg varchar(2000),lastesttime integer,groupmsgtype integer,newmsgcount integer)";
    public static final String CREATE_APP = "create table app (id integer primary key autoincrement, "
            + "app_id  varchar(45),app_name varchar(45), app_size integer,local_path varchar(80),is_download integer)";

    public MyDatabaseHelper(Context context, String name, SQLiteDatabase.CursorFactory factory, int version) {
        super(context, name, factory, version);
        this.context = context;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_MESSAGE);
        db.execSQL(CREATE_FILE);
        db.execSQL(CREATE_USERS);
        db.execSQL(CREATE_RECENT_MESSAGE);
        db.execSQL(CREATE_APP);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("drop table if exists message");
        db.execSQL("drop table if exists file");
        db.execSQL("drop table if exists recentmessage");
        db.execSQL(CREATE_MESSAGE);
        db.execSQL(CREATE_FILE);
        db.execSQL(CREATE_RECENT_MESSAGE);
    }
}
