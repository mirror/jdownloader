package org.jdownloader.gui.views.linkgrabber.properties;

import java.awt.Dimension;

import javax.swing.JPopupMenu;

import org.appwork.swing.MigPanel;
import org.jdownloader.gui.views.linkgrabber.LinkGrabberTable;
import org.jdownloader.settings.staticreferences.CFG_GUI;
import org.jdownloader.updatev2.gui.LAFOptions;

import jd.controlling.linkcrawler.CrawledLink;
import jd.controlling.linkcrawler.CrawledPackage;
import jd.controlling.packagecontroller.AbstractNode;

public class LinkgrabberProperties extends MigPanel {

    /**
     *
     */
    private static final long                   serialVersionUID = -195024600818162517L;
    private final CrawledPackagePropertiesPanel pkgPanel;
    private final CrawledLinkPropertiesPanel    linkPanel;

    public LinkgrabberProperties(LinkGrabberTable table) {
        super(LAFOptions.getInstance().getExtension().customizeLinkPropertiesPanelLayout(), "[grow,fill]", "[grow,fill]");
        LAFOptions.getInstance().applyPanelBackground(this);
        pkgPanel = new CrawledPackagePropertiesPanel();
        linkPanel = new CrawledLinkPropertiesPanel();
        add(pkgPanel, "hidemode 3");
        add(linkPanel, "hidemode 3");
        pkgPanel.setVisible(false);
        linkPanel.setVisible(false);
        LAFOptions.getInstance().getExtension().customizeLinkPropertiesPanel(this);
    }

    @Override
    public Dimension getPreferredSize() {
        Dimension ret = super.getPreferredSize();
        if (CFG_GUI.CFG.isPropertiesPanelHeightNormalized()) {
            ret.height = Math.max(pkgPanel.getPreferredSize().height, linkPanel.getPreferredSize().height);
        }
        return ret;
    }

    public void update(AbstractNode objectbyRow) {
        if (objectbyRow != null) {
            if (objectbyRow instanceof CrawledPackage) {
                final CrawledPackage pkg = (CrawledPackage) objectbyRow;
                linkPanel.setVisible(false);
                pkgPanel.setVisible(true);
                linkPanel.setSelectedItem(null);
                pkgPanel.setSelectedItem(pkg);
            } else if (objectbyRow instanceof CrawledLink) {
                final CrawledLink link = (CrawledLink) objectbyRow;
                linkPanel.setVisible(true);
                pkgPanel.setVisible(false);
                pkgPanel.setSelectedItem(null);
                linkPanel.setSelectedItem(link);
            }
        } else {
            linkPanel.setVisible(false);
            pkgPanel.setVisible(false);
            linkPanel.setSelectedItem(null);
            pkgPanel.setSelectedItem(null);
        }
        // System.out.println("UPDATE");
        // new Exception().printStackTrace();
    }

    public void fillPopup(JPopupMenu pu) {
        if (linkPanel.isVisible()) {
            linkPanel.fillPopup(pu);
        } else if (pkgPanel.isVisible()) {
            pkgPanel.fillPopup(pu);
        }
    }

    public void refreshAfterTabSwitch() {
        linkPanel.refresh();
        pkgPanel.refresh();
    }

    public void save() {
        if (linkPanel.isVisible()) {
            linkPanel.save();
        } else if (pkgPanel.isVisible()) {
            pkgPanel.save();
        }
    }

}
