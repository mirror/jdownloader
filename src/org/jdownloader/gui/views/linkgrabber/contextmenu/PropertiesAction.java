package org.jdownloader.gui.views.linkgrabber.contextmenu;

import java.awt.event.ActionEvent;

import jd.controlling.linkcrawler.CrawledLink;
import jd.controlling.linkcrawler.CrawledPackage;

import org.jdownloader.actions.AbstractSelectionContextAction;
import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.SelectionInfo;
import org.jdownloader.settings.staticreferences.CFG_GUI;

public class PropertiesAction extends AbstractSelectionContextAction<CrawledPackage, CrawledLink> {

    public PropertiesAction(SelectionInfo<CrawledPackage, CrawledLink> si) {
        super(si);
        setName(_GUI._.PropertiesAction_PropertiesAction());
        setIconKey(IconKey.ICON_BOTTOMBAR);

    }

    public boolean isVisible() {
        return super.isVisible() && !CFG_GUI.LINKGRABBER_TAB_PROPERTIES_PANEL_VISIBLE.isEnabled();
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        CFG_GUI.LINKGRABBER_TAB_PROPERTIES_PANEL_VISIBLE.setValue(true);
    }

    @Override
    public void setData(String data) {
    }

}
