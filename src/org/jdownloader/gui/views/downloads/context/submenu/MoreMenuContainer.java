package org.jdownloader.gui.views.downloads.context.submenu;

import org.jdownloader.controlling.contextmenu.MenuContainer;
import org.jdownloader.gui.translate._GUI;

public class MoreMenuContainer extends MenuContainer {
    public MoreMenuContainer() {
        setName(_GUI._.ContextMenuFactory_createPopup_other());
        setIconKey("batch");

    }
}
