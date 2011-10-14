package org.jdownloader.plugins.controller.host;

import jd.plugins.PluginForHost;

public class LazyHostPlugin {

    private AbstractHostPlugin   info;
    private Class<PluginForHost> plgClass;
    private PluginForHost        prototypeInstance;

    public LazyHostPlugin(AbstractHostPlugin ap, Class<PluginForHost> plgClass, PluginForHost prototype) {
        this.info = ap;
        this.plgClass = plgClass;
        this.prototypeInstance = prototype;
    }

    public LazyHostPlugin(AbstractHostPlugin ap) {
        info = ap;
    }

}
