package org.jdownloader.gui.views.components.packagetable.context;

import java.awt.event.ActionEvent;
import java.io.File;
import java.util.List;

import jd.controlling.packagecontroller.AbstractPackageChildrenNode;
import jd.controlling.packagecontroller.AbstractPackageNode;

import org.appwork.utils.event.queue.Queue;
import org.appwork.utils.event.queue.QueueAction;
import org.appwork.utils.swing.dialog.Dialog;
import org.appwork.utils.swing.dialog.DialogCanceledException;
import org.appwork.utils.swing.dialog.DialogClosedException;
import org.appwork.utils.swing.dialog.DialogNoAnswerException;
import org.jdownloader.actions.AbstractSelectionContextAction;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.DownloadFolderChooserDialog;
import org.jdownloader.gui.views.SelectionInfo;
import org.jdownloader.gui.views.components.packagetable.LinkTreeUtils;
import org.jdownloader.translate._JDT;

public abstract class SetDownloadFolderAction<PackageType extends AbstractPackageNode<ChildrenType, PackageType>, ChildrenType extends AbstractPackageChildrenNode<PackageType>> extends AbstractSelectionContextAction<PackageType, ChildrenType> {

    private File path;

    public SetDownloadFolderAction(SelectionInfo<PackageType, ChildrenType> si) {
        super(si);
        setName(_GUI._.SetDownloadFolderAction_SetDownloadFolderAction_());
        setIconKey("save");
    }

    @Override
    public void setSelection(SelectionInfo<PackageType, ChildrenType> selection) {
        super.setSelection(selection);
        if (hasSelection()) {
            path = LinkTreeUtils.getRawDownloadDirectory(getSelection().getContextPackage());
            if (path.getName().equals(getSelection().getContextPackage().getName())) {
                path = new File(path.getParentFile(), DownloadFolderChooserDialog.PACKAGETAG);
            }
        }
    }

    @Override
    public boolean isEnabled() {
        return hasSelection();
    }

    /**
     * checks if the given file is valid as a downloadfolder, this means it must be an existing folder or at least its parent folder must
     * exist
     * 
     * @param file
     * @return
     */
    public static boolean isDownloadFolderValid(File file) {
        if (file == null || file.isFile()) return false;
        if (file.isDirectory()) return true;
        File parent = file.getParentFile();
        if (parent != null && parent.isDirectory() && parent.exists()) return true;
        return false;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (!isEnabled()) return;
        try {
            final File file = dialog(path);
            if (file == null) return;

            getQueue().add(new QueueAction<Object, RuntimeException>() {

                @Override
                protected Object run() {
                    for (PackageType pkg : getSelection().getFullPackages()) {
                        set(pkg, file.getAbsolutePath());
                    }
                    return null;
                }
            });
            for (final PackageType entry : getSelection().getIncompletePackages()) {
                try {
                    File oldPath = LinkTreeUtils.getDownloadDirectory(entry);
                    File newPath = file;
                    if (oldPath.equals(newPath)) continue;
                    Dialog.getInstance().showConfirmDialog(Dialog.LOGIC_DONOTSHOW_BASED_ON_TITLE_ONLY | Dialog.STYLE_SHOW_DO_NOT_DISPLAY_AGAIN, _JDT._.SetDownloadFolderAction_actionPerformed_(entry.getName()), _JDT._.SetDownloadFolderAction_msg(entry.getName(), getSelection().getSelectedLinksByPackage(entry).size()), null, _JDT._.SetDownloadFolderAction_yes(), _JDT._.SetDownloadFolderAction_no());
                    getQueue().add(new QueueAction<Object, RuntimeException>() {

                        @Override
                        protected Object run() {
                            set(entry, file.getAbsolutePath());
                            return null;
                        }
                    });
                    continue;
                } catch (DialogClosedException e1) {
                    return;
                } catch (DialogCanceledException e2) {
                    /* user clicked no */
                }
                getQueue().add(new QueueAction<Object, RuntimeException>() {

                    @Override
                    protected Object run() {
                        final PackageType pkg = createNewByPrototype(getSelection(), entry);
                        set(pkg, file.getAbsolutePath());
                        getQueue().add(new QueueAction<Object, RuntimeException>() {

                            @Override
                            protected Object run() {
                                move(pkg, getSelection().getSelectedLinksByPackage(entry));
                                return null;
                            }

                        });
                        return null;
                    }
                });
            }
        } catch (DialogNoAnswerException e1) {
        }
    }

    abstract protected Queue getQueue();

    protected File dialog(File path) throws DialogClosedException, DialogCanceledException {
        return DownloadFolderChooserDialog.open(path, true, _GUI._.OpenDownloadFolderAction_actionPerformed_object_(getSelection().getContextPackage().getName()));
    }

    abstract protected void move(PackageType pkg, List<ChildrenType> selectedLinksByPackage);

    abstract protected PackageType createNewByPrototype(SelectionInfo<PackageType, ChildrenType> si, PackageType entry);

    protected abstract void set(PackageType pkg, String absolutePath);

}
