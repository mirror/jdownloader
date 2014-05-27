package org.jdownloader.gui.views.linkgrabber.properties;

import java.awt.event.ActionEvent;
import java.io.File;
import java.util.List;

import javax.swing.Icon;
import javax.swing.JPopupMenu;

import jd.controlling.linkcollector.LinkCollector;
import jd.controlling.linkcollector.LinkCollectorCrawler;
import jd.controlling.linkcollector.LinkCollectorEvent;
import jd.controlling.linkcollector.LinkCollectorListener;
import jd.controlling.linkcrawler.CrawledLink;
import jd.controlling.linkcrawler.CrawledPackage;
import jd.plugins.DownloadLink;

import org.appwork.utils.ImageProvider.ImageProvider;
import org.appwork.utils.swing.EDTRunner;
import org.jdownloader.controlling.Priority;
import org.jdownloader.extensions.extraction.Archive;
import org.jdownloader.extensions.extraction.BooleanStatus;
import org.jdownloader.extensions.extraction.contextmenu.downloadlist.ArchiveValidator;
import org.jdownloader.gui.components.CheckboxMenuItem;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.SelectionInfo;
import org.jdownloader.gui.views.components.packagetable.LinkTreeUtils;
import org.jdownloader.gui.views.downloads.properties.AbstractNodePropertiesPanel;
import org.jdownloader.gui.views.linkgrabber.contextmenu.SetDownloadFolderInLinkgrabberAction;
import org.jdownloader.settings.staticreferences.CFG_GUI;

public class LinkPropertiesPanel extends AbstractNodePropertiesPanel implements LinkCollectorListener {

    protected volatile CrawledLink currentLink;

    //
    // protected void saveInEDT() {
    // if (currentPackage != null) {
    // if (priority.isShowing()) {
    // Priority priop = priority.getSelectedItem();
    // currentLink.setPriority(priop);
    // }
    // if (comment.isShowing()) {
    // currentLink.getDownloadLink().setComment(comment.getText());
    // }
    // if (filename.isShowing()) {
    // currentLink.setName(filename.getText());
    // }
    // if (downloadpassword.isShowing()) {
    // currentLink.getDownloadLink().setDownloadPassword(downloadpassword.getText());
    // }
    // if (checksum.isShowing()) {
    // String cs = checksum.getText();
    // cs = cs.replaceAll("\\[.*?\\]", "").trim();
    // if (cs.length() == 32) {
    // currentLink.getDownloadLink().setMD5Hash(cs);
    // } else if (cs.length() == 40) {
    // currentLink.getDownloadLink().setSha1Hash(cs);
    // } else {
    // currentLink.getDownloadLink().setMD5Hash(null);
    // currentLink.getDownloadLink().setSha1Hash(null);
    // }
    // }
    // if (packagename.isShowing()) {
    // if (!currentPackage.getName().equals(packagename.getText())) {
    // currentPackage.setName(packagename.getText());
    // PackageHistoryManager.getInstance().add(packagename.getText());
    // }
    // }
    //
    // if (password.isShowing()) {
    // if (currentArchive != null) {
    // System.out.println("SAVE");
    // ArrayList<String> passwords = null;
    // String txt = password.getText().trim();
    // if (txt.startsWith("[") && txt.endsWith("]")) {
    // passwords = JSonStorage.restoreFromString(password.getText(), new TypeRef<ArrayList<String>>() {
    // }, null);
    // }
    // if (passwords != null && passwords.size() > 0) {
    // currentArchive.getSettings().setPasswords(new HashSet<String>(passwords));
    // } else {
    // HashSet<String> hs = new HashSet<String>();
    // if (StringUtils.isNotEmpty(password.getText())) hs.add(password.getText().trim());
    // currentArchive.getSettings().setPasswords(hs);
    // }
    // currentArchive.getSettings().setAutoExtract(autoExtract.getSelectedItem());
    //
    // if (!LinkTreeUtils.getRawDownloadDirectory(currentPackage).equals(new File(destination.getPath()))) {
    // currentPackage.setDownloadFolder(destination.getPath());
    // DownloadPathHistoryManager.getInstance().add(destination.getPath());
    // }
    // }
    // }
    //
    // }
    // }
    @Override
    protected void saveSaveTo(final String str) {
        new SetDownloadFolderInLinkgrabberAction(new SelectionInfo<CrawledPackage, CrawledLink>(currentLink, null, false)) {
            protected java.io.File dialog(java.io.File path) throws org.appwork.utils.swing.dialog.DialogClosedException, org.appwork.utils.swing.dialog.DialogCanceledException {

                return new File(str);
            };
        }.actionPerformed(null);

    }

