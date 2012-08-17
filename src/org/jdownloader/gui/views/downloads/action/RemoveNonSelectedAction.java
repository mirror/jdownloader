package org.jdownloader.gui.views.downloads.action;

import java.awt.event.ActionEvent;
import java.util.ArrayList;

import jd.controlling.packagecontroller.AbstractNode;

import org.jdownloader.actions.AppAction;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.downloads.table.DownloadsTable;

public class RemoveNonSelectedAction extends AppAction {

    /**
     * 
     */
    private static final long       serialVersionUID = 6855083561629297363L;
    private java.util.List<AbstractNode> selection;
    private DownloadsTable          table;

    public RemoveNonSelectedAction(DownloadsTable table, java.util.List<AbstractNode> selection) {
        this.selection = selection;
        this.table = table;
        setName(_GUI._.RemoveNonSelectedAction_RemoveNonSelectedAction_object_());
        setIconKey("ok");
    }

    public void actionPerformed(ActionEvent e) {
        if (!isEnabled()) return;
        // try {
        // Dialog.getInstance().showConfirmDialog(Dialog.STYLE_SHOW_DO_NOT_DISPLAY_AGAIN
        // | Dialog.LOGIC_DONT_SHOW_AGAIN_IGNORES_CANCEL,
        // _GUI._.ClearAction_actionPerformed_(),
        // _GUI._.ClearAction_actionPerformed_notselected_msg(), null,
        // _GUI._.literally_yes(), _GUI._.literall_no());
        //
        // IOEQ.add(new Runnable() {
        //
        // public void run() {
        //
        // final java.util.List<CrawledLink> selected =
        // LinkTreeUtils.getSelectedChildren(selection);
        // List<CrawledLink> nonselected =
        // LinkCollector.getInstance().getChildrenByFilter(new
        // AbstractPackageChildrenNodeFilter<CrawledLink>() {
        //
        // public boolean isChildrenNodeFiltered(CrawledLink node) {
        // return !selected.contains(node);
        // }
        //
        // public int returnMaxResults() {
        // return -1;
        // }
        //
        // });
        // LinkCollector.getInstance().removeChildren(nonselected);
        // }
        //
        // }, true);
        // } catch (DialogNoAnswerException e1) {
        // }
    }

    @Override
    public boolean isEnabled() {
        return selection != null && selection.size() > 0;
    }

}
