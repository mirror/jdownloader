package jd.gui.swing.jdgui.menu;

import javax.swing.AbstractAction;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JSeparator;

import jd.SecondLevelLaunch;
import jd.gui.swing.jdgui.menu.actions.ExitAction;
import jd.gui.swing.jdgui.menu.actions.RestartAction;

import org.appwork.utils.os.CrossSystem;
import org.appwork.utils.swing.EDTRunner;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.linkgrabber.actions.AddContainerAction;
import org.jdownloader.gui.views.linkgrabber.actions.AddLinksAction;

public class FileMenu extends JMenu {

    private static final long serialVersionUID = -6088167424746457972L;

    public FileMenu() {
        super(_GUI._.jd_gui_skins_simple_simplegui_menubar_filemenu());

        SecondLevelLaunch.GUI_COMPLETE.executeWhenReached(new Runnable() {

            public void run() {
                new EDTRunner() {

                    @Override
                    protected void runInEDT() {
                        AddLinksAction ala = new AddLinksAction();
                        ala.putValue(AbstractAction.NAME, _GUI._.AddOptionsAction_actionPerformed_addlinks());
                        add(new JMenuItem(ala));
                        add(new JMenuItem(new AddContainerAction()));
                        add(new JSeparator());
                        add(new RestartAction());

                        if (!CrossSystem.isMac()) {
                            // add exit action, used by tray extension
                            add(new ExitAction());
                        }
                    }
                };
            }

        });

    }

}