package org.jdownloader.plugins.tasks;

public abstract class AbstractPluginSubTask implements PluginSubTask {

    private long startTime;

    public long getStartTime() {
        return startTime;
    }

    @Override
    public boolean isClosed() {
        return endTime > 0 && endTime > startTime;
    }

    public void reopen() {
        endTime = -1;
    }

    public long getRuntime() {
        return endTime - startTime;

    }

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
