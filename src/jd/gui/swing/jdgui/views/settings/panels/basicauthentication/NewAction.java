package jd.gui.swing.jdgui.views.settings.panels.basicauthentication;

import java.awt.event.ActionEvent;

import jd.controlling.IOEQ;
import jd.controlling.authentication.AuthenticationController;
import jd.controlling.authentication.AuthenticationInfo;

import org.jdownloader.gui.views.components.AbstractAddAction;

public class NewAction extends AbstractAddAction {
    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    private AuthTable         table;

    public NewAction(AuthTable table) {
        this.table = table;
        this.setIconKey("add");
    }

    public void actionPerformed(ActionEvent e) {
        IOEQ.add(new Runnable() {
            public void run() {
                AuthenticationController.getInstance().add(new AuthenticationInfo());
                table.getExtTableModel()._fireTableStructureChanged(AuthenticationController.getInstance().list(), false);
            }
        });

    }

}
