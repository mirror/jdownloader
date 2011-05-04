package jd.gui.swing.jdgui.views.settings.panels.basicauthentication;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

import jd.controlling.authentication.AuthenticationController;
import jd.controlling.authentication.AuthenticationInfo;

import org.jdownloader.gui.translate.T;
import org.jdownloader.images.Theme;

public class NewAction extends AbstractAction {
    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    private AuthTable         table;

    public NewAction(AuthTable table) {
        this.table = table;
        this.putValue(NAME, T._.settings_auth_add());
        this.putValue(AbstractAction.SMALL_ICON, Theme.getIcon("add", 20));
    }

    public void actionPerformed(ActionEvent e) {
        AuthenticationController.getInstance().add(new AuthenticationInfo());
        ((AuthTableModel) table.getExtTableModel()).update();
    }

}
