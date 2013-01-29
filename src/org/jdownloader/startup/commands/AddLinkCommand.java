package org.jdownloader.startup.commands;

import jd.controlling.linkcollector.LinkCollectingJob;
import jd.controlling.linkcollector.LinkCollector;

public class AddLinkCommand extends AbstractStartupCommand {

    public AddLinkCommand() {
        super("add-links", "add-link", "a");
    }

    @Override
    public void run(String command, String... parameters) {
        for (String s : parameters) {
            LinkCollector.getInstance().addCrawlerJob(new LinkCollectingJob(s));
        }
    }

    @Override
    public String getParameterHelp() {
        return "<Link1> <Link2> ... <Link*>";
    }

    @Override
    public String getDescription() {
        return "Add Links to the LinkGrabber";
    }

}
