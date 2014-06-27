package org.jdownloader.controlling.packagizer;

import java.io.File;

import org.jdownloader.controlling.filter.BooleanFilter;
import org.jdownloader.gui.views.DownloadFolderChooserDialog;
import org.jdownloader.settings.staticreferences.CFG_GENERAL;
import org.jdownloader.translate._JDT;

public class SubFolderByPackageRule extends PackagizerRule {

    public static final String ID = "SubFolderByPackageRule";

    public SubFolderByPackageRule() {
        super();
        setMatchAlwaysFilter(new BooleanFilter(true));
        setDownloadDestination("<jd:packagename>");
        setIconKey("folder");
        setName(_JDT._.PackagizerSettings_folderbypackage_rule_name());
        setEnabled(false);
        setId(ID);
        setStaticRule(true);
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);

        String folder = CFG_GENERAL.DEFAULT_DOWNLOAD_FOLDER.getValue();
        if (enabled) {
            if (!folder.endsWith(DownloadFolderChooserDialog.PACKAGETAG)) {
                CFG_GENERAL.DEFAULT_DOWNLOAD_FOLDER.setValue(new File(new File(folder), DownloadFolderChooserDialog.PACKAGETAG).getAbsolutePath());
            }
        } else {
            if (folder.endsWith(DownloadFolderChooserDialog.PACKAGETAG)) {
                CFG_GENERAL.DEFAULT_DOWNLOAD_FOLDER.setValue(folder.substring(0, folder.length() - DownloadFolderChooserDialog.PACKAGETAG.length()));
            }
        }
    }

}
