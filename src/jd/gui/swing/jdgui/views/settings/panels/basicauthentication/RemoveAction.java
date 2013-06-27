package jd.gui.swing.jdgui.views.settings.panels.basicauthentication;

import java.awt.event.ActionEvent;
import java.util.ArrayList;

import jd.controlling.IOEQ;
import jd.controlling.authentication.AuthenticationController;
import jd.controlling.authentication.AuthenticationInfo;

import org.jdownloader.gui.views.components.AbstractRemoveAction;

public class RemoveAction extends AbstractRemoveAction {
    /**
     * 
     */
    private static final long             serialVersionUID = 1L;
    private AuthTable                     table;
    private java.util.List<AuthenticationInfo> selection        = null;
    private boolean                       ignoreSelection  = false;

    public RemoveAction(AuthTable table) {
        this.table = table;

        this.ignoreSelection = true;

    }

    public RemoveAction(AuthTable authTable, java.util.List<AuthenticationInfo> selection, boolean force) {
        this.table = authTable;
        this.selection = selection;
      
    }

    public void actionPerformed(ActionEvent e) {
        if (selection == null) {
            selection = ((AuthTableModel) table.getModel()).getSelectedObjects();
        }
        IOEQ.add(new Runnable() {
            public void run() {
                AuthenticationController.getInstance().remove(selection);
            }
        });

    }

    @Override
    public boolean isEnabled() {
        if (ignoreSelection) return super.isEnabled();
        return selection != null && selection.size() > 0;
    }

}
