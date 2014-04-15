package org.jdownloader.gui.views.downloads.properties;

import java.awt.event.ActionListener;
import java.io.File;
import java.util.List;

import javax.swing.Icon;
import javax.swing.JPopupMenu;

import jd.controlling.downloadcontroller.DownloadController;
import jd.controlling.downloadcontroller.DownloadWatchDog;
import jd.controlling.packagecontroller.AbstractNode;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLinkProperty;
import jd.plugins.FilePackage;
import jd.plugins.FilePackageProperty;
import jd.plugins.LinkStatusProperty;

import org.appwork.storage.config.events.GenericConfigEventListener;
import org.appwork.swing.components.ExtTextField;
import org.appwork.utils.ImageProvider.ImageProvider;
import org.appwork.utils.swing.EDTRunner;
import org.jdownloader.controlling.Priority;
import org.jdownloader.controlling.download.DownloadControllerListener;
import org.jdownloader.extensions.extraction.Archive;
import org.jdownloader.extensions.extraction.BooleanStatus;
import org.jdownloader.extensions.extraction.contextmenu.downloadlist.ArchiveValidator;
import org.jdownloader.gui.components.CheckboxMenuItem;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.SelectionInfo;
import org.jdownloader.gui.views.components.packagetable.LinkTreeUtils;
import org.jdownloader.gui.views.downloads.action.SetDownloadFolderInDownloadTableAction;
import org.jdownloader.settings.staticreferences.CFG_GUI;

public class DownloadLinkPropertiesPanel extends AbstractNodePropertiesPanel implements ActionListener, GenericConfigEventListener<Boolean>, DownloadControllerListener {

    protected volatile DownloadLink currentLink;
    protected volatile FilePackage  currentPackage;

