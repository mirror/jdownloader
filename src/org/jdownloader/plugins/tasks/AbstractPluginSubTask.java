package org.jdownloader.plugins.tasks;

import org.jdownloader.plugins.PluginTaskID;

public abstract class AbstractPluginSubTask implements PluginSubTask {

    private long startTime;

    @Override
    public long getStartTime() {
        return startTime;
    }

    private PluginTaskID id;

    public PluginTaskID getId() {
        return id;
    }

    protected void setId(PluginTaskID id) {
        this.id = id;
    }

    @Override
    public boolean isClosed() {
        return endTime > 0 && endTime > startTime;
    }

    public void reopen() {
        endTime = -1;
    }

    @Override
    public long getRuntime() {
        return endTime - startTime;

    }

    @Override
    public long getEndTime() {
        return endTime;
    }

    private long endTime;

    @Override
    public String toString() {
        return "Task: " + getRuntime();
    }

    @Override
    public void open() {
        this.startTime = System.currentTimeMillis();
    }

    @Override
    public void close() {
        if (isClosed()) return;
        this.endTime = System.currentTimeMillis();
    }

}
