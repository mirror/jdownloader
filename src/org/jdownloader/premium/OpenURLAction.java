package org.jdownloader.premium;

import java.awt.event.ActionEvent;

import jd.controlling.AccountController;

import org.appwork.utils.os.CrossSystem;
import org.jdownloader.DomainInfo;
import org.jdownloader.actions.AppAction;
import org.jdownloader.gui.translate._GUI;

public class OpenURLAction extends AppAction {

    /**
	 * 
	 */
    private static final long serialVersionUID = -7939070621339510855L;
    private DomainInfo        info;
    private String            id;

    public OpenURLAction(DomainInfo info, String id) {
        super();
        this.id = id;
        setName(_GUI._.OpenURLAction_OpenURLAction_());
        this.info = info;
    }

    public void actionPerformed(ActionEvent e) {

        CrossSystem.openURLOrShowMessage(AccountController.createFullBuyPremiumUrl(info.getTld(), id));
    }
}
