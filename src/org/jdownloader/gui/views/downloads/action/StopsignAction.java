package org.jdownloader.gui.views.downloads.action;

import java.awt.event.ActionEvent;

import jd.controlling.downloadcontroller.DownloadSession;
import jd.controlling.downloadcontroller.DownloadWatchDog;
import jd.controlling.downloadcontroller.DownloadWatchDogJob;
import jd.controlling.packagecontroller.AbstractNode;
import jd.gui.swing.jdgui.JDGui;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;

import org.appwork.utils.swing.EDTRunner;
import org.jdownloader.controlling.contextmenu.CustomizableTableContextAppAction;
import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.SelectionInfo;
import org.jdownloader.gui.views.downloads.table.DownloadsTableModel;
import org.jdownloader.images.AbstractIcon;

public class StopsignAction extends CustomizableTableContextAppAction<FilePackage, DownloadLink> {

    private static final long serialVersionUID = 3332656936365114557L;

    @Override
    public void requestUpdate(Object requestor) {
        super.requestUpdate(requestor);
        if (hasSelection()) {
            if (DownloadWatchDog.getInstance().getSession().isStopMark(getSelection().getRawContext())) {
                setName(_GUI.T.gui_table_contextmenu_stopmark_unset());
            } else {
                setName(_GUI.T.gui_table_contextmenu_stopmark_set());
            }
        } else {
            setName(_GUI.T.gui_table_contextmenu_stopmark());
        }
    }

    public StopsignAction() {

        setIconKey(IconKey.ICON_STOPSIGN);
    }

    public void actionPerformed(ActionEvent e) {
        final SelectionInfo<FilePackage, DownloadLink> selection = getSelection();
        if (selection.isLinkContext()) {
            JDGui.help(_GUI.T.StopsignAction_actionPerformed_help_title_(), _GUI.T.StopsignAction_actionPerformed_help_msg_(), new AbstractIcon(IconKey.ICON_STOPSIGN, 32));
        } else {
            JDGui.help(_GUI.T.StopsignAction_actionPerformed_help_title_package_(), _GUI.T.StopsignAction_actionPerformed_help_msg_package_(), new AbstractIcon(IconKey.ICON_STOPSIGN, 32));
        }
        final AbstractNode context = selection.getRawContext();
        DownloadWatchDog.getInstance().enqueueJob(new DownloadWatchDogJob() {

            @Override
            public void execute(DownloadSession currentSession) {
                currentSession.toggleStopMark(context);
                new EDTRunner() {

                    @Override
                    protected void runInEDT() {
                        DownloadsTableModel.getInstance().setStopSignColumnVisible(true);
                    }
                };
            }

            @Override
            public void interrupt() {
            }

            @Override
            public boolean isHighPriority() {
                return true;
            }
        });

    }
}