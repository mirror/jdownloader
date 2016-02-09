package org.jdownloader.gui.mainmenu.container;

import org.jdownloader.controlling.contextmenu.MenuContainer;
import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.translate._GUI;

public class SettingsMenuContainer extends MenuContainer {
    public SettingsMenuContainer() {
        setName(_GUI.T.SettingsMenu_SettingsMenu_());
        setIconKey(IconKey.ICON_SETTINGS);

    }

}
