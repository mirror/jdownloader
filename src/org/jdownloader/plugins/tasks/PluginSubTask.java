package org.jdownloader.plugins.tasks;

public interface PluginSubTask {
    public void open();

    public void close();

    public boolean isClosed();
}
