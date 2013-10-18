package org.jdownloader.controlling.contextmenu.gui;

import java.awt.Dialog.ModalityType;
import java.awt.Dimension;
import java.awt.Point;
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

import org.appwork.swing.MigPanel;
import org.appwork.swing.components.ExtButton;
import org.appwork.uio.UIOManager;
import org.appwork.utils.logging2.LogSource;
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
import org.jdownloader.logging.LogController;
import org.jdownloader.updatev2.gui.LAFOptions;

public class MenuManagerDialog extends AbstractDialog<Object> implements TreeSelectionListener, MenuManagerDialogInterface {

    private InfoPanel                infoPanel;
    private ContextMenuManager<?, ?> manager;
    private ManagerTreeModel         model;
    private MenuManagerTree          tree;
    private LogSource                logger;

    public MenuManagerDialog(ContextMenuManager<?, ?> manager) {
        super(UIOManager.BUTTONS_HIDE_CANCEL | UIOManager.BUTTONS_HIDE_OK, _GUI._.ManagerFrame_ManagerFrame_title(manager.getName()), null, null, null);

        this.manager = manager;
        ext = manager.getFileExtension();
        setLocator(new RememberAbsoluteDialogLocator("dialogframe-" + manager.getClass().getName()));
        setDimensor(new RememberLastDialogDimension("dialogframe-" + manager.getClass().getName()));
        logger = LogController.getInstance().getLogger(MenuManagerDialog.class.getName());
    }

    @Override
    public ModalityType getModalityType() {
        return ModalityType.MODELESS;
    }

    protected MigPanel createBottomPanel() {
        // TODO Auto-generated method stub
        MigPanel ret = new MigPanel("ins 0", "[]20[grow,fill][]", "[][]");

        MigPanel topline = new MigPanel("ins 0", "[][][][grow,fill][][][][]", "[]");
        // bottom.add(topline, "spanx,pushx,growx,wrap");
        topline.setOpaque(false);

        ExtButton add = new ExtButton(new AddActionAction(this)) {
            @Override
            public int getTooltipDelay(Point mousePositionOnScreen) {
                return 500;
            }
        };

        ExtButton addSubmenu = new ExtButton(new AddSubMenuAction(this)) {
            @Override
            public int getTooltipDelay(Point mousePositionOnScreen) {
                return 500;
            }
        };
        ExtButton addSpecials = new ExtButton(new AddSpecialAction(this)) {
            @Override
            public int getTooltipDelay(Point mousePositionOnScreen) {
                return 500;
            }
        };

        ExtButton export = new ExtButton(new AppAction() {
            {
                setIconKey(IconKey.ICON_EXPORT);
                setTooltipText(_GUI._.lit_export());
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

        }) {
            @Override
            public int getTooltipDelay(Point mousePositionOnScreen) {
                return 500;
            }
        };

        ExtButton importButton = new ExtButton(new AppAction() {
            {
                setIconKey(IconKey.ICON_IMPORT);
                setTooltipText(_GUI._.lit_import());
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

        }) {
            @Override
            public int getTooltipDelay(Point mousePositionOnScreen) {
                return 500;
            }
        };

        ExtButton reset = new ExtButton(new AppAction() {
            {
                setTooltipText(_GUI._.ManagerFrame_layoutPanel_resettodefault());
                setSmallIcon(NewTheme.I().getIcon("undo", 20));

            }

            @Override
            public void actionPerformed(ActionEvent e) {
                MenuContainerRoot data = manager.setupDefaultStructure();
                data.validateFull();
                model.set(data);

                if (tree.getRowCount() > 0) tree.setSelectionRow(0);
            }

        }) {
            @Override
            public int getTooltipDelay(Point mousePositionOnScreen) {
                return 500;
            }
        };
        ExtButton remove = new ExtButton(new RemoveAction(this));
        add.setText(null);
        addSubmenu.setText(null);
        addSpecials.setText(null);
        remove.setText(null);
        topline.add(add, "height 24!");
        topline.add(addSubmenu, "height 24!");
        topline.add(addSpecials, "height 24!");

        topline.add(Box.createHorizontalBox());

        topline.add(remove, "height 24!");
        topline.add(reset, "height 24!");
        topline.add(importButton, "height 24!");
        topline.add(export, "height 24!");

        ret.add(topline, "wrap,spanx");
        return ret;
    }

    protected DefaultButtonPanel getDefaultButtonPanel() {
        DefaultButtonPanel bottom = new DefaultButtonPanel("ins 0", "[grow,fill][][]", "[]");
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
        ExtButton apply = new ExtButton(new AppAction() {
            {
                setSmallIcon(NewTheme.I().getIcon(IconKey.ICON_TRUE, 20));
                setName(_GUI._.lit_apply());
            }

            @Override
            public void actionPerformed(ActionEvent e) {
                MenuContainerRoot data = (MenuContainerRoot) model.getRoot();
                manager.setMenuData(data);

                data = manager.getMenuData();
                data.validateFull();

                // tree.getSelectionModel().removeTreeSelectionListener(MenuManagerDialog.this);
                TreePath[] sel = tree.getSelectionPaths();

                // tree.getSelectionModel().addTreeSelectionListener(MenuManagerDialog.this);
                model.set(data);
                if (sel != null) {
                    tree.setSelectionPaths(sel);
                } else {
                    if (tree.getRowCount() > 0) tree.setSelectionRow(0);
                }

            }
        });
        bottom.add(save, "tag ok,height 24!");
        bottom.add(cancel, "tag cancel,height 24!");
        bottom.add(apply, "tag apply,height 24!");
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

    @Override
    protected int getPreferredHeight() {
        return 600;
    }

    @Override
    protected int getPreferredWidth() {
        return 800;
    }

    // @Override
    @Override
    public JComponent layoutDialogContent() {
        final MigPanel panel = new MigPanel("ins 2,wrap 2", "[grow,fill][]", "[grow,fill][]");
        panel.setOpaque(false);

        LAFOptions.getInstance().applyPanelBackground((JComponent) getDialog().getContentPane());
        LAFOptions.getInstance().applyPanelBackground(panel);

        model = new ManagerTreeModel(manager.getMenuData());
        tree = new MenuManagerTree(this);

        tree.getSelectionModel().addTreeSelectionListener(this);
        LAFOptions.getInstance().applyPanelBackground(tree);

        // tree.set
        // tree.setShowsRootHandles(false);
        HeaderScrollPane sp = new HeaderScrollPane(tree) {

            @Override
            public Dimension getPreferredSize() {
                Dimension pref = tree.getPreferredSize();

                pref.width = 100;
                return pref;
            }

        };
        sp.setColumnHeaderView(new TreeHeader());
        panel.add(sp);
        infoPanel = new InfoPanel(this);
        LAFOptions.getInstance().applyPanelBackground(infoPanel);
        sp = new HeaderScrollPane(infoPanel) {
            public Dimension getPreferredSize() {
                Dimension ret = super.getPreferredSize();
                ret.width = Math.max(ret.width, 300);
                return ret;
            }
        };
        sp.setColumnHeaderView(new OptionsPaneHeader());
        panel.add(sp);

        LAFOptions.getInstance().applyPanelBackground(sp);

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
        super.setVisible(b);

        MenuContainerRoot md = manager.getMenuData();
        long t = System.currentTimeMillis();
        md.validateFull();
        System.out.println("Validate: " + (System.currentTimeMillis() - t));
        model.set(md);

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

    public void repaint() {
        fireUpdate();
    }

    public LogSource getLogger() {
        return logger;
    }

}
