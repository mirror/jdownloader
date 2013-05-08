package org.jdownloader.controlling.contextmenu.gui;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;

import javax.swing.Box;
import javax.swing.JComponent;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.filechooser.FileFilter;
import javax.swing.tree.TreePath;

import jd.gui.swing.laf.LookAndFeelController;

import org.appwork.swing.MigPanel;
import org.appwork.swing.components.ExtButton;
import org.appwork.utils.swing.dialog.AbstractDialog;
import org.appwork.utils.swing.dialog.DefaultButtonPanel;
import org.appwork.utils.swing.dialog.Dialog;
import org.appwork.utils.swing.dialog.DialogCanceledException;
import org.appwork.utils.swing.dialog.DialogClosedException;
import org.appwork.utils.swing.dialog.ExtFileChooserDialog;
import org.appwork.utils.swing.dialog.FileChooserSelectionMode;
import org.appwork.utils.swing.dialog.FileChooserType;
import org.appwork.utils.swing.dialog.dimensor.RememberLastDialogDimension;
import org.appwork.utils.swing.dialog.locator.RememberAbsoluteDialogLocator;
import org.jdownloader.actions.AppAction;
import org.jdownloader.controlling.contextmenu.ActionData;
import org.jdownloader.controlling.contextmenu.ContextMenuManager;
import org.jdownloader.controlling.contextmenu.MenuContainerRoot;
import org.jdownloader.controlling.contextmenu.MenuItemData;
import org.jdownloader.controlling.contextmenu.MenuStructure;
import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.components.HeaderScrollPane;
import org.jdownloader.images.NewTheme;

public class ManagerFrame extends AbstractDialog<Object> implements TreeSelectionListener {

    private InfoPanel                infoPanel;
    private ContextMenuManager<?, ?> manager;
    private ManagerTreeModel         model;
    private ExtTree                  tree;

    public ManagerFrame(ContextMenuManager<?, ?> manager) {
        super(Dialog.BUTTONS_HIDE_CANCEL | Dialog.BUTTONS_HIDE_OK, _GUI._.ManagerFrame_ManagerFrame_title(manager.getName()), null, null, null);

        this.manager = manager;
        ext = manager.getFileExtension();
        setLocator(new RememberAbsoluteDialogLocator("dialogframe-" + manager.getClass().getName()));
        setDimensor(new RememberLastDialogDimension("dialogframe-" + manager.getClass().getName()));

    }

