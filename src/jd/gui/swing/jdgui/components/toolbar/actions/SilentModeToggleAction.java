package jd.gui.swing.jdgui.components.toolbar.actions;

import org.jdownloader.gui.toolbar.action.AbstractToolbarToggleAction;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.SelectionInfo;
import org.jdownloader.settings.staticreferences.CFG_SILENTMODE;

public class SilentModeToggleAction extends AbstractToolbarToggleAction {

    public SilentModeToggleAction(SelectionInfo<?, ?> selection) {
        super(CFG_SILENTMODE.MANUAL_ENABLED);
        setIconKey("silentmode");
    }

    @Override
    protected String createTooltip() {
        return _GUI._.action_silentmode_tooltip();
    }

}
