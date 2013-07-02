package org.jdownloader.controlling.packagizer;

import jd.gui.swing.jdgui.views.settings.panels.linkgrabberfilter.editdialog.OnlineStatusFilter;
import jd.gui.swing.jdgui.views.settings.panels.linkgrabberfilter.editdialog.OnlineStatusFilter.OnlineStatus;
import jd.gui.swing.jdgui.views.settings.panels.linkgrabberfilter.editdialog.OnlineStatusFilter.OnlineStatusMatchtype;

import org.jdownloader.controlling.filter.FiletypeFilter;
import org.jdownloader.controlling.filter.FiletypeFilter.TypeMatchType;
import org.jdownloader.translate._JDT;

public class DisableRevFilesPackageRule extends PackagizerRule {

    public static final String ID = "DisableRevFilesPackageRule";

    public DisableRevFilesPackageRule() {
        super();
        // setFilenameFilter(new RegexFilter(true, MatchType.EQUALS, "*.rev", false));
        setFiletypeFilter(new FiletypeFilter(TypeMatchType.IS, true, false, false, false, false, "rev", false));
        setOnlineStatusFilter(new OnlineStatusFilter(OnlineStatusMatchtype.IS, true, OnlineStatus.ONLINE));
        setIconKey("archive");
        setName(_JDT._.DisableRevFilesPackageRulee_rule_name());
        setLinkEnabled(false);
        setEnabled(true);
        setId(ID);
        setStaticRule(true);
    }

}
