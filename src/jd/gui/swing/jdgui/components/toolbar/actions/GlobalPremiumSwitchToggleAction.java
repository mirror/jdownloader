package jd.gui.swing.jdgui.components.toolbar.actions;

import org.jdownloader.gui.toolbar.action.AbstractToolbarToggleAction;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.SelectionInfo;

public class GlobalPremiumSwitchToggleAction extends AbstractToolbarToggleAction {

    public GlobalPremiumSwitchToggleAction() {
        super(org.jdownloader.settings.staticreferences.CFG_GENERAL.USE_AVAILABLE_ACCOUNTS);
        setIconKey("premium");
    }

    @Override
    protected String createTooltip() {
        return _GUI._.action_premium_toggle_tooltip();
    }

    @Override
    protected String getNameWhenDisabled() {
        return _GUI._.GlobalPremiumSwitchToggleAction_getNameWhenDisabled_();
    }

    @Override
    protected String getNameWhenEnabled() {
        return _GUI._.GlobalPremiumSwitchToggleAction_getNameWhenEnabled_();
    }

}