    protected volatile CrawledPackage currentPackage;

    public LinkPropertiesPanel() {
        super();
        CFG_GUI.LINK_PROPERTIES_PANEL_SAVE_TO_VISIBLE.getEventSender().addListener(this, true);
        CFG_GUI.LINK_PROPERTIES_PANEL_FILENAME_VISIBLE.getEventSender().addListener(this, true);
        CFG_GUI.LINK_PROPERTIES_PANEL_PACKAGENAME_VISIBLE.getEventSender().addListener(this, true);
        CFG_GUI.LINK_PROPERTIES_PANEL_DOWNLOAD_FROM_VISIBLE.getEventSender().addListener(this, true);
        CFG_GUI.LINK_PROPERTIES_PANEL_DOWNLOAD_PASSWORD_VISIBLE.getEventSender().addListener(this, true);
        CFG_GUI.LINK_PROPERTIES_PANEL_CHECKSUM_VISIBLE.getEventSender().addListener(this, true);
        CFG_GUI.LINK_PROPERTIES_PANEL_COMMENT_VISIBLE.getEventSender().addListener(this, true);
        CFG_GUI.LINK_PROPERTIES_PANEL_ARCHIVEPASSWORD_VISIBLE.getEventSender().addListener(this, true);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        refresh();
    }

    public void fillPopup(JPopupMenu pu) {
        pu.add(new CheckboxMenuItem(_GUI._.LinkgrabberPropertiesHeader_packagename(), CFG_GUI.LINK_PROPERTIES_PANEL_PACKAGENAME_VISIBLE));

        pu.add(new CheckboxMenuItem(_GUI._.LinkgrabberPropertiesHeader_filename(), CFG_GUI.LINK_PROPERTIES_PANEL_FILENAME_VISIBLE));
        pu.add(new CheckboxMenuItem(_GUI._.LinkgrabberPropertiesHeader_saveto(), CFG_GUI.LINK_PROPERTIES_PANEL_SAVE_TO_VISIBLE));
        pu.add(new CheckboxMenuItem(_GUI._.LinkgrabberPropertiesHeader_downloadfrom(), CFG_GUI.LINK_PROPERTIES_PANEL_DOWNLOAD_FROM_VISIBLE));
        pu.add(new CheckboxMenuItem(_GUI._.LinkgrabberPropertiesHeader_downloadpassword(), CFG_GUI.LINK_PROPERTIES_PANEL_DOWNLOAD_PASSWORD_VISIBLE));
        pu.add(new CheckboxMenuItem(_GUI._.LinkgrabberPropertiesHeader_checksum(), CFG_GUI.LINK_PROPERTIES_PANEL_CHECKSUM_VISIBLE));
        pu.add(new CheckboxMenuItem(_GUI._.LinkgrabberPropertiesHeader_comment_and_priority(), CFG_GUI.LINK_PROPERTIES_PANEL_COMMENT_VISIBLE));
        pu.add(new CheckboxMenuItem(_GUI._.LinkgrabberPropertiesHeader_archiveline(), CFG_GUI.LINK_PROPERTIES_PANEL_ARCHIVEPASSWORD_VISIBLE));
    }

    @Override
    protected Icon getHighestPackagePriorityIcon() {
        return ImageProvider.getDisabledIcon(currentPackage.getView().getHighestPriority().loadIcon(18));
    }

    @Override
    protected boolean isArchiveLineEnabled() {
        return CFG_GUI.LINK_PROPERTIES_PANEL_ARCHIVEPASSWORD_VISIBLE.isEnabled();
    }

    @Override
    protected boolean isCheckSumEnabled() {
        return CFG_GUI.LINK_PROPERTIES_PANEL_CHECKSUM_VISIBLE.isEnabled();
    }

    @Override
    protected boolean isCommentAndPriorityEnabled() {
        return CFG_GUI.LINK_PROPERTIES_PANEL_COMMENT_VISIBLE.isEnabled();
    }

    @Override
    protected boolean isDownloadFromEnabled() {
        return CFG_GUI.LINK_PROPERTIES_PANEL_DOWNLOAD_FROM_VISIBLE.isEnabled();
    }

