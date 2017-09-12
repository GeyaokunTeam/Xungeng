package com.punuo.sys.app.xungeng.model;

/**
 * Created by acer on 2016/11/30.
 */

public class PointInfo {
    int id;//点的id
    String name;//点的名字
    double lang;//经度
    double lat;//纬度

    boolean isCheck;

    public boolean isCheck() {
        return isCheck;
    }

    public void setCheck(boolean check) {
        isCheck = check;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public double getLang() {
        return lang;
    }

    public void setLang(double lang) {
        this.lang = lang;
    }

    public double getLat() {
        return lat;
    }

    public void setLat(double lat) {
        this.lat = lat;
    }

    @Override
    public boolean equals(Object o) {
        return id == ((PointInfo) o).getId();
    }
}
