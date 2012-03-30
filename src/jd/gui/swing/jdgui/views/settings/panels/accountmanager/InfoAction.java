package jd.gui.swing.jdgui.views.settings.panels.accountmanager;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

import jd.plugins.Account;
import jd.plugins.PluginForHost;
import jd.plugins.infogenerator.PluginInfoGenerator;
import jd.utils.JDUtilities;

import org.appwork.utils.os.CrossSystem;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.NewTheme;

public class InfoAction extends TableBarAction {
    private static final long serialVersionUID = 8927011292367107922L;

    public InfoAction() {

        this.putValue(NAME, _GUI._.settings_accountmanager_info());
        this.putValue(AbstractAction.SMALL_ICON, NewTheme.I().getIcon("info", ActionColumn.SIZE));
    }

    public void actionPerformed(ActionEvent e) {
        Account lAcc = getAccount();
        if (lAcc == null) { return; }
        PluginForHost plugin = JDUtilities.getNewPluginForHostInstance(lAcc.getHoster());
        PluginInfoGenerator pig = ((PluginInfoGenerator) plugin.getInfoGenerator(lAcc));
        if (pig != null) {
            pig.show();
        } else {
            CrossSystem.openURLOrShowMessage(plugin.getBuyPremiumUrl());
        }
    }

}
