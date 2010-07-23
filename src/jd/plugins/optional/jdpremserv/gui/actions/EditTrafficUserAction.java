package jd.plugins.optional.jdpremserv.gui.actions;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

import jd.plugins.optional.jdpremserv.model.PremServUser;

import org.appwork.utils.formatter.SizeFormater;
import org.appwork.utils.swing.dialog.Dialog;

public class EditTrafficUserAction extends AbstractAction {

    private static final long serialVersionUID = -9025882574366289273L;
    private PremServUser obj;

    public EditTrafficUserAction(PremServUser obj) {
        super("Edit max Traffic");

        this.obj = obj;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    public void actionPerformed(ActionEvent arg0) {

        String ret = Dialog.getInstance().showInputDialog(0, "Set Max Traffic for " + obj.getUsername(), "Format: 1,34GB", SizeFormater.formatBytes(obj.getAllowedTrafficPerMonth()), null, null, null);
        if (ret == null) return;
        obj.setAllowedTrafficPerMonth(SizeFormater.getSize(ret));

    }
}
