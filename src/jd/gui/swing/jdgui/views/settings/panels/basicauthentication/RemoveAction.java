package jd.gui.swing.jdgui.views.settings.panels.basicauthentication;

import java.awt.event.ActionEvent;
import java.util.ArrayList;

import javax.swing.AbstractAction;

import jd.controlling.authentication.AuthenticationController;
import jd.controlling.authentication.AuthenticationInfo;

import org.appwork.utils.swing.table.utils.MinimumSelectionObserver;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.NewTheme;

public class RemoveAction extends AbstractAction {
    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    private AuthTable         table;

    public RemoveAction(AuthTable table) {
        this.table = table;
        this.putValue(NAME, _GUI._.settings_auth_delete());
        this.putValue(AbstractAction.SMALL_ICON, NewTheme.I().getIcon("remove", 20));
        table.getSelectionModel().addListSelectionListener(new MinimumSelectionObserver(table, this, 1));

    }

    public RemoveAction(AuthTable authTable, ArrayList<AuthenticationInfo> selection) {
        this.table = authTable;
        this.putValue(NAME, _GUI._.settings_auth_delete());
        this.putValue(AbstractAction.SMALL_ICON, NewTheme.I().getIcon("remove", 20));
        this.setEnabled(selection != null && selection.size() > 0);

    }

    public void actionPerformed(ActionEvent e) {

        AuthenticationController.getInstance().remove(((AuthTableModel) table.getExtTableModel()).getSelectedObjects());
        ((AuthTableModel) table.getExtTableModel()).update();

    }

}
