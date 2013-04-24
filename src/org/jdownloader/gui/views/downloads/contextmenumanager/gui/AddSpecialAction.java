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
import org.jdownloader.gui.views.downloads.contextmenumanager.AddonSubMenuLink;
import org.jdownloader.gui.views.downloads.contextmenumanager.DownloadListContextMenuManager;
import org.jdownloader.gui.views.downloads.contextmenumanager.MenuLink;
import org.jdownloader.gui.views.downloads.contextmenumanager.SeparatorData;
import org.jdownloader.images.NewTheme;

public class AddSpecialAction extends AppAction {

    private DownloadListContextMenuManager manager;
    private ManagerFrame                   managerFrame;

    public AddSpecialAction(ManagerFrame managerFrame) {
        this.manager = managerFrame.getManager();
        this.managerFrame = managerFrame;
        setName(_GUI._.ManagerFrame_layoutPanel_addspecials());
        setSmallIcon(NewTheme.I().getIcon("add", 20));
    }

    @Override
    public void actionPerformed(ActionEvent e) {

        List<Object> actions = new ArrayList<Object>();
        actions.add(_GUI._.AddSpecialAction_actionPerformed_seperator());
        actions.add(new AddonSubMenuLink());

        ComboBoxDialog d = new ComboBoxDialog(0, _GUI._.AddSpecialAction_actionPerformed_title(), _GUI._.AddSpecialAction_actionPerformed_msg(), actions.toArray(new Object[] {}), 0, null, _GUI._.lit_add(), null, null) {
            protected ListCellRenderer getRenderer(final ListCellRenderer orgRenderer) {
                // TODO Auto-generated method stub
                return new ListCellRenderer() {

                    @Override
                    public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                        if (value instanceof MenuLink) {

                            JLabel ret = (JLabel) orgRenderer.getListCellRendererComponent(list, _GUI._.AddSubMenuAction_getListCellRendererComponent(((MenuLink) value).getName()), index, isSelected, cellHasFocus);
                            ret.setIcon(NewTheme.I().getIcon(((MenuLink) value).getIconKey(), 22));
                            return ret;

                        } else {
                            JLabel ret = (JLabel) orgRenderer.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                            ret.setIcon(null);
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
                managerFrame.addMenuItem(new SeparatorData());

            } else if (ret == 1) {
                managerFrame.addMenuItem(new AddonSubMenuLink());

            }

        } catch (DialogClosedException e1) {
            e1.printStackTrace();
        } catch (DialogCanceledException e1) {
            e1.printStackTrace();
        }
    }

}