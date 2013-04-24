package jd.gui.swing.jdgui.menu;

import javax.swing.JMenuBar;

import jd.SecondLevelLaunch;

import org.appwork.utils.swing.EDTRunner;

public class JDMenuBar extends JMenuBar {

    private static final long serialVersionUID = 6758718947311901334L;

    public JDMenuBar() {
        super();
        SecondLevelLaunch.GUI_COMPLETE.executeWhenReached(new Runnable() {

            public void run() {
                new EDTRunner() {

                    @Override
                    protected void runInEDT() {
                        add(new FileMenu());
                        // add(new EditMenu());
                        add(new SettingsMenu());
                        add(AddonsMenu.getInstance());

                        add(new AboutMenu());
                    }
                };
            }
        });

    }

}
