package org.jdownloader.gui.views.downloads.contextmenumanager.gui;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.Box;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.ListCellRenderer;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.TreePath;

import jd.gui.swing.laf.LookAndFeelController;

import org.appwork.app.gui.BasicGui;
import org.appwork.swing.MigPanel;
import org.appwork.swing.components.ExtButton;
import org.appwork.utils.Application;
import org.appwork.utils.swing.EDTRunner;
import org.appwork.utils.swing.dialog.ComboBoxDialog;
import org.appwork.utils.swing.dialog.Dialog;
import org.appwork.utils.swing.dialog.DialogCanceledException;
import org.appwork.utils.swing.dialog.DialogClosedException;
import org.jdownloader.actions.AppAction;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.components.HeaderScrollPane;
import org.jdownloader.gui.views.downloads.contextmenumanager.ActionData;
import org.jdownloader.gui.views.downloads.contextmenumanager.DownloadListContextMenuManager;
import org.jdownloader.gui.views.downloads.contextmenumanager.MenuContainer;
import org.jdownloader.gui.views.downloads.contextmenumanager.MenuContainerRoot;
import org.jdownloader.gui.views.downloads.contextmenumanager.MenuItemData;
import org.jdownloader.images.NewTheme;

public class ManagerFrame extends BasicGui {
    public static void main(String[] args) {
        new EDTRunner() {

            @Override
            protected void runInEDT() {
                Application.setApplication(".jd_home");
                LookAndFeelController.getInstance().init();

                new ManagerFrame();
            }
        };

    }

    private InfoPanel                      infoPanel;
    private DownloadListContextMenuManager manager;

    public ManagerFrame() {
        super(_GUI._.ManagerFrame_ManagerFrame_());

    }

    @Override
    protected void layoutPanel() {
        manager = new DownloadListContextMenuManager();
        MigPanel panel = new MigPanel("ins 2,wrap 2", "[grow,fill][300!,fill]", "[grow,fill][]");
        panel.setOpaque(false);
        LookAndFeelController.getInstance().getLAFOptions().applyPanelBackgroundColor(panel);

        final ManagerTreeModel model = new ManagerTreeModel(manager.getMenuData());
        final ExtTree tree = new ExtTree(model);
        tree.getSelectionModel().addTreeSelectionListener(new TreeSelectionListener() {

            @Override
            public void valueChanged(TreeSelectionEvent e) {
                TreePath sel = tree.getSelectionPath();
                if (sel == null) {
                    infoPanel.updateInfo(null);
                } else {
                    infoPanel.updateInfo((MenuItemData) sel.getLastPathComponent());
                }
            }
        });
        LookAndFeelController.getInstance().getLAFOptions().applyPanelBackgroundColor(tree);

        // tree.set
        // tree.setShowsRootHandles(false);
        HeaderScrollPane sp = new HeaderScrollPane(tree);
        sp.setColumnHeaderView(new TreeHeader());
        panel.add(sp);
        infoPanel = new InfoPanel();
        LookAndFeelController.getInstance().getLAFOptions().applyPanelBackgroundColor(infoPanel);
        sp = new HeaderScrollPane(infoPanel);
        sp.setColumnHeaderView(new OptionsPaneHeader());
        panel.add(sp);
        MigPanel bottom = new MigPanel("ins 0", "[][][][][grow,fill][][]", "[]");
        ExtButton save = new ExtButton(new AppAction() {
            {
                setName(_GUI._.lit_save());
                setSmallIcon(NewTheme.I().getIcon("save", 20));
            }

            @Override
            public void actionPerformed(ActionEvent e) {

                manager.setMenuData((MenuContainerRoot) model.getRoot());
            }

        });

        ExtButton add = new ExtButton(new AppAction() {
            {
                setName(_GUI._.ManagerFrame_layoutPanel_add());
                setSmallIcon(NewTheme.I().getIcon("add", 20));
            }

            @Override
            public void actionPerformed(ActionEvent e) {

                List<ActionData> actions = manager.list();
                List<MenuItemData> menuitems = manager.listSpecialItems();

                ComboBoxDialog d = new ComboBoxDialog(0, _GUI._.ManagerFrame_actionPerformed_addaction_title(), _GUI._.ManagerFrame_actionPerformed_addaction_msg(), actions.toArray(new Object[] {}), 0, null, "Add Action", null, null) {
                    protected ListCellRenderer getRenderer(final ListCellRenderer orgRenderer) {
                        // TODO Auto-generated method stub
                        return new ListCellRenderer() {

                            @Override
                            public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                                AppAction mi = new MenuItemData(((ActionData) value)).createAction(null);

                                JLabel ret = (JLabel) orgRenderer.getListCellRendererComponent(list, mi.getName(), index, isSelected, cellHasFocus);
                                ret.setIcon(mi.getSmallIcon());
                                return ret;
                            }
                        };
                    }
                };

                try {
                    Integer ret = Dialog.getInstance().showDialog(d);
                    ActionData action = actions.get(ret);

                    TreePath sel = model.addAction(tree.getSelectionPath(), new MenuItemData(action));
                    tree.setSelectionPath(sel);
                } catch (DialogClosedException e1) {
                    e1.printStackTrace();
                } catch (DialogCanceledException e1) {
                    e1.printStackTrace();
                }
            }

        });

