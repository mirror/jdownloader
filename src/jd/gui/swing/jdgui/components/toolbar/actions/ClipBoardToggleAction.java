package jd.gui.swing.jdgui.components.toolbar.actions;

import org.jdownloader.gui.toolbar.action.AbstractToolbarToggleAction;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.SelectionInfo;

public class ClipBoardToggleAction extends AbstractToolbarToggleAction {

    public ClipBoardToggleAction(SelectionInfo<?, ?> selection) {
        super(org.jdownloader.settings.staticreferences.CFG_GUI.CLIPBOARD_MONITORED);
        setIconKey("clipboard");
    }

    @Override
    protected String createTooltip() {
        return _GUI._.action_clipboard_observer_tooltip();
    }

    @Override
    protected String getNameWhenDisabled() {
        return _GUI._.ClipBoardToggleAction_getNameWhenDisabled_();
    }

    @Override
    protected String getNameWhenEnabled() {
        return _GUI._.ClipBoardToggleAction_getNameWhenEnabled_();
    }

}
