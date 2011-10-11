package jd.gui.swing.jdgui.menu;

import javax.swing.JMenu;

import jd.gui.swing.jdgui.menu.actions.SettingsAction;

import org.jdownloader.gui.translate._GUI;

public class SettingsMenu extends JMenu {
    public SettingsMenu() {
        super(_GUI._.SettingsMenu_SettingsMenu_());

        add(new SettingsAction());
        addSeparator();
        add(new ChunksEditor());
        add(new ParalellDownloadsEditor());
        add(new ParallelDownloadsPerHostEditor());
        add(new SpeedlimitEditor());

    }
}
