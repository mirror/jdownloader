package jd.gui.swing.jdgui.menu;

import java.awt.event.ActionEvent;

import javax.swing.JMenu;

import org.jdownloader.actions.AppAction;

public class MenuJToggleButton extends JMenu {

    public MenuJToggleButton(AppAction action) {
        super(action);

    }

    @Override
    public void setPopupMenuVisible(boolean b) {
        if (b) {
            getAction().actionPerformed(new ActionEvent(this, -1, "click"));
        }
    }

}
