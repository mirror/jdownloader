package org.jdownloader.gui.views.downloads.properties;

import javax.swing.JPopupMenu;

import jd.controlling.packagecontroller.AbstractNode;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;

import org.appwork.swing.MigPanel;
import org.jdownloader.gui.components.CheckboxMenuItem;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.settings.staticreferences.CFG_GUI;

public class FilePackagePropertiesPanel extends DownloadListPropertiesPanel<FilePackageNodeProperties> {

    /**
     *
     */
    private static final long serialVersionUID = -4215628601799696315L;

    @Override
    protected void addFilename(int height, MigPanel p) {
    }

    @Override
    protected void addDownloadFrom(int height, MigPanel p) {
    }

    public void fillPopup(JPopupMenu pu) {
        pu.add(new CheckboxMenuItem(_GUI.T.LinkgrabberPropertiesHeader_saveto(), CFG_GUI.DOWNLOADS_PROPERTIES_PANEL_SAVE_TO_VISIBLE));
        pu.add(new CheckboxMenuItem(_GUI.T.LinkgrabberPropertiesHeader_packagename(), CFG_GUI.DOWNLOADS_PROPERTIES_PANEL_PACKAGENAME_VISIBLE));
        pu.add(new CheckboxMenuItem(_GUI.T.LinkgrabberPropertiesHeader_comment_and_priority(), CFG_GUI.DOWNLOADS_PROPERTIES_PANEL_COMMENT_VISIBLE));
        pu.add(new CheckboxMenuItem(_GUI.T.LinkgrabberPropertiesHeader_archiveline(), CFG_GUI.DOWNLOADS_PROPERTIES_PANEL_ARCHIVEPASSWORD_VISIBLE));
    }

    @Override
    protected void addChecksum(int height, MigPanel p) {
    }

    @Override
    protected void addDownloadPassword(int height, MigPanel p) {
    }

    @Override
    protected FilePackageNodeProperties createAbstractNodeProperties(AbstractNode abstractNode) {
        return new FilePackageNodeProperties((FilePackage) abstractNode);
    }

    @Override
    protected void refreshOnDownloadLinkUpdate(DownloadLink downloadLink) {
        final FilePackageNodeProperties current = getAbstractNodeProperties();
        if (current != null && current.samePackage(downloadLink.getFilePackage())) {
            refresh();
        }
    }

    @Override
    protected void refreshOnFilePackageUpdate(FilePackage pkg) {
        final FilePackageNodeProperties current = getAbstractNodeProperties();
        if (current != null && current.samePackage(pkg)) {
            refresh();
        }
    }
}
