package org.jdownloader.controlling.contextmenu.gui;

import java.awt.Window;
import java.awt.event.ActionEvent;

import org.appwork.utils.swing.dialog.Dialog;
import org.appwork.utils.swing.dialog.DialogCanceledException;
import org.appwork.utils.swing.dialog.DialogClosedException;
import org.appwork.utils.swing.dialog.locator.RememberRelativeDialogLocator;
import org.jdownloader.actions.AppAction;
import org.jdownloader.controlling.contextmenu.MenuContainer;
import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.translate._GUI;

public class AddSubMenuAction extends AppAction {

    private MenuManagerDialog managerFrame;

    public AddSubMenuAction(MenuManagerDialog managerFrame) {
        this.managerFrame = managerFrame;
        setTooltipText(_GUI.T.ManagerFrame_layoutPanel_addSubmenu());
        setIconKey(IconKey.ICON_MENU);
        setName(_GUI.T.ManagerFrame_layoutPanel_addSubmenu());
    }

    @Override
    public void actionPerformed(ActionEvent e) {

        try {

            NewSubMenuDialog newDialog = new NewSubMenuDialog() {
                @Override
                public Window getOwner() {
                    return managerFrame.getDialog();
                }
            };
            newDialog.setLocator(new RememberRelativeDialogLocator("NewSubMenuDialog", managerFrame.getDialog()));
            Dialog.getInstance().showDialog(newDialog);
            String name = newDialog.getName();
            String iconKey = newDialog.getIconKey();

            managerFrame.addMenuItem(new MenuContainer(name, iconKey));

        } catch (DialogClosedException e1) {
            e1.printStackTrace();
        } catch (DialogCanceledException e1) {
            e1.printStackTrace();
        }
    }

}
