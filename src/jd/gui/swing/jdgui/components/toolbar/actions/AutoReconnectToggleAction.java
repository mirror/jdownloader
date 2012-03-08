package jd.gui.swing.jdgui.components.toolbar.actions;

import org.jdownloader.gui.translate._GUI;

public class AutoReconnectToggleAction extends AbstractToolbarToggleAction {
    private static final AutoReconnectToggleAction INSTANCE = new AutoReconnectToggleAction();

    /**
     * get the only existing instance of AutoReconnectToggleAction. This is a
     * singleton
     * 
     * @return
     */
    public static AutoReconnectToggleAction getInstance() {
        return AutoReconnectToggleAction.INSTANCE;
    }

    /**
     * Create a new instance of AutoReconnectToggleAction. This is a singleton
     * class. Access the only existing instance by using {@link #getInstance()}.
     */
    private AutoReconnectToggleAction() {
        super(org.jdownloader.settings.staticreferences.CFG_GENERAL.AUTO_RECONNECT_ENABLED);
    }

    @Override
    public String createIconKey() {
        return "auto-reconnect";
    }

    @Override
    protected String createMnemonic() {
        return _GUI._.action_reconnect_toggle_mnemonic();
    }

    @Override
    protected String createAccelerator() {
        return _GUI._.action_reconnect_toggle_accelerator();
    }

    @Override
    protected String createTooltip() {
        return _GUI._.action_reconnect_toggle_tooltip();
    }

}
