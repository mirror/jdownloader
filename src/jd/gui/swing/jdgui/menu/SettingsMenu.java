package jd.gui.swing.jdgui.menu;

import javax.swing.JMenu;

import jd.Main;
import jd.gui.swing.jdgui.menu.actions.SettingsAction;

import org.jdownloader.gui.translate._GUI;

public class SettingsMenu extends JMenu {
    /**
	 * 
	 */
    private static final long serialVersionUID = -9005381954255451222L;

    public SettingsMenu() {
        super(_GUI._.SettingsMenu_SettingsMenu_());

        Main.GUI_COMPLETE.executeWhenReached(new Runnable() {

            public void run() {
                add(new SettingsAction());
                addSeparator();
                add(new ChunksEditor());
                add(new ParalellDownloadsEditor());
                add(new ParallelDownloadsPerHostEditor());
                add(new SpeedlimitEditor());
            }

        });

    }
}
