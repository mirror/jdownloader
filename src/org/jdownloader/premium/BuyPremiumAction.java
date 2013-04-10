package org.jdownloader.premium;

import java.awt.event.ActionEvent;

import org.appwork.uio.UIOManager;
import org.jdownloader.DomainInfo;
import org.jdownloader.actions.AppAction;

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

        UIOManager.I().show(BuyAndAddPremiumDialogInterface.class, new BuyAndAddPremiumAccount(info, id));

    }

}
