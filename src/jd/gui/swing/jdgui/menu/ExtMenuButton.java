package jd.gui.swing.jdgui.menu;

import java.awt.event.ActionEvent;

import javax.swing.JMenu;

import org.jdownloader.actions.AppAction;

public class ExtMenuButton extends JMenu {

    /*
     * JMenuBar uses Boxlayout. BoxLayout always tries to strech the components to their Maximum Width. Fixes
     * http://svn.jdownloader.org/issues/8509
     */
    public ExtMenuButton(AppAction action) {
        super(action);

    }

    @Override
    public void setPopupMenuVisible(boolean b) {
        if (b) {
            getAction().actionPerformed(new ActionEvent(this, -1, "click"));
        }
    }

    // private SynthContext context;
    // private Object painter;
    // private Method paintMenuBackground;
    // private SynthContext mouseOverContext;
    // private SynthContext mouseOutContext;

}
