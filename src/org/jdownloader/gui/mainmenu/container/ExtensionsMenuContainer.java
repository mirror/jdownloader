package org.jdownloader.gui.mainmenu.container;

import org.jdownloader.controlling.contextmenu.MenuContainer;
import org.jdownloader.gui.IconKey;
import org.jdownloader.translate._JDT;

public class ExtensionsMenuContainer extends MenuContainer {

    public ExtensionsMenuContainer(/* STorable */) {

        setName(_JDT.T.gui_menu_extensions());
        setIconKey(IconKey.ICON_EXTENSION);

    }

}
