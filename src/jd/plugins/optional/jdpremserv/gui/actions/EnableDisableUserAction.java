package jd.plugins.optional.jdpremserv.gui.actions;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

import jd.plugins.optional.jdpremserv.model.PremServUser;

public class EnableDisableUserAction extends AbstractAction {

    private static final long serialVersionUID = -3846867113041461380L;
    private PremServUser obj;

    public EnableDisableUserAction(PremServUser obj) {
        super(obj.isEnabled() ? "Disable" : "Enable");

        this.obj = obj;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    public void actionPerformed(ActionEvent arg0) {
        obj.setEnabled(!obj.isEnabled());
    }

}