    public DownloadLinkPropertiesPanel() {
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

            // protected boolean processKeyBinding(KeyStroke ks, KeyEvent e, int condition, boolean pressed) {
            // InputMap map = getInputMap(condition);
            // ActionMap am = getActionMap();
            //
            // if (map != null && am != null && isEnabled()) {
            // Object binding = map.get(ks);
            // Action action = (binding == null) ? null : am.get(binding);
            //
            // if (action != null) {
            //
            // if (action instanceof CopyAction) { return super.processKeyBinding(ks, e, condition, pressed); }
            // if ("select-all".equals(binding)) return super.processKeyBinding(ks, e, condition, pressed);
            // if (action instanceof TextAction) { return false; }
            //
            // }
            //
            // }
            // return super.processKeyBinding(ks, e, condition, pressed);
            // }
        };
    }

    public void fillPopup(JPopupMenu pu) {
        pu.add(new CheckboxMenuItem(_GUI._.LinkgrabberPropertiesHeader_packagename(), CFG_GUI.DOWNLOADS_PROPERTIES_PANEL_PACKAGENAME_VISIBLE));
        pu.add(new CheckboxMenuItem(_GUI._.LinkgrabberPropertiesHeader_filename(), CFG_GUI.DOWNLOADS_PROPERTIES_PANEL_FILENAME_VISIBLE));
        pu.add(new CheckboxMenuItem(_GUI._.LinkgrabberPropertiesHeader_saveto(), CFG_GUI.DOWNLOADS_PROPERTIES_PANEL_SAVE_TO_VISIBLE));
        pu.add(new CheckboxMenuItem(_GUI._.LinkgrabberPropertiesHeader_downloadfrom(), CFG_GUI.DOWNLOADS_PROPERTIES_PANEL_DOWNLOAD_FROM_VISIBLE));
        pu.add(new CheckboxMenuItem(_GUI._.LinkgrabberPropertiesHeader_downloadpassword(), CFG_GUI.DOWNLOADS_PROPERTIES_PANEL_DOWNLOAD_PASSWORD_VISIBLE));
        pu.add(new CheckboxMenuItem(_GUI._.LinkgrabberPropertiesHeader_checksum(), CFG_GUI.DOWNLOADS_PROPERTIES_PANEL_CHECKSUM_VISIBLE));
        pu.add(new CheckboxMenuItem(_GUI._.LinkgrabberPropertiesHeader_comment_and_priority(), CFG_GUI.DOWNLOADS_PROPERTIES_PANEL_COMMENT_VISIBLE));
        pu.add(new CheckboxMenuItem(_GUI._.LinkgrabberPropertiesHeader_archiveline(), CFG_GUI.DOWNLOADS_PROPERTIES_PANEL_ARCHIVEPASSWORD_VISIBLE));
    }

    protected Icon getHighestPackagePriorityIcon() {
        return ImageProvider.getDisabledIcon(currentPackage.getView().getHighestPriority().loadIcon(18));
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
    protected List<Archive> loadArchives() {
        return ArchiveValidator.validate(new SelectionInfo<FilePackage, DownloadLink>(currentLink, null, false));

    }

    @Override
    protected String loadComment() {
        return currentLink.getComment();
    }

    @Override
    protected String loadDownloadFrom() {
        DownloadLink dlLink = currentLink.getDownloadLink();
        if (dlLink.getLinkType() == DownloadLink.LINKTYPE_CONTAINER) {
            if (dlLink.gotBrowserUrl()) {
                return dlLink.getBrowserUrl();
            } else {
                return ("*******************************");
            }
        } else {
            return (dlLink.getBrowserUrl());
        }

    }

    @Override
    protected String loadDownloadPassword() {
        return currentLink.getDownloadPassword();
    }

    @Override
    protected String loadFilename() {
        return currentLink.getView().getDisplayName();
    }

    @Override
    protected String loadMD5() {
        return currentLink.getMD5Hash();
    }

    @Override
    protected String loadPackageName() {
        return currentPackage.getName();
    }

    @Override
    protected Priority loadPriority() {
        return currentLink.getPriorityEnum();
    }

    @Override
    protected String loadSaveTo() {
        return LinkTreeUtils.getRawDownloadDirectory(currentPackage).getAbsolutePath();
    }

    @Override
    protected String loadSha1() {
        return currentLink.getSha1Hash();
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
        refresh();
    }

    @Override
    public void onDownloadControllerStructureRefresh(FilePackage pkg) {
        if (pkg == currentPackage) {
            refresh();
        }
    }

    @Override
    public void onDownloadControllerUpdatedData(DownloadLink downloadlink) {
        refreshOnLinkUpdate(downloadlink);
    }

    @Override
    public void onDownloadControllerUpdatedData(DownloadLink downloadlink, DownloadLinkProperty property) {
        refreshOnLinkUpdate(downloadlink);
    }

    @Override
    public void onDownloadControllerUpdatedData(DownloadLink downloadlink, LinkStatusProperty property) {
        refreshOnLinkUpdate(downloadlink);
    }

    @Override
    public void onDownloadControllerUpdatedData(FilePackage pkg) {
        if (currentPackage == pkg) {
            refresh();
        }
    }

    @Override
    public void onDownloadControllerUpdatedData(FilePackage pkg, FilePackageProperty property) {
        if (currentPackage == pkg) {
            refresh();
        }
    }

    protected void refreshOnLinkUpdate(DownloadLink downloadlink) {
        if (downloadlink != null && currentLink != null && (currentLink == downloadlink || downloadlink.getFilePackage() == currentLink.getFilePackage())) {
            refresh();
        } else if (downloadlink != null && downloadlink.getParentNode() == currentPackage) {
            // example:package is selected,and we change the archive password. this fires events on the packages links
            refresh();
        }
    }

    @Override
    protected void saveArchivePasswords(List<String> hashSet) {
        if (currentArchive == null) return;
        currentArchive.getSettings().setPasswords(hashSet);
    }

    @Override
    protected void saveAutoExtract(BooleanStatus selectedItem) {
        if (currentArchive == null) return;
        currentArchive.getSettings().setAutoExtract(selectedItem);
    }

    @Override
    protected void saveComment(String text) {
        if (currentArchive == null) return;
        currentLink.getDownloadLink().setComment(text);
    }

    @Override
    protected void saveDownloadPassword(String text) {
        if (currentArchive == null) return;
        currentLink.getDownloadLink().setDownloadPassword(text);
    }

    @Override
    protected void saveFilename(String text) {
        DownloadWatchDog.getInstance().renameLink(currentLink, filename.getText());

    }

    @Override
    protected void saveMd5(String cs) {
        currentLink.getDownloadLink().setMD5Hash(cs);
    }

    @Override
    protected void savePackageName(String text) {
        currentPackage.setName(packagename.getText());
    }

    @Override
    protected void savePriority(Priority priop) {
        currentLink.setPriorityEnum(priop);
    }

    @Override
    protected void saveSaveTo(final String stringpath) {
        new SetDownloadFolderInDownloadTableAction(new SelectionInfo<FilePackage, DownloadLink>(currentLink, null, false)) {
            protected java.io.File dialog(java.io.File path) throws org.appwork.utils.swing.dialog.DialogClosedException, org.appwork.utils.swing.dialog.DialogCanceledException {
                return new File(stringpath);
            };
        }.actionPerformed(null);

        // currentPackage.setDownloadDirectory(PackagizerController.replaceDynamicTags(destination.getPath(), currentPackage.getName()));
    }

    @Override
    protected void saveSha1(String cs) {
        currentLink.getDownloadLink().setSha1Hash(cs);
    }

    public void setSelectedItem(final DownloadLink link) {
        new EDTRunner() {

            @Override
            protected void runInEDT() {
                boolean changesDetected = currentLink != link || (link != null && currentPackage != link.getParentNode());
                currentLink = link;
                if (link == null) {
                    currentPackage = null;
                } else {
                    currentPackage = link.getParentNode();
                }
                refresh(changesDetected);
            }
        };
    }

    public void setSelectedItem(final FilePackage pkg) {
        new EDTRunner() {

            @Override
            protected void runInEDT() {
                boolean changesDetected = currentPackage != pkg;
                currentLink = null;
                currentPackage = pkg;
                refresh(changesDetected);
            }
        };

    }

    @Override
    protected void onHidden() {
        super.onHidden();
        DownloadController.getInstance().getEventSender().removeListener(this);
    }

    @Override
    protected void onShowing() {
        super.onShowing();
        DownloadController.getInstance().getEventSender().addListener(this, true);
    }

    @Override
    protected boolean isAbstractNodeSelected() {
        return currentLink != null || currentPackage != null;
    }
}
