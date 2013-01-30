package org.jdownloader.startup.commands;

import jd.Launcher;

import org.jdownloader.plugins.controller.crawler.CrawlerPluginController;
import org.jdownloader.plugins.controller.host.HostPluginController;

public class ReScanPluginsCommand extends AbstractStartupCommand {

    public ReScanPluginsCommand() {
        super("scanextensions", "scanplugins");
    }

    @Override
    public void run(String command, String... parameters) {
        HostPluginController.getInstance().invalidateCache();
        CrawlerPluginController.invalidateCache();
        Launcher.INIT_COMPLETE.executeWhenReached(new Runnable() {

            @Override
            public void run() {

                if (HostPluginController.getInstance().isCacheInvalidated()) {
                    HostPluginController.getInstance().init(true);
                }

            }
        });
    }

    @Override
    public String getDescription() {
        return "Rescan Plugins at Startup";
    }

}
