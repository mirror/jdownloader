package org.jdownloader.extensions.jdtrayicon;

import jd.gui.swing.jdgui.views.settings.components.Checkbox;
import jd.gui.swing.jdgui.views.settings.components.ComboBox;
import jd.gui.swing.jdgui.views.settings.components.PasswordInput;

import org.appwork.storage.config.handler.KeyHandler;
import org.jdownloader.extensions.ExtensionConfigPanel;
import org.jdownloader.extensions.jdtrayicon.translate.T;
import org.jdownloader.settings.staticreferences.CFG_GUI;

public class TrayConfigPanel extends ExtensionConfigPanel<TrayExtension> {

    public TrayConfigPanel(TrayExtension trayExtension) {
        super(trayExtension);

        @SuppressWarnings("unchecked")
        KeyHandler<OnCloseAction> keyHandler = (KeyHandler<OnCloseAction>) CFG_TRAY_CONFIG.SH.getKeyHandler("OnCloseAction", KeyHandler.class);
        addPair(T._.plugins_optional_JDLightTray_closetotray2(), null, new ComboBox<OnCloseAction>(keyHandler, new OnCloseAction[] { OnCloseAction.ASK, OnCloseAction.TO_TRAY, OnCloseAction.TO_TASKBAR, OnCloseAction.EXIT }, new String[] { OnCloseAction.ASK.getTranslation(), OnCloseAction.TO_TRAY.getTranslation(), OnCloseAction.TO_TASKBAR.getTranslation(), OnCloseAction.EXIT.getTranslation() }));

        KeyHandler<OnMinimizeAction> keyHandler2 = (KeyHandler<OnMinimizeAction>) CFG_TRAY_CONFIG.SH.getKeyHandler("OnMinimizeAction", KeyHandler.class);
        addPair(T._.plugins_optional_JDLightTray_minimizetotray(), null, new ComboBox<OnMinimizeAction>(keyHandler2, new OnMinimizeAction[] { OnMinimizeAction.TO_TRAY, OnMinimizeAction.TO_TASKBAR }, new String[] { OnMinimizeAction.TO_TRAY.getTranslation(), OnMinimizeAction.TO_TASKBAR.getTranslation() }));
        addPair(T._.plugins_optional_JDLightTray_startMinimized(), null, new Checkbox(CFG_TRAY_CONFIG.START_MINIMIZED_ENABLED));
        addPair(T._.plugins_optional_JDLightTray_singleClick(), null, new Checkbox(CFG_TRAY_CONFIG.TOOGLE_WINDOW_STATUS_WITH_SINGLE_CLICK_ENABLED));
        addPair(T._.plugins_optional_JDLightTray_tooltip(), null, new Checkbox(CFG_TRAY_CONFIG.TOOL_TIP_ENABLED));
        addPair(T._.plugins_optional_JDLightTray_hideifframevisible(), null, new Checkbox(CFG_TRAY_CONFIG.TRAY_ONLY_VISIBLE_IF_WINDOW_IS_HIDDEN_ENABLED));
        addPair(T._.plugins_optional_JDLightTray_linkgrabberresults(), null, new ComboBox<LinkgrabberResultsOption>(CFG_TRAY_CONFIG.SH.getKeyHandler("ShowLinkgrabbingResultsOption", KeyHandler.class), LinkgrabberResultsOption.values(), new String[] { T._.plugins_optional_JDLightTray_minimized(), T._.plugins_optional_JDLightTray_always(), T._.plugins_optional_JDLightTray_never() }));
        addPair(T._.plugins_optional_JDLightTray_passwordRequired(), CFG_GUI.PASSWORD_PROTECTION_ENABLED, new PasswordInput(CFG_GUI.PASSWORD));

    }

    @Override
    public void save() {
    }

    @Override
    public void updateContents() {

    }

}
