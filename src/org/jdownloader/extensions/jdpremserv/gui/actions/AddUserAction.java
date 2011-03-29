package org.jdownloader.extensions.jdpremserv.gui.actions;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;


import org.appwork.utils.swing.dialog.Dialog;
import org.appwork.utils.swing.dialog.DialogCanceledException;
import org.appwork.utils.swing.dialog.DialogClosedException;
import org.jdownloader.extensions.jdpremserv.controlling.UserController;

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
        String username;
        try {
            username = Dialog.getInstance().showInputDialog(0, "Username", "");
            String password = Dialog.getInstance().showInputDialog(0, "Password", "");
            UserController.getInstance().addUser(username, password);
        } catch (DialogClosedException e) {
            e.printStackTrace();
        } catch (DialogCanceledException e) {
            e.printStackTrace();
        }
    }

}
