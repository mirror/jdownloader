package org.jdownloader.gui.toolbar.action;

import jd.gui.swing.jdgui.interfaces.View;

import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.downloads.DownloadsView;
import org.jdownloader.gui.views.downloads.table.DownloadsTable;
import org.jdownloader.gui.views.downloads.table.DownloadsTableModel;

public class MoveToTopAction extends AbstractMoveAction {

    public MoveToTopAction() {
        setName(_GUI._.MoveToTopAction_MoveToTopAction());
        setIconKey("go-top");

    }

    @Override
    public void onGuiMainTabSwitch(View oldView, View newView) {
        if (newView instanceof DownloadsView) {
            DownloadsTable table = ((DownloadsTable) DownloadsTableModel.getInstance().getTable());
            setDelegateAction(table.getMoveTopAction());
        } else {
            setDelegateAction(null);
        }
    }

}
