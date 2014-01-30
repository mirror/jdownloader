package org.jdownloader.plugins;

import jd.plugins.PluginProgress;

public class UserIOProgress extends PluginProgress {

    private String message;

    public UserIOProgress(String message) {
        super(-1, 100, null);
        this.message = message;
    }

    @Override
    public String getMessage(Object requestor) {
        return message;
    }

    @Override
    public PluginTaskID getID() {
        return PluginTaskID.USERIO;
    }

}
