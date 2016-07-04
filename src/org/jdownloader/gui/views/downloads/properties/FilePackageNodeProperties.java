package org.jdownloader.gui.views.downloads.properties;

import java.io.File;
import java.util.List;

import jd.controlling.packagecontroller.AbstractNode;
import jd.controlling.packagecontroller.AbstractPackageNode;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.download.HashInfo;

import org.jdownloader.controlling.Priority;
import org.jdownloader.extensions.extraction.Archive;
import org.jdownloader.extensions.extraction.BooleanStatus;
import org.jdownloader.extensions.extraction.contextmenu.downloadlist.ArchiveValidator;
import org.jdownloader.gui.views.SelectionInfo;
import org.jdownloader.gui.views.components.packagetable.LinkTreeUtils;
import org.jdownloader.gui.views.downloads.action.SetDownloadFolderInDownloadTableAction;

public class FilePackageNodeProperties extends AbstractNodeProperties<FilePackage> {

    private final FilePackage currentPackage;
    private List<Archive>     archives = null;

    public FilePackageNodeProperties(FilePackage filePackage) {
        this.currentPackage = filePackage;
    }

    @Override
    protected String loadDownloadFrom() {
        return null;
    }

    @Override
    protected String loadDownloadPassword() {
        return null;
    }

    @Override
    protected List<Archive> loadArchives() {
        if (archives == null) {
            final FilePackage pkg = currentPackage;
            final boolean readL2 = pkg.getModifyLock().readLock();
            try {
                archives = ArchiveValidator.getArchivesFromPackageChildren(pkg.getChildren(), 2);
            } finally {
                pkg.getModifyLock().readUnlock(readL2);
            }
        }
        return archives;
    }

    @Override
    protected String loadFilename() {
        return null;
    }

    @Override
    protected String loadComment() {
        return currentPackage.getComment();
    }

    @Override
    protected Priority loadPriority() {
        return currentPackage.getPriorityEnum();
    }

    @Override
    protected String loadSaveTo() {
        return LinkTreeUtils.getRawDownloadDirectory(currentPackage).getAbsolutePath();
    }

    @Override
    protected void savePackageName(String text) {
        currentPackage.setName(text);
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
    protected void saveDownloadPassword(String text) {
    }

    @Override
    protected void saveFilename(String text) {
    }

    @Override
    protected void savePriority(Priority priop) {
        if (priop != null) {
            currentPackage.setPriorityEnum(priop);
        }
    }

    @Override
    protected void saveComment(String text) {
        currentPackage.setComment(text);
    }

    @Override
    protected void saveSaveTo(final String stringpath) {
        new SetDownloadFolderInDownloadTableAction(new SelectionInfo<FilePackage, DownloadLink>(currentPackage)) {

            private static final long serialVersionUID = 395350065855311575L;

            protected java.io.File dialog(java.io.File path) throws org.appwork.utils.swing.dialog.DialogClosedException, org.appwork.utils.swing.dialog.DialogCanceledException {

                return new File(stringpath);
            };
        }.actionPerformed(null);
    }

    @Override
    protected String loadPackageName() {
        return currentPackage.getName();
    }

    @Override
    protected boolean samePackage(AbstractPackageNode pkg) {
        return currentPackage == pkg;
    }

    @Override
    protected boolean isDifferent(AbstractNode node) {
        if (node != null && node instanceof AbstractPackageNode) {
            final AbstractPackageNode parent = (AbstractPackageNode) node;
            return currentPackage != parent;
        }
        return true;
    }

    @Override
    protected boolean hasLoadedArchives() {
        return archives != null;
    }

    @Override
    protected HashInfo loadHashInfo() {
        return null;
    }

    @Override
    protected void saveHashInfo(HashInfo hashInfo) {
    }

    @Override
    protected FilePackage getCurrentNode() {
        return currentPackage;
    }

}
