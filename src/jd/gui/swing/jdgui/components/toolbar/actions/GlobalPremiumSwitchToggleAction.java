package jd.gui.swing.jdgui.components.toolbar.actions;

import org.jdownloader.gui.shortcuts.ShortcutController;
import org.jdownloader.gui.translate._GUI;

public class GlobalPremiumSwitchToggleAction extends AbstractToolbarToggleAction {
    private static final GlobalPremiumSwitchToggleAction INSTANCE = new GlobalPremiumSwitchToggleAction();

    /**
     * get the only existing instance of GlobalPremiumSwitchToggleAction. This is a singleton
     * 
     * @return
     */
    public static GlobalPremiumSwitchToggleAction getInstance() {
        return GlobalPremiumSwitchToggleAction.INSTANCE;
    }

    /**
     * Create a new instance of GlobalPremiumSwitchToggleAction. This is a singleton class. Access the only existing instance by using
     * {@link #getInstance()}.
     */
    private GlobalPremiumSwitchToggleAction() {
        super(org.jdownloader.settings.staticreferences.CFG_GENERAL.USE_AVAILABLE_ACCOUNTS);
    }

    @Override
    public String createIconKey() {
        return "premium";
    }

    public boolean isDefaultVisible() {
        return true;
    }

    @Override
    protected String createAccelerator() {
        return ShortcutController._.getGlobalPremiumToggleAction();
    }

    @Override
    protected String createTooltip() {
        return _GUI._.action_premium_toggle_tooltip();
    }

}
