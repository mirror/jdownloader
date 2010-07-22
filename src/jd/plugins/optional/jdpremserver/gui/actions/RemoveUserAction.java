package jd.plugins.optional.jdpremserver.gui.actions;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

import jd.plugins.optional.jdpremserver.controlling.UserController;
import jd.plugins.optional.jdpremserver.model.PremServUser;

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
