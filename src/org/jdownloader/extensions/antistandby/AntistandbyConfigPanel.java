package org.jdownloader.extensions.antistandby;

import jd.gui.swing.jdgui.views.settings.components.ComboBox;

import org.appwork.storage.config.handler.KeyHandler;
import org.jdownloader.extensions.ExtensionConfigPanel;
import org.jdownloader.extensions.antistandby.translate.T;

public class AntistandbyConfigPanel extends ExtensionConfigPanel<AntiStandbyExtension> {

    public AntistandbyConfigPanel(AntiStandbyExtension trayExtension) {
        super(trayExtension);
        // Property subConfig = getPropertyConfig();
        KeyHandler<Mode> keyHandler2 = (KeyHandler<Mode>) CFG_ANTISTANDBY.SH.getKeyHandler(CFG_ANTISTANDBY.MODE.getKey(), KeyHandler.class);

        addPair(T._.mode(), null, new ComboBox<Mode>(keyHandler2, new Mode[] { Mode.DOWNLOADING, Mode.RUNNING }, new String[] { T._.gui_config_antistandby_whiledl2(), T._.gui_config_antistandby_whilejd2() }));

    }

    @Override
    public void save() {
    }

    @Override
    public void updateContents() {

    }

}
