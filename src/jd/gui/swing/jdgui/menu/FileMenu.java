package jd.gui.swing.jdgui.menu;

import javax.swing.JMenu;
import javax.swing.JMenuItem;

import jd.gui.swing.jdgui.menu.actions.ExitAction;
import jd.gui.swing.jdgui.menu.actions.RestartAction;
import jd.gui.swing.jdgui.menu.actions.RestoreAction;
import jd.gui.swing.jdgui.menu.actions.SettingsAction;
import jd.nutils.OSDetector;

import org.jdownloader.gui.translate._GUI;

public class FileMenu extends JMenu {

    private static final long serialVersionUID = -6088167424746457972L;

    public FileMenu() {
        super(_GUI._.jd_gui_skins_simple_simplegui_menubar_filemenu());

        add(new SaveMenu());
        addSeparator();
        add(new SettingsAction());
        addSeparator();
        add(new RestoreAction());
        add(new RestartAction());

        // add exit action, used by tray extension
        JMenuItem exitItem = add(new ExitAction());
        // but hide it from menu action list in case we are on Mac
        if (OSDetector.isMac()) {
            exitItem.setVisible(false);
        }
    }

}