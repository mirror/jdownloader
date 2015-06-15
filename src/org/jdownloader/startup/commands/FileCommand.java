package org.jdownloader.startup.commands;

import java.util.Arrays;

import jd.controlling.linkcollector.LinkOrigin;

public class FileCommand extends AbstractStartupCommand {

    public FileCommand() {
        super(new String[] { null });
    }

    @Override
    public void run(String command, String... parameters) {
        logger.info("FileCommand: " + Arrays.toString(parameters));
        for (final String parameter : parameters) {
            AddLinkCommand.add(LinkOrigin.START_PARAMETER, parameter);
        }
    }

    @Override
    public String getDescription() {
        return "Load Container files";
    }
}
