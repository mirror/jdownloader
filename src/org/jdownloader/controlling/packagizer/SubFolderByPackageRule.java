package org.jdownloader.controlling.packagizer;

import org.jdownloader.controlling.filter.BooleanFilter;
import org.jdownloader.translate._JDT;

public class SubFolderByPackageRule extends PackagizerRule {

    public static final String ID = "SubFolderByPackageRule";

    public SubFolderByPackageRule() {
        super();

    }

    public void init() {
        setMatchAlwaysFilter(new BooleanFilter(true));
        setDownloadDestination("<jd:packagename>");
        setIconKey("folder");
        setName(_JDT._.PackagizerSettings_folderbypackage_rule_name());
        setEnabled(true);
        setId(ID);
        setStaticRule(true);
    }

}
