package org.jdownloader.gui.mainmenu.container;

import org.jdownloader.controlling.contextmenu.MenuContainer;
import org.jdownloader.gui.translate._GUI;

public class ReportErrorContainer extends MenuContainer {
    public ReportErrorContainer() {
        setName(_GUI._.gui_menu_error_report());
        setIconKey("error");

        // add(new AboutMenu());

    }

}
