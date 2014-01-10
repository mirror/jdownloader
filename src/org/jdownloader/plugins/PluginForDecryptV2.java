package org.jdownloader.plugins;

import jd.PluginWrapper;
import jd.plugins.PluginForDecrypt;

import org.appwork.utils.logging2.LogSource;

public abstract class PluginForDecryptV2 extends PluginForDecrypt {
    public PluginForDecryptV2(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public LogSource getLogger() {
        return (LogSource) super.getLogger();
    }
}
