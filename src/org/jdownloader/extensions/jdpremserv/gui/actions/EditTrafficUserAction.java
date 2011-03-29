package org.jdownloader.extensions.jdpremserv.gui.actions;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;


import org.appwork.utils.formatter.SizeFormatter;
import org.appwork.utils.swing.dialog.Dialog;
import org.appwork.utils.swing.dialog.DialogCanceledException;
import org.appwork.utils.swing.dialog.DialogClosedException;
import org.jdownloader.extensions.jdpremserv.model.PremServUser;

public class EditTrafficUserAction extends AbstractAction {

    private static final long serialVersionUID = -9025882574366289273L;
    private PremServUser      obj;

    public EditTrafficUserAction(PremServUser obj) {
        super("Edit max Traffic");

        this.obj = obj;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    public void actionPerformed(ActionEvent arg0) {

        String ret;
        try {
            ret = Dialog.getInstance().showInputDialog(0, "Set Max Traffic for " + obj.getUsername(), "Format: 1,34GB", SizeFormatter.formatBytes(obj.getAllowedTrafficPerMonth()), null, null, null);
            if (ret == null) return;
            obj.setAllowedTrafficPerMonth(SizeFormatter.getSize(ret));

        } catch (DialogClosedException e) {
            e.printStackTrace();
        } catch (DialogCanceledException e) {
            e.printStackTrace();
        }

    }
}
