package com.fate.findhookpoint;

import android.app.Application;
import android.content.pm.ApplicationInfo;
import android.graphics.drawable.Drawable;

public class Process {
    private String packageName;
    private String appName;
    private Drawable icon;
    private Integer pid;
    private ApplicationInfo applicationInfo;

    public ApplicationInfo getApplicationInfo() {
        return applicationInfo;
    }

    public void setApplicationInfo(ApplicationInfo applicationInfo) {
        this.applicationInfo = applicationInfo;
    }

    public Integer getPid() {
        return pid;
    }

    public void setPid(Integer pid) {
        this.pid = pid;
    }

    public String getPackageName() {
        return packageName;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    public String getAppName() {
        return appName;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }

    public Drawable getIcon() {
        return icon;
    }

    public void setIcon(Drawable icon) {
        this.icon = icon;
    }

    public Process(Process process){
        this.setIcon(process.getIcon());
        this.setAppName(process.getAppName());
        this.setPackageName(process.getPackageName());
        this.setPid(process.getPid());
        this.setApplicationInfo(process.getApplicationInfo());
    }

    public Process(){

    }
}
