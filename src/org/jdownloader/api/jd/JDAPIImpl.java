package org.jdownloader.api.jd;

import jd.SecondLevelLaunch;
import jd.utils.JDUtilities;

import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;
import org.appwork.utils.Application;
import org.appwork.utils.IO;
import org.appwork.utils.swing.dialog.Dialog;
import org.jdownloader.plugins.controller.crawler.CrawlerPluginController;
import org.jdownloader.plugins.controller.host.HostPluginController;

@Deprecated
public class JDAPIImpl implements JDAPI {
    public long uptime() {
        return System.currentTimeMillis() - SecondLevelLaunch.startup;
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
    public int sum(int a, int b) {
        return a + b;
    }

    @Override
    public void doSomethingCool() {
        new Thread() {
            public void run() {
                Dialog.getInstance().showMessageDialog("Awesome");
            }
        }.start();
    }

    @Override
    public Integer getCoreRevision() {
        if (Application.isJared(JDAPIImpl.class)) {
            try {
                org.jdownloader.myjdownloader.client.json.JsonMap map = JSonStorage.restoreFromString(IO.readFileToString(Application.getResource("build.json")), new TypeRef<org.jdownloader.myjdownloader.client.json.JsonMap>() {
                });
                return (Integer) map.get("JDownloaderRevision");
            } catch (Throwable t) {
                org.appwork.utils.logging2.extmanager.LoggerFactory.getDefaultLogger().log(t);
            }
        }
        return -1;
    }
}
