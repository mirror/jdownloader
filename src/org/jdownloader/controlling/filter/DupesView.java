package org.jdownloader.controlling.filter;

import jd.controlling.linkcollector.VariousCrawledLinkFlags;
import jd.gui.swing.jdgui.views.settings.panels.linkgrabberfilter.editdialog.ConditionFilter;
import jd.gui.swing.jdgui.views.settings.panels.linkgrabberfilter.editdialog.ConditionFilter.Matchtype;

import org.jdownloader.gui.IconKey;
import org.jdownloader.translate._JDT;

public class DupesView extends LinkgrabberFilterRule {
    public static final String ID = "DupesView";

    public DupesView() {

    }

    public LinkgrabberFilterRule init() {
        setConditionFilter(new ConditionFilter(Matchtype.IS_TRUE, true, new VariousCrawledLinkFlags[] { VariousCrawledLinkFlags.DOWNLOAD_LIST_DUPE }));
        setName(_JDT._.LinkFilterSettings_DefaultFilterList_dupes());
        setIconKey(IconKey.ICON_COPY);
        setEnabled(true);
        setAccept(true);
        setId(ID);
        setStaticRule(true);
        return this;
    }
}
