package org.jdownloader.controlling.contextmenu.gui;

import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.NewTheme;

public class OptionsPaneHeader extends Header {

    public OptionsPaneHeader() {
        super(_GUI._.ManagerFrame_layoutPanel_info(), NewTheme.I().getIcon("edit", 16));
    }

}
