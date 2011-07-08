package jd.gui.swing.jdgui.views.settings.panels.basicauthentication;

import java.awt.event.ActionEvent;
import java.util.ArrayList;

import javax.swing.AbstractAction;

import jd.controlling.IOEQ;
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
    private boolean                       ignoreSelection  = false;

    public RemoveAction(AuthTable table) {
        this.table = table;
        this.putValue(NAME, _GUI._.settings_auth_delete());
        this.putValue(AbstractAction.SMALL_ICON, NewTheme.I().getIcon("remove", 20));
        this.ignoreSelection = true;

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
        IOEQ.add(new Runnable() {
            public void run() {
                AuthenticationController.getInstance().remove(selection);
                table.getExtTableModel()._fireTableStructureChanged(AuthenticationController.getInstance().list(), false);
            }
        });

    }

    @Override
    public boolean isEnabled() {
        if (ignoreSelection) return super.isEnabled();
        return selection != null && selection.size() > 0;
    }

}
