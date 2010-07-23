package jd.plugins.optional.jdpremserv.gui.actions;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

import jd.plugins.optional.jdpremserv.controlling.UserController;
import jd.plugins.optional.jdpremserv.model.PremServUser;

public class RemoveUserAction extends AbstractAction {

    private static final long serialVersionUID = -891725383105747015L;
    private PremServUser obj;

    public RemoveUserAction(PremServUser obj) {
        super("Remove");

        this.obj = obj;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    public void actionPerformed(ActionEvent arg0) {
        UserController.getInstance().removeUser(obj);
    }

}
