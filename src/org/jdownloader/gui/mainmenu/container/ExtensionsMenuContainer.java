package org.jdownloader.gui.mainmenu.container;

import java.util.HashSet;

import org.jdownloader.controlling.contextmenu.MenuContainer;
import org.jdownloader.controlling.contextmenu.MenuItemProperty;
import org.jdownloader.translate._JDT;

public class ExtensionsMenuContainer extends MenuContainer {
    public ExtensionsMenuContainer(/* STorable */) {
        this((MenuItemProperty[]) null);
    }

    public ExtensionsMenuContainer(MenuItemProperty... properties) {

        setName(_JDT._.gui_menu_extensions());
        setIconKey("extension");
        if (properties != null && properties.length > 0) {
            HashSet<MenuItemProperty> props = new HashSet<MenuItemProperty>();
            for (MenuItemProperty mip : properties) {
                props.add(mip);
            }
            setProperties(props);
        }

    }

}
