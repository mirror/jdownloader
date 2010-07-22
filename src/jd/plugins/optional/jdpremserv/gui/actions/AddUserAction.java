package jd.plugins.optional.jdpremserv.gui.actions;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

import jd.plugins.optional.jdpremserv.controlling.UserController;
import jd.plugins.optional.jdpremserv.model.PremServUser;

import org.appwork.utils.swing.dialog.Dialog;

public class AddUserAction extends AbstractAction {

    private PremServUser obj;

    public AddUserAction(PremServUser obj) {
        this.obj = obj;

        putValue(AbstractAction.NAME, "Add");
    }

    @Override
    public boolean isEnabled() {

        return true;
    }

    public void actionPerformed(ActionEvent arg0) {

        String username = Dialog.getInstance().showInputDialog(0, "Username", "");
        String password = Dialog.getInstance().showInputDialog(0, "Password", "");
        UserController.getInstance().addUser(username, password);
    }

}
