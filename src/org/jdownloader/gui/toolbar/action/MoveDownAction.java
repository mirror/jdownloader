package org.jdownloader.gui.toolbar.action;

import jd.gui.swing.jdgui.interfaces.View;

import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.downloads.DownloadsView;
import org.jdownloader.gui.views.downloads.table.DownloadsTable;
import org.jdownloader.gui.views.downloads.table.DownloadsTableModel;

public class MoveDownAction extends AbstractMoveAction {

    public MoveDownAction() {
        setName(_GUI._.MoveDownAction_MoveDownAction());
        setIconKey("go-down");

    }

    @Override
    public void onGuiMainTabSwitch(View oldView, View newView) {
        if (newView instanceof DownloadsView) {
            DownloadsTable table = ((DownloadsTable) DownloadsTableModel.getInstance().getTable());
            setDelegateAction(table.getMoveDownAction());
        } else {
            setDelegateAction(null);
        }
    }

}
