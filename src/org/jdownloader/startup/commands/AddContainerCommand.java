package org.jdownloader.startup.commands;

import java.util.Arrays;

import jd.controlling.linkcollector.LinkOrigin;

public class AddContainerCommand extends AbstractStartupCommand {

    public AddContainerCommand() {
        super("add-containers", "add-container", "co");
    }

    @Override
    public void run(String command, String... parameters) {
        logger.info("AddContainerCommand: " + Arrays.toString(parameters));
        for (final String parameter : parameters) {
            AddLinkCommand.add(LinkOrigin.START_PARAMETER, parameter);
        }
    }

    @Override
    public String getParameterHelp() {
        return "<ContainerPath1> <ContainerPath2> ... <ContainerPath*>";
    }

    @Override
    public String getDescription() {
        return "Add Container Files to the LinkGrabber";
    }

}
