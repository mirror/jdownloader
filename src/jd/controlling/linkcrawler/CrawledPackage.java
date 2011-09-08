package jd.controlling.linkcrawler;

import java.util.ArrayList;
import java.util.List;

import jd.controlling.packagecontroller.AbstractPackageNode;
import jd.controlling.packagecontroller.PackageController;

public class CrawledPackage implements AbstractPackageNode<CrawledLink, CrawledPackage> {

    private ArrayList<CrawledLink>                         children         = new ArrayList<CrawledLink>();
    private PackageController<CrawledPackage, CrawledLink> controller       = null;
    private boolean                                        expanded         = false;
    private String                                         autoPackageName  = null;
    private boolean                                        allowAutoPackage = true;
    private transient CrawledPackageInfo                   fpInfo           = null;

    /**
     * @return the allowAutoPackage
     */
    public boolean isAllowAutoPackage() {
        return allowAutoPackage;
    }

    /**
     * @param allowAutoPackage
     *            the allowAutoPackage to set
     */
    public void setAllowAutoPackage(boolean allowAutoPackage) {
        this.allowAutoPackage = allowAutoPackage;
    }

    public CrawledPackage() {

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
    private long   created    = -1;

    /**
     * @param created
     *            the created to set
     */
    public void setCreated(long created) {
        this.created = created;
    }

    public PackageController<CrawledPackage, CrawledLink> getControlledBy() {
        return controller;
    }

    public void setControlledBy(PackageController<CrawledPackage, CrawledLink> controller) {
        this.controller = controller;
    }

    public List<CrawledLink> getChildren() {
        return children;
    }

    public void notifyChanges() {
        if (fpInfo != null) {
            synchronized (fpInfo) {
                fpInfo.structureVersion++;
            }
        }
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
        return true;
    }

    public long getCreated() {
        return created;
    }

    public long getFinishedDate() {
        return 0;
    }

    /*
     * fpInfo is null by default, will get created on first use
     */
    public CrawledPackageInfo getCrawledPackageInfo() {
        if (fpInfo != null) return fpInfo;
        synchronized (this) {
            if (fpInfo == null) {
                fpInfo = new CrawledPackageInfo(this);
            }
        }
        return fpInfo;
    }

}