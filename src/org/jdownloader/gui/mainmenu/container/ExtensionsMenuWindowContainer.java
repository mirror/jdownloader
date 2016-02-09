package org.jdownloader.gui.mainmenu.container;

import org.jdownloader.controlling.contextmenu.MenuContainer;
import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.translate._GUI;

public class ExtensionsMenuWindowContainer extends MenuContainer {
    public ExtensionsMenuWindowContainer() {
        setName(_GUI.T.AddonsMenu_updateMenu_windows_());
        setIconKey(IconKey.ICON_EXTENSION);

    }

}
