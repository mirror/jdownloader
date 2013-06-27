package org.jdownloader.api.jd;

import java.util.HashMap;

import jd.SecondLevelLaunch;
import jd.utils.JDUtilities;

import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;
import org.appwork.utils.Application;
import org.appwork.utils.IO;
import org.appwork.utils.logging.Log;
import org.appwork.utils.swing.dialog.Dialog;
import org.jdownloader.plugins.controller.crawler.CrawlerPluginController;
import org.jdownloader.plugins.controller.host.HostPluginController;

public class JDAPIImpl implements JDAPI {

    public long uptime() {
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
        HashMap<String, Object> map = null;
        try {
            map = JSonStorage.restoreFromString(IO.readFileToString(Application.getResource("build.json")), new TypeRef<HashMap<String, Object>>() {
            });
            return (Integer) map.get("JDownloaderRevision");

        } catch (Throwable t) {
            Log.exception(t);
        }
        return -1;
    }
}
