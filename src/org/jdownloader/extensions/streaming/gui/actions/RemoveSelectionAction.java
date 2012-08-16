package org.jdownloader.extensions.streaming.gui.actions;

import java.awt.event.ActionEvent;
import java.util.ArrayList;

import jd.controlling.packagecontroller.AbstractNode;

import org.jdownloader.actions.AppAction;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.downloads.table.DownloadsTable;

public class RemoveSelectionAction extends AppAction {

    /**
     * 
     */
    private static final long       serialVersionUID = -3008851305036758872L;
    private ArrayList<AbstractNode> selection;
    private DownloadsTable          table;

    public RemoveSelectionAction(DownloadsTable table, ArrayList<AbstractNode> selection) {
        setIconKey("remove");
        setName(_GUI._.RemoveSelectionAction_RemoveSelectionAction_object_());
        this.selection = selection;
        this.table = table;
    }

    public void actionPerformed(ActionEvent e) {
        if (!isEnabled()) return;
        // try {
        // Dialog.getInstance().showConfirmDialog(Dialog.STYLE_SHOW_DO_NOT_DISPLAY_AGAIN
        // | Dialog.LOGIC_DONT_SHOW_AGAIN_IGNORES_CANCEL,
        // _GUI._.ClearAction_actionPerformed_(),
        // _GUI._.ClearAction_actionPerformed_selected_msg(), null,
        // _GUI._.literally_yes(), _GUI._.literall_no());
        //
        // IOEQ.add(new Runnable() {
        //
        // public void run() {
        // ArrayList<CrawledLink> remove =
        // LinkTreeUtils.getSelectedChildren(selection);
        // LinkCollector.getInstance().removeChildren(remove);
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
