package org.jdownloader.premium;

import java.awt.event.ActionEvent;

import org.appwork.utils.os.CrossSystem;
import org.jdownloader.DomainInfo;
import org.jdownloader.actions.AppAction;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.jdserv.JD_SERV_CONSTANTS;

public class OpenURLAction extends AppAction {

    private DomainInfo info;

    public OpenURLAction(DomainInfo info) {
        super();
        setName(_GUI._.OpenURLAction_OpenURLAction_());
        this.info = info;
    }

    public void actionPerformed(ActionEvent e) {

        CrossSystem.openURLOrShowMessage(JD_SERV_CONSTANTS.redirect(info.getAffiliateSettings().getBuyPremiumLink()));
    }

}
