package org.jdownloader.gui.toolbar.action;

import jd.gui.swing.jdgui.interfaces.View;

import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.downloads.DownloadsView;
import org.jdownloader.gui.views.downloads.table.DownloadsTable;
import org.jdownloader.gui.views.downloads.table.DownloadsTableModel;
import org.jdownloader.gui.views.linkgrabber.LinkGrabberTable;
import org.jdownloader.gui.views.linkgrabber.LinkGrabberTableModel;

public class MoveToBottomAction extends AbstractMoveAction {

    public MoveToBottomAction() {
        setName(_GUI._.MoveToBottomAction_MoveToBottomAction());
        setIconKey("go-bottom");

    }

    @Override
    public void onGuiMainTabSwitch(View oldView, View newView) {
        if (newView instanceof DownloadsView) {
            DownloadsTable table = ((DownloadsTable) DownloadsTableModel.getInstance().getTable());
            setDelegateAction(table.getMoveToBottomAction());
        } else {
            LinkGrabberTable table = ((LinkGrabberTable) LinkGrabberTableModel.getInstance().getTable());
            setDelegateAction(table.getMoveTopAction());
        }
    }

}
