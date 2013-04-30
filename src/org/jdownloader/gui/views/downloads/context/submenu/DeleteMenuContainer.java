package org.jdownloader.gui.views.downloads.context.submenu;

import org.jdownloader.controlling.contextmenu.MenuContainer;
import org.jdownloader.gui.translate._GUI;

public class DeleteMenuContainer extends MenuContainer {
    public DeleteMenuContainer() {
        setName(_GUI._.ContextMenuFactory_createPopup_cleanup_only());
        setIconKey("delete");

    }
}