        ExtButton additem = new ExtButton(new AppAction() {
            {
                setName(_GUI._.ManagerFrame_layoutPanel_addSubmenu());
                setSmallIcon(NewTheme.I().getIcon("add", 20));
            }

            @Override
            public void actionPerformed(ActionEvent e) {

                List<MenuItemData> list = manager.listSpecialItems();
                ArrayList<Object> actions = new ArrayList<Object>();
                actions.add(_GUI._.ManagerFrame_actionPerformed_customfolder());

                actions.addAll(list);
                ComboBoxDialog d = new ComboBoxDialog(0, _GUI._.ManagerFrame_actionPerformed_addaction_title(), _GUI._.ManagerFrame_actionPerformed_addaction_msg(), actions.toArray(new Object[] {}), 0, null, "Add Submenu", null, null) {
                    protected ListCellRenderer getRenderer(final ListCellRenderer orgRenderer) {
                        // TODO Auto-generated method stub
                        return new ListCellRenderer() {

                            @Override
                            public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                                if (value instanceof MenuContainer) {

                                    JLabel ret = (JLabel) orgRenderer.getListCellRendererComponent(list, ((MenuContainer) value).getName(), index, isSelected, cellHasFocus);
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

                        TreePath sel = model.addAction(tree.getSelectionPath(), new MenuContainer(name, iconKey));
                        tree.setSelectionPath(sel);

                    } else if (ret > 0) {
                        Object action = actions.get(ret);
                        TreePath sel = model.addAction(tree.getSelectionPath(), (MenuItemData) action);
                        tree.setSelectionPath(sel);
                    }

                } catch (DialogClosedException e1) {
                    e1.printStackTrace();
                } catch (DialogCanceledException e1) {
                    e1.printStackTrace();
                }
            }

        });

        ExtButton addSpecials = new ExtButton(new AppAction() {
            {
                setName(_GUI._.ManagerFrame_layoutPanel_addspecials());
                setSmallIcon(NewTheme.I().getIcon("add", 20));
            }

            @Override
            public void actionPerformed(ActionEvent e) {

                List<ActionData> actions = manager.list();
                List<MenuItemData> menuitems = manager.listSpecialItems();

                ComboBoxDialog d = new ComboBoxDialog(0, _GUI._.ManagerFrame_actionPerformed_addaction_title(), _GUI._.ManagerFrame_actionPerformed_addaction_msg(), actions.toArray(new Object[] {}), 0, null, "Add Action", null, null) {
                    protected ListCellRenderer getRenderer(final ListCellRenderer orgRenderer) {
                        // TODO Auto-generated method stub
                        return new ListCellRenderer() {

                            @Override
                            public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                                AppAction mi = new MenuItemData(((ActionData) value)).createAction(null);

                                JLabel ret = (JLabel) orgRenderer.getListCellRendererComponent(list, mi.getName(), index, isSelected, cellHasFocus);
                                ret.setIcon(mi.getSmallIcon());
                                return ret;
                            }
                        };
                    }
                };

                try {
                    Integer ret = Dialog.getInstance().showDialog(d);
                    ActionData action = actions.get(ret);

                    TreePath sel = model.addAction(tree.getSelectionPath(), new MenuItemData(action));
                    tree.setSelectionPath(sel);
                } catch (DialogClosedException e1) {
                    e1.printStackTrace();
                } catch (DialogCanceledException e1) {
                    e1.printStackTrace();
                }
            }

        });
        ExtButton remove = new ExtButton(new AppAction() {
            {
                setName(_GUI._.literally_remove());
                setSmallIcon(NewTheme.I().getIcon("delete", 20));
            }

            @Override
            public void actionPerformed(ActionEvent e) {

                int[] rows = tree.getSelectionRows();
                model.remove(tree.getSelectionPath());

                if (rows.length > 0) {
                    if (tree.getRowCount() <= rows[0]) {
                        rows[0]--;
                    }
                    if (rows[0] > 0) {
                        tree.setSelectionRows(rows);
                    }
                }

            }

        });
        ExtButton cancel = new ExtButton(new AppAction() {
            {
                setName(_GUI._.lit_cancel());
                setSmallIcon(NewTheme.I().getIcon("cancel", 20));
            }

            @Override
            public void actionPerformed(ActionEvent e) {
            }

        });

        bottom.add(add, "sg 1,tag ok");
        bottom.add(additem, "sg 1,tag ok");
        bottom.add(addSpecials, "sg 1,tag ok");
        bottom.add(remove, "sg 1,tag ok");
        bottom.add(Box.createHorizontalBox());
        bottom.add(save, "sg 1,tag ok");
        bottom.add(cancel, "sg 1,tag cancel");
        bottom.setOpaque(false);
        panel.add(bottom, "spanx");

        LookAndFeelController.getInstance().getLAFOptions().applyPanelBackgroundColor(sp);
        getFrame().setContentPane(panel);
    }

    @Override
    protected void requestExit() {
    }
}
