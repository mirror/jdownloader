package org.jdownloader.gui.mainmenu.container;

import org.jdownloader.controlling.contextmenu.MenuContainer;
import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.translate._GUI;

public class AboutMenuContainer extends MenuContainer {
    public AboutMenuContainer() {
        setName(_GUI.T.gui_menu_about());
        setIconKey(IconKey.ICON_HELP);

        // add(new AboutMenu());

    }

}
