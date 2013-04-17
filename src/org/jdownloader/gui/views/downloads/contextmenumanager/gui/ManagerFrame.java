package org.jdownloader.gui.views.downloads.contextmenumanager.gui;

import java.awt.event.ActionEvent;

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
import org.jdownloader.gui.views.downloads.contextmenumanager.DownloadListContextMenuManager;
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

    private InfoPanel infoPanel;

    public ManagerFrame() {
        super(_GUI._.ManagerFrame_ManagerFrame_());
    }

    @Override
    protected void layoutPanel() {
        MigPanel panel = new MigPanel("ins 2,wrap 2", "[grow,fill][300!,fill]", "[grow,fill][]");
        panel.setOpaque(false);
        LookAndFeelController.getInstance().getLAFOptions().applyPanelBackgroundColor(panel);

        final ManagerTreeModel model = new ManagerTreeModel(new DownloadListContextMenuManager().getMenuData());
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
        MigPanel bottom = new MigPanel("ins 0", "[][][grow,fill][][]", "[]");
        ExtButton save = new ExtButton(new AppAction() {
            {
                setName(_GUI._.lit_save());
                setSmallIcon(NewTheme.I().getIcon("save", 20));
            }

            @Override
            public void actionPerformed(ActionEvent e) {
            }

        });

        ExtButton add = new ExtButton(new AppAction() {
            {
                setName(_GUI._.ManagerFrame_layoutPanel_add());
                setSmallIcon(NewTheme.I().getIcon("add", 20));
            }

            @Override
            public void actionPerformed(ActionEvent e) {
            }

        });

        ExtButton remove = new ExtButton(new AppAction() {
            {
                setName(_GUI._.literally_remove());
                setSmallIcon(NewTheme.I().getIcon("delete", 20));
            }

            @Override
            public void actionPerformed(ActionEvent e) {
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
