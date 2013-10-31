package org.jdownloader.gui.views.linkgrabber.properties;

import jd.controlling.linkcrawler.CrawledLink;
import jd.controlling.linkcrawler.CrawledPackage;
import jd.controlling.packagecontroller.AbstractNode;

import org.appwork.swing.MigPanel;
import org.jdownloader.gui.views.linkgrabber.LinkGrabberTable;
import org.jdownloader.updatev2.gui.LAFOptions;

public class LinkgrabberProperties extends MigPanel {

    /**
     * 
     */
    private static final long   serialVersionUID = -195024600818162517L;
    private LinkGrabberTable    table;
    private LinkPropertiesPanel pkgPanel;
    private LinkPropertiesPanel linkPanel;

    public LinkgrabberProperties(LinkGrabberTable table) {
        super("ins 0", "[grow,fill]", "[grow,fill]");
        this.table = table;
        LAFOptions.getInstance().applyPanelBackground(this);
        pkgPanel = new PackagePropertiesPanel();
        linkPanel = new LinkPropertiesPanel();
        add(pkgPanel, "hidemode 3");
        add(linkPanel, "hidemode 3");
    }

    public void update(AbstractNode objectbyRow) {
        if (objectbyRow instanceof CrawledPackage) {
            CrawledPackage pkg = (CrawledPackage) objectbyRow;
            linkPanel.setVisible(false);
            pkgPanel.setVisible(true);
            pkgPanel.update(pkg);
        } else if (objectbyRow instanceof CrawledLink) {
            CrawledLink link = (CrawledLink) objectbyRow;
            linkPanel.setVisible(true);
            pkgPanel.setVisible(false);
            linkPanel.update(link);
        }
    }

}
