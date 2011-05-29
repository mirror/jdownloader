package jd.gui.swing.jdgui.views.settings.panels.accountmanager;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.NewTheme;

public class PremiumzoneAction extends TableBarAction {
    private static final long serialVersionUID = 9001864924163048011L;

    public PremiumzoneAction() {

        this.putValue(NAME, _GUI._.settings_accountmanager_premiumzone());
        this.putValue(AbstractAction.SMALL_ICON, NewTheme.I().getIcon("premiumzone", ActionColumn.SIZE));
    }

    public void actionPerformed(ActionEvent e) {
    }

}
