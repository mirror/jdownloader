package org.jdownloader.controlling.contextmenu.gui;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.ListCellRenderer;
import javax.swing.tree.TreePath;

import org.appwork.utils.StringUtils;
import org.appwork.utils.swing.dialog.ComboBoxDialog;
import org.appwork.utils.swing.dialog.Dialog;
import org.appwork.utils.swing.dialog.DialogCanceledException;
import org.appwork.utils.swing.dialog.DialogClosedException;
import org.jdownloader.actions.AppAction;
import org.jdownloader.controlling.contextmenu.ActionData;
import org.jdownloader.controlling.contextmenu.MenuItemData;
import org.jdownloader.controlling.contextmenu.MenuItemData.Type;
import org.jdownloader.gui.translate._GUI;

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
        TreePath addAt = managerFrame.getSelectionPath();
        MenuItemData parent = null;
        if (((MenuItemData) addAt.getLastPathComponent()).getType() == Type.CONTAINER) {

            parent = ((MenuItemData) addAt.getLastPathComponent());
        } else {
            parent = (MenuItemData) addAt.getPathComponent(addAt.getPathCount() - 2);

        }
        for (MenuItemData mid : parent.getItems()) {
            for (Iterator<ActionData> it = actions.iterator(); it.hasNext();) {
                ActionData next = it.next();
                if (mid.getActionData() != null && StringUtils.equals(next.getClazzName(), mid.getActionData().getClazzName())) {
                    it.remove();
                }
            }

        }
        actions = new ArrayList<ActionData>(actions);
        Collections.sort(actions, new Comparator<ActionData>() {

            @Override
            public int compare(ActionData o1, ActionData o2) {
                return o1.getClazzName().compareTo(o2.getClazzName());
            }
        });
        ComboBoxDialog d = new ComboBoxDialog(0, _GUI._.ManagerFrame_actionPerformed_addaction_title(), _GUI._.ManagerFrame_actionPerformed_addaction_msg(), actions.toArray(new Object[] {}), 0, null, _GUI._.lit_add(), null, null) {
            protected ListCellRenderer getRenderer(final ListCellRenderer orgRenderer) {
                // TODO Auto-generated method stub
                return new ListCellRenderer() {

                    @Override
                    public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                        if (value == null) return (JLabel) orgRenderer.getListCellRendererComponent(list, _GUI._.AddActionAction_getListCellRendererComponent_no_action_(), index, isSelected, cellHasFocus);
                        AppAction mi;
                        try {
                            mi = new MenuItemData(((ActionData) value)).createValidatedItem().createAction(null);

                            String name = mi.getName();

                            if (StringUtils.isEmpty(name)) {
                                name = mi.getClass().getSimpleName() + "(" + mi.getTooltipText() + ")";
                            }

                            JLabel ret = (JLabel) orgRenderer.getListCellRendererComponent(list, name, index, isSelected, cellHasFocus);
                            ret.setIcon(mi.getSmallIcon());

                            return ret;
                        } catch (Exception e) {
                            e.printStackTrace();
                            JLabel ret = (JLabel) orgRenderer.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

                            return ret;
                        }
                    }
                };
            }
        };

        try {
            Integer ret = Dialog.getInstance().showDialog(d);
            if (ret >= 0) {
                ActionData action = actions.get(ret);
                // new MenuItemData(action)
                managerFrame.addAction(action);
            }
        } catch (DialogClosedException e1) {
            e1.printStackTrace();
        } catch (DialogCanceledException e1) {
            e1.printStackTrace();
        }
    }

}
