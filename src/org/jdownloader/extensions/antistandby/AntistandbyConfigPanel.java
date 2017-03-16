package org.jdownloader.extensions.antistandby;

import jd.gui.swing.jdgui.views.settings.components.Checkbox;
import jd.gui.swing.jdgui.views.settings.components.ComboBox;

import org.appwork.storage.config.handler.BooleanKeyHandler;
import org.appwork.storage.config.handler.EnumKeyHandler;
import org.appwork.utils.os.CrossSystem;
import org.jdownloader.extensions.ExtensionConfigPanel;
import org.jdownloader.extensions.antistandby.translate.T;

public class AntistandbyConfigPanel extends ExtensionConfigPanel<AntiStandbyExtension> {

    public AntistandbyConfigPanel(AntiStandbyExtension trayExtension) {
        super(trayExtension);
        final EnumKeyHandler modeKeyHandler = CFG_ANTISTANDBY.SH.getKeyHandler(CFG_ANTISTANDBY.MODE.getKey(), EnumKeyHandler.class);
        addPair(T.T.mode(), null, new ComboBox<Enum>(modeKeyHandler, new Mode[] { Mode.DOWNLOADING, Mode.RUNNING, Mode.CRAWLING, Mode.DOWNLOADINGDORCRAWLING }, new String[] { T.T.gui_config_antistandby_whiledl2(), T.T.gui_config_antistandby_whilejd2(), T.T.gui_config_antistandby_whilecrawl(), T.T.gui_config_antistandby_whiledl2orcrawl() }));
        if (CrossSystem.isWindows()) {
            final BooleanKeyHandler displayRequired = CFG_ANTISTANDBY.SH.getKeyHandler(CFG_ANTISTANDBY.DISPLAY_REQUIRED.getKey(), BooleanKeyHandler.class);
            addPair(T.T.prevent_screensaver(), null, new Checkbox(displayRequired));
        }
    }

    @Override
    public void save() {
    }

    @Override
    public void updateContents() {

    }

}
