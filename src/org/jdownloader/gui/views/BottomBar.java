package org.jdownloader.gui.views;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.Timer;

import jd.controlling.DownloadWatchDog;

import org.appwork.app.gui.MigPanel;
import org.appwork.storage.config.JsonConfig;
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
        label = new JLabel();
        label.setEnabled(false);
        add(label);
        add(Box.createGlue(), "pushx,growx");
        if (config.isShowMoveToTopButton()) addButton(table.getMoveTopAction());
        if (config.isShowMoveUpButton()) addButton(table.getMoveUpAction());
        if (config.isShowMoveDownButton()) addButton(table.getMoveDownAction());
        if (config.isShowMoveToBottomButton()) addButton(table.getMoveToBottomAction());

        if (getComponentCount() == 2 || !config.isDownloadViewBottombarEnabled()) {
            setVisible(false);
            return;
        }

        timer = new Timer(1000, this);
        timer.setRepeats(true);
        timer.start();
    }

    private void addButton(AppAction action) {
        JButton bt = new JButton(action);
        // bt.setText("");
        add(bt, "height 22!");
    }

    public void actionPerformed(ActionEvent e) {
        label.setText(_GUI._.BottomBar_actionPerformed_running_downloads(DownloadWatchDog.getInstance().getActiveDownloads(), DownloadWatchDog.getInstance().getConnectionManager().getIncommingConnections()));
    }
}
