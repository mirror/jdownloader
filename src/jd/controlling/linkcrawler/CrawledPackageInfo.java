package jd.controlling.linkcrawler;

import java.util.ArrayList;
import java.util.List;

import jd.controlling.packagecontroller.AbstractPackageNode;
import jd.controlling.packagecontroller.PackageController;

public class CrawledPackageInfo implements AbstractPackageNode<CrawledLinkInfo, CrawledPackageInfo> {

    private ArrayList<CrawledLinkInfo>                             children   = new ArrayList<CrawledLinkInfo>();
    private PackageController<CrawledPackageInfo, CrawledLinkInfo> controller = null;

    public PackageController<CrawledPackageInfo, CrawledLinkInfo> getControlledBy() {

        return controller;
    }

    public void setControlledBy(PackageController<CrawledPackageInfo, CrawledLinkInfo> controller) {
        this.controller = controller;
    }

    public List<CrawledLinkInfo> getChildren() {
        return children;
    }

    public void notifyChanges() {
    }

}