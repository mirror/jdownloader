package jd.gui.swing.jdgui.views.settings.panels.accountmanager;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.Theme;

public class PremiumzoneAction extends TableBarAction {
    public PremiumzoneAction() {

        this.putValue(NAME, _GUI._.settings_accountmanager_premiumzone());
        this.putValue(AbstractAction.SMALL_ICON, Theme.getIcon("premiumzone", ActionColumn.SIZE));
    }

    public void actionPerformed(ActionEvent e) {
    }

}
