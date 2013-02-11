package org.jdownloader.premium;

import java.awt.event.ActionEvent;

import org.appwork.utils.swing.dialog.DialogCanceledException;
import org.appwork.utils.swing.dialog.DialogClosedException;
import org.jdownloader.DomainInfo;
import org.jdownloader.actions.AppAction;
import org.jdownloader.gui.userio.NewUIO;

public class BuyPremiumAction extends AppAction {
    /**
	 * 
	 */
    private static final long serialVersionUID = 2671995579523131972L;
    private DomainInfo        info;
    private String            id;

    public BuyPremiumAction(DomainInfo info, String id) {
        this.info = info;
        this.id = id;

    }

    public void actionPerformed(ActionEvent e) {

        try {
            NewUIO.I().show(BuyAndAddPremiumDialogInterface.class, new BuyAndAddPremiumAccount(info, id));
        } catch (DialogClosedException e1) {
            e1.printStackTrace();
        } catch (DialogCanceledException e1) {
            e1.printStackTrace();
        }

    }

}
