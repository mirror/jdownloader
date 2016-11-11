package org.jdownloader.gui.views.linkgrabber.properties;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import jd.controlling.linkcrawler.CrawledLink;
import jd.controlling.linkcrawler.CrawledPackage;
import jd.controlling.packagecontroller.AbstractNode;
import jd.controlling.packagecontroller.AbstractPackageChildrenNode;
import jd.controlling.packagecontroller.AbstractPackageNode;
import jd.plugins.download.HashInfo;

import org.appwork.storage.config.JsonConfig;
import org.jdownloader.controlling.Priority;
import org.jdownloader.extensions.extraction.Archive;
import org.jdownloader.extensions.extraction.BooleanStatus;
import org.jdownloader.extensions.extraction.contextmenu.downloadlist.ArchiveValidator;
import org.jdownloader.gui.views.SelectionInfo;
import org.jdownloader.gui.views.components.packagetable.LinkTreeUtils;
import org.jdownloader.gui.views.downloads.properties.AbstractNodeProperties;
import org.jdownloader.gui.views.linkgrabber.contextmenu.SetDownloadFolderInLinkgrabberAction;
import org.jdownloader.settings.GeneralSettings;

public class CrawledLinkNodeProperties extends AbstractNodeProperties<CrawledLink> {

    private final CrawledLink    currentLink;
    private final CrawledPackage currentPackage;
    private List<Archive>        archives = null;

    public CrawledLinkNodeProperties(CrawledLink crawledLink) {
        this.currentLink = crawledLink;
        this.currentPackage = crawledLink.getParentNode();
    }

    @Override
    protected List<Archive> loadArchives() {
        if (archives == null) {
            final ArrayList<CrawledLink> children = new ArrayList<CrawledLink>();
            final CrawledLink lCurrentLink = currentLink;
            if (lCurrentLink != null) {
                children.add(lCurrentLink);
            }
            archives = ArchiveValidator.getArchivesFromPackageChildren(children);
        }
        return archives;
    }

    @Override
    protected String loadComment() {
        return currentLink.getDownloadLink().getComment();
    }

    @Override
    protected String loadDownloadFrom() {
        final String dlLink = currentLink.getDownloadLink().getView().getDisplayUrl();
        if (dlLink == null) {
            return "*******************************";
        }
        return dlLink;
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
    protected Priority loadPriority() {
        return currentLink.getPriority();
    }

    @Override
    protected String loadSaveTo() {
        if (currentPackage != null) {
            return LinkTreeUtils.getRawDownloadDirectory(currentPackage).getAbsolutePath();
        } else {
            return JsonConfig.create(GeneralSettings.class).getDefaultDownloadFolder();
        }
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
        currentLink.setName(text);
    }

    @Override
    protected void savePriority(Priority priop) {
        currentLink.setPriority(priop);
    }

    @Override
    protected boolean samePackage(AbstractPackageNode pkg) {
        return currentPackage == pkg;
    }

    @Override
    protected boolean isDifferent(AbstractNode node) {
        if (node != null && node instanceof AbstractPackageChildrenNode) {
            final AbstractPackageChildrenNode child = (AbstractPackageChildrenNode) node;
            if (node == currentLink.getDownloadLink()) {
                return false;
            } else {
                return currentLink != child || child.getParentNode() != currentPackage;
            }
        }
        return true;
    }

    @Override
    protected String loadPackageName() {
        if (currentPackage != null) {
            return currentPackage.getName();
        } else {
            return "";
        }
    }

    @Override
    protected void savePackageName(String text) {
        if (currentPackage != null) {
            currentPackage.setName(text);
        }
    }

    @Override
    protected void saveSaveTo(final String str) {
        if (currentPackage != null) {
            new SetDownloadFolderInLinkgrabberAction(new SelectionInfo<CrawledPackage, CrawledLink>(currentPackage)) {
                /**
                 *
                 */
                private static final long serialVersionUID = -7244902643764170242L;

                protected java.io.File dialog(java.io.File path) throws org.appwork.utils.swing.dialog.DialogClosedException, org.appwork.utils.swing.dialog.DialogCanceledException {

                    return new File(str);
                };
            }.actionPerformed(null);
        }
    }

    @Override
    protected void saveArchivePasswords(List<String> hashSet) {
        if (archives != null && archives.size() == 1) {
            archives.get(0).getSettings().setPasswords(hashSet);
        }
    }

    @Override
    protected void saveAutoExtract(BooleanStatus selectedItem) {
        if (archives != null && archives.size() == 1) {
            archives.get(0).getSettings().setAutoExtract(selectedItem);
        }
    }

    @Override
    protected boolean hasLoadedArchives() {
        return archives != null;
    }

    @Override
    protected HashInfo loadHashInfo() {
        return currentLink.getDownloadLink().getHashInfo();
    }

    @Override
    protected void saveHashInfo(HashInfo hashInfo) {
        currentLink.getDownloadLink().setHashInfo(hashInfo);
    }

    @Override
    protected CrawledLink getCurrentNode() {
        return currentLink;
    }

}
