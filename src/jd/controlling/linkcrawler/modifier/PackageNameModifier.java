package jd.controlling.linkcrawler.modifier;

import java.util.List;

import jd.controlling.linkcrawler.CrawledLink;
import jd.controlling.linkcrawler.CrawledLinkModifier;
import jd.controlling.linkcrawler.PackageInfo;

import org.appwork.utils.StringUtils;

public class PackageNameModifier implements CrawledLinkModifier {
    protected final String name;

    public String getName() {
        return name;
    }

    public boolean isOverwriteFlag() {
        return overwriteFlag;
    }

    protected final boolean overwriteFlag;

    public PackageNameModifier(final String name, final boolean overwriteFlag) {
        this.name = name;
        this.overwriteFlag = overwriteFlag;
    }

    @Override
    public boolean modifyCrawledLink(CrawledLink link) {
        PackageInfo existing = link.getDesiredPackageInfo();
        if (overwriteFlag || existing == null || StringUtils.isEmpty(existing.getName())) {
            if (existing == null) {
                existing = new PackageInfo();
            }
            existing.setName(name);
            if (overwriteFlag) {
                existing.setIgnoreVarious(true);
            }
            existing.setUniqueId(null);
            link.setDesiredPackageInfo(existing);
            return true;
        }
        return false;
    }

    @Override
    public List<CrawledLinkModifier> getSubCrawledLinkModifier(CrawledLink link) {
        return null;
    }
}
