package org.jdownloader.gui.views.downloads.contextmenumanager.gui;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.ListCellRenderer;

import org.appwork.utils.swing.dialog.ComboBoxDialog;
import org.appwork.utils.swing.dialog.Dialog;
import org.appwork.utils.swing.dialog.DialogCanceledException;
import org.appwork.utils.swing.dialog.DialogClosedException;
import org.jdownloader.actions.AppAction;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.downloads.contextmenumanager.MenuContainer;
import org.jdownloader.gui.views.downloads.contextmenumanager.MenuItemData;
import org.jdownloader.images.NewTheme;

public class AddSubMenuAction extends AppAction {

    private ManagerFrame managerFrame;

    public AddSubMenuAction(ManagerFrame managerFrame) {
        this.managerFrame = managerFrame;
        setName(_GUI._.ManagerFrame_layoutPanel_addSubmenu());
        setSmallIcon(NewTheme.I().getIcon("add", 20));
    }

    @Override
    public void actionPerformed(ActionEvent e) {

        List<MenuItemData> list = managerFrame.getManager().listSpecialItems();
        ArrayList<Object> actions = new ArrayList<Object>();
        actions.add(_GUI._.ManagerFrame_actionPerformed_customfolder());

        actions.addAll(list);
        ComboBoxDialog d = new ComboBoxDialog(0, _GUI._.ManagerFrame_actionPerformed_addaction_title(), _GUI._.ManagerFrame_actionPerformed_addaction_msg(), actions.toArray(new Object[] {}), 0, null, _GUI._.lit_add(), null, null) {
            protected ListCellRenderer getRenderer(final ListCellRenderer orgRenderer) {
                // TODO Auto-generated method stub
                return new ListCellRenderer() {

                    @Override
                    public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                        if (value instanceof MenuContainer) {

                            JLabel ret = (JLabel) orgRenderer.getListCellRendererComponent(list, _GUI._.AddSubMenuAction_getListCellRendererComponent(((MenuContainer) value).getName()), index, isSelected, cellHasFocus);
                            ret.setIcon(NewTheme.I().getIcon(((MenuContainer) value).getIconKey(), 22));
                            return ret;
                        } else {
                            JLabel ret = (JLabel) orgRenderer.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                            ret.setIcon(NewTheme.I().getIcon("help", 22));
                            return ret;
                        }

                    }
                };
            }
        };
        //
        try {
            Integer ret = Dialog.getInstance().showDialog(d);

            if (ret == 0) {
                NewSubMenuDialog newDialog = new NewSubMenuDialog();
                Dialog.getInstance().showDialog(newDialog);
                String name = newDialog.getName();
                String iconKey = newDialog.getIconKey();

                managerFrame.addMenuItem((MenuItemData) new MenuContainer(name, iconKey));

            } else if (ret > 0) {
                Object action = actions.get(ret);
                managerFrame.addMenuItem((MenuItemData) action);

            }

        } catch (DialogClosedException e1) {
            e1.printStackTrace();
        } catch (DialogCanceledException e1) {
            e1.printStackTrace();
        }
    }

}
