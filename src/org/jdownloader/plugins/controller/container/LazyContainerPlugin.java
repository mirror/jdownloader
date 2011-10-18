package org.jdownloader.plugins.controller.container;

import jd.plugins.PluginsC;

import org.jdownloader.plugins.controller.LazyPlugin;

public class LazyContainerPlugin extends LazyPlugin<PluginsC> {
    private static final String JD_PLUGINS_DECRYPTER = "jd.plugins.a.";

    public LazyContainerPlugin(AbstractContainerPlugin ap) {
        super(ap.getPattern(), JD_PLUGINS_DECRYPTER + ap.getClassname(), ap.getDisplayName(), ap.getVersion());
    }

    @Override
    public PluginsC newInstance() {
        PluginsC ret = super.newInstance();
        ret.setLazyCo(this);
        return ret;
    }

}
