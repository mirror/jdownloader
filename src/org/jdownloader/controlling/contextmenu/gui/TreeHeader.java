package org.jdownloader.controlling.contextmenu.gui;

import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.AbstractIcon;

public class TreeHeader extends Header {

    public TreeHeader() {
        super(_GUI.T.ManagerFrame_layoutPanel_menustructure_(), new AbstractIcon(IconKey.ICON_MENU, 16));
    }

}
