package org.jdownloader.startup.commands;

import jd.controlling.linkcollector.LinkCollectingJob;
import jd.controlling.linkcollector.LinkCollector;
import jd.controlling.linkcollector.LinkSource;

public class AddContainerCommand extends AbstractStartupCommand {

    public AddContainerCommand() {
        super("add-containers", "add-container", "co");
    }

    @Override
    public void run(String command, String... parameters) {
        for (String s : parameters) {
            LinkCollector.getInstance().addCrawlerJob(new LinkCollectingJob("file://" + s).setSource(LinkSource.START_PARAMETER));
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
