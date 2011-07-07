package jd.gui.swing.jdgui.views.settings.panels.basicauthentication;

import java.awt.event.ActionEvent;
import java.util.ArrayList;

import javax.swing.AbstractAction;

import jd.controlling.authentication.AuthenticationController;
import jd.controlling.authentication.AuthenticationInfo;

import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.NewTheme;

public class RemoveAction extends AbstractAction {
    /**
     * 
     */
    private static final long             serialVersionUID = 1L;
    private AuthTable                     table;
    private ArrayList<AuthenticationInfo> selection        = null;

    public RemoveAction(AuthTable table) {
        this.table = table;
        this.putValue(NAME, _GUI._.settings_auth_delete());
        this.putValue(AbstractAction.SMALL_ICON, NewTheme.I().getIcon("remove", 20));

    }

    public RemoveAction(AuthTable authTable, ArrayList<AuthenticationInfo> selection, boolean force) {
        this.table = authTable;
        this.selection = selection;
        this.putValue(NAME, _GUI._.settings_auth_delete());
        this.putValue(AbstractAction.SMALL_ICON, NewTheme.I().getIcon("remove", 16));
    }

    public void actionPerformed(ActionEvent e) {
        if (selection == null) {
            selection = ((AuthTableModel) table.getExtTableModel()).getSelectedObjects();
        }
        AuthenticationController.getInstance().remove(selection);
        /* WE ARE LAZY :) no eventsystem for this, so we update table ourselves */
        ((AuthTableModel) table.getExtTableModel()).update();
    }

    @Override
    public boolean isEnabled() {
        return selection != null && selection.size() > 0;
    }

}
