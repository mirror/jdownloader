package org.jdownloader.gui.views.linkgrabber.contextmenu;

import org.jdownloader.controlling.contextmenu.MenuContainer;
import org.jdownloader.gui.translate._GUI;

public class LinkgrabberCleanUpSubMenu extends MenuContainer {
    public LinkgrabberCleanUpSubMenu() {
        super(_GUI._.ContextMenuFactory_linkgrabber_createPopup_cleanup(), "clear");

    }
}
