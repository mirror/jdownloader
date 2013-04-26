package org.jdownloader.api.jd;

import jd.SecondLevelLaunch;
import jd.utils.JDUtilities;

import org.appwork.exceptions.WTFException;
import org.jdownloader.plugins.controller.crawler.CrawlerPluginController;
import org.jdownloader.plugins.controller.host.HostPluginController;

public class JDAPIImpl implements JDAPI {

    public long uptime() {
        if (true) throw new WTFException("DEBUG");
        return System.currentTimeMillis() - SecondLevelLaunch.startup;
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
