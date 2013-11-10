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

import org.appwork.utils.StringUtils;
import org.appwork.utils.event.queue.QueueAction;
import org.appwork.utils.swing.dialog.Dialog;
import org.appwork.utils.swing.dialog.DialogCanceledException;
import org.appwork.utils.swing.dialog.DialogClosedException;
import org.jdownloader.controlling.contextmenu.CustomizableSelectionAppAction;
import org.jdownloader.gui.translate._GUI;

public class SetCommentAction<PackageType extends AbstractPackageNode<ChildrenType, PackageType>, ChildrenType extends AbstractPackageChildrenNode<PackageType>> extends CustomizableSelectionAppAction<PackageType, ChildrenType> {

    public SetCommentAction() {
        super();
        setName(_GUI._.SetCommentAction_SetCommentAction_object_());
        setIconKey("list");
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        String def = null;
        for (AbstractNode n : getSelection().getRawSelection()) {
            if (n instanceof DownloadLink) {
                def = ((DownloadLink) n).getComment();
            } else if (n instanceof CrawledLink) {
                def = ((CrawledLink) n).getDownloadLink().getComment();
            } else if (n instanceof FilePackage) {
                def = ((FilePackage) n).getComment();
            } else if (n instanceof CrawledPackage) {
                def = ((CrawledPackage) n).getComment();
            }
            if (!StringUtils.isEmpty(def)) break;
        }

        try {
            final String comment = Dialog.getInstance().showInputDialog(Dialog.STYLE_LARGE | Dialog.STYLE_HIDE_ICON, _GUI._.SetCommentAction_actionPerformed_dialog_title_(), "", def, null, null, null);
            TaskQueue.getQueue().add(new QueueAction<Void, RuntimeException>() {

                @Override
                protected Void run() throws RuntimeException {
                    for (AbstractNode n : getSelection().getRawSelection()) {
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
