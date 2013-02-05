package jd.controlling.linkcrawler;

import jd.controlling.linkcollector.LinknameCleaner;

import org.appwork.utils.StringUtils;
import org.jdownloader.controlling.UniqueAlltimeID;

public class PackageInfo {
    private UniqueAlltimeID uniqueId              = null;

    private boolean         packagizerRuleMatched = false;
    private boolean         ignoreVarious         = false;

    public UniqueAlltimeID getUniqueId() {
        return uniqueId;
    }

    public void setUniqueId(UniqueAlltimeID uniqueId) {
        this.uniqueId = uniqueId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDestinationFolder() {
        return destinationFolder;
    }

    public void setDestinationFolder(String destinationFolder) {
        this.destinationFolder = destinationFolder;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    private String name              = null;
    private String destinationFolder = null;
    private String comment           = null;

    /**
     * Returns a packageID or null, of no id specific values are set. if this method returns a value !=null, it should get an own package, which is not part of
     * autopackaging.
     * 
     * @return
     */
    public String createPackageID() {
        StringBuilder sb = new StringBuilder();
        if (getUniqueId() != null) {
            if (sb.length() > 0) sb.append("_");
            sb.append(getUniqueId().toString());
        }
        // if (!StringUtils.isEmpty(getDestinationFolder())) {
        // if (sb.length() > 0) sb.append("_");
        // sb.append(getDestinationFolder());
        // }
        if (!StringUtils.isEmpty(getName())) {
            if (sb.length() > 0) sb.append("_");
            sb.append(getName());
        }
        return sb.length() == 0 ? null : sb.toString();
    }

    public static CrawledPackage createCrawledPackage(CrawledLink link) {
        PackageInfo dpi = link.getDesiredPackageInfo();
        if (dpi == null) return null;
        CrawledPackage ret = new CrawledPackage();
        /* fetch desired Packagename from info */
        String pkgName = dpi.getName();
        if (StringUtils.isEmpty(pkgName)) {
            /* no info available, so lets cleanup filename */
            pkgName = LinknameCleaner.cleanFileName(link.getName());
            ret.setName(pkgName);
        } else {
            ret.setName(pkgName);
        }
        ret.setCreated(link.getCreated());
        ret.setComment(dpi.getComment());

        if (!StringUtils.isEmpty(dpi.getDestinationFolder())) {
            ret.setDownloadFolder(dpi.getDestinationFolder());
        }
        return ret;
    }

    /**
     * @return the packagizerRuleMatched
     */
    public boolean isPackagizerRuleMatched() {
        return packagizerRuleMatched;
    }

    /**
     * @param packagizerRuleMatched
     *            the packagizerRuleMatched to set
     */
    public void setPackagizerRuleMatched(boolean packagizerRuleMatched) {
        this.packagizerRuleMatched = packagizerRuleMatched;
    }

    /**
     * @return the ignoreVarious
     */
    public boolean isIgnoreVarious() {
        return ignoreVarious;
    }

    /**
     * @param ignoreVarious
     *            the ignoreVarious to set
     */
    public void setIgnoreVarious(boolean ignoreVarious) {
        this.ignoreVarious = ignoreVarious;
    }

}
