package org.jdownloader.gui.views.downloads;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.Box;
import javax.swing.JLabel;
import javax.swing.JPopupMenu;
import javax.swing.Timer;

import jd.controlling.DownloadWatchDog;
import jd.gui.swing.jdgui.menu.actions.CleanupDownloads;
import jd.gui.swing.jdgui.menu.actions.CleanupPackages;
import jd.gui.swing.jdgui.menu.actions.RemoveDisabledAction;
import jd.gui.swing.jdgui.menu.actions.RemoveDupesAction;
import jd.gui.swing.jdgui.menu.actions.RemoveFailedAction;
import jd.gui.swing.jdgui.menu.actions.RemoveOfflineAction;
import jd.gui.swing.laf.LookAndFeelController;

import org.appwork.app.gui.MigPanel;
import org.appwork.storage.config.JsonConfig;
import org.appwork.swing.components.ExtButton;
import org.jdownloader.actions.AppAction;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.downloads.table.DownloadsTable;
import org.jdownloader.settings.GraphicalUserInterfaceSettings;

public class BottomBar extends MigPanel implements ActionListener {

    private GraphicalUserInterfaceSettings config;
    private JLabel                         label;
    private Timer                          timer;

    public BottomBar(DownloadsTable table) {
        super("ins 2 0 1 0", "[]", "[]");

        config = JsonConfig.create(GraphicalUserInterfaceSettings.class);
        if (config.isDownloadViewBottombarEnabled()) {
            label = new JLabel();
            label.setEnabled(false);
            add(label);
            add(Box.createGlue(), "pushx,growx");

            // add();
            addButton(new AppAction() {
                {
                    setTooltipText(_GUI._.BottomBar_BottomBar_add());
                    setIconKey("add");

                    // setIconSizes(20);
                }

                public void actionPerformed(ActionEvent e) {

                }
            });
            addButton(new AppAction() {
                {
                    setTooltipText(_GUI._.BottomBar_BottomBar_cleanup());
                    setIconKey("remove");
                    // setIconSizes(18);
                }

                public void actionPerformed(ActionEvent e) {
                    JPopupMenu pu = new JPopupMenu();
                    pu.add(new CleanupDownloads());
                    pu.add(new CleanupPackages());
                    pu.addSeparator();
                    pu.add(new RemoveDupesAction());
                    pu.add(new RemoveDisabledAction());
                    pu.add(new RemoveOfflineAction());
                    pu.add(new RemoveFailedAction());
                    int[] insets = LookAndFeelController.getInstance().getLAFOptions().getPopupBorderInsets();
                    pu.show((Component) e.getSource(), -insets[1], -pu.getPreferredSize().height + insets[2]);
                    // new CleanupMenu()
                }
            });
            boolean first = true;
            if (config.isShowMoveToTopButton()) {
                if (first) {
                    add(Box.createHorizontalGlue(), "width 10!");
                    first = false;
                }
                addButton(table.getMoveTopAction());
            }
            if (config.isShowMoveUpButton()) {
                if (first) {
                    add(Box.createHorizontalGlue(), "width 10!");
                    first = false;
                }
                addButton(table.getMoveUpAction());
            }
            if (config.isShowMoveDownButton()) {
                if (first) {
                    add(Box.createHorizontalGlue(), "width 10!");
                    first = false;
                }
                addButton(table.getMoveDownAction());
            }
            if (config.isShowMoveToBottomButton()) {
                if (first) {
                    add(Box.createHorizontalGlue(), "width 10!");
                    first = false;
                }
                addButton(table.getMoveToBottomAction());
            }
            add(Box.createHorizontalGlue(), "width 10!");
            addButton(new AppAction() {
                {
                    setTooltipText(_GUI._.BottomBar_BottomBar_settings());
                    setIconKey("settings");
                    // setIconSizes(18);
                }

                public void actionPerformed(ActionEvent e) {
                    QuickSettingsPopup pu = new QuickSettingsPopup();
                    int[] insets = LookAndFeelController.getInstance().getLAFOptions().getPopupBorderInsets();
                    pu.show((Component) e.getSource(), -pu.getPreferredSize().width + insets[3] + ((Component) e.getSource()).getWidth(), -pu.getPreferredSize().height + insets[2]);
                    // new CleanupMenu()
                }
            });
            timer = new Timer(1000, this);
            timer.setRepeats(true);
            timer.start();
        }
    }

    private void addButton(AppAction action) {
        ExtButton bt = new ExtButton(action);
        // bt.setText("");
        bt.setRolloverEffectEnabled(true);

        add(bt, "width 22!,height 22!,gapleft 3");
    }

    public void actionPerformed(ActionEvent e) {
        label.setText(_GUI._.BottomBar_actionPerformed_running_downloads(DownloadWatchDog.getInstance().getActiveDownloads(), DownloadWatchDog.getInstance().getConnectionManager().getIncommingConnections()));
    }
}
