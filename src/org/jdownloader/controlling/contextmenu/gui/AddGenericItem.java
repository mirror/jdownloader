package org.jdownloader.controlling.contextmenu.gui;

import java.awt.event.ActionEvent;

import org.jdownloader.actions.AppAction;
import org.jdownloader.controlling.contextmenu.MenuItemData;
import org.jdownloader.gui.translate._GUI;

public class AddGenericItem extends AppAction {

    private MenuManagerDialog managerFrame;
    private MenuItemData item;

    public AddGenericItem(MenuManagerDialog managerFrame, MenuItemData separatorData) {
        setName(_GUI._.AddGenericItem_AddGenericItem_(separatorData.getName()));
        this.managerFrame = managerFrame;
        this.item = separatorData;
    }

    @Override
    public void actionPerformed(ActionEvent e) {

        managerFrame.addMenuItem(item);
    }

}
