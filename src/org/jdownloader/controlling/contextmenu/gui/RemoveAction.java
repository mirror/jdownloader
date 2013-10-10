package org.jdownloader.controlling.contextmenu.gui;

import java.awt.event.ActionEvent;

import org.jdownloader.actions.AppAction;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.NewTheme;

public class RemoveAction extends AppAction {

    private MenuManagerDialog managerFrame;

    public RemoveAction(MenuManagerDialog managerFrame) {
        this.managerFrame = managerFrame;
        setTooltipText(_GUI._.literally_remove());
        setSmallIcon(NewTheme.I().getIcon("delete", 20));
        setName(_GUI._.ManagerFrame_layoutPanel_remove());
    }

    @Override
    public void actionPerformed(ActionEvent e) {

        managerFrame.deleteSelection();

    }

}
