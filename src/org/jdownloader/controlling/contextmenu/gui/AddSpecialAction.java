package org.jdownloader.controlling.contextmenu.gui;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.ListCellRenderer;

import org.appwork.utils.StringUtils;
import org.appwork.utils.swing.dialog.ComboBoxDialog;
import org.appwork.utils.swing.dialog.Dialog;
import org.appwork.utils.swing.dialog.DialogCanceledException;
import org.appwork.utils.swing.dialog.DialogClosedException;
import org.jdownloader.actions.AppAction;
import org.jdownloader.controlling.contextmenu.ContextMenuManager;
import org.jdownloader.controlling.contextmenu.MenuContainer;
import org.jdownloader.controlling.contextmenu.MenuItemData;
import org.jdownloader.controlling.contextmenu.MenuLink;
import org.jdownloader.controlling.contextmenu.SeperatorData;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.NewTheme;

public class AddSpecialAction extends AppAction {

    private ContextMenuManager manager;
    private ManagerFrame       managerFrame;

    public AddSpecialAction(ManagerFrame managerFrame) {
        this.manager = managerFrame.getManager();
        this.managerFrame = managerFrame;
        setTooltipText(_GUI._.ManagerFrame_layoutPanel_addspecials());
        setIconKey("add_special");
        setName(_GUI._.ManagerFrame_layoutPanel_addspecials());
    }

    @Override
    public void actionPerformed(ActionEvent e) {

        List<MenuItemData> actions = new ArrayList<MenuItemData>();

        actions.addAll(manager.listSpecialItems());

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
                        } else if (value instanceof MenuContainer) {

                            JLabel ret = (JLabel) orgRenderer.getListCellRendererComponent(list, _GUI._.AddSubMenuAction_getListCellRendererComponent(((MenuContainer) value).getName()), index, isSelected, cellHasFocus);
                            if (StringUtils.isNotEmpty(((MenuContainer) value).getIconKey())) {
                                ret.setIcon(NewTheme.I().getIcon(((MenuContainer) value).getIconKey(), 22));
                            } else {
                                ret.setIcon(null);
                            }
                            return ret;
                        } else if (value instanceof SeperatorData) {
                            JLabel ret = (JLabel) orgRenderer.getListCellRendererComponent(list, _GUI._.AddSpecialAction_actionPerformed_seperator(), index, isSelected, cellHasFocus);
                            ret.setIcon(null);
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
            if (ret >= 0) {

                managerFrame.addMenuItem(actions.get(ret));

            }

        } catch (DialogClosedException e1) {
            e1.printStackTrace();
        } catch (DialogCanceledException e1) {
            e1.printStackTrace();
        }
    }
}