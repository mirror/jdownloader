package jd.gui.swing.jdgui.components.toolbar.actions;

import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.toolbar.action.AbstractToolbarToggleAction;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.settings.staticreferences.CFG_SILENTMODE;

public class SilentModeToggleAction extends AbstractToolbarToggleAction {

    public SilentModeToggleAction() {
        super(CFG_SILENTMODE.MANUAL_ENABLED);
        setIconKey(IconKey.ICON_SILENTMODE);
    }

    @Override
    protected String createTooltip() {
        return _GUI.T.action_silentmode_tooltip();
    }

    @Override
    protected String getNameWhenDisabled() {
        return _GUI.T.SilentModeToggleAction_getNameWhenDisabled_();
    }

    @Override
    protected String getNameWhenEnabled() {
        return _GUI.T.SilentModeToggleAction_getNameWhenEnabled_();
    }

}
