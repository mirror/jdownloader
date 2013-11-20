package org.jdownloader.controlling.contextmenu.gui;

import java.awt.event.ActionEvent;

import org.jdownloader.actions.AppAction;
import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.translate._GUI;

public class RemoveAction extends AppAction {

    private MenuManagerDialog managerFrame;

    public RemoveAction(MenuManagerDialog managerFrame) {
        this.managerFrame = managerFrame;
        setTooltipText(_GUI._.literally_remove());
        setName(_GUI._.literally_remove());
        setIconKey(IconKey.ICON_REMOVE);
    }

    @Override
    public void actionPerformed(ActionEvent e) {

        managerFrame.deleteSelection();

    }

}
