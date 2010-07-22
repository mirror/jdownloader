package jd.plugins.optional.jdpremserv.gui.actions;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

import jd.plugins.optional.jdpremserv.controlling.UserController;
import jd.plugins.optional.jdpremserv.model.PremServUser;

public class RemoveUserAction extends AbstractAction {

    private PremServUser obj;

    public RemoveUserAction(PremServUser obj) {
        this.obj = obj;

        putValue(AbstractAction.NAME, "Remove");
    }

    @Override
    public boolean isEnabled() {

        return true;
    }

    public void actionPerformed(ActionEvent arg0) {

        UserController.getInstance().removeUser(obj);
    }

}
