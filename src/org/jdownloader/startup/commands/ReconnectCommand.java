package org.jdownloader.startup.commands;

import jd.controlling.downloadcontroller.DownloadWatchDog;

public class ReconnectCommand extends AbstractStartupCommand {

    public ReconnectCommand() {
        super("reconnect", "r");
    }

    @Override
    public void run(String command, String... parameters) {
        try {
            DownloadWatchDog.getInstance().requestReconnect(false);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Override
    public String getDescription() {
        return "Do a Reconnect";
    }

}
