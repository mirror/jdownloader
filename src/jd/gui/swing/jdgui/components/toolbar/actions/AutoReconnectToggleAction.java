package jd.gui.swing.jdgui.components.toolbar.actions;

import org.appwork.storage.config.ValidationException;
import org.appwork.storage.config.events.GenericConfigEventListener;
import org.appwork.storage.config.handler.KeyHandler;
import org.appwork.utils.swing.EDTRunner;
import org.jdownloader.gui.translate._GUI;

public class AutoReconnectToggleAction extends AbstractToolbarToggleAction {
    private static final AutoReconnectToggleAction INSTANCE = new AutoReconnectToggleAction();

    /**
     * get the only existing instance of AutoReconnectToggleAction. This is a singleton
     * 
     * @return
     */
    public static AutoReconnectToggleAction getInstance() {
        return AutoReconnectToggleAction.INSTANCE;
    }

    /**
     * Create a new instance of AutoReconnectToggleAction. This is a singleton class. Access the only existing instance by using {@link #getInstance()}.
     */
    private AutoReconnectToggleAction() {
        super(org.jdownloader.settings.staticreferences.CFG_GENERAL.AUTO_RECONNECT_ENABLED);
        org.jdownloader.settings.staticreferences.CFG_RECONNECT.ACTIVE_PLUGIN_ID.getEventSender().addListener(new GenericConfigEventListener<String>() {

            @Override
            public void onConfigValueModified(KeyHandler<String> keyHandler, String newValue) {
                if ("DummyRouterPlugin".equalsIgnoreCase(newValue)) {
                    new EDTRunner() {

                        @Override
                        protected void runInEDT() {
                            AutoReconnectToggleAction.this.setEnabled(false);
                            AutoReconnectToggleAction.this.setSelected(false);
                        }
                    };
                } else {
                    new EDTRunner() {

                        @Override
                        protected void runInEDT() {
                            AutoReconnectToggleAction.this.setEnabled(true);
                        }
                    };
                }
            }

            @Override
            public void onConfigValidatorError(KeyHandler<String> keyHandler, String invalidValue, ValidationException validateException) {
            }
        });
        if ("DummyRouterPlugin".equalsIgnoreCase(org.jdownloader.settings.staticreferences.CFG_RECONNECT.ACTIVE_PLUGIN_ID.getValue())) {
            AutoReconnectToggleAction.this.setEnabled(false);
            AutoReconnectToggleAction.this.setSelected(false);
        } else {
            AutoReconnectToggleAction.this.setEnabled(true);
        }
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
