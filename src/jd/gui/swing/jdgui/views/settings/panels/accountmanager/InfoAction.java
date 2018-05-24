package jd.gui.swing.jdgui.views.settings.panels.accountmanager;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

import jd.controlling.AccountController;
import jd.plugins.Account;
import jd.plugins.PluginForHost;
import jd.plugins.infogenerator.PluginInfoGenerator;
import jd.utils.JDUtilities;

import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.AbstractIcon;

public class InfoAction extends TableBarAction {
    private static final long serialVersionUID = 8927011292367107922L;

    public InfoAction() {
        this.putValue(NAME, _GUI.T.settings_accountmanager_info());
        this.putValue(AbstractAction.SMALL_ICON, new AbstractIcon(IconKey.ICON_INFO, ActionColumn.SIZE));
    }

    public void actionPerformed(ActionEvent e) {
        final Account lAcc = getAccount();
        if (lAcc != null) {
            final PluginForHost plugin = JDUtilities.getNewPluginForHostInstance(lAcc.getHoster());
            if (plugin != null) {
                final PluginInfoGenerator pig = ((PluginInfoGenerator) plugin.getInfoGenerator(lAcc));
                if (pig != null) {
                    pig.show();
                    return;
                }
            }
            final String customURL;
            if (plugin == null) {
                customURL = "http://" + lAcc.getHoster();
            } else {
                customURL = null;
            }
            AccountController.openAfflink(plugin, customURL, "InfoAction");
        }
    }
}
