package org.jdownloader.extensions.shutdown;

import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;

import jd.gui.swing.jdgui.views.settings.components.Checkbox;
import jd.gui.swing.jdgui.views.settings.components.ComboBox;

import org.appwork.storage.config.ValidationException;
import org.appwork.storage.config.events.GenericConfigEventListener;
import org.appwork.storage.config.handler.KeyHandler;
import org.appwork.swing.components.ExtButton;
import org.appwork.utils.os.CrossSystem;
import org.jdownloader.actions.AppAction;
import org.jdownloader.extensions.ExtensionConfigPanel;
import org.jdownloader.extensions.shutdown.translate.T;

public class ShutdownConfigPanel extends ExtensionConfigPanel<ShutdownExtension> {
    public ShutdownConfigPanel(ShutdownExtension extension) {
        super(extension);
        final KeyHandler<Mode> modeKeyHandler = CFG_SHUTDOWN.SH.getKeyHandler(CFG_SHUTDOWN.SHUTDOWN_MODE.getKey(), KeyHandler.class);
        final ShutdownInterface shutdownInterface = extension.getShutdownInterface();
        final List<String> translations = new ArrayList<String>();
        for (final Mode mode : shutdownInterface.getSupportedModes()) {
            translations.add(mode.getTranslation());
        }
        if (!shutdownInterface.isSupported(modeKeyHandler.getValue())) {
            modeKeyHandler.setValue(shutdownInterface.getSupportedModes()[0]);
        }
        addPair(T.T.gui_config_jdshutdown_mode(), null, new ComboBox<Mode>(modeKeyHandler, shutdownInterface.getSupportedModes(), translations.toArray(new String[0])));
        addPair(T.T.gui_config_jdshutdown_forceshutdown(), null, new Checkbox(CFG_SHUTDOWN.FORCE_SHUTDOWN_ENABLED));
        addPair(T.T.config_active_by_default(), null, new Checkbox(CFG_SHUTDOWN.SHUTDOWN_ACTIVE_BY_DEFAULT_ENABLED));
        CFG_SHUTDOWN.SHUTDOWN_MODE.getEventSender().addListener(new GenericConfigEventListener<Enum>() {
            @Override
            public void onConfigValidatorError(KeyHandler<Enum> keyHandler, Enum invalidValue, ValidationException validateException) {
            }

            @Override
            public void onConfigValueModified(KeyHandler<Enum> keyHandler, Enum newValue) {
                shutdownInterface.prepareMode((Mode) newValue);
            }
        });
        if (CrossSystem.isMac()) {
            add(new ExtButton(new AppAction() {
                {
                    setName(T.T.install_force());
                }

                @Override
                public void actionPerformed(ActionEvent e) {
                    shutdownInterface.prepareMode(modeKeyHandler.getValue());
                }
            }), "gapleft 37");
        }
    }

    @Override
    public void save() {
    }

    @Override
    public void updateContents() {
    }
}
