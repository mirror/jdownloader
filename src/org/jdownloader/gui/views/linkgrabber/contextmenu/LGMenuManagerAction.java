package org.jdownloader.gui.views.linkgrabber.contextmenu;

import java.awt.event.ActionEvent;

import org.jdownloader.controlling.contextmenu.CustomizableAppAction;
import org.jdownloader.controlling.contextmenu.TableContext;
import org.jdownloader.gui.translate._GUI;

public class LGMenuManagerAction extends CustomizableAppAction {

    public LGMenuManagerAction() {

        setName(_GUI._.MenuManagerAction_MenuManagerAction());
        setIconKey("menu");
        addContextSetup(new TableContext(true, true));

    }

    @Override
    public void actionPerformed(ActionEvent e) {
        MenuManagerLinkgrabberTableContext.getInstance().openGui();
    }
}
