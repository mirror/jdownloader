package org.jdownloader.controlling.contextmenu.gui;

import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.AbstractIcon;

public class OptionsPaneHeader extends Header {

    public OptionsPaneHeader() {
        super(_GUI.T.ManagerFrame_layoutPanel_info(), new AbstractIcon(IconKey.ICON_EDIT, 16));
    }

}
