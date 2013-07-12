package org.jdownloader.gui.mainmenu.container;

import org.jdownloader.controlling.contextmenu.MenuContainer;
import org.jdownloader.translate._JDT;

public class ExtensionsMenuContainer extends MenuContainer {

    public ExtensionsMenuContainer(/* STorable */) {

        setName(_JDT._.gui_menu_extensions());
        setIconKey("extension");

    }

}
