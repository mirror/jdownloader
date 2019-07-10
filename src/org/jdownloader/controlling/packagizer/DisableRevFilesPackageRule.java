package org.jdownloader.controlling.packagizer;

import org.jdownloader.controlling.filter.FiletypeFilter;
import org.jdownloader.controlling.filter.FiletypeFilter.TypeMatchType;
import org.jdownloader.translate._JDT;

public class DisableRevFilesPackageRule extends PackagizerRule {
    public static final String ID = "DisableRevFilesPackageRule";

    public DisableRevFilesPackageRule() {
        super();
    }

    public void init() {
        setFiletypeFilter(new FiletypeFilter(TypeMatchType.IS, true, false, false, false, false, false, false, false, "rev", false));
        setIconKey(org.jdownloader.gui.IconKey.ICON_EXTRACT);
        setName(_JDT.T.DisableRevFilesPackageRulee_rule_name());
        setLinkEnabled(false);
        setEnabled(true);
        setId(ID);
        setStaticRule(true);
    }
}
