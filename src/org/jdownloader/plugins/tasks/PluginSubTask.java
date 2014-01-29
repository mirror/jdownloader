package org.jdownloader.plugins.tasks;

import org.jdownloader.plugins.PluginTaskID;

public interface PluginSubTask {
    public void open();

    public void close();

    public PluginTaskID getId();

    public boolean isClosed();

    public abstract long getEndTime();

    public abstract long getRuntime();

    public abstract long getStartTime();
}
