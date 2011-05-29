package jd.gui.swing.jdgui.views.settings.panels.accountmanager;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.NewTheme;

public class DeleteAction extends TableBarAction {
    private static final long serialVersionUID = -2155909996266580482L;

    public DeleteAction() {

        this.putValue(NAME, _GUI._.settings_accountmanager_delete());
        this.putValue(AbstractAction.SMALL_ICON, NewTheme.I().getIcon("delete", ActionColumn.SIZE));
    }

    public void actionPerformed(ActionEvent e) {
    }

}
