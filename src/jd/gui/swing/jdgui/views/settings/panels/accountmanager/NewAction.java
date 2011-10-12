package jd.gui.swing.jdgui.views.settings.panels.accountmanager;

import java.awt.event.ActionEvent;

import jd.gui.swing.dialog.AddAccountDialog;
import jd.plugins.PluginForHost;

import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.components.AbstractAddAction;

public class NewAction extends AbstractAddAction {
    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    public NewAction() {
        super();
    }

    public void actionPerformed(ActionEvent e) {
        /*
         * no need for IOEQ or Thread, as we want to show Dialog and that blocks
         * EDT anyway
         */
        if (e.getSource() instanceof PluginForHost) {
            AddAccountDialog.showDialog((PluginForHost) e.getSource(), null);
        } else {
            AddAccountDialog.showDialog(null, null);
        }
    }

    @Override
    public String getTooltipText() {
        return _GUI._.action_add_premium_account_tooltip();
    }

}
