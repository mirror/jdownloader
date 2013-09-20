package org.jdownloader.controlling.filter;

import jd.gui.swing.jdgui.views.settings.panels.linkgrabberfilter.editdialog.PluginStatusFilter;
import jd.gui.swing.jdgui.views.settings.panels.linkgrabberfilter.editdialog.PluginStatusFilter.PluginStatus;
import jd.gui.swing.jdgui.views.settings.panels.linkgrabberfilter.editdialog.PluginStatusFilter.PluginStatusMatchtype;

import org.jdownloader.gui.IconKey;
import org.jdownloader.translate._JDT;

public class DirectHTTPView extends LinkgrabberFilterRule {
    public static final String ID = "DirectHTTPView";

    public DirectHTTPView() {

        setPluginStatusFilter(new PluginStatusFilter(PluginStatusMatchtype.ISNOT, true, PluginStatus.NO_DIRECT_HTTP));
        setName(_JDT._.LinkFilterSettings_DefaultFilterList_directhttp());
        setIconKey(IconKey.ICON_DOWNLOAD);
        setAccept(true);
        setEnabled(false);
        setId(ID);
        setStaticRule(true);
    }
}
