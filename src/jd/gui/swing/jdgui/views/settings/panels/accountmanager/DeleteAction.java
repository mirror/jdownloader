package jd.gui.swing.jdgui.views.settings.panels.accountmanager;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

import org.jdownloader.gui.translate.T;
import org.jdownloader.images.Theme;

public class DeleteAction extends TableBarAction {
    public DeleteAction() {

        this.putValue(NAME, T._.settings_accountmanager_delete());
        this.putValue(AbstractAction.SMALL_ICON, Theme.getIcon("delete", ActionColumn.SIZE));
    }

    public void actionPerformed(ActionEvent e) {
    }

}
