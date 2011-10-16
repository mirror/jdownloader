package org.jdownloader.plugins.controller.crawler;

import jd.plugins.PluginForDecrypt;

import org.jdownloader.plugins.controller.LazyPlugin;

public class LazyCrawlerPlugin extends LazyPlugin<PluginForDecrypt> {

    private static final String JD_PLUGINS_DECRYPTER = "jd.plugins.decrypter.";

    public LazyCrawlerPlugin(AbstractCrawlerPlugin ap, Class<PluginForDecrypt> plgClass, PluginForDecrypt prototype) {
        super(ap.getPattern(), JD_PLUGINS_DECRYPTER + ap.getClassname(), ap.getDisplayName());

        this.pluginClass = plgClass;
        this.prototypeInstance = prototype;

    }

    public LazyCrawlerPlugin(AbstractCrawlerPlugin ap) {
        super(ap.getPattern(), JD_PLUGINS_DECRYPTER + ap.getClassname(), ap.getDisplayName());
    }

    @Override
    public PluginForDecrypt newInstance() {
        PluginForDecrypt ret = super.newInstance();
        ret.setLazyC(this);
        return ret;
    }
}
