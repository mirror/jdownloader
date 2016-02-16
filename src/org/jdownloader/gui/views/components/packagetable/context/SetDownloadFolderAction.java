package org.jdownloader.gui.views.components.packagetable.context;

import java.awt.event.ActionEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import jd.controlling.packagecontroller.AbstractPackageChildrenNode;
import jd.controlling.packagecontroller.AbstractPackageNode;

import org.appwork.utils.event.queue.Queue;
import org.appwork.utils.event.queue.QueueAction;
import org.appwork.utils.swing.EDTRunner;
import org.appwork.utils.swing.dialog.Dialog;
import org.appwork.utils.swing.dialog.DialogCanceledException;
import org.appwork.utils.swing.dialog.DialogClosedException;
import org.appwork.utils.swing.dialog.DialogNoAnswerException;
import org.jdownloader.controlling.contextmenu.CustomizableTableContextAppAction;
import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.DownloadFolderChooserDialog;
import org.jdownloader.gui.views.SelectionInfo;
import org.jdownloader.gui.views.SelectionInfo.PackageView;
import org.jdownloader.gui.views.components.packagetable.LinkTreeUtils;
import org.jdownloader.translate._JDT;

public abstract class SetDownloadFolderAction<PackageType extends AbstractPackageNode<ChildrenType, PackageType>, ChildrenType extends AbstractPackageChildrenNode<PackageType>> extends CustomizableTableContextAppAction<PackageType, ChildrenType> {

    private File path;

    // protected SelectionInfo<PackageType, ChildrenType> selection;

    public SetDownloadFolderAction() {
        super();
        setName(_GUI.T.SetDownloadFolderAction_SetDownloadFolderAction_());
        setIconKey(IconKey.ICON_SAVE);
    }

    @Override
    public void requestUpdate(Object requestor) {
        super.requestUpdate(requestor);
        final SelectionInfo<PackageType, ChildrenType> lselection = getSelection();
        if (hasSelection(lselection)) {
            final PackageType cp = lselection.getFirstPackage();
            path = LinkTreeUtils.getRawDownloadDirectory(cp);
            if (path.getName().equals(cp.getName())) {
                path = new File(path.getParentFile(), DownloadFolderChooserDialog.PACKAGETAG);
            }
        }

    }

    /**
     * checks if the given file is valid as a downloadfolder, this means it must be an existing folder or at least its parent folder must
     * exist
     *
     * @param file
     * @return
     */
    public static boolean isDownloadFolderValid(File file) {
        if (file == null || file.isFile()) {
            return false;
        }
        if (file.isDirectory()) {
            return true;
        }
        File parent = file.getParentFile();
        if (parent != null && parent.isDirectory() && parent.exists()) {
            return true;
        }
        return false;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (!isEnabled()) {
            return;
        }
        try {
            final File file = dialog(path);
            if (file == null) {
                return;
            }
            final String dontShowAgainKey = getClass().getName();
            final SelectionInfo<PackageType, ChildrenType> lselection = getSelection();
            final List<PackageView<PackageType, ChildrenType>> incompletePackageViews = new ArrayList<PackageView<PackageType, ChildrenType>>(0);
            getQueue().add(new QueueAction<Object, RuntimeException>() {

                @Override
                protected Object run() {
                    for (final PackageView<PackageType, ChildrenType> packageView : lselection.getPackageViews()) {
                        if (lselection.isPackageSelectionComplete(packageView.getPackage())) {
                            set(packageView.getPackage(), file.getAbsolutePath());
                        } else {
                            incompletePackageViews.add(packageView);
                        }
                    }
                    new EDTRunner() {

                        @Override
                        protected void runInEDT() {
                            for (final PackageView<PackageType, ChildrenType> packageView : incompletePackageViews) {
                                final PackageType entry = packageView.getPackage();
                                try {
                                    File oldPath = LinkTreeUtils.getDownloadDirectory(entry);
                                    File newPath = file;
                                    if (oldPath.equals(newPath)) {
                                        continue;
                                    }
                                    Dialog.getInstance().showConfirmDialog(Dialog.LOGIC_DONOTSHOW_BASED_ON_TITLE_ONLY | Dialog.STYLE_SHOW_DO_NOT_DISPLAY_AGAIN, _JDT.T.SetDownloadFolderAction_actionPerformed_(entry.getName()), _JDT.T.SetDownloadFolderAction_msg(entry.getName(), packageView.getChildren().size()), null, _JDT.T.SetDownloadFolderAction_yes(), _JDT.T.SetDownloadFolderAction_no(), dontShowAgainKey);

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
                                        final PackageType pkg = createNewByPrototype(lselection, entry);
                                        set(pkg, file.getAbsolutePath());
                                        getQueue().add(new QueueAction<Object, RuntimeException>() {

                                            @Override
                                            protected Object run() {
                                                move(pkg, packageView.getChildren());
                                                return null;
                                            }

                                        });
                                        return null;
                                    }
                                });
                            }
                        }
                    };
                    return null;
                }
            });
        } catch (DialogNoAnswerException e1) {
        }
    }

    abstract protected Queue getQueue();

    protected File dialog(File path) throws DialogClosedException, DialogCanceledException {
        return DownloadFolderChooserDialog.open(path, true, _GUI.T.OpenDownloadFolderAction_actionPerformed_object_(getSelection().getFirstPackage().getName()));
    }

    abstract protected void move(PackageType pkg, List<ChildrenType> selectedLinksByPackage);

    abstract protected PackageType createNewByPrototype(SelectionInfo<PackageType, ChildrenType> si, PackageType entry);

    protected abstract void set(PackageType pkg, String absolutePath);

}
