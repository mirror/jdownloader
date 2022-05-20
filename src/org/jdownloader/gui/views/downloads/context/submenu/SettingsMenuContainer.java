package org.jdownloader.gui.views.downloads.context.submenu;

import org.jdownloader.controlling.contextmenu.MenuContainer;
import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.translate._GUI;

public class SettingsMenuContainer extends MenuContainer {
    private final static String NAME = _GUI.T.ContextMenuFactory_createPopup_properties_package();

    public SettingsMenuContainer() {
        setName(NAME);
        setIconKey(IconKey.ICON_SETTINGS);
    }
}
