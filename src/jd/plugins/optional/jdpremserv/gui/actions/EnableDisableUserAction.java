package jd.plugins.optional.jdpremserv.gui.actions;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

import jd.plugins.optional.jdpremserv.model.PremServUser;

public class EnableDisableUserAction extends AbstractAction {

    private PremServUser obj;

    public EnableDisableUserAction(PremServUser obj) {
        this.obj = obj;

        putValue(AbstractAction.NAME, obj.isEnabled() ? "Disable" : "Enable");
    }

    @Override
    public boolean isEnabled() {

        return true;
    }

    public void actionPerformed(ActionEvent arg0) {

        obj.setEnabled(!obj.isEnabled());
    }

}
