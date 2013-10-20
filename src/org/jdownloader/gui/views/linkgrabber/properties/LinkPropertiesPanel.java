package org.jdownloader.gui.views.linkgrabber.properties;

import jd.controlling.linkcrawler.CrawledLink;

import org.appwork.swing.MigPanel;
import org.jdownloader.updatev2.gui.LAFOptions;

public class LinkPropertiesPanel extends MigPanel {
    public LinkPropertiesPanel() {
        super("ins 0", "[grow,fill]", "[grow,fill]");
        LAFOptions.getInstance().applyPanelBackground(this);
    }

    public void update(CrawledLink link) {
    }
}
