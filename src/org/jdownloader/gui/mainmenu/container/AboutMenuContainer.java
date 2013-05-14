package org.jdownloader.gui.mainmenu.container;

import org.jdownloader.controlling.contextmenu.MenuContainer;
import org.jdownloader.gui.translate._GUI;

public class AboutMenuContainer extends MenuContainer {
    public AboutMenuContainer() {
        setName(_GUI._.gui_menu_about());
        setIconKey("help");

        // add(new AboutMenu());

    }

}
