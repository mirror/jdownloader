package org.jdownloader.gui.views.linkgrabber.contextmenu;

import java.awt.event.ActionEvent;

import jd.controlling.IOEQ;
import jd.controlling.linkcollector.LinkCollector;
import jd.controlling.linkcrawler.CrawledLink;
import jd.controlling.linkcrawler.CrawledPackage;
import jd.controlling.linkcrawler.CrawledPackage.TYPE;
import jd.plugins.DownloadLink.AvailableStatus;

import org.appwork.uio.UIOManager;
import org.appwork.utils.swing.dialog.Dialog;
import org.appwork.utils.swing.dialog.DialogNoAnswerException;
import org.jdownloader.actions.AppAction;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.SelectionInfo;

public class RemoveSelectionLinkgrabberAction extends AppAction {

    /**
     * 
     */
    private static final long                          serialVersionUID = -3008851305036758872L;
    private SelectionInfo<CrawledPackage, CrawledLink> si;

    public RemoveSelectionLinkgrabberAction(SelectionInfo<CrawledPackage, CrawledLink> si) {
        setIconKey("remove");
        setName(_GUI._.RemoveSelectionLinkgrabberAction_RemoveSelectionLinkgrabberAction_object_());
        this.si = si;

    }

    public void actionPerformed(ActionEvent e) {
        if (!isEnabled()) return;
        try {
            boolean containsOnline = false;

            for (CrawledLink cl : si.getChildren()) {
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
            IOEQ.add(new Runnable() {

                public void run() {

                    LinkCollector.getInstance().removeChildren(si.getChildren());
                }

            }, true);
        } catch (DialogNoAnswerException e1) {
        }
    }

    @Override
    public boolean isEnabled() {
        return !si.isEmpty();
    }

}
