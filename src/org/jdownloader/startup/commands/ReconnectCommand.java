package org.jdownloader.startup.commands;

import jd.controlling.reconnect.Reconnecter;

public class ReconnectCommand extends AbstractStartupCommand {

    public ReconnectCommand() {
        super("reconnect", "r");
    }

    @Override
    public void run(String command, String... parameters) {
        Reconnecter.getInstance().forceReconnect();
    }

    @Override
    public String getDescription() {
        return "Do a Reconnect";
    }

}
