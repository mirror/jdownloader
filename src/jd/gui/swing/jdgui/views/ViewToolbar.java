package jd.gui.swing.jdgui.views;

import javax.swing.JToolBar;

import org.jdownloader.actions.AppAction;

public class ViewToolbar extends JToolBar {

    public ViewToolbar(AppAction... actions) {
        super();
        setFloatable(false);
        this.setRollover(true);
        for (AppAction a : actions) {
            add(a);
        }
    }

}
