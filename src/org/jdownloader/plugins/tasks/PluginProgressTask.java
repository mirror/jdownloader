package org.jdownloader.plugins.tasks;

import jd.plugins.PluginProgress;

public class PluginProgressTask extends AbstractPluginSubTask {

    private PluginProgress progress;
    private String         name;

    public PluginProgress getProgress() {
        return progress;
    }

    @Override
    public String toString() {
        return "Task: " + name + ":" + getRuntime();
    }

    public PluginProgressTask(PluginProgress newProgress) {
        this.progress = newProgress;
        name = progress.getMessage(this);
    }

}
