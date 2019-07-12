package org.jdownloader.controlling.packagizer;

import org.jdownloader.controlling.filter.FiletypeFilter;
import org.jdownloader.controlling.filter.FiletypeFilter.TypeMatchType;
import org.jdownloader.translate._JDT;

public class DisableParFilesPackageRule extends PackagizerRule {
    public static final String ID = "DisableParFilesPackageRule";

    public DisableParFilesPackageRule() {
        super();
    }

    public void init() {
        setFiletypeFilter(new FiletypeFilter(TypeMatchType.IS, true, false, false, false, false, false, false, false, false, "par,par2", false));
        setIconKey(org.jdownloader.gui.IconKey.ICON_EXTRACT);
        setName(_JDT.T.DisableParFilesPackageRulee_rule_name());
        setLinkEnabled(false);
        setEnabled(true);
        setId(ID);
        setStaticRule(true);
    }
}
