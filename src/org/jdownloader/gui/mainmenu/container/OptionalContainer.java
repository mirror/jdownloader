package org.jdownloader.gui.mainmenu.container;

import java.util.HashSet;

import org.jdownloader.controlling.contextmenu.MenuContainer;
import org.jdownloader.controlling.contextmenu.MenuItemProperty;
import org.jdownloader.translate._JDT;

public class OptionalContainer extends MenuContainer {
    public OptionalContainer(/* STorable */) {
        this((MenuItemProperty[]) null);
    }

    public OptionalContainer(MenuItemProperty... properties) {

        setName(_JDT._.OptionalContainer_OptionalContainer());
        setIconKey("menu");
        if (properties != null && properties.length > 0) {
            HashSet<MenuItemProperty> props = new HashSet<MenuItemProperty>();
            for (MenuItemProperty mip : properties) {
                props.add(mip);
            }
            setProperties(props);
        }

    }

}
