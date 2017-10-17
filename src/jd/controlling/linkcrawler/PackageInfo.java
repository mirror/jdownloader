package jd.controlling.linkcrawler;

import org.appwork.utils.StringUtils;
import org.appwork.utils.os.CrossSystem;
import org.jdownloader.controlling.UniqueAlltimeID;

public class PackageInfo {
    private UniqueAlltimeID uniqueId              = null;
    private boolean         packagizerRuleMatched = false;
    private Boolean         ignoreVarious         = null;
    private Boolean         allowInheritance      = null;

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
        ret.setName(getName());
        ret.setDestinationFolder(getDestinationFolder());
        ret.setIgnoreVarious(isIgnoreVarious());
        ret.setPackagizerRuleMatched(isPackagizerRuleMatched());
        ret.setUniqueId(getUniqueId());
        ret.setAllowInheritance(isAllowInheritance());
        return ret;
    }

    public void setName(String name) {
        if (StringUtils.isEmpty(name)) {
            name = null;
        }
        this.name = CrossSystem.alleviatePathParts(name);
    }

    public String getDestinationFolder() {
        return destinationFolder;
    }

    public void setDestinationFolder(String destinationFolder) {
        if (StringUtils.isEmpty(destinationFolder)) {
            destinationFolder = null;
        }
        this.destinationFolder = destinationFolder;
    }

    private String name              = null;
    private String destinationFolder = null;

    /**
     * Returns a packageID or null, of no id specific values are set. if this method returns a value !=null, it should get an own package,
     * which is not part of autopackaging.
     *
     * @return
     */
    public String createPackageID() {
        StringBuilder sb = new StringBuilder();
        if (getUniqueId() != null) {
            if (sb.length() > 0) {
                sb.append("_");
            }
            sb.append(getUniqueId().toString());
        }
        // if (!StringUtils.isEmpty(getDestinationFolder())) {
        // if (sb.length() > 0) sb.append("_");
        // sb.append(getDestinationFolder());
        // }
        if (!StringUtils.isEmpty(getName())) {
            if (sb.length() > 0) {
                sb.append("_");
            }
            sb.append(getName());
        }
        return sb.length() == 0 ? null : sb.toString();
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
        return ignoreVarious != null || uniqueId != null || destinationFolder != null || name != null || packagizerRuleMatched;
    }
}
