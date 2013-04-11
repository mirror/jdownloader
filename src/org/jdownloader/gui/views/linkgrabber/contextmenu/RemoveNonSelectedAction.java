package org.jdownloader.gui.views.linkgrabber.contextmenu;

import java.awt.event.ActionEvent;
import java.util.List;

import jd.controlling.IOEQ;
import jd.controlling.linkcollector.LinkCollector;
import jd.controlling.linkcrawler.CrawledLink;
import jd.controlling.linkcrawler.CrawledPackage;
import jd.controlling.packagecontroller.AbstractPackageChildrenNodeFilter;

import org.appwork.utils.swing.dialog.Dialog;
import org.appwork.utils.swing.dialog.DialogNoAnswerException;
import org.jdownloader.actions.AppAction;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.SelectionInfo;

public class RemoveNonSelectedAction extends AppAction {

    /**
     * 
     */
    private static final long serialVersionUID = 6855083561629297363L;
    private List<CrawledLink> selection;

    public RemoveNonSelectedAction(SelectionInfo<CrawledPackage, CrawledLink> si) {
        this.selection = si.getSelectedChildren();
        setName(_GUI._.RemoveNonSelectedAction_RemoveNonSelectedAction_object_());
        setIconKey("ok");
    }

    public void actionPerformed(ActionEvent e) {
        if (!isEnabled()) return;
        try {
            Dialog.getInstance().showConfirmDialog(Dialog.STYLE_SHOW_DO_NOT_DISPLAY_AGAIN | Dialog.LOGIC_DONT_SHOW_AGAIN_IGNORES_CANCEL, _GUI._.literally_are_you_sure(), _GUI._.ClearAction_actionPerformed_notselected_msg(), null, _GUI._.literally_yes(), _GUI._.literall_no());

            IOEQ.add(new Runnable() {

                public void run() {

                    List<CrawledLink> nonselected = LinkCollector.getInstance().getChildrenByFilter(new AbstractPackageChildrenNodeFilter<CrawledLink>() {

                        public boolean acceptNode(CrawledLink node) {
                            return !selection.contains(node);
                        }

                        public int returnMaxResults() {
                            return -1;
                        }

                    });
                    LinkCollector.getInstance().removeChildren(nonselected);
                }

            }, true);
        } catch (DialogNoAnswerException e1) {
        }
    }

    @Override
    public boolean isEnabled() {
        return selection != null && selection.size() > 0;
    }

}
