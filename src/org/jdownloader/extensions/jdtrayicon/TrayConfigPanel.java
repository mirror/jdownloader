package org.jdownloader.extensions.jdtrayicon;

import jd.gui.swing.jdgui.views.settings.components.Checkbox;
import jd.gui.swing.jdgui.views.settings.components.ComboBox;
import jd.gui.swing.jdgui.views.settings.components.PasswordInput;

import org.appwork.storage.config.handler.KeyHandler;
import org.jdownloader.extensions.ExtensionConfigPanel;
import org.jdownloader.extensions.jdtrayicon.translate.T;

public class TrayConfigPanel extends ExtensionConfigPanel<TrayExtension> {

    public TrayConfigPanel(TrayExtension trayExtension) {
        super(trayExtension);
        addPair(T._.plugins_optional_JDLightTray_closetotray(), null, new Checkbox(CFG_TRAY_CONFIG.CLOSE_TO_TRAY_ENABLED));
        addPair(T._.plugins_optional_JDLightTray_startMinimized(), null, new Checkbox(CFG_TRAY_CONFIG.START_MINIMIZED_ENABLED));
        addPair(T._.plugins_optional_JDLightTray_singleClick(), null, new Checkbox(CFG_TRAY_CONFIG.TOOGLE_WINDOW_STATUS_WITH_SINGLE_CLICK_ENABLED));
        addPair(T._.plugins_optional_JDLightTray_tooltip(), null, new Checkbox(CFG_TRAY_CONFIG.TOOL_TIP_ENABLED));
        addPair(T._.plugins_optional_JDLightTray_linkgrabberresults(), null, new ComboBox<LinkgrabberResultsOption>(CFG_TRAY_CONFIG.SH.getKeyHandler("ShowLinkgrabbingResultsOption", KeyHandler.class), LinkgrabberResultsOption.values(), new String[] { T._.plugins_optional_JDLightTray_minimized(), T._.plugins_optional_JDLightTray_always(), T._.plugins_optional_JDLightTray_never() }));
        addPair(T._.plugins_optional_JDLightTray_passwordRequired(), CFG_TRAY_CONFIG.PASSWORD_PROTECTION_ENABLED, new PasswordInput(CFG_TRAY_CONFIG.PASSWORD));

    }

    @Override
    public void save() {
    }

    @Override
    public void updateContents() {

    }

}
