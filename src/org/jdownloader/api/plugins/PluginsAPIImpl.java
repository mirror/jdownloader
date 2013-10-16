package org.jdownloader.api.plugins;

import java.util.ArrayList;
import java.util.List;

import org.appwork.remoteapi.QueryResponseMap;
import org.jdownloader.plugins.controller.crawler.CrawlerPluginController;
import org.jdownloader.plugins.controller.crawler.LazyCrawlerPlugin;
import org.jdownloader.plugins.controller.host.HostPluginController;
import org.jdownloader.plugins.controller.host.LazyHostPlugin;

public class PluginsAPIImpl implements PluginsAPI {
    @Override
    public List<String> getPluginRegex(String URL) {
        URL = URL.replaceAll("^https?://(www.)?", "");
        List<String> ret = new ArrayList<String>();
        if (URL != null) {
            for (LazyHostPlugin lhp : HostPluginController.getInstance().list()) {
                if (URL.equals(lhp.getHost())) {
                    ret.add(lhp.getPattern().pattern());
                }
            }
            for (LazyCrawlerPlugin lhp : CrawlerPluginController.getInstance().list()) {
                if (URL.equals(lhp.getDisplayName())) {
                    ret.add(lhp.getPattern().pattern());
                }
            }
        }
        return ret;
    }

    @Override
    public QueryResponseMap getAllPluginRegex() {
        QueryResponseMap ret = new QueryResponseMap();

        for (LazyHostPlugin lhp : HostPluginController.getInstance().list()) {
            if (!ret.containsKey(lhp.getDisplayName())) {
                ret.put(lhp.getDisplayName(), new ArrayList<String>());
            }
            ((ArrayList<String>) ret.get(lhp.getDisplayName())).add(lhp.getPattern().pattern());
        }
        for (LazyCrawlerPlugin lhp : CrawlerPluginController.getInstance().list()) {
            if (!ret.containsKey(lhp.getDisplayName())) {
                ret.put(lhp.getDisplayName(), new ArrayList<String>());
            }
            ((ArrayList<String>) ret.get(lhp.getDisplayName())).add(lhp.getPattern().pattern());
        }
        return ret;
    }
}