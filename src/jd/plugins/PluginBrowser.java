package jd.plugins;

import jd.http.Browser;

public class PluginBrowser<T extends Plugin> extends Browser {

    private final T plugin;

    public T getPlugin() {
        return plugin;
    }

    public PluginBrowser(final T plugin) {
        super();
        this.plugin = plugin;
    }

    @Override
    public Browser createNewBrowserInstance() {
        return getPlugin().createNewBrowserInstance();
    }

}
