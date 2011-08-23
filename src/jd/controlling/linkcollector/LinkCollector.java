package jd.controlling.linkcollector;

import java.util.List;

import jd.controlling.linkcrawler.CrawledLinkInfo;
import jd.controlling.linkcrawler.CrawledPackageInfo;
import jd.controlling.packagecontroller.PackageController;

public class LinkCollector extends PackageController<CrawledPackageInfo, CrawledLinkInfo> {

    private static LinkCollector INSTANCE = new LinkCollector();

    public static LinkCollector getInstance() {
        return INSTANCE;
    }

    private LinkCollector() {
    }

    @Override
    protected void _controllerParentlessLinks(List<CrawledLinkInfo> links) {
    }

    @Override
    protected void _controllerPackageNodeRemoved(CrawledPackageInfo pkg) {
    }

    @Override
    protected void _controllerStructureChanged() {
    }

    @Override
    protected void _controllerPackageNodeAdded(CrawledPackageInfo pkg) {
    }

}
