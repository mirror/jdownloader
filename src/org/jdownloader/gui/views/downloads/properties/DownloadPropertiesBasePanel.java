package org.jdownloader.gui.views.downloads.properties;

import java.awt.Dimension;

import javax.swing.JPopupMenu;

import jd.controlling.packagecontroller.AbstractNode;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;

import org.appwork.swing.MigPanel;
import org.jdownloader.gui.views.downloads.table.DownloadsTable;
import org.jdownloader.settings.staticreferences.CFG_GUI;
import org.jdownloader.updatev2.gui.LAFOptions;

public class DownloadPropertiesBasePanel extends MigPanel {

    /**
     * 
     */
    private static final long           serialVersionUID = -195024600818162517L;
    private DownloadsTable              table;
    private DownloadLinkPropertiesPanel pkgPanel;
    private DownloadLinkPropertiesPanel linkPanel;

    public DownloadPropertiesBasePanel(DownloadsTable table2) {
        super("ins 0", "[grow,fill]", "[grow,fill]");
        this.table = table2;
        LAFOptions.getInstance().applyPanelBackground(this);
        pkgPanel = new FilePackagePropertiesPanel();
        linkPanel = new DownloadLinkPropertiesPanel();
        add(pkgPanel, "hidemode 3");
        add(linkPanel, "hidemode 3");

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
        if (objectbyRow instanceof FilePackage) {
            FilePackage pkg = (FilePackage) objectbyRow;
            linkPanel.setVisible(false);
            pkgPanel.setVisible(true);
            pkgPanel.setSelectedItem(pkg);
        } else if (objectbyRow instanceof DownloadLink) {
            DownloadLink link = (DownloadLink) objectbyRow;
            linkPanel.setVisible(true);
            pkgPanel.setVisible(false);
            linkPanel.setSelectedItem(link);
        }
    }

    public void fillPopup(JPopupMenu pu) {

        if (linkPanel.isVisible()) {
            linkPanel.fillPopup(pu);
        } else {
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
        } else {
            pkgPanel.save();
        }
    }

}
