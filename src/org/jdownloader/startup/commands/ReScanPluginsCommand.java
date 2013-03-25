package org.jdownloader.startup.commands;

import jd.SecondLevelLaunch;

import org.jdownloader.extensions.ExtensionController;
import org.jdownloader.plugins.controller.crawler.CrawlerPluginController;
import org.jdownloader.plugins.controller.host.HostPluginController;

public class ReScanPluginsCommand extends AbstractStartupCommand {

    public ReScanPluginsCommand() {
        super("scanextensions", "scanplugins", "scan");
    }

    @Override
    public void run(String command, String... parameters) {
        logger.info("Rescan Plugins and Extensions");
        HostPluginController.getInstance().invalidateCache();
        ExtensionController.getInstance().invalidateCache();
        CrawlerPluginController.invalidateCache();
        SecondLevelLaunch.INIT_COMPLETE.executeWhenReached(new Runnable() {

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
