package jd.gui.swing.jdgui.components.toolbar.actions;

import org.appwork.storage.config.ValidationException;
import org.appwork.storage.config.events.GenericConfigEventListener;
import org.appwork.storage.config.handler.KeyHandler;
import org.appwork.utils.swing.EDTRunner;
import org.jdownloader.gui.shortcuts.ShortcutController;
import org.jdownloader.gui.toolbar.action.AbstractToolbarToggleAction;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.SelectionInfo;

public class AutoReconnectToggleAction extends AbstractToolbarToggleAction {

    public AutoReconnectToggleAction(SelectionInfo<?, ?> selection) {
        super(org.jdownloader.settings.staticreferences.CFG_GENERAL.AUTO_RECONNECT_ENABLED);
        setIconKey("auto-reconnect");
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
        }, true);
        if ("DummyRouterPlugin".equalsIgnoreCase(org.jdownloader.settings.staticreferences.CFG_RECONNECT.ACTIVE_PLUGIN_ID.getValue())) {
            AutoReconnectToggleAction.this.setEnabled(false);
            AutoReconnectToggleAction.this.setSelected(false);
        } else {
            AutoReconnectToggleAction.this.setEnabled(true);
        }
    }

    @Override
    protected String createAccelerator() {
        return ShortcutController._.getToolbarAutoReconnectToggle();

    }

    public static void main(String[] args) {

    }

    @Override
    protected String createTooltip() {
        return _GUI._.action_reconnect_toggle_tooltip();
    }

}
