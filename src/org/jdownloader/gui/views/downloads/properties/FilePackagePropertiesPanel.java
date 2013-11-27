package org.jdownloader.gui.views.downloads.properties;

import java.io.File;
import java.util.List;

import javax.swing.JPopupMenu;

import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;

import org.appwork.swing.MigPanel;
import org.jdownloader.controlling.Priority;
import org.jdownloader.extensions.extraction.Archive;
import org.jdownloader.extensions.extraction.contextmenu.downloadlist.ArchiveValidator;
import org.jdownloader.gui.components.CheckboxMenuItem;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.SelectionInfo;
import org.jdownloader.gui.views.downloads.action.SetDownloadFolderInDownloadTableAction;
import org.jdownloader.settings.staticreferences.CFG_GUI;

public class FilePackagePropertiesPanel extends DownloadLinkPropertiesPanel {

    @Override
    protected void addFilename(int height, MigPanel p) {

    }

    @Override
    protected void addDownloadFrom(int height, MigPanel p) {

    }

    @Override
    protected List<Archive> loadArchives() {
        return ArchiveValidator.validate(new SelectionInfo<FilePackage, DownloadLink>(currentPackage, null, false));

    }

    public void fillPopup(JPopupMenu pu) {
        pu.add(new CheckboxMenuItem(_GUI._.LinkgrabberPropertiesHeader_saveto(), CFG_GUI.DOWNLOADS_PROPERTIES_PANEL_SAVE_TO_VISIBLE));
        pu.add(new CheckboxMenuItem(_GUI._.LinkgrabberPropertiesHeader_packagename(), CFG_GUI.DOWNLOADS_PROPERTIES_PANEL_PACKAGENAME_VISIBLE));
        pu.add(new CheckboxMenuItem(_GUI._.LinkgrabberPropertiesHeader_comment_and_priority(), CFG_GUI.DOWNLOADS_PROPERTIES_PANEL_COMMENT_VISIBLE));
        pu.add(new CheckboxMenuItem(_GUI._.LinkgrabberPropertiesHeader_archiveline(), CFG_GUI.DOWNLOADS_PROPERTIES_PANEL_ARCHIVEPASSWORD_VISIBLE));
    }

    @Override
    protected void savePriority(Priority priop) {
        if (priop == null) return;
        boolean readL = currentPackage.getModifyLock().readLock();
        try {
            for (DownloadLink dl : currentPackage.getChildren()) {
                dl.setPriorityEnum(priop);
            }
        } finally {
            currentPackage.getModifyLock().readUnlock(readL);
        }
    }

    @Override
    protected void saveComment(String text) {
        currentPackage.setComment(text);
    }

    @Override
    protected void addChecksum(int height, MigPanel p) {

    }

    @Override
    protected void saveSaveTo(final String stringpath) {

        new SetDownloadFolderInDownloadTableAction(new SelectionInfo<FilePackage, DownloadLink>(currentPackage, null, false)) {
            protected java.io.File dialog(java.io.File path) throws org.appwork.utils.swing.dialog.DialogClosedException, org.appwork.utils.swing.dialog.DialogCanceledException {

                return new File(stringpath);
            };
        }.actionPerformed(null);

        // currentPackage.setDownloadDirectory(PackagizerController.replaceDynamicTags(destination.getPath(), currentPackage.getName()));
    }

    @Override
    protected void addDownloadPassword(int height, MigPanel p) {

    }

    @Override
    protected String loadComment() {
        return currentPackage.getComment();
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

}
