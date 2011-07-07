package org.jdownloader.gui.views;

import javax.swing.Box;
import javax.swing.JButton;

import org.appwork.app.gui.MigPanel;
import org.appwork.storage.config.JsonConfig;
import org.jdownloader.actions.AppAction;
import org.jdownloader.gui.views.downloads.table.DownloadsTable;
import org.jdownloader.settings.GraphicalUserInterfaceSettings;

public class BottomBar extends MigPanel {

    private GraphicalUserInterfaceSettings config;

    public BottomBar(DownloadsTable table) {
        super("ins 2 0 1 0", "[]", "[]");

        config = JsonConfig.create(GraphicalUserInterfaceSettings.class);
        add(Box.createGlue(), "pushx,growx");
        if (config.isShowMoveToTopButton()) addButton(table.getMoveTopAction());
        if (config.isShowMoveUpButton()) addButton(table.getMoveUpAction());
        if (config.isShowMoveDownButton()) addButton(table.getMoveDownAction());
        if (config.isShowMoveToBottomButton()) addButton(table.getMoveToBottomAction());

        if (getComponentCount() == 1 || !config.isDownloadViewBottombarEnabled()) setVisible(false);
    }

    private void addButton(AppAction action) {
        JButton bt = new JButton(action);
        // bt.setText("");
        add(bt, "height 22!");
    }
}
