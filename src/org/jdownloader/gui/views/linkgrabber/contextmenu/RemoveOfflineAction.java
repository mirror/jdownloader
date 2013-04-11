package org.jdownloader.gui.views.linkgrabber.contextmenu;

import java.awt.event.ActionEvent;
import java.util.List;

import jd.controlling.IOEQ;
import jd.controlling.linkcollector.LinkCollector;
import jd.controlling.linkcrawler.CrawledLink;
import jd.controlling.linkcrawler.CrawledLink.LinkState;
import jd.controlling.packagecontroller.AbstractPackageChildrenNodeFilter;

import org.appwork.utils.swing.dialog.Dialog;
import org.appwork.utils.swing.dialog.DialogNoAnswerException;
import org.jdownloader.actions.AppAction;
import org.jdownloader.gui.translate._GUI;

public class RemoveOfflineAction extends AppAction {

    /**
     * 
     */
    private static final long serialVersionUID = -6341297356888158708L;

    public RemoveOfflineAction() {
        setName(_GUI._.RemoveOfflineAction_RemoveOfflineAction_object_());
        setIconKey("remove_offline");
    }

    public void actionPerformed(ActionEvent e) {
        try {
            Dialog.getInstance().showConfirmDialog(0, _GUI._.literally_are_you_sure(), _GUI._.ClearAction_actionPerformed_offline_msg(), null, _GUI._.literally_yes(), _GUI._.literall_no());
            IOEQ.add(new Runnable() {

                public void run() {
                    List<CrawledLink> offline = LinkCollector.getInstance().getChildrenByFilter(new AbstractPackageChildrenNodeFilter<CrawledLink>() {

                        public boolean acceptNode(CrawledLink node) {
                            return LinkState.OFFLINE.equals(node.getLinkState());
                        }

                        public int returnMaxResults() {
                            return -1;
                        }

                    });
                    LinkCollector.getInstance().removeChildren(offline);
                }

            }, true);
        } catch (DialogNoAnswerException e1) {
        }
    }

}
