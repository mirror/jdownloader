package org.jdownloader.gui.views.downloads.properties;

import java.awt.event.ActionListener;
import java.util.List;

import javax.swing.JPopupMenu;

import jd.controlling.downloadcontroller.DownloadController;
import jd.controlling.packagecontroller.AbstractNode;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLinkProperty;
import jd.plugins.FilePackage;
import jd.plugins.FilePackageProperty;

import org.appwork.swing.components.ExtTextField;
import org.jdownloader.controlling.download.DownloadControllerListener;
import org.jdownloader.gui.components.CheckboxMenuItem;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.settings.staticreferences.CFG_GUI;

public abstract class DownloadListPropertiesPanel<E extends AbstractNodeProperties> extends AbstractNodePropertiesPanel<E> implements ActionListener, DownloadControllerListener {

    /**
     *
     */
    private static final long serialVersionUID = 1L;

    public DownloadListPropertiesPanel() {
        super();
        CFG_GUI.DOWNLOADS_PROPERTIES_PANEL_SAVE_TO_VISIBLE.getEventSender().addListener(this, true);
        CFG_GUI.DOWNLOADS_PROPERTIES_PANEL_FILENAME_VISIBLE.getEventSender().addListener(this, true);
        CFG_GUI.DOWNLOADS_PROPERTIES_PANEL_PACKAGENAME_VISIBLE.getEventSender().addListener(this, true);
        CFG_GUI.DOWNLOADS_PROPERTIES_PANEL_DOWNLOAD_FROM_VISIBLE.getEventSender().addListener(this, true);
        CFG_GUI.DOWNLOADS_PROPERTIES_PANEL_DOWNLOAD_PASSWORD_VISIBLE.getEventSender().addListener(this, true);
        CFG_GUI.DOWNLOADS_PROPERTIES_PANEL_CHECKSUM_VISIBLE.getEventSender().addListener(this, true);
        CFG_GUI.DOWNLOADS_PROPERTIES_PANEL_COMMENT_VISIBLE.getEventSender().addListener(this, true);
        CFG_GUI.DOWNLOADS_PROPERTIES_PANEL_ARCHIVEPASSWORD_VISIBLE.getEventSender().addListener(this, true);
    }

    @Override
    protected ExtTextField createFileNameTextField() {
        return new ExtTextField() {

            @Override
            public void onChanged() {
                // delayedSave();
            }

        };
    }

    public void fillPopup(JPopupMenu pu) {
        pu.add(new CheckboxMenuItem(_GUI.T.LinkgrabberPropertiesHeader_packagename(), CFG_GUI.DOWNLOADS_PROPERTIES_PANEL_PACKAGENAME_VISIBLE));
        pu.add(new CheckboxMenuItem(_GUI.T.LinkgrabberPropertiesHeader_filename(), CFG_GUI.DOWNLOADS_PROPERTIES_PANEL_FILENAME_VISIBLE));
        pu.add(new CheckboxMenuItem(_GUI.T.LinkgrabberPropertiesHeader_saveto(), CFG_GUI.DOWNLOADS_PROPERTIES_PANEL_SAVE_TO_VISIBLE));
        pu.add(new CheckboxMenuItem(_GUI.T.LinkgrabberPropertiesHeader_downloadfrom(), CFG_GUI.DOWNLOADS_PROPERTIES_PANEL_DOWNLOAD_FROM_VISIBLE));
        pu.add(new CheckboxMenuItem(_GUI.T.LinkgrabberPropertiesHeader_downloadpassword(), CFG_GUI.DOWNLOADS_PROPERTIES_PANEL_DOWNLOAD_PASSWORD_VISIBLE));
        pu.add(new CheckboxMenuItem(_GUI.T.LinkgrabberPropertiesHeader_checksum(), CFG_GUI.DOWNLOADS_PROPERTIES_PANEL_CHECKSUM_VISIBLE));
        pu.add(new CheckboxMenuItem(_GUI.T.LinkgrabberPropertiesHeader_comment_and_priority(), CFG_GUI.DOWNLOADS_PROPERTIES_PANEL_COMMENT_VISIBLE));
        pu.add(new CheckboxMenuItem(_GUI.T.LinkgrabberPropertiesHeader_archiveline(), CFG_GUI.DOWNLOADS_PROPERTIES_PANEL_ARCHIVEPASSWORD_VISIBLE));
    }

