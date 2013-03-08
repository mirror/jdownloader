package org.jdownloader.api.jd;

import jd.Launcher;
import jd.utils.JDUtilities;

import org.jdownloader.plugins.controller.crawler.CrawlerPluginController;
import org.jdownloader.plugins.controller.host.HostPluginController;

public class JDAPIImpl implements JDAPI {

    public long uptime() {
        return System.currentTimeMillis() - Launcher.startup;
    }

    public long version() {
        return JDUtilities.getRevisionNumber();
    }

    @Override
    public boolean refreshPlugins() {
        HostPluginController.getInstance().init(true);
        CrawlerPluginController.getInstance().init(true);
        return true;
    }

    @Override
    public int sum(int a, int b) {
        return a + b;
    }
}
