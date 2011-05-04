package jd.gui.swing.jdgui.views.settings.panels.basicauthentication;

import java.util.ArrayList;

import javax.swing.JPopupMenu;

import jd.controlling.authentication.AuthenticationInfo;
import jd.gui.swing.jdgui.views.settings.panels.components.SettingsTable;

public class AuthTable extends SettingsTable<AuthenticationInfo> {

    public AuthTable() {
        super(new AuthTableModel());
    }

    @Override
    protected JPopupMenu onContextMenu(JPopupMenu popup, AuthenticationInfo contextObject, ArrayList<AuthenticationInfo> selection) {

        popup.add(new NewAction(this));
        popup.add(new RemoveAction(this, selection));

        return popup;
    }

}
