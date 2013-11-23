package org.jdownloader.gui.views.downloads.action;

import java.util.List;

import jd.controlling.downloadcontroller.DownloadController;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;

import org.appwork.utils.event.queue.Queue;
import org.appwork.utils.event.queue.QueueAction;
import org.jdownloader.controlling.packagizer.PackagizerController;
import org.jdownloader.gui.views.SelectionInfo;
import org.jdownloader.gui.views.components.packagetable.context.SetDownloadFolderAction;

public class SetDownloadFolderInDownloadTableAction extends SetDownloadFolderAction<FilePackage, DownloadLink> {

    /**
     * 
     */

    public SetDownloadFolderInDownloadTableAction() {

    }

    public SetDownloadFolderInDownloadTableAction(SelectionInfo<FilePackage, DownloadLink> selectionInfo) {
        selection = selectionInfo;
    }

    @Override
    protected void move(final FilePackage pkg, final List<DownloadLink> selectedLinksByPackage) {
        final FilePackage source = getSelection().getPackageViews().get(0).getPackage();
        DownloadController.getInstance().getQueue().add(new QueueAction<Void, RuntimeException>() {

            @Override
            protected Void run() throws RuntimeException {
                int index = DownloadController.getInstance().indexOf(source);
                DownloadController.getInstance().moveOrAddAt(pkg, selectedLinksByPackage, -1, index);
                return null;
            }
        });

    }

    @Override
    protected FilePackage createNewByPrototype(SelectionInfo<FilePackage, DownloadLink> si, FilePackage entry) {
        final FilePackage pkg = FilePackage.getInstance();
        pkg.setExpanded(true);
        pkg.setCreated(System.currentTimeMillis());
        pkg.setName(entry.getName());
        pkg.setComment(entry.getComment());
        pkg.getProperties().putAll(entry.getProperties());
        return pkg;
    }

    @Override
    protected void set(FilePackage pkg, String absolutePath) {
        pkg.setDownloadDirectory(PackagizerController.replaceDynamicTags(absolutePath, pkg.getName()));
    }

    @Override
    protected Queue getQueue() {
        return DownloadController.getInstance().getQueue();
    }

}
