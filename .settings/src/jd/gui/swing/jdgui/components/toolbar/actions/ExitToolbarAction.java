package jd.gui.swing.jdgui.components.toolbar.actions;

import jd.gui.swing.jdgui.components.toolbar.AbstractToolbarAdapterAction;
import jd.gui.swing.jdgui.menu.actions.ExitAction;

public class ExitToolbarAction extends AbstractToolbarAdapterAction {

    private static final ExitToolbarAction INSTANCE = new ExitToolbarAction();

    /**
     * get the only existing instance of ExitToolbarAction. This is a singleton
     * 
     * @return
     */
    public static ExitToolbarAction getInstance() {
        return ExitToolbarAction.INSTANCE;
    }

    public boolean isDefaultVisible() {
        return false;
    }

    /**
     * Create a new instance of ExitToolbarAction. This is a singleton class.
     * Access the only existing instance by using {@link #getInstance()}.
     */
    private ExitToolbarAction() {
        super(new ExitAction());
    }

}
