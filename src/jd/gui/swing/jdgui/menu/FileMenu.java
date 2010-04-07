package jd.gui.swing.jdgui.menu;

import javax.swing.JMenu;

import jd.gui.swing.jdgui.menu.actions.ExitAction;
import jd.gui.swing.jdgui.menu.actions.RestartAction;
import jd.gui.swing.jdgui.menu.actions.RestoreAction;
import jd.gui.swing.jdgui.menu.actions.SettingsAction;
import jd.utils.locale.JDL;

public class FileMenu extends JMenu {

    private static final long serialVersionUID = -6088167424746457972L;

    public FileMenu() {
        super(JDL.L("jd.gui.skins.simple.simplegui.menubar.filemenu", "File"));

        add(new SaveMenu());
        addSeparator();
        add(new SettingsAction());
        addSeparator();
        add(new RestoreAction());
        add(new RestartAction());
        add(new ExitAction());
    }

}
