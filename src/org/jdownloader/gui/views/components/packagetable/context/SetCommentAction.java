package org.jdownloader.gui.views.components.packagetable.context;

import java.awt.event.ActionEvent;

import jd.controlling.TaskQueue;
import jd.controlling.linkcrawler.CrawledLink;
import jd.controlling.linkcrawler.CrawledPackage;
import jd.controlling.packagecontroller.AbstractNode;
import jd.controlling.packagecontroller.AbstractPackageChildrenNode;
import jd.controlling.packagecontroller.AbstractPackageNode;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;

import org.appwork.uio.InputDialogInterface;
import org.appwork.uio.UIOManager;
import org.appwork.utils.StringUtils;
import org.appwork.utils.event.queue.QueueAction;
import org.appwork.utils.swing.dialog.Dialog;
import org.appwork.utils.swing.dialog.DialogCanceledException;
import org.appwork.utils.swing.dialog.DialogClosedException;
import org.appwork.utils.swing.dialog.InputDialog;
import org.appwork.utils.swing.dialog.dimensor.RememberLastDialogDimension;
import org.appwork.utils.swing.dialog.locator.RememberAbsoluteDialogLocator;
import org.jdownloader.controlling.contextmenu.CustomizableTableContextAppAction;
import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.SelectionInfo;

public class SetCommentAction<PackageType extends AbstractPackageNode<ChildrenType, PackageType>, ChildrenType extends AbstractPackageChildrenNode<PackageType>> extends CustomizableTableContextAppAction<PackageType, ChildrenType> {
    public SetCommentAction() {
        super();
        setName(_GUI.T.SetCommentAction_SetCommentAction_object_());
        setIconKey(IconKey.ICON_LIST);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        String def = null;
        final SelectionInfo<PackageType, ChildrenType> selection = getSelection();
        for (AbstractNode n : selection.getRawSelection()) {
            if (n instanceof DownloadLink) {
                def = ((DownloadLink) n).getComment();
            } else if (n instanceof CrawledLink) {
                def = ((CrawledLink) n).getDownloadLink().getComment();
            } else if (n instanceof FilePackage) {
                def = ((FilePackage) n).getComment();
            } else if (n instanceof CrawledPackage) {
                def = ((CrawledPackage) n).getComment();
            }
            if (!StringUtils.isEmpty(def)) {
                break;
            }
        }
        try {
            final InputDialog dialog = new InputDialog(Dialog.STYLE_LARGE | Dialog.STYLE_HIDE_ICON, _GUI.T.SetCommentAction_actionPerformed_dialog_title_(), "", def, null, null, null);
            dialog.setLocator(new RememberAbsoluteDialogLocator("SetCommentAction"));
            dialog.setDimensor(new RememberLastDialogDimension("SetCommentAction"));
            final InputDialogInterface d = UIOManager.I().show(InputDialogInterface.class, dialog);
            d.throwCloseExceptions();
            final String comment = d.getText();
            TaskQueue.getQueue().add(new QueueAction<Void, RuntimeException>() {
                @Override
                protected Void run() throws RuntimeException {
                    for (AbstractNode n : selection.getRawSelection()) {
                        if (n instanceof DownloadLink) {
                            ((DownloadLink) n).setComment(comment);
                        } else if (n instanceof CrawledLink) {
                            ((CrawledLink) n).getDownloadLink().setComment(comment);
                        } else if (n instanceof FilePackage) {
                            ((FilePackage) n).setComment(comment);
                        } else if (n instanceof CrawledPackage) {
                            ((CrawledPackage) n).setComment(comment);
                        }
                    }
                    return null;
                }
            });
        } catch (DialogClosedException e1) {
        } catch (DialogCanceledException e1) {
        }
    }
}
