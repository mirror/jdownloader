package org.jdownloader.startup.commands;

import jd.http.Browser;

public class BRDebugCommand extends AbstractStartupCommand {

    public BRDebugCommand() {
        super("brdebug");
    }

    @Override
    public void run(String command, String... parameters) {
        logger.info("Set Browser Verbose: true");
        Browser.setGlobalVerbose(true);
    }

    @Override
    public String getDescription() {
        return "Set the Browser Debug Mode. Verbose Connection Infos";
    }
}
