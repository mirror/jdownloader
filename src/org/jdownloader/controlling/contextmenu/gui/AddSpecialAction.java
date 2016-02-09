package org.jdownloader.controlling.contextmenu.gui;

import java.awt.Window;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.swing.Icon;
import javax.swing.JComboBox;
import javax.swing.tree.TreePath;

import org.appwork.swing.components.searchcombo.SearchComboBox;
import org.appwork.utils.StringUtils;
import org.appwork.utils.logging2.extmanager.LoggerFactory;
import org.appwork.utils.swing.dialog.ComboBoxDialog;
import org.appwork.utils.swing.dialog.Dialog;
import org.appwork.utils.swing.dialog.DialogCanceledException;
import org.appwork.utils.swing.dialog.DialogClosedException;
import org.appwork.utils.swing.dialog.locator.RememberRelativeDialogLocator;
import org.jdownloader.actions.AppAction;
import org.jdownloader.controlling.contextmenu.ActionData;
import org.jdownloader.controlling.contextmenu.ContextMenuManager;
import org.jdownloader.controlling.contextmenu.MenuContainer;
import org.jdownloader.controlling.contextmenu.MenuItemData;
import org.jdownloader.controlling.contextmenu.MenuLink;
import org.jdownloader.controlling.contextmenu.SeparatorData;
import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.NewTheme;

public class AddSpecialAction extends AppAction {

    private ContextMenuManager manager;
    private MenuManagerDialog  managerFrame;

    public AddSpecialAction(MenuManagerDialog managerFrame) {
        this.manager = managerFrame.getManager();
        this.managerFrame = managerFrame;
        setName(_GUI.T.ManagerFrame_layoutPanel_add());
        setTooltipText(_GUI.T.ManagerFrame_layoutPanel_add());
        setIconKey(IconKey.ICON_ADD);
    }

    public String getName(Object value) {
        try {
            if (value == null) {
                return _GUI.T.AddActionAction_getListCellRendererComponent_no_action_();
            }
            if (value instanceof ActionData) {
                AppAction mi;

                mi = new MenuItemData(((ActionData) value)).createValidatedItem().createAction();

                String name = mi.getName();

                if (StringUtils.isEmpty(name)) {
                    name = mi.getClass().getSimpleName() + "(" + mi.getTooltipText() + ")";
                }

                return name;

            } else if (value instanceof MenuLink) {
                return _GUI.T.AddSubMenuAction_component(((MenuLink) value).getName());

            } else if (value instanceof SeparatorData) {
                return _GUI.T.AddSpecialAction_actionPerformed_separator();
            } else if (value instanceof MenuContainer) {

                return _GUI.T.AddSubMenuAction_getListCellRendererComponent_container(((MenuContainer) value).getName());

            }

        } catch (Exception e) {
            e.printStackTrace();

        }
        return value + "";
    }

    @Override
    public void actionPerformed(ActionEvent e) {

        List<Object> actions = new ArrayList<Object>();

        List<Object> normalActions = managerFrame.getManager().list();
        actions.addAll(normalActions);
        TreePath addAt = managerFrame.getSelectionPath();

        Collections.sort(actions, new Comparator<Object>() {

            @Override
            public int compare(Object o1, Object o2) {

                return getName(o1).compareTo(getName(o2));
            }
        });

        ComboBoxDialog d = new ComboBoxDialog(0, _GUI.T.AddSpecialAction_actionPerformed_title(), _GUI.T.AddSpecialAction_actionPerformed_msg(), actions.toArray(new Object[] {}), 0, null, _GUI.T.lit_add(), null, null) {
            @Override
            protected JComboBox getComboBox(Object[] options2) {
                // return super.getComboBox(options2);

                SearchComboBox<Object> ret = new SearchComboBox<Object>(options2) {

                    @Override
                    protected Icon getIconForValue(Object value) {
                        try {
                            if (value == null) {
                                return null;
                            }
                            if (value instanceof ActionData) {
                                AppAction mi;

                                mi = new MenuItemData(((ActionData) value)).createValidatedItem().createAction();

                                return mi.getSmallIcon();

                            } else if (value instanceof MenuLink) {

                                return (NewTheme.I().getIcon(((MenuLink) value).getIconKey(), 22));

                            } else if (value instanceof MenuContainer) {

                                return (NewTheme.I().getIcon(((MenuContainer) value).getIconKey(), 22));

                            }
                        } catch (Exception e) {
                            LoggerFactory.getDefaultLogger().log(e);
                        }
                        return null;
                    }

                    @Override
                    protected String getTextForValue(Object value) {
                        try {
                            return AddSpecialAction.this.getName(value);

                        } catch (Exception e) {
                            LoggerFactory.getDefaultLogger().log(e);
                        }
                        return value + "";

                    }

                };

                return ret;
            }

            @Override
            protected int getPreferredWidth() {
                return 450;
            }

            @Override
            public Window getOwner() {
                return managerFrame.getDialog();
            }
        };
        d.setLocator(new RememberRelativeDialogLocator("AddSpecialAction", managerFrame.getDialog()));
        //
        try {
            Integer ret = Dialog.getInstance().showDialog(d);
            if (ret >= 0) {
                if (actions.get(ret) instanceof ActionData) {
                    managerFrame.addAction((ActionData) actions.get(ret));
                } else {
                    managerFrame.addMenuItem((MenuItemData) actions.get(ret));
                }
            }

        } catch (DialogClosedException e1) {
            e1.printStackTrace();
        } catch (DialogCanceledException e1) {
            e1.printStackTrace();
        }
    }
}