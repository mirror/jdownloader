package org.jdownloader.plugins;

import jd.PluginWrapper;
import jd.plugins.PluginForHost;

import org.appwork.utils.logging2.LogSource;

public abstract class PluginForHostV2 extends PluginForHost {
    @Override
    public LogSource getLogger() {
        return (LogSource) super.getLogger();
    }

    public PluginForHostV2(PluginWrapper wrapper) {
        super(wrapper);
    }

}
