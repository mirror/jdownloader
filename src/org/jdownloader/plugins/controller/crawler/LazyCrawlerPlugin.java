package org.jdownloader.plugins.controller.crawler;

import org.jdownloader.plugins.controller.LazyPlugin;

import jd.plugins.PluginForDecrypt;

public class LazyCrawlerPlugin extends LazyPlugin<PluginForDecrypt> {

    private static final String     JD_PLUGINS_DECRYPTER = "jd.plugins.decrypter.";
    private Class<PluginForDecrypt> plgClass;
    private PluginForDecrypt        prototypeInstance;

    public LazyCrawlerPlugin(AbstractCrawlerPlugin ap, Class<PluginForDecrypt> plgClass, PluginForDecrypt prototype) {
        super(ap.getPattern(), JD_PLUGINS_DECRYPTER + ap.getClassname(), ap.getDisplayName());

        this.plgClass = plgClass;
        this.prototypeInstance = prototype;

    }

    public LazyCrawlerPlugin(AbstractCrawlerPlugin ap) {
        super(ap.getPattern(), JD_PLUGINS_DECRYPTER + ap.getClassname(), ap.getDisplayName());

    }

}
