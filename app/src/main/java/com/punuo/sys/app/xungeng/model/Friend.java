package com.punuo.sys.app.xungeng.model;

import java.io.Serializable;

/**
 * Created by chenblue23 on 2016/6/6.
 */
public class Friend implements Comparable ,Serializable {
    private static final long serialVersionUID = 1L;
    private String userId;
    private String username;
    private String phoneNum;
    private String telNum;
    private String realName;
    private String unit;
    private boolean isLive;
    private int newMsgCount = 0;
    private boolean isSelect = false;

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public String getUserId() {
        return userId;
    }

    public String getUsername() {
        return username;
    }

    public String getPhoneNum() {
        return phoneNum;
    }

    public String getTelNum() {
        return telNum;
    }

    public boolean isLive() {
        return isLive;
    }

    public int getNewMsgCount() {
        return newMsgCount;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setPhoneNum(String phoneNum) {
        this.phoneNum = phoneNum;
    }

    public void setTelNum(String telNum) {
        this.telNum = telNum;
    }

    public void setLive(boolean live) {
        isLive = live;
    }

    public void setNewMsgCount(int newMsgCount) {
        this.newMsgCount = newMsgCount;
    }

    public void addMsgCount(int newMsgCount) {
        this.newMsgCount += newMsgCount;
    }

    public void setRealName(String realName) {
        this.realName = realName;
    }

    public String getRealName() {
        return realName;
    }

    public boolean isSelect() {
        return isSelect;
    }

    public void setSelect(boolean select) {
        isSelect = select;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o != null && o.getClass() == Friend.class) {
            Friend friend = (Friend) o;
            if (this.getUserId().equals(friend.getUserId())) {
                return true;
            }
        }
        return false;
    }

    @Override
    public int compareTo(Object another) throws ClassCastException, NullPointerException {
        if (another != null) {
            Friend dev = (Friend) another;
            if (this.isLive && !dev.isLive()) {
                return -1;
            } else if (!this.isLive && dev.isLive()) {
                return 1;
            } else if (this.isLive == dev.isLive()) {
                return this.username.compareTo(dev.getUsername());
            }
        } else {
            throw new NullPointerException("比较对象为空");
        }
        return 0;
    }
}
