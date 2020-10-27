package jd.gui.swing.jdgui.components.toolbar.actions;

import org.appwork.storage.config.ValidationException;
import org.appwork.storage.config.events.GenericConfigEventListener;
import org.appwork.storage.config.handler.KeyHandler;
import org.appwork.utils.swing.EDTRunner;
import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.toolbar.action.AbstractToolbarToggleAction;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.settings.staticreferences.CFG_RECONNECT;

public class AutoReconnectToggleAction extends AbstractToolbarToggleAction {
    private final GenericConfigEventListener<String> listener;

    public AutoReconnectToggleAction() {
        super(CFG_RECONNECT.AUTO_RECONNECT_ENABLED);
        setIconKey(IconKey.ICON_AUTO_RECONNECT);
        listener = new GenericConfigEventListener<String>() {
            @Override
            public void onConfigValueModified(KeyHandler<String> keyHandler, String newValue) {
                updateAction(newValue);
            }

            @Override
            public void onConfigValidatorError(KeyHandler<String> keyHandler, String invalidValue, ValidationException validateException) {
            }
        };
        org.jdownloader.settings.staticreferences.CFG_RECONNECT.ACTIVE_PLUGIN_ID.getEventSender().addListener(listener);
        updateAction(org.jdownloader.settings.staticreferences.CFG_RECONNECT.ACTIVE_PLUGIN_ID.getValue());
    }

    private void updateAction(final String activePlugin) {
        if ("DummyRouterPlugin".equalsIgnoreCase(activePlugin)) {
            new EDTRunner() {
                @Override
                protected void runInEDT() {
                    AutoReconnectToggleAction.this.setEnabled(false);
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
    protected String createTooltip() {
        return _GUI.T.action_reconnect_toggle_tooltip();
    }

    @Override
    protected String getNameWhenDisabled() {
        return _GUI.T.AutoReconnectToggleAction_getNameWhenDisabled_();
    }

    @Override
    protected String getNameWhenEnabled() {
        return _GUI.T.AutoReconnectToggleAction_getNameWhenEnabled_();
    }
}
