package org.jdownloader.gui.views.downloads.contextmenumanager.gui;

import java.awt.Dimension;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.Box;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.TreePath;

import jd.gui.swing.laf.LookAndFeelController;

import org.appwork.app.gui.BasicGui;
import org.appwork.swing.MigPanel;
import org.appwork.swing.components.ExtButton;
import org.appwork.utils.Application;
import org.appwork.utils.swing.EDTRunner;
import org.jdownloader.actions.AppAction;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.components.HeaderScrollPane;
import org.jdownloader.gui.views.downloads.contextmenumanager.ActionData;
import org.jdownloader.gui.views.downloads.contextmenumanager.ContextMenuManager;
import org.jdownloader.gui.views.downloads.contextmenumanager.DownloadListContextMenuManager;
import org.jdownloader.gui.views.downloads.contextmenumanager.MenuContainerRoot;
import org.jdownloader.gui.views.downloads.contextmenumanager.MenuItemData;
import org.jdownloader.images.NewTheme;

public class ManagerFrame extends BasicGui implements TreeSelectionListener {
    private static ManagerFrame FRAME;

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
    private ManagerTreeModel               model;
    private ExtTree                        tree;

    private ManagerFrame() {
        super(_GUI._.ManagerFrame_ManagerFrame_());

    }

    protected List<? extends Image> getAppIconList() {
        final java.util.List<Image> list = new ArrayList<Image>();
        list.add(NewTheme.I().getImage("menu", 16));
        list.add(NewTheme.I().getImage("menu", 32));
        return list;
    }

    @Override
    protected void layoutPanel() {
        manager = DownloadListContextMenuManager.getInstance();
        MigPanel panel = new MigPanel("ins 2,wrap 2", "[grow,fill][]", "[grow,fill][]");
        panel.setOpaque(false);
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
        MigPanel bottom = new MigPanel("ins 0", "[][][][][grow,fill][][]", "[]");
        ExtButton save = new ExtButton(new AppAction() {
            {
                setName(_GUI._.lit_save());
                setSmallIcon(NewTheme.I().getIcon("save", 20));
            }

            @Override
            public void actionPerformed(ActionEvent e) {
                MenuContainerRoot data = (MenuContainerRoot) model.getRoot();

                manager.setMenuData(data);
                dispose();
            }

        });

        ExtButton add = new ExtButton(new AddActionAction(this));

        ExtButton addSubmenu = new ExtButton(new AddSubMenuAction(this));

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

                dispose();
            }

        });

        bottom.add(add, "sg 1,tag ok");
        bottom.add(addSubmenu, "sg 1,tag ok");
        bottom.add(addSpecials, "sg 1,tag ok");
        bottom.add(remove, "sg 1,tag ok");
        bottom.add(Box.createHorizontalBox());
        bottom.add(save, "tag ok");
        bottom.add(reset, "tag cancel");
        bottom.add(cancel, ",tag cancel");
        bottom.setOpaque(false);
        panel.add(bottom, "spanx");

        LookAndFeelController.getInstance().getLAFOptions().applyPanelBackgroundColor(sp);
        getFrame().setContentPane(panel);
        if (tree.getRowCount() > 0) tree.setSelectionRow(0);
    }

    public ManagerTreeModel getModel() {
        return model;
    }

    @Override
    protected void requestExit() {
        dispose();
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

    public static void show() {
        new EDTRunner() {

            @Override
            protected void runInEDT() {
                if (FRAME == null) {

                    FRAME = new ManagerFrame();
                }
                if (!FRAME.getFrame().isVisible()) {
                    FRAME.setVisible(true);
                }

            }
        };

    }

    protected void setVisible(boolean b) {
        model.set(manager.getMenuData());
        getFrame().setVisible(b);
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

}
