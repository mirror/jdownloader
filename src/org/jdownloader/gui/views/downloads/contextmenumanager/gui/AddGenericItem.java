package org.jdownloader.gui.views.downloads.contextmenumanager.gui;

import java.awt.event.ActionEvent;

import org.jdownloader.actions.AppAction;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.downloads.contextmenumanager.MenuItemData;

public class AddGenericItem extends AppAction {

    private ManagerFrame managerFrame;
    private MenuItemData item;

    public AddGenericItem(ManagerFrame managerFrame, MenuItemData separatorData) {
        setName(_GUI._.AddGenericItem_AddGenericItem_(separatorData.getName()));
        this.managerFrame = managerFrame;
        this.item = separatorData;
    }

    @Override
    public void actionPerformed(ActionEvent e) {

        managerFrame.addMenuItem(item);
    }

}
