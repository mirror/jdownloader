package org.jdownloader.gui.mainmenu.container;

import org.jdownloader.controlling.contextmenu.MenuContainer;
import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.translate._GUI;

public class FileMenuContainer extends MenuContainer {
    public FileMenuContainer() {
        setName(_GUI.T.jd_gui_skins_simple_simplegui_menubar_filemenu());
        setIconKey(IconKey.ICON_FILE);

    }

}
