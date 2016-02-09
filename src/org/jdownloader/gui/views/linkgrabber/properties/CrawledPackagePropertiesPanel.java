package org.jdownloader.gui.views.linkgrabber.properties;

import javax.swing.JPopupMenu;

import jd.controlling.linkcollector.LinkCollectorEvent;
import jd.controlling.linkcrawler.CrawledLink;
import jd.controlling.linkcrawler.CrawledPackage;
import jd.controlling.packagecontroller.AbstractNode;
import jd.controlling.packagecontroller.AbstractPackageNode;

import org.appwork.swing.MigPanel;
import org.jdownloader.gui.components.CheckboxMenuItem;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.settings.staticreferences.CFG_GUI;

public class CrawledPackagePropertiesPanel extends LinkcrawlerListPropertiesPanel<CrawledPackageNodeProperties> {

    @Override
    protected void addFilename(int height, MigPanel p) {
    }

    protected void addDownloadFrom(int height, MigPanel p) {
    }

    public void fillPopup(JPopupMenu pu) {
        pu.add(new CheckboxMenuItem(_GUI.T.LinkgrabberPropertiesHeader_saveto(), CFG_GUI.LINK_PROPERTIES_PANEL_SAVE_TO_VISIBLE));
        pu.add(new CheckboxMenuItem(_GUI.T.LinkgrabberPropertiesHeader_packagename(), CFG_GUI.LINK_PROPERTIES_PANEL_PACKAGENAME_VISIBLE));
        pu.add(new CheckboxMenuItem(_GUI.T.LinkgrabberPropertiesHeader_comment_and_priority(), CFG_GUI.LINK_PROPERTIES_PANEL_COMMENT_VISIBLE));
        pu.add(new CheckboxMenuItem(_GUI.T.LinkgrabberPropertiesHeader_archiveline(), CFG_GUI.LINK_PROPERTIES_PANEL_ARCHIVEPASSWORD_VISIBLE));
    }

    @Override
    protected void addChecksum(int height, MigPanel p) {
    }

    @Override
    protected void addDownloadPassword(int height, MigPanel p) {
    }

    @Override
    protected CrawledPackageNodeProperties createAbstractNodeProperties(AbstractNode abstractNode) {
        return new CrawledPackageNodeProperties((CrawledPackage) abstractNode);
    }

    @Override
    public void onLinkCollectorDataRefresh(LinkCollectorEvent event) {
        final CrawledPackageNodeProperties current = getAbstractNodeProperties();
        if (current != null) {
            if (event.getParameter() instanceof CrawledLink) {
                final CrawledLink crawledLink = (CrawledLink) event.getParameter();
                if (current.samePackage(crawledLink.getParentNode())) {
                    refresh();
                }
            } else if (event.getParameter() instanceof CrawledPackage) {
                if (current.samePackage((AbstractPackageNode) event.getParameter())) {
                    refresh();
                }
            }
        }
    }

}
