package org.jdownloader.gui.views.linkgrabber.properties;

import javax.swing.JPopupMenu;

import jd.controlling.linkcrawler.CrawledLink;

import org.appwork.swing.MigPanel;
import org.jdownloader.controlling.Priority;
import org.jdownloader.gui.components.CheckboxMenuItem;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.settings.staticreferences.CFG_GUI;

public class PackagePropertiesPanel extends LinkPropertiesPanel {

    @Override
    protected void addFilename(int height, MigPanel p) {

    }

    @Override
    protected void addDownloadFrom(int height, MigPanel p) {

    }

    public void fillPopup(JPopupMenu pu) {
        pu.add(new CheckboxMenuItem(_GUI._.LinkgrabberPropertiesHeader_saveto(), CFG_GUI.LINK_PROPERTIES_PANEL_SAVE_TO_VISIBLE));
        // pu.add(new CheckboxMenuItem(_GUI._.LinkgrabberPropertiesHeader_filename(), CFG_GUI.LINK_PROPERTIES_PANEL_FILENAME_VISIBLE));
        pu.add(new CheckboxMenuItem(_GUI._.LinkgrabberPropertiesHeader_packagename(), CFG_GUI.LINK_PROPERTIES_PANEL_PACKAGENAME_VISIBLE));
        // pu.add(new CheckboxMenuItem(_GUI._.LinkgrabberPropertiesHeader_downloadfrom(),
        // CFG_GUI.LINK_PROPERTIES_PANEL_DOWNLOAD_FROM_VISIBLE));
        // pu.add(new CheckboxMenuItem(_GUI._.LinkgrabberPropertiesHeader_downloadpassword(),
        // CFG_GUI.LINK_PROPERTIES_PANEL_DOWNLOAD_PASSWORD_VISIBLE));
        // pu.add(new CheckboxMenuItem(_GUI._.LinkgrabberPropertiesHeader_checksum(), CFG_GUI.LINK_PROPERTIES_PANEL_CHECKSUM_VISIBLE));
        pu.add(new CheckboxMenuItem(_GUI._.LinkgrabberPropertiesHeader_comment_and_priority(), CFG_GUI.LINK_PROPERTIES_PANEL_COMMENT_VISIBLE));
        pu.add(new CheckboxMenuItem(_GUI._.LinkgrabberPropertiesHeader_archiveline(), CFG_GUI.LINK_PROPERTIES_PANEL_ARCHIVEPASSWORD_VISIBLE));
    }

    @Override
    protected Priority loadPriority() {
        Priority p = currentPackage.getView().getHighestPriority();

        if (p != currentPackage.getView().getLowestPriority()) {

            return null;
        } else {
            return p;
        }

    }

    @Override
    protected void savePriority(Priority priop) {
        if (priop != null) {

            boolean readL = currentPackage.getModifyLock().readLock();
            try {
                for (CrawledLink dl : currentPackage.getChildren()) {
                    dl.setPriority(priop);
                }
            } finally {
                currentPackage.getModifyLock().readUnlock(readL);
            }

        }
    }

    @Override
    protected void saveComment(String text) {
        currentPackage.setComment(text);
    }

    @Override
    protected String loadComment() {
        return currentPackage.getComment();
    }

    @Override
    protected void addChecksum(int height, MigPanel p) {

    }

    @Override
    protected void addDownloadPassword(int height, MigPanel p) {

    }

}
