package org.jdownloader.gui.views.linkgrabber.properties;

import java.io.File;
import java.util.List;

import javax.swing.JPopupMenu;

import jd.controlling.linkcrawler.CrawledLink;
import jd.controlling.linkcrawler.CrawledPackage;

import org.appwork.swing.MigPanel;
import org.jdownloader.controlling.Priority;
import org.jdownloader.extensions.extraction.Archive;
import org.jdownloader.extensions.extraction.contextmenu.downloadlist.ArchiveValidator;
import org.jdownloader.gui.components.CheckboxMenuItem;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.SelectionInfo;
import org.jdownloader.gui.views.linkgrabber.contextmenu.SetDownloadFolderInLinkgrabberAction;
import org.jdownloader.settings.staticreferences.CFG_GUI;

public class PackagePropertiesPanel extends LinkPropertiesPanel {

    @Override
    protected void addFilename(int height, MigPanel p) {

    }

    protected void addDownloadFrom(int height, MigPanel p) {

    }

    @Override
    protected List<Archive> loadArchives() {
        return ArchiveValidator.validate(new SelectionInfo<CrawledPackage, CrawledLink>(currentPackage, null, false));

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
    protected void saveSaveTo(final String str) {
        new SetDownloadFolderInLinkgrabberAction(new SelectionInfo<CrawledPackage, CrawledLink>(currentPackage, null, false)) {
            protected java.io.File dialog(java.io.File path) throws org.appwork.utils.swing.dialog.DialogClosedException, org.appwork.utils.swing.dialog.DialogCanceledException {

                return new File(str);
            };
        }.actionPerformed(null);

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
