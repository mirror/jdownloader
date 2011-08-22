package jd.gui.swing.jdgui.views.settings.panels.basicauthentication;

import java.awt.event.KeyEvent;
import java.util.ArrayList;

import javax.swing.JPopupMenu;

import jd.controlling.authentication.AuthenticationInfo;
import jd.gui.swing.jdgui.BasicJDTable;

import org.appwork.swing.exttable.ExtColumn;

public class AuthTable extends BasicJDTable<AuthenticationInfo> {
    private static final long serialVersionUID = 1L;

    public AuthTable() {
        super(new AuthTableModel());
        this.setSearchEnabled(true);
    }

    @Override
    protected JPopupMenu onContextMenu(JPopupMenu popup, AuthenticationInfo contextObject, ArrayList<AuthenticationInfo> selection, ExtColumn<AuthenticationInfo> col) {
        popup.add(new NewAction(this));
        popup.add(new RemoveAction(this, selection, false));
        return popup;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.appwork.swing.exttable.ExtTable#onShortcutDelete(java.util.ArrayList
     * , java.awt.event.KeyEvent, boolean)
     */
    @Override
    protected boolean onShortcutDelete(ArrayList<AuthenticationInfo> selectedObjects, KeyEvent evt, boolean direct) {
        new RemoveAction(this, selectedObjects, direct).actionPerformed(null);
        return true;
    }

}
