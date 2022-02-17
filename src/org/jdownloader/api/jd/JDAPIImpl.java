package org.jdownloader.api.jd;

import java.io.File;

import jd.SecondLevelLaunch;
import jd.utils.JDUtilities;

import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;
import org.appwork.utils.Application;
import org.appwork.utils.IO;
import org.appwork.utils.Time;
import org.jdownloader.plugins.controller.crawler.CrawlerPluginController;
import org.jdownloader.plugins.controller.host.HostPluginController;

@Deprecated
public class JDAPIImpl implements JDAPI {
    public long uptime() {
        return Time.systemIndependentCurrentJVMTimeMillis() - SecondLevelLaunch.startup;
    }

    public long version() {
        return JDUtilities.getRevisionNumber();
    }

    @Override
    public boolean refreshPlugins() {
        HostPluginController.getInstance().init();
        CrawlerPluginController.getInstance().init();
        return true;
    }

    @Override
    public int getCoreRevision() {
        if (Application.isJared(JDAPIImpl.class)) {
            try {
                final File buildJson = Application.getResource("build.json");
                if (buildJson.isFile()) {
                    org.jdownloader.myjdownloader.client.json.JsonMap map = JSonStorage.restoreFromString(IO.readFileToString(buildJson), new TypeRef<org.jdownloader.myjdownloader.client.json.JsonMap>() {
                    });
                    return ((Number) map.get("JDownloaderRevision")).intValue();
                }
            } catch (Throwable t) {
                org.appwork.utils.logging2.extmanager.LoggerFactory.getDefaultLogger().log(t);
            }
        }
        return -1;
    }
}
