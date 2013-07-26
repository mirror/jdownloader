package org.jdownloader.gui.jdtrayicon;

import jd.gui.swing.jdgui.views.settings.components.Checkbox;
import jd.gui.swing.jdgui.views.settings.components.ComboBox;
import jd.gui.swing.jdgui.views.settings.components.PasswordInput;

import org.appwork.storage.config.handler.KeyHandler;
import org.jdownloader.extensions.ExtensionConfigPanel;
import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.jdtrayicon.translate._TRAY;
import org.jdownloader.images.NewTheme;
import org.jdownloader.settings.staticreferences.CFG_GUI;

public class TrayConfigPanel extends ExtensionConfigPanel<TrayExtension> {

    public TrayConfigPanel(TrayExtension trayExtension) {
        super(trayExtension);

        @SuppressWarnings("unchecked")
        KeyHandler<OnCloseAction> keyHandler = (KeyHandler<OnCloseAction>) CFG_TRAY_CONFIG.SH.getKeyHandler("OnCloseAction", KeyHandler.class);
        addPair(_TRAY._.plugins_optional_JDLightTray_closetotray2(), null, new ComboBox<OnCloseAction>(keyHandler, new OnCloseAction[] { OnCloseAction.ASK, OnCloseAction.TO_TRAY, OnCloseAction.TO_TASKBAR, OnCloseAction.EXIT }, new String[] { OnCloseAction.ASK.getTranslation(), OnCloseAction.TO_TRAY.getTranslation(), OnCloseAction.TO_TASKBAR.getTranslation(), OnCloseAction.EXIT.getTranslation() }));

        KeyHandler<OnMinimizeAction> keyHandler2 = (KeyHandler<OnMinimizeAction>) CFG_TRAY_CONFIG.SH.getKeyHandler("OnMinimizeAction", KeyHandler.class);
        addPair(_TRAY._.plugins_optional_JDLightTray_minimizetotray(), null, new ComboBox<OnMinimizeAction>(keyHandler2, new OnMinimizeAction[] { OnMinimizeAction.TO_TRAY, OnMinimizeAction.TO_TASKBAR }, new String[] { OnMinimizeAction.TO_TRAY.getTranslation(), OnMinimizeAction.TO_TASKBAR.getTranslation() }));
        addPair(_TRAY._.plugins_optional_JDLightTray_startMinimized(), null, new Checkbox(CFG_TRAY_CONFIG.START_MINIMIZED_ENABLED));
        addPair(_TRAY._.plugins_optional_JDLightTray_singleClick(), null, new Checkbox(CFG_TRAY_CONFIG.TOOGLE_WINDOW_STATUS_WITH_SINGLE_CLICK_ENABLED));

        addPair(_TRAY._.plugins_optional_JDLightTray_tooltip(), null, new Checkbox(CFG_TRAY_CONFIG.TOOL_TIP_ENABLED));
        addPair(_TRAY._.plugins_optional_JDLightTray_hideifframevisible(), null, new Checkbox(CFG_TRAY_CONFIG.TRAY_ONLY_VISIBLE_IF_WINDOW_IS_HIDDEN_ENABLED));
        addPair(_TRAY._.plugins_optional_JDLightTray_passwordRequired(), CFG_GUI.PASSWORD_PROTECTION_ENABLED, new PasswordInput(CFG_GUI.PASSWORD));
        addHeader(_TRAY._.plugins_optional_JDLightTray_ballon(), NewTheme.I().getIcon(IconKey.ICON_INFO, 32));
        addDescription(_TRAY._.plugins_optional_JDLightTray_ballon_desc());
        addPair(_TRAY._.plugins_optional_JDLightTray_ballon_newPackages(), null, new Checkbox(CFG_TRAY_CONFIG.BALLON_NOTIFY_ON_NEW_LINKGRABBER_PACKAGE_ENABLED));
        addPair(_TRAY._.plugins_optional_JDLightTray_ballon_newlinks(), null, new Checkbox(CFG_TRAY_CONFIG.BALLON_NOTIFY_ON_NEW_LINKGRABBER_LINKS_ENABLED));
    }

    @Override
    public void save() {
    }

    @Override
    public void updateContents() {

    }

}
