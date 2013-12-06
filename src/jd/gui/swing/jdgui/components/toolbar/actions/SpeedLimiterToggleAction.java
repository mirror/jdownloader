package jd.gui.swing.jdgui.components.toolbar.actions;

import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.toolbar.action.AbstractToolbarToggleAction;
import org.jdownloader.gui.translate._GUI;

public class SpeedLimiterToggleAction extends AbstractToolbarToggleAction {

    public SpeedLimiterToggleAction() {
        super(org.jdownloader.settings.staticreferences.CFG_GENERAL.DOWNLOAD_SPEED_LIMIT_ENABLED);
        setIconKey(IconKey.ICON_SPEED);
    }

    @Override
    protected String createTooltip() {
        return _GUI._.SpeedLimiterToggleAction_tooltip();
    }

    @Override
    protected String getNameWhenDisabled() {
        return _GUI._.SpeedLimiterToggleAction_getNameWhenDisabled_();
    }

    @Override
    protected String getNameWhenEnabled() {
        return _GUI._.SpeedLimiterToggleAction_getNameWhenDisabled_();
    }

}
