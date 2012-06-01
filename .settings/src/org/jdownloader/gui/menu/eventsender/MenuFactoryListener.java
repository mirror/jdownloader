package org.jdownloader.gui.menu.eventsender;

import java.util.EventListener;

import org.jdownloader.gui.menu.MenuContext;

public interface MenuFactoryListener extends EventListener {

    void onExtendPopupMenu(MenuContext<?> context);

}