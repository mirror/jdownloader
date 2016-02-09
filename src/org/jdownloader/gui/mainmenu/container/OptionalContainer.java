package org.jdownloader.gui.mainmenu.container;

import org.jdownloader.controlling.contextmenu.MenuContainer;
import org.jdownloader.gui.IconKey;
import org.jdownloader.translate._JDT;

public class OptionalContainer extends MenuContainer {

    public OptionalContainer(/* STorable */) {

        setName(_JDT.T.OptionalContainer_OptionalContainer());
        setIconKey(IconKey.ICON_MENU);

    }

    public OptionalContainer(boolean b) {
        this();
        setVisible(b);
    }

}
