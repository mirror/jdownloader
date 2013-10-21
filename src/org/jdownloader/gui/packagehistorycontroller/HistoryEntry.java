package org.jdownloader.gui.packagehistorycontroller;

public interface HistoryEntry extends Comparable<HistoryEntry> {
    public String getName();

    public long getTime();

    public void setTime(long time);
}
