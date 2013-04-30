package org.jdownloader.controlling.contextmenu.gui;

import java.awt.event.ActionEvent;

import org.appwork.utils.swing.dialog.Dialog;
import org.appwork.utils.swing.dialog.DialogCanceledException;
import org.appwork.utils.swing.dialog.DialogClosedException;
import org.jdownloader.actions.AppAction;
import org.jdownloader.controlling.contextmenu.MenuContainer;
import org.jdownloader.controlling.contextmenu.MenuItemData;
import org.jdownloader.gui.translate._GUI;

public class AddSubMenuAction extends AppAction {

    private ManagerFrame managerFrame;

    public AddSubMenuAction(ManagerFrame managerFrame) {
        this.managerFrame = managerFrame;
        setName(_GUI._.ManagerFrame_layoutPanel_addSubmenu());

    }

    @Override
    public void actionPerformed(ActionEvent e) {

        try {

            NewSubMenuDialog newDialog = new NewSubMenuDialog();
            Dialog.getInstance().showDialog(newDialog);
            String name = newDialog.getName();
            String iconKey = newDialog.getIconKey();

            managerFrame.addMenuItem((MenuItemData) new MenuContainer(name, iconKey));

        } catch (DialogClosedException e1) {
            e1.printStackTrace();
        } catch (DialogCanceledException e1) {
            e1.printStackTrace();
        }
    }

}
