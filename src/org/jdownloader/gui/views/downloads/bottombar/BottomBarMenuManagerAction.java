package org.jdownloader.gui.views.downloads.bottombar;

import java.awt.event.ActionEvent;

import org.jdownloader.controlling.contextmenu.CustomizableAppAction;
import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.downloads.MenuManagerDownloadTabBottomBar;

public class BottomBarMenuManagerAction extends CustomizableAppAction {

    public BottomBarMenuManagerAction() {

        setName(_GUI._.BottomBarMenuManagerAction_BottomBarMenuManagerAction());
        setIconKey(IconKey.ICON_EDIT);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        MenuManagerDownloadTabBottomBar.getInstance().openGui();

    }

}
