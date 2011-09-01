package jd.controlling.linkcrawler;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import jd.controlling.packagecontroller.AbstractPackageNode;
import jd.controlling.packagecontroller.PackageController;

public class CrawledPackageInfo implements AbstractPackageNode<CrawledLinkInfo, CrawledPackageInfo> {

    private static AtomicInteger                                   packageCounter  = new AtomicInteger(0);
    private ArrayList<CrawledLinkInfo>                             children        = new ArrayList<CrawledLinkInfo>();
    private PackageController<CrawledPackageInfo, CrawledLinkInfo> controller      = null;
    private boolean                                                expanded        = true;
    private String                                                 autoPackageName = null;
    private final int                                              ID;

    /**
     * @return the iD
     */
    public int getID() {
        return ID;
    }

    public CrawledPackageInfo() {
        ID = packageCounter.incrementAndGet();
    }

    /**
     * @return the autoPackageName
     */
    public String getAutoPackageName() {
        return autoPackageName;
    }

    /**
     * @param autoPackageName
     *            the autoPackageName to set
     */
    public void setAutoPackageName(String autoPackageName) {
        this.autoPackageName = autoPackageName;
    }

    private String customName = null;

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

    public boolean isExpanded() {
        return expanded;
    }

    public void setExpanded(boolean b) {
        this.expanded = b;
    }

    public String getName() {
        if (customName != null) return customName;
        return autoPackageName;
    }

    public boolean isEnabled() {
        return false;
    }

    public long getCreated() {
        return 0;
    }

    public long getFinishedDate() {
        return 0;
    }

}