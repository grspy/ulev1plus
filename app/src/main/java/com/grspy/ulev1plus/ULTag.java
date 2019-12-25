package com.grspy.ulev1plus;

class ULTag {
    private String uid;
    private String pwd;
    private String pack;

    ULTag() {
    }

    ULTag(final String tagUid) {
        uid = tagUid;
    }

    String getUid() {
        return uid;
    }

    String getPwd() {
        return pwd;
    }

    String getPack() {
        return pack;
    }

    void setUid(String uid) {
        this.uid = uid;
    }

    void setPwd(String pwd) {
        this.pwd = pwd;
    }

    void setPack(String pack) {
        this.pack = pack;
    }

    boolean isEqualWith(ULTag ulTag) {
        return (this.getUid().equals(ulTag.getUid()) && this.getPwd().equals(ulTag.getPwd()) && this.getPack().equals(ulTag.getPack()));
    }
}