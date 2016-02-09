package org.jdownloader.controlling.filter;

import org.jdownloader.gui.IconKey;
import org.jdownloader.translate._JDT;

import jd.gui.swing.jdgui.views.settings.panels.linkgrabberfilter.editdialog.OnlineStatusFilter;
import jd.gui.swing.jdgui.views.settings.panels.linkgrabberfilter.editdialog.OnlineStatusFilter.OnlineStatus;
import jd.gui.swing.jdgui.views.settings.panels.linkgrabberfilter.editdialog.OnlineStatusFilter.OnlineStatusMatchtype;

public class OfflineView extends LinkgrabberFilterRule {
    public static final String ID = "OfflineView";

    public OfflineView() {

    }

    public LinkgrabberFilterRule init() {
        setOnlineStatusFilter(new OnlineStatusFilter(OnlineStatusMatchtype.IS, true, OnlineStatus.OFFLINE));
        setName(_JDT.T.LinkFilterSettings_DefaultFilterList_getDefaultValue_());
        setIconKey(IconKey.ICON_ERROR);
        setAccept(true);
        setEnabled(true);
        setId(ID);
        setStaticRule(true);
        return this;
    }
}
