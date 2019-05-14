package org.jdownloader.extensions.schedulerV2.actions;

import java.util.List;

import jd.controlling.downloadcontroller.DownloadController;
import jd.controlling.linkcollector.LinkCollector;
import jd.controlling.linkcrawler.CrawledLink;
import jd.plugins.FilePackage;

import org.appwork.utils.logging2.LogInterface;
import org.jdownloader.extensions.schedulerV2.translate.T;

@ScheduleActionIDAnnotation("ADD_ALL_DOWNLOADS")
public class AddAllDownloadsAction extends AbstractScheduleAction<ScheduleActionEmptyConfig> {
    public AddAllDownloadsAction(String configJson) {
        super(configJson);
    }

    @Override
    public String getReadableName() {
        return T.T.action_addAllDownloads();
    }

    @Override
    public void execute(LogInterface logger) {
        final List<CrawledLink> links = LinkCollector.getInstance().getAllChildren();
        final List<FilePackage> convertedLinks = LinkCollector.getInstance().convert(links, true);
        if (convertedLinks != null) {
            final boolean addTop = org.jdownloader.settings.staticreferences.CFG_LINKGRABBER.LINKGRABBER_ADD_AT_TOP.isEnabled();
            DownloadController.getInstance().addAllAt(convertedLinks, addTop ? 0 : -(convertedLinks.size() + 10));
        }
    }
}
