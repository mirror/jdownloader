package org.jdownloader.gui.views.downloads.context.submenu;

import org.jdownloader.controlling.contextmenu.MenuContainer;
import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.translate._GUI;

public class DeleteMenuContainer extends MenuContainer {
    public DeleteMenuContainer() {
        setName(_GUI.T.DeleteMenuContainer_DeleteMenuContainer_delete_2());
        setIconKey(IconKey.ICON_CLEAR);

    }
}