    @Override
    protected boolean isDownloadPasswordEnabled() {
        return CFG_GUI.LINK_PROPERTIES_PANEL_DOWNLOAD_PASSWORD_VISIBLE.isEnabled();
    }

    @Override
    protected boolean isFileNameEnabled() {
        return CFG_GUI.LINK_PROPERTIES_PANEL_FILENAME_VISIBLE.isEnabled();
    }

    @Override
    protected boolean isPackagenameEnabled() {
        return CFG_GUI.LINK_PROPERTIES_PANEL_PACKAGENAME_VISIBLE.isEnabled();
    }

    @Override
    protected boolean isSaveToEnabled() {
        return CFG_GUI.LINK_PROPERTIES_PANEL_SAVE_TO_VISIBLE.isEnabled();
    }

    @Override
    protected List<Archive> loadArchives() {
        return ArchiveValidator.validate(new SelectionInfo<CrawledPackage, CrawledLink>(currentLink, null, false));

    }

    @Override
    protected String loadComment() {
        return currentLink.getDownloadLink().getComment();
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
        return currentLink.getDownloadLink().getDownloadPassword();
    }

    @Override
    protected String loadFilename() {
        return currentLink.getName();
    }

    @Override
    protected String loadMD5() {
        return currentLink.getDownloadLink().getMD5Hash();
    }

    @Override
    protected String loadPackageName() {
        return currentPackage.getName();
    }

    @Override
    protected Priority loadPriority() {
        return currentLink.getPriority();
    }

    @Override
    protected String loadSaveTo() {
        return LinkTreeUtils.getRawDownloadDirectory(currentPackage).getAbsolutePath();
    }

    @Override
    protected String loadSha1() {
        return currentLink.getDownloadLink().getSha1Hash();
    }

    @Override
    public void onLinkCollectorAbort(LinkCollectorEvent event) {
    }

    @Override
    public void onLinkCollectorContentAdded(LinkCollectorEvent event) {
    }

    @Override
    public void onLinkCollectorContentRemoved(LinkCollectorEvent event) {
    }

    @Override
    public void onLinkCollectorDataRefresh(LinkCollectorEvent event) {
        //
        if (event.getParameter() == currentPackage || event.getParameter() == currentLink) {
            refresh();
        } else if (event.getParameter() instanceof CrawledLink && ((CrawledLink) event.getParameter()).getParentNode() == currentPackage) {
            // example:package is selected,and we change the archive password. this fires events on the packages links
            refresh();
        }
        ;
    }

    @Override
    public void onLinkCollectorDupeAdded(LinkCollectorEvent event, CrawledLink parameter) {
    }

    @Override
    public void onLinkCollectorFilteredLinksAvailable(LinkCollectorEvent event) {
    }

    @Override
    public void onLinkCollectorFilteredLinksEmpty(LinkCollectorEvent event) {
    }

    @Override
    public void onLinkCollectorLinkAdded(LinkCollectorEvent event, CrawledLink parameter) {
    }

    @Override
    public void onLinkCollectorStructureRefresh(LinkCollectorEvent event) {
        refresh();
    }

    @Override
    public void onLinkCrawlerAdded(LinkCollectorCrawler parameter) {
    }

    @Override
    public void onLinkCrawlerStarted(LinkCollectorCrawler parameter) {
    }

    @Override
    public void onLinkCrawlerStopped(LinkCollectorCrawler parameter) {
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
        currentLink.getDownloadLink().setComment(text);
    }

    @Override
    protected void saveDownloadPassword(String text) {
        currentLink.getDownloadLink().setDownloadPassword(text);
    }

    @Override
    protected void saveFilename(String text) {
        currentLink.setName(filename.getText());
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
        currentLink.setPriority(priop);
    }

    @Override
    protected void saveSha1(String cs) {
        currentLink.getDownloadLink().setSha1Hash(cs);
    }

    public void setSelectedItem(final CrawledLink link) {
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

    public void setSelectedItem(final CrawledPackage pkg) {
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
        LinkCollector.getInstance().getEventsender().removeListener(this);

    }

    @Override
    protected void onShowing() {
        super.onShowing();
        // remove Listeners to avoid dupes

        LinkCollector.getInstance().getEventsender().addListener(this, true);

    }

    @Override
    protected boolean isAbstractNodeSelected() {
        return currentLink != null || currentPackage != null;
    }
}
