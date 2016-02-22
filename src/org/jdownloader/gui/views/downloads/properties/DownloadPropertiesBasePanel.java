package org.jdownloader.gui.views.downloads.properties;

import java.awt.Dimension;

import javax.swing.JPopupMenu;

import org.appwork.swing.MigPanel;
import org.jdownloader.gui.views.downloads.table.DownloadsTable;
import org.jdownloader.settings.staticreferences.CFG_GUI;
import org.jdownloader.updatev2.gui.LAFOptions;

import jd.controlling.packagecontroller.AbstractNode;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;

public class DownloadPropertiesBasePanel extends MigPanel {

    /**
     *
     */
    private static final long                 serialVersionUID = -195024600818162517L;
    private final FilePackagePropertiesPanel  pkgPanel;
    private final DownloadLinkPropertiesPanel linkPanel;

    public DownloadPropertiesBasePanel(DownloadsTable table2) {
        super(LAFOptions.getInstance().getExtension().customizeLinkPropertiesPanelLayout(), "[grow,fill]", "[grow,fill]");
        LAFOptions.getInstance().applyPanelBackground(this);
        pkgPanel = new FilePackagePropertiesPanel();
        linkPanel = new DownloadLinkPropertiesPanel();
        add(pkgPanel, "hidemode 3");
        add(linkPanel, "hidemode 3");
        pkgPanel.setVisible(false);
        linkPanel.setVisible(false);
        LAFOptions.getInstance().getExtension().customizeLinkPropertiesPanel(this);
    }

    @Override
    public Dimension getPreferredSize() {
        final Dimension ret = super.getPreferredSize();
        if (CFG_GUI.CFG.isPropertiesPanelHeightNormalized()) {
            ret.height = Math.max(pkgPanel.getPreferredSize().height, linkPanel.getPreferredSize().height);
        }
        return ret;
    }

    public void update(AbstractNode objectbyRow) {
        if (objectbyRow instanceof FilePackage) {
            final FilePackage pkg = (FilePackage) objectbyRow;
            linkPanel.setVisible(false);
            pkgPanel.setVisible(true);
            linkPanel.setSelectedItem(null);
            pkgPanel.setSelectedItem(pkg);
        } else if (objectbyRow instanceof DownloadLink) {
            final DownloadLink link = (DownloadLink) objectbyRow;
            linkPanel.setVisible(true);
            pkgPanel.setVisible(false);
            pkgPanel.setSelectedItem(null);
            linkPanel.setSelectedItem(link);
        } else {
            linkPanel.setVisible(false);
            pkgPanel.setVisible(false);
            linkPanel.setSelectedItem(null);
            pkgPanel.setSelectedItem(null);
        }
    }

    public void fillPopup(JPopupMenu pu) {
        if (linkPanel.isVisible()) {
            linkPanel.fillPopup(pu);
        } else if (pkgPanel.isVisible()) {
            pkgPanel.fillPopup(pu);
        }
    }

    public void refreshAfterTabSwitch() {
        pkgPanel.refresh();
        linkPanel.refresh();
    }

    public void save() {
        if (linkPanel.isVisible()) {
            linkPanel.save();
        } else if (pkgPanel.isVisible()) {
            pkgPanel.save();
        }
    }

}
