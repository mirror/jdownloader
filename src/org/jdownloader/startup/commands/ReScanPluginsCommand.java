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
        CrawlerPluginController.invalidateCache();
        ExtensionController.getInstance().invalidateCache();
        SecondLevelLaunch.INIT_COMPLETE.executeWhenReached(new Runnable() {
            @Override
            public void run() {
                new Thread() {
                    {
                        setDaemon(true);
                    }

                    public void run() {
                        HostPluginController.getInstance().ensureLoaded();
                        CrawlerPluginController.getInstance().ensureLoaded();
                    };
                }.start();
            }
        });
    }

    @Override
    public String getDescription() {
        return "Rescan Plugins at Startup";
    }
}
