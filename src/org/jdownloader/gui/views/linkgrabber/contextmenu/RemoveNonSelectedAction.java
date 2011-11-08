package org.jdownloader.gui.views.linkgrabber.contextmenu;

import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;

import jd.controlling.IOEQ;
import jd.controlling.linkcollector.LinkCollector;
import jd.controlling.linkcrawler.CrawledLink;
import jd.controlling.packagecontroller.AbstractNode;
import jd.controlling.packagecontroller.AbstractPackageChildrenNodeFilter;

import org.appwork.utils.swing.dialog.Dialog;
import org.appwork.utils.swing.dialog.DialogNoAnswerException;
import org.jdownloader.actions.AppAction;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.linkgrabber.LinkGrabberTable;

public class RemoveNonSelectedAction extends AppAction {

    /**
     * 
     */
    private static final long       serialVersionUID = 6855083561629297363L;
    private ArrayList<AbstractNode> selection;
    private LinkGrabberTable        table;

    public RemoveNonSelectedAction(LinkGrabberTable table, ArrayList<AbstractNode> selection) {
        this.selection = selection;
        this.table = table;
        setName(_GUI._.RemoveNonSelectedAction_RemoveNonSelectedAction_object_());
        setIconKey("ok");
    }

    public void actionPerformed(ActionEvent e) {
        if (!isEnabled()) return;
        try {
            Dialog.getInstance().showConfirmDialog(0, _GUI._.ClearAction_actionPerformed_(), _GUI._.ClearAction_actionPerformed_selected_msg(), null, _GUI._.literally_yes(), _GUI._.literall_no());

            IOEQ.add(new Runnable() {

                public void run() {
                    ArrayList<CrawledLink> selected = table.getAllSelectedChildren(selection);
                    List<CrawledLink> nonselected = LinkCollector.getInstance().getChildrenByFilter(new AbstractPackageChildrenNodeFilter<CrawledLink>() {

                        public boolean isChildrenNodeFiltered(CrawledLink node) {
                            return selection.contains(node);
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
