package org.jdownloader.gui.views.linkgrabber.contextmenu;

import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import jd.controlling.linkcollector.LinkCollector;
import jd.controlling.linkcrawler.CrawledLink;
import jd.controlling.linkcrawler.CrawledPackage;
import jd.controlling.linkcrawler.CrawledPackage.TYPE;
import jd.plugins.DownloadLink.AvailableStatus;

import org.appwork.uio.UIOManager;
import org.appwork.utils.event.queue.QueueAction;
import org.appwork.utils.swing.dialog.Dialog;
import org.appwork.utils.swing.dialog.DialogNoAnswerException;
import org.jdownloader.controlling.contextmenu.CustomizableSelectionAppAction;
import org.jdownloader.gui.translate._GUI;

public class RemoveSelectionLinkgrabberAction extends CustomizableSelectionAppAction<CrawledPackage, CrawledLink> {

    /**
     * 
     */
    private static final long serialVersionUID = -3008851305036758872L;

    public RemoveSelectionLinkgrabberAction() {
        super();
        setIconKey("remove");
        setName(_GUI._.RemoveSelectionLinkgrabberAction_RemoveSelectionLinkgrabberAction_object_());
        setAccelerator(KeyEvent.VK_DELETE);

    }

    public void actionPerformed(ActionEvent e) {
        if (!isEnabled()) {
            Toolkit.getDefaultToolkit().beep();
            return;
        }
        try {
            boolean containsOnline = false;

            for (CrawledLink cl : getSelection().getChildren()) {
                if (TYPE.OFFLINE == cl.getParentNode().getType()) continue;
                if (TYPE.POFFLINE == cl.getParentNode().getType()) continue;
                if (cl.getDownloadLink().getAvailableStatus() != AvailableStatus.FALSE) {
                    containsOnline = true;
                    break;
                }

            }

            if (containsOnline) {
                Dialog.getInstance().showConfirmDialog(Dialog.STYLE_SHOW_DO_NOT_DISPLAY_AGAIN | UIOManager.LOGIC_DONT_SHOW_AGAIN_IGNORES_CANCEL, _GUI._.literally_are_you_sure(), _GUI._.ClearAction_actionPerformed_selected_msg(), null, _GUI._.literally_yes(), _GUI._.literall_no());
            }
            LinkCollector.getInstance().getQueue().add(new QueueAction<Void, RuntimeException>() {
                @Override
                protected Void run() throws RuntimeException {
                    LinkCollector.getInstance().removeChildren(getSelection().getChildren());

                    return null;
                }
            });
        } catch (DialogNoAnswerException e1) {
        }
    }

}
