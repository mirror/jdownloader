package jd.controlling.linkcrawler.modifier;

import java.io.File;

import org.appwork.utils.StringUtils;
import org.appwork.utils.os.CrossSystem;

import jd.controlling.linkcrawler.CrawledLink;
import jd.controlling.linkcrawler.CrawledLinkModifier;
import jd.controlling.linkcrawler.PackageInfo;

public class DownloadFolderModifier implements CrawledLinkModifier {
    protected final String folder;

    public String getFolder() {
        return folder;
    }

    public boolean isOverwriteFlag() {
        return overwriteFlag;
    }

    protected final boolean overwriteFlag;

    public DownloadFolderModifier(final String folder, final boolean overwriteFlag) {
        this.folder = StringUtils.isNotEmpty(folder) ? CrossSystem.fixPathSeparators(folder + File.separator) : null;
        this.overwriteFlag = overwriteFlag;
    }

    @Override
    public boolean modifyCrawledLink(CrawledLink link) {
        PackageInfo existing = link.getDesiredPackageInfo();
        if (overwriteFlag || existing == null || StringUtils.isEmpty(existing.getDestinationFolderRoot())) {
            if (existing == null) {
                existing = new PackageInfo();
            }
            if (overwriteFlag) {
                existing.setIgnoreVarious(true);
                existing.setDestinationFolder(folder);
            } else {
                existing.setDestinationFolderRoot(folder);
            }
            existing.setUniqueId(null);
            link.setDesiredPackageInfo(existing);
            return true;
        }
        return false;
    }
}