    protected boolean isArchiveLineEnabled() {
        return CFG_GUI.DOWNLOADS_PROPERTIES_PANEL_ARCHIVEPASSWORD_VISIBLE.isEnabled();
    }

    protected boolean isCheckSumEnabled() {
        return CFG_GUI.DOWNLOADS_PROPERTIES_PANEL_CHECKSUM_VISIBLE.isEnabled();
    }

    protected boolean isCommentAndPriorityEnabled() {
        return CFG_GUI.DOWNLOADS_PROPERTIES_PANEL_COMMENT_VISIBLE.isEnabled();
    }

    protected boolean isDownloadFromEnabled() {
        return CFG_GUI.DOWNLOADS_PROPERTIES_PANEL_DOWNLOAD_FROM_VISIBLE.isEnabled();
    }

    protected boolean isDownloadPasswordEnabled() {
        return CFG_GUI.DOWNLOADS_PROPERTIES_PANEL_DOWNLOAD_PASSWORD_VISIBLE.isEnabled();
    }

    protected boolean isFileNameEnabled() {
        return CFG_GUI.DOWNLOADS_PROPERTIES_PANEL_FILENAME_VISIBLE.isEnabled();
    }

    protected boolean isPackagenameEnabled() {
        return CFG_GUI.DOWNLOADS_PROPERTIES_PANEL_PACKAGENAME_VISIBLE.isEnabled();
    }

    protected boolean isSaveToEnabled() {
        return CFG_GUI.DOWNLOADS_PROPERTIES_PANEL_SAVE_TO_VISIBLE.isEnabled();
    }

    @Override
    public void onDownloadControllerAddedPackage(FilePackage pkg) {
    }

    @Override
    public void onDownloadControllerRemovedLinklist(List<DownloadLink> list) {
    }

    @Override
    public void onDownloadControllerRemovedPackage(FilePackage pkg) {
    }

    @Override
    public void onDownloadControllerStructureRefresh() {
        refresh();
    }

    @Override
    public void onDownloadControllerStructureRefresh(AbstractNode node, Object param) {
        if (node instanceof FilePackage) {
            refreshOnFilePackageUpdate((FilePackage) node);
        } else if (node instanceof DownloadLink) {
            refreshOnDownloadLinkUpdate((DownloadLink) node);
        }
    }

    @Override
    public void onDownloadControllerStructureRefresh(FilePackage pkg) {
        refreshOnFilePackageUpdate(pkg);
    }

    @Override
    public void onDownloadControllerUpdatedData(DownloadLink downloadlink) {
        refreshOnDownloadLinkUpdate(downloadlink);
    }

    @Override
    public void onDownloadControllerUpdatedData(DownloadLink downloadlink, DownloadLinkProperty property) {
        refreshOnDownloadLinkUpdate(downloadlink);
    }

    @Override
    public void onDownloadControllerUpdatedData(FilePackage pkg) {
        refreshOnFilePackageUpdate(pkg);
    }

    @Override
    public void onDownloadControllerUpdatedData(FilePackage pkg, FilePackageProperty property) {
        refreshOnFilePackageUpdate(pkg);
    }

    protected abstract void refreshOnDownloadLinkUpdate(DownloadLink downloadlink);

    protected abstract void refreshOnFilePackageUpdate(FilePackage pkg);

    @Override
    protected void onHidden() {
        try {
            super.onHidden();
        } finally {
            DownloadController.getInstance().getEventSender().removeListener(this);
        }
    }

    @Override
    protected void onShowing() {
        try {
            super.onShowing();
        } finally {
            DownloadController.getInstance().getEventSender().addListener(this, true);
        }
    }

}
