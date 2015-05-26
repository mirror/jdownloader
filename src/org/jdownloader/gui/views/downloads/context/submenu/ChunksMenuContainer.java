package org.jdownloader.gui.views.downloads.context.submenu;

import org.jdownloader.controlling.contextmenu.MenuContainer;
import org.jdownloader.gui.translate._GUI;

public class ChunksMenuContainer extends MenuContainer {
    final static String NAME = _GUI._.ChunksEditor_ChunksEditor_();

    public ChunksMenuContainer() {
        setName(NAME);
        setIconKey("chunks");
    }
}
