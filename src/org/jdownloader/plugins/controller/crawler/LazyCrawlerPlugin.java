package org.jdownloader.plugins.controller.crawler;

import jd.plugins.PluginForDecrypt;

public class LazyCrawlerPlugin {

    private AbstractCrawlerPlugin   info;
    private Class<PluginForDecrypt> plgClass;
    private PluginForDecrypt        prototypeInstance;

    public LazyCrawlerPlugin(AbstractCrawlerPlugin ap, Class<PluginForDecrypt> plgClass, PluginForDecrypt prototype) {
        this.info = ap;
        this.plgClass = plgClass;
        this.prototypeInstance = prototype;
    }

    public LazyCrawlerPlugin(AbstractCrawlerPlugin ap) {
        this.info = ap;
    }

}
