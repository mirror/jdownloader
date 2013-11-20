package jd.gui.swing.jdgui.views.settings.panels.basicauthentication;

import java.awt.event.ActionEvent;

import jd.controlling.TaskQueue;
import jd.controlling.authentication.AuthenticationController;
import jd.controlling.authentication.AuthenticationInfo;

import org.appwork.utils.event.queue.QueueAction;
import org.jdownloader.actions.AppAction;
import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.translate._GUI;

public class RemoveAction extends AppAction {
    /**
     * 
     */
    private static final long                  serialVersionUID = 1L;
    private AuthTable                          table;
    private java.util.List<AuthenticationInfo> selection        = null;
    private boolean                            ignoreSelection  = false;

    public RemoveAction(AuthTable table) {
        this.table = table;
        this.ignoreSelection = true;
        setName(_GUI._.literally_remove());
        setIconKey(IconKey.ICON_REMOVE);

    }

    public RemoveAction(AuthTable authTable, java.util.List<AuthenticationInfo> selection, boolean force) {
        this.table = authTable;
        this.selection = selection;
        setName(_GUI._.literally_remove());
        setIconKey(IconKey.ICON_REMOVE);

    }

    public void actionPerformed(ActionEvent e) {
        if (!isEnabled()) return;
        final java.util.List<AuthenticationInfo> finalSelection;
        if (selection == null) {
            finalSelection = ((AuthTableModel) table.getModel()).getSelectedObjects();
        } else {
            finalSelection = selection;
        }
        if (finalSelection != null && finalSelection.size() > 0) {
            TaskQueue.getQueue().add(new QueueAction<Void, RuntimeException>() {

                @Override
                protected Void run() throws RuntimeException {
                    AuthenticationController.getInstance().remove(finalSelection);
                    return null;
                }
            });
        }

    }

    @Override
    public boolean isEnabled() {
        if (ignoreSelection) return super.isEnabled();
        return selection != null && selection.size() > 0;
    }

}
