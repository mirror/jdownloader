package org.jdownloader.gui.mainmenu.container;

import org.jdownloader.controlling.contextmenu.MenuContainer;
import org.jdownloader.translate._JDT;

public class OptionalContainer extends MenuContainer {

    public OptionalContainer(/* STorable */) {

        setName(_JDT._.OptionalContainer_OptionalContainer());
        setIconKey("menu");

    }

    public OptionalContainer(boolean b) {
        this();
        setVisible(b);
    }

}
