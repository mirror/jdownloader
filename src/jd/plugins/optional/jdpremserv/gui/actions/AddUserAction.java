package jd.plugins.optional.jdpremserv.gui.actions;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

import jd.plugins.optional.jdpremserv.controlling.UserController;

import org.appwork.utils.swing.dialog.Dialog;

public class AddUserAction extends AbstractAction {

    private static final long serialVersionUID = -5010628103917348306L;

    public AddUserAction() {
        super("Add");
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
