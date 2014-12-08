package org.jdownloader.gui.views.linkgrabber.actions;

import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import jd.controlling.linkcollector.LinkCollectingJob;

import org.appwork.uio.UIOManager;
import org.appwork.utils.swing.dialog.Dialog;
import org.appwork.utils.swing.dialog.DialogNoAnswerException;
import org.jdownloader.controlling.contextmenu.CustomizableAppAction;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.linkgrabber.addlinksdialog.AddLinksDialog;

public class AddLinksAction extends CustomizableAppAction {
    /**
     * 
     */
    private static final long serialVersionUID = -1824957567580275989L;

    public AddLinksAction(String string) {
        setName(string);
        setIconKey("add");
        setTooltipText(_GUI._.AddLinksAction_AddLinksAction_tt());
        setAccelerator(KeyEvent.VK_L, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask());
    }

    public AddLinksAction() {
        this(_GUI._.AddLinksToLinkgrabberAction());

    }

    @Override
    public void setEnabled(boolean newValue) {
        super.setEnabled(true);
    }

    public void actionPerformed(ActionEvent e) {
        new Thread("AddLinksAction") {
            public void run() {
                try {
                    AddLinksDialog dialog = new AddLinksDialog();
                    UIOManager.I().show(null, dialog);
                    dialog.throwCloseExceptions();
                    final LinkCollectingJob crawljob = dialog.getReturnValue();

                    AddLinksProgress d = new AddLinksProgress(crawljob);
                    if (d.isHiddenByDontShowAgain()) {
                        d.getAddLinksDialogThread(crawljob, null).start();
                    } else {
                        Dialog.getInstance().showDialog(d);
                    }

                } catch (DialogNoAnswerException e1) {
                }
            }
        }.start();

    }

}
