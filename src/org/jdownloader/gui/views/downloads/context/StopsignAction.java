package org.jdownloader.gui.views.downloads.context;

import java.awt.event.ActionEvent;

import jd.controlling.downloadcontroller.DownloadWatchDog;
import jd.gui.swing.jdgui.JDGui;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;

import org.jdownloader.actions.AppAction;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.SelectionInfo;
import org.jdownloader.images.NewTheme;

public class StopsignAction extends AppAction {

    private static final long                        serialVersionUID = 3332656936365114557L;

    private SelectionInfo<FilePackage, DownloadLink> si;

    public StopsignAction(SelectionInfo<FilePackage, DownloadLink> si) {
        this.si = si;

        setIconKey("stopsign");
        if (si != null) {
            if (DownloadWatchDog.getInstance().isStopMark(si.getRawContext())) {
                setName(_GUI._.gui_table_contextmenu_stopmark_unset());
            } else {
                setName(_GUI._.gui_table_contextmenu_stopmark_set());
            }
        } else {
            setName(_GUI._.gui_table_contextmenu_stopmark());
        }
    }

    public void actionPerformed(ActionEvent e) {
        if (si.isLinkContext()) {
            JDGui.help(_GUI._.StopsignAction_actionPerformed_help_title_(), _GUI._.StopsignAction_actionPerformed_help_msg_(), NewTheme.I().getIcon("stopsign", 32));
            DownloadWatchDog.getInstance().toggleStopMark(si.getRawContext());
        } else {
            JDGui.help(_GUI._.StopsignAction_actionPerformed_help_title_package_(), _GUI._.StopsignAction_actionPerformed_help_msg_package_(), NewTheme.I().getIcon("stopsign", 32));
            DownloadWatchDog.getInstance().toggleStopMark(si.getRawContext());
        }

    }

}