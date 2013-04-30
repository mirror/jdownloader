package org.jdownloader.gui.views.downloads.contextmenumanager.gui;

import java.awt.Component;
import java.awt.event.ActionEvent;
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
import org.jdownloader.gui.views.downloads.contextmenumanager.ActionData;
import org.jdownloader.gui.views.downloads.contextmenumanager.MenuItemData;

public class AddActionAction extends AppAction {

    private ManagerFrame managerFrame;

    {
        setName(_GUI._.ManagerFrame_layoutPanel_add());

    }

    public AddActionAction(ManagerFrame managerFrame) {
        this.managerFrame = managerFrame;
    }

    @Override
    public void actionPerformed(ActionEvent e) {

        List<ActionData> actions = managerFrame.getManager().list();
        List<MenuItemData> menuitems = managerFrame.getManager().listSpecialItems();

        ComboBoxDialog d = new ComboBoxDialog(0, _GUI._.ManagerFrame_actionPerformed_addaction_title(), _GUI._.ManagerFrame_actionPerformed_addaction_msg(), actions.toArray(new Object[] {}), 0, null, _GUI._.lit_add(), null, null) {
            protected ListCellRenderer getRenderer(final ListCellRenderer orgRenderer) {
                // TODO Auto-generated method stub
                return new ListCellRenderer() {

                    @Override
                    public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                        AppAction mi;
                        try {
                            mi = new MenuItemData(((ActionData) value)).createAction(null);

                            JLabel ret = (JLabel) orgRenderer.getListCellRendererComponent(list, mi.getName(), index, isSelected, cellHasFocus);
                            ret.setIcon(mi.getSmallIcon());

                            return ret;
                        } catch (Exception e) {
                            JLabel ret = (JLabel) orgRenderer.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

                            return ret;
                        }
                    }
                };
            }
        };

        try {
            Integer ret = Dialog.getInstance().showDialog(d);
            ActionData action = actions.get(ret);
            // new MenuItemData(action)
            managerFrame.addAction(action);

        } catch (DialogClosedException e1) {
            e1.printStackTrace();
        } catch (DialogCanceledException e1) {
            e1.printStackTrace();
        }
    }

}
