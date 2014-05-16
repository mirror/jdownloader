package jd.controlling.linkchecker;

import jd.http.BrowserSettingsThread;
import jd.plugins.PluginForHost;
import jd.plugins.UseSetLinkStatusThread;

public class LinkCheckerThread extends BrowserSettingsThread implements UseSetLinkStatusThread {

    public LinkCheckerThread() {
        super();
    }

    protected PluginForHost plugin;

    public PluginForHost getPlugin() {
        return plugin;
    }
}
