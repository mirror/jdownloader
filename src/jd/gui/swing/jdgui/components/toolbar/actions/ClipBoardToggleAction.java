package jd.gui.swing.jdgui.components.toolbar.actions;

import org.jdownloader.gui.shortcuts.ShortcutController;
import org.jdownloader.gui.translate._GUI;

public class ClipBoardToggleAction extends AbstractToolbarToggleAction {
    private static final ClipBoardToggleAction INSTANCE = new ClipBoardToggleAction();

    /**
     * get the only existing instance of ClipBoardAction. This is a singleton
     * 
     * @return
     */
    public static ClipBoardToggleAction getInstance() {
        return ClipBoardToggleAction.INSTANCE;
    }

    /**
     * Create a new instance of ClipBoardAction. This is a singleton class. Access the only existing instance by using
     * {@link #getInstance()}.
     */
    private ClipBoardToggleAction() {
        super(org.jdownloader.settings.staticreferences.CFG_GUI.CLIPBOARD_MONITORED);
    }

    @Override
    public String createIconKey() {
        return "clipboard";
    }

    @Override
    protected String createAccelerator() {
        return ShortcutController._.getToolbarClipboardToggle();

    }

    @Override
    protected String createTooltip() {
        return _GUI._.action_clipboard_observer_tooltip();
    }

}
