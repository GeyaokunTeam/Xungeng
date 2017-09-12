package com.punuo.sys.app.xungeng.model;

/**
 * Created by acer on 2016/10/24.
 */

public class App {
    String appid;
    String appname;
    long size;
    String url;
    String iconUrl;
    String desc;
    int progress;
    String localPath;
    int isDownload;
    String name;
    String iconname;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getIconname() {
        return iconname;
    }

    public void setIconname(String iconname) {
        this.iconname = iconname;
    }

    public String getLocalPath() {
        return localPath;
    }

    public void setLocalPath(String localPath) {
        this.localPath = localPath;
    }

    public int getIsDownload() {
        return isDownload;
    }

    public void setIsDownload(int isDownload) {
        this.isDownload = isDownload;
    }

    public int getProgress() {
        return progress;
    }

    public void setProgress(int progress) {
        this.progress = progress;
    }

    public String getAppid() {
        return appid;
    }

    public void setAppid(String appid) {
        this.appid = appid;
    }

    public String getAppname() {
        return appname;
    }

    public void setAppname(String appname) {
        this.appname = appname;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getIconUrl() {
        return iconUrl;
    }

    public void setIconUrl(String iconUrl) {
        this.iconUrl = iconUrl;
    }

    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o != null && o.getClass() == App.class) {
            App app = (App) o;
            if (this.getAppid().equals(app.getAppid())) {
                return true;
            }
        }
        return false;
    }
}
