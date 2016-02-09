package jd.gui.swing.jdgui.components.toolbar;

import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.toolbar.action.AbstractToolbarToggleAction;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.settings.staticreferences.CFG_GUI;

public class AlwaysOnTopGuiToogleAction extends AbstractToolbarToggleAction {

    public AlwaysOnTopGuiToogleAction() {
        super(CFG_GUI.MAIN_WINDOW_ALWAYS_ON_TOP);
        setIconKey(IconKey.ICON_GUI);
    }

    @Override
    protected String createTooltip() {
        return _GUI.T.AlwaysOnTopGuiToogleAction_tooltip();
    }

    @Override
    protected String getNameWhenDisabled() {
        return _GUI.T.AlwaysOnTopGuiToogleAction_disabled();
    }

    @Override
    protected String getNameWhenEnabled() {
        return _GUI.T.AlwaysOnTopGuiToogleAction_enabled();
    }

}
