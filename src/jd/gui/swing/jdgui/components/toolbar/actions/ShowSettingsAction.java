package jd.gui.swing.jdgui.components.toolbar.actions;

import jd.gui.swing.jdgui.components.toolbar.AbstractToolbarAdapterAction;
import jd.gui.swing.jdgui.menu.actions.SettingsAction;

public class ShowSettingsAction extends AbstractToolbarAdapterAction {
    private static final ShowSettingsAction INSTANCE = new ShowSettingsAction();

    /**
     * get the only existing instance of ShowSettingsAction. This is a singleton
     * 
     * @return
     */
    public static ShowSettingsAction getInstance() {
        return ShowSettingsAction.INSTANCE;
    }

    /**
     * Create a new instance of ShowSettingsAction. This is a singleton class.
     * Access the only existing instance by using {@link #getInstance()}.
     */
    private ShowSettingsAction() {
        super(new SettingsAction());

    }

}
