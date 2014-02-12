package org.jdownloader.statistics;

import java.util.ArrayList;

import org.appwork.storage.Storable;

public class TimeWrapper implements Storable {
    private ArrayList<LogEntryWrapper> list;
    private long                       time;

    public ArrayList<LogEntryWrapper> getList() {
        return list;
    }

    public void setList(ArrayList<LogEntryWrapper> list) {
        this.list = list;
    }

    public long getTime() {
        return time;
    }

    public void setTime(long time) {
        this.time = time;
    }

    public TimeWrapper(/* Storable */) {
    }

    public TimeWrapper(ArrayList<LogEntryWrapper> sendTo) {
        this.list = sendTo;
        this.time = System.currentTimeMillis();
    }

}
