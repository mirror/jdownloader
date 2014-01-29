package org.jdownloader.plugins.tasks;

import jd.plugins.PluginProgress;

import org.jdownloader.plugins.PluginTaskID;

public class PluginProgressTask extends AbstractPluginSubTask {

    private PluginProgress progress;

    public PluginProgress getProgress() {
        return progress;
    }

    @Override
    public String toString() {
        return "Task: " + getId() + ":" + getRuntime();
    }

    public PluginProgressTask(PluginProgress newProgress) {
        this.progress = newProgress;
        setId(progress == null ? PluginTaskID.PLUGIN : progress.getID());
    }

}
