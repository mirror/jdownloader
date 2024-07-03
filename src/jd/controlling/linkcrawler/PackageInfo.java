package jd.controlling.linkcrawler;

import jd.controlling.linkcollector.LinknameCleaner;

import org.appwork.utils.StringUtils;
import org.jdownloader.controlling.UniqueAlltimeID;

public class PackageInfo {
    private UniqueAlltimeID uniqueId              = null;
    private boolean         packagizerRuleMatched = false;
    private Boolean         ignoreVarious         = null;
    private Boolean         allowInheritance      = null;
    private String          packageKey            = null;
    private String          comment               = null;

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public String getPackageKey() {
        return packageKey;
    }

    public void setPackageKey(String packageKey) {
        this.packageKey = packageKey;
    }

    public Boolean isAllowInheritance() {
        return allowInheritance;
    }

    public void setAllowInheritance(Boolean allowInheritance) {
        this.allowInheritance = allowInheritance;
    }

    public UniqueAlltimeID getUniqueId() {
        return uniqueId;
    }

    public void setUniqueId(UniqueAlltimeID uniqueId) {
        this.uniqueId = uniqueId;
    }

    public String getName() {
        return name;
    }

    public PackageInfo getCopy() {
        final PackageInfo ret = new PackageInfo();
        ret.name = this.name;// avoid cleanPackagename
        ret.setDestinationFolder(getDestinationFolder());
        ret.setDestinationFolderRoot(getDestinationFolderRoot());
        ret.setIgnoreVarious(isIgnoreVarious());
        ret.setPackagizerRuleMatched(isPackagizerRuleMatched());
        ret.setUniqueId(getUniqueId());
        ret.setAllowInheritance(isAllowInheritance());
        ret.setPackageKey(getPackageKey());
        ret.setComment(getComment());
        return ret;
    }

    public void setName(String name) {
        name = LinknameCleaner.cleanPackagename(name, false);
        if (StringUtils.isEmpty(name)) {
            this.name = null;
        } else {
            this.name = name;
        }
    }

    public String getDestinationFolder() {
        return destinationFolder;
    }

    public void setDestinationFolder(String destinationFolder) {
        if (StringUtils.isEmpty(destinationFolder)) {
            this.destinationFolder = null;
        } else {
            this.destinationFolder = destinationFolder;
        }
    }

    public String getDestinationFolderRoot() {
        return destinationFolderRoot;
    }

    public void setDestinationFolderRoot(String destinationFolderRoot) {
        if (StringUtils.isEmpty(destinationFolderRoot)) {
            this.destinationFolderRoot = null;
        } else {
            this.destinationFolderRoot = destinationFolderRoot;
        }
    }

    private String name                  = null;
    private String destinationFolder     = null;
    private String destinationFolderRoot = null;

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
    public Boolean isIgnoreVarious() {
        return ignoreVarious;
    }

    /**
     * @param ignoreVarious
     *            the ignoreVarious to set
     */
    public void setIgnoreVarious(Boolean ignoreVarious) {
        this.ignoreVarious = ignoreVarious;
    }

    public boolean isEmpty() {
        return !isNotEmpty();
    }

    public boolean isNotEmpty() {
        return ignoreVarious != null || packageKey != null || uniqueId != null || destinationFolder != null || destinationFolderRoot != null || name != null || packagizerRuleMatched;
    }
}
