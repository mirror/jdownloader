package org.jdownloader.startup.commands;

import jd.controlling.linkcollector.LinkCollectingJob;
import jd.controlling.linkcollector.LinkCollector;
import jd.controlling.linkcollector.LinkOrigin;
import jd.controlling.linkcollector.LinkOriginDetails;

public class AddContainerCommand extends AbstractStartupCommand {

    public AddContainerCommand() {
        super("add-containers", "add-container", "co");
    }

    @Override
    public void run(String command, String... parameters) {
        for (String s : parameters) {
            LinkCollector.getInstance().addCrawlerJob(new LinkCollectingJob(new LinkOriginDetails(LinkOrigin.START_PARAMETER, null), "file://" + s));
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
