package org.jdownloader.premium;

import java.awt.event.ActionEvent;

import org.appwork.utils.swing.dialog.DialogCanceledException;
import org.appwork.utils.swing.dialog.DialogClosedException;
import org.jdownloader.DomainInfo;
import org.jdownloader.actions.AppAction;
import org.jdownloader.gui.uiserio.NewUIO;

public class BuyPremiumAction extends AppAction {
    /**
	 * 
	 */
    private static final long serialVersionUID = 2671995579523131972L;
    private DomainInfo        info;

    public BuyPremiumAction(DomainInfo info) {
        this.info = info;

    }

    public void actionPerformed(ActionEvent e) {

        try {
            NewUIO.I().show(BuyAndAddPremiumDialogInterface.class, new BuyAndAddPremiumAccount(info));
        } catch (DialogClosedException e1) {
            e1.printStackTrace();
        } catch (DialogCanceledException e1) {
            e1.printStackTrace();
        }

    }

}
