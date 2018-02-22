package org.jdownloader.controlling.packagizer;

import org.jdownloader.controlling.filter.BooleanFilter;
import org.jdownloader.gui.IconKey;
import org.jdownloader.translate._JDT;

public class SubFolderByPackageRule extends PackagizerRule {
    public static enum COUNT {
        NAMES,
        ITEMS,
    }

    public static final String ID = "SubFolderByPackageRule";

    public SubFolderByPackageRule() {
        super();
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
    }

    public void init() {
        setMatchAlwaysFilter(new BooleanFilter(true));
        setDownloadDestination("<jd:append><jd:packagename>");
        setIconKey(IconKey.ICON_FOLDER);
        setName(_JDT.T.PackagizerSettings_folderbypackage_rule_name());
        setEnabled(true);
        setId(ID);
        setStaticRule(true);
    }
}