    protected DefaultButtonPanel getDefaultButtonPanel() {
        DefaultButtonPanel bottom = new DefaultButtonPanel("ins 0", "[][][][][][][][grow,fill][][]", "[]");
        ExtButton save = new ExtButton(new AppAction() {
            {
                setName(_GUI._.lit_save());
                setSmallIcon(NewTheme.I().getIcon("save", 20));
            }

            @Override
            public void actionPerformed(ActionEvent e) {
                MenuContainerRoot data = (MenuContainerRoot) model.getRoot();

                manager.setMenuData(data);
                setReturnmask(true);

                dispose();
            }

        });

        ExtButton add = new ExtButton(new AddActionAction(this));

        ExtButton addSubmenu = new ExtButton(new AddSubMenuAction(this));

        ExtButton export = new ExtButton(new AppAction() {
            {
                setIconKey(IconKey.ICON_EXPORT);
                setName(_GUI._.lit_export());
            }

            @Override
            public void actionPerformed(ActionEvent e) {

                ExtFileChooserDialog d = new ExtFileChooserDialog(0, _GUI._.ManagerFrame_actionPerformed_export_title(), null, null);
                d.setFileFilter(new FileFilter() {

                    @Override
                    public String getDescription() {

                        return "*" + ext;
                    }

                    @Override
                    public boolean accept(File f) {
                        return f.isDirectory() || f.getName().endsWith(ext);
                    }
                });

                d.setFileSelectionMode(FileChooserSelectionMode.FILES_AND_DIRECTORIES);
                d.setMultiSelection(false);

                d.setStorageID(ext);
                d.setType(FileChooserType.SAVE_DIALOG);
                try {
                    Dialog.getInstance().showDialog(d);

                    File saveTo = d.getSelectedFile();
                    if (!saveTo.getName().endsWith(ext)) {
                        saveTo = new File(saveTo.getAbsolutePath() + ext);
                    }
                    manager.saveTo((MenuContainerRoot) model.getRoot(), saveTo);

                } catch (DialogClosedException e1) {
                    e1.printStackTrace();
                } catch (DialogCanceledException e1) {
                    e1.printStackTrace();
                } catch (UnsupportedEncodingException e1) {
                    Dialog.getInstance().showExceptionDialog(_GUI._.lit_error_occured(), e1.getMessage(), e1);
                } catch (IOException e1) {
                    Dialog.getInstance().showExceptionDialog(_GUI._.lit_error_occured(), e1.getMessage(), e1);
                }
            }

        });

        ExtButton importButton = new ExtButton(new AppAction() {
            {
                setIconKey(IconKey.ICON_IMPORT);
                setName(_GUI._.lit_import());
            }

            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    ExtFileChooserDialog d = new ExtFileChooserDialog(0, _GUI._.ManagerFrame_actionPerformed_export_title(), null, null);
                    d.setFileFilter(new FileFilter() {

                        @Override
                        public String getDescription() {

                            return "*" + ext;
                        }

                        @Override
                        public boolean accept(File f) {
                            return f.isDirectory() || f.getName().endsWith(ext);
                        }
                    });

                    d.setFileSelectionMode(FileChooserSelectionMode.FILES_AND_DIRECTORIES);
                    d.setMultiSelection(false);

                    d.setStorageID(ext);
                    d.setType(FileChooserType.OPEN_DIALOG);

                    Dialog.getInstance().showDialog(d);

                    File selected = d.getSelectedFile();
                    if (selected.exists() && selected.getName().endsWith(ext)) {
                        MenuStructure data = manager.readFrom(selected);
                        model.set(data.getRoot());
                    }
                } catch (DialogClosedException e1) {
                    e1.printStackTrace();
                } catch (DialogCanceledException e1) {
                    e1.printStackTrace();
                } catch (IOException e1) {
                    Dialog.getInstance().showExceptionDialog(_GUI._.lit_error_occured(), e1.getMessage(), e1);
                }
            }

        });
        ExtButton addSpecials = new ExtButton(new AddSpecialAction(this));
        ExtButton reset = new ExtButton(new AppAction() {
            {
                setName(_GUI._.ManagerFrame_layoutPanel_resettodefault());
                setSmallIcon(NewTheme.I().getIcon("reset", 20));
            }

            @Override
            public void actionPerformed(ActionEvent e) {
                model.set(manager.setupDefaultStructure());
                if (tree.getRowCount() > 0) tree.setSelectionRow(0);
            }

        });
        ExtButton remove = new ExtButton(new RemoveAction(this));
        ExtButton cancel = new ExtButton(new AppAction() {
            {
                setName(_GUI._.lit_cancel());
                setSmallIcon(NewTheme.I().getIcon("cancel", 20));
            }

            @Override
            public void actionPerformed(ActionEvent e) {

                setReturnmask(false);

                dispose();
            }

        });

        bottom.add(add, "height 24!");
        bottom.add(addSubmenu, "height 24!");
        bottom.add(addSpecials, "height 24!");
        bottom.add(remove, "height 24!");
        bottom.add(reset, "height 24!");
        bottom.add(importButton, "height 24!");
        bottom.add(export, "height 24!");
        bottom.add(Box.createHorizontalBox());

        bottom.add(save, "tag ok,height 24!");
        bottom.add(cancel, "tag cancel,height 24!");
        bottom.setOpaque(false);
        return bottom;

    }

    // protected List<? extends Image> getAppIconList() {
    // final java.util.List<Image> list = new ArrayList<Image>();
    // list.add(NewTheme.I().getImage("menu", 16));
    // list.add(NewTheme.I().getImage("menu", 32));
    // return list;
    // }

    private String ext = ".jdDlMenu";

    @Override
    protected boolean isResizable() {
        return true;
    }

    // @Override
    @Override
    public JComponent layoutDialogContent() {
        MigPanel panel = new MigPanel("ins 2,wrap 2", "[grow,fill][]", "[grow,fill][]");
        panel.setOpaque(false);

        LookAndFeelController.getInstance().getLAFOptions().applyPanelBackgroundColor((JComponent) getDialog().getContentPane());
        LookAndFeelController.getInstance().getLAFOptions().applyPanelBackgroundColor(panel);

        model = new ManagerTreeModel(manager.getMenuData());
        tree = new ExtTree(this);

        tree.getSelectionModel().addTreeSelectionListener(this);
        LookAndFeelController.getInstance().getLAFOptions().applyPanelBackgroundColor(tree);

        // tree.set
        // tree.setShowsRootHandles(false);
        HeaderScrollPane sp = new HeaderScrollPane(tree) {

            @Override
            public Dimension getPreferredSize() {
                return tree.getPreferredSize();
            }

        };
        sp.setColumnHeaderView(new TreeHeader());
        panel.add(sp);
        infoPanel = new InfoPanel(this);
        LookAndFeelController.getInstance().getLAFOptions().applyPanelBackgroundColor(infoPanel);
        sp = new HeaderScrollPane(infoPanel);
        sp.setColumnHeaderView(new OptionsPaneHeader());
        panel.add(sp);

        LookAndFeelController.getInstance().getLAFOptions().applyPanelBackgroundColor(sp);

        if (tree.getRowCount() > 0) tree.setSelectionRow(0);
        return panel;
    }

    public ManagerTreeModel getModel() {
        return model;
    }

    public ContextMenuManager getManager() {
        return manager;
    }

    public void addAction(ActionData action) {
        TreePath sel = model.addAction(tree.getSelectionPath(), new MenuItemData(action));
        tree.setSelectionPath(sel);
    }

    public void addMenuItem(MenuItemData action) {
        TreePath sel = model.addAction(tree.getSelectionPath(), (MenuItemData) action);
        tree.setSelectionPath(sel);
    }

    public void deleteSelection() {
        int[] rows = tree.getSelectionRows();
        model.remove(tree.getSelectionPath());

        if (rows != null && rows.length > 0) {
            if (tree.getRowCount() <= rows[0]) {
                rows[0]--;
            }
            if (rows[0] > 0) {
                tree.setSelectionRows(rows);
            }
        }
    }

    public void setVisible(boolean b) {
        model.set(manager.getMenuData());
        super.setVisible(b);
    }

    @Override
    public void valueChanged(TreeSelectionEvent e) {
        TreePath sel = tree.getSelectionPath();
        if (sel == null) {
            infoPanel.updateInfo(null);
        } else {
            infoPanel.updateInfo((MenuItemData) sel.getLastPathComponent());
        }
    }

    public void fireUpdate() {

        tree.getSelectionModel().removeTreeSelectionListener(this);
        TreePath[] sel = tree.getSelectionPaths();
        model.fireUpdate();
        if (sel != null) tree.setSelectionPaths(sel);
        tree.getSelectionModel().addTreeSelectionListener(this);
    }

    public TreePath getSelectionPath() {
        TreePath ret = tree.getSelectionPath();
        if (ret == null) ret = new TreePath(model.getRoot());
        return ret;
    }

    @Override
    protected Object createReturnValue() {
        return null;
    }

}
