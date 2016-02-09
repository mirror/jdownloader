package org.jdownloader.gui.views.downloads.context.submenu;

import org.jdownloader.controlling.contextmenu.MenuContainer;
import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.translate._GUI;

public class DevChunksMenuContainer extends MenuContainer {
    final static String NAME = _GUI.T.ChunksEditor_ChunksEditor_();

    public DevChunksMenuContainer() {
        setName(NAME);
        setIconKey(IconKey.ICON_CHUNKS);
    }
}
