package org.jdownloader.startup.commands;

import java.util.Arrays;

import jd.controlling.linkcollector.LinkCollectingJob;
import jd.controlling.linkcollector.LinkCollector;

import org.appwork.utils.StringUtils;

public class FileCommand extends AbstractStartupCommand {

    public FileCommand() {
        // For example dlcs
        super(new String[] { null });
    }

    @Override
    public void run(String command, String... parameters) {
        logger.info("Load File: " + Arrays.toString(parameters));

        for (String s : parameters) {
            if (StringUtils.isNotEmpty(s)) {
                LinkCollector.getInstance().addCrawlerJob(new LinkCollectingJob("file://" + s));
            }
        }

    }

    @Override
    public String getDescription() {
        return "Load Container files";
    }
}
