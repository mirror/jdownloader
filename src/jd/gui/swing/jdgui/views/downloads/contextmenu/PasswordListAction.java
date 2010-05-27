package jd.gui.swing.jdgui.views.downloads.contextmenu;

import java.awt.event.ActionEvent;

import jd.gui.UserIF;
import jd.gui.swing.SwingGui;
import jd.gui.swing.jdgui.interfaces.ContextMenuAction;
import jd.gui.swing.jdgui.views.settings.panels.passwords.PasswordList;
import jd.utils.locale.JDL;

public class PasswordListAction extends ContextMenuAction {

    private static final long serialVersionUID = -4111402172655120550L;

    public PasswordListAction() {
        init();
    }

    @Override
    protected String getIcon() {
        return PasswordList.getIconKey();
    }

    @Override
    protected String getName() {
        return JDL.L("jd.gui.swing.jdgui.views.downloads.contextmenu.PasswordListAction.name", "Open Password List");
    }

    public void actionPerformed(ActionEvent e) {
        SwingGui.getInstance().requestPanel(UserIF.Panels.CONFIGPANEL, PasswordList.class);
    }

}
