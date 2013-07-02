package org.jdownloader.controlling.filter;

import jd.gui.swing.jdgui.views.settings.panels.linkgrabberfilter.editdialog.OnlineStatusFilter;
import jd.gui.swing.jdgui.views.settings.panels.linkgrabberfilter.editdialog.OnlineStatusFilter.OnlineStatus;
import jd.gui.swing.jdgui.views.settings.panels.linkgrabberfilter.editdialog.OnlineStatusFilter.OnlineStatusMatchtype;

import org.jdownloader.translate._JDT;

public class OfflineView extends LinkgrabberFilterRule {
    public static final String ID = "OfflineView";

    public OfflineView() {

        setOnlineStatusFilter(new OnlineStatusFilter(OnlineStatusMatchtype.IS, true, OnlineStatus.OFFLINE));
        setName(_JDT._.LinkFilterSettings_DefaultFilterList_getDefaultValue_());
        setIconKey("error");
        setAccept(true);
        setEnabled(true);
        setId(ID);
        setStaticRule(true);
    }
}
