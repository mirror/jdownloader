package org.jdownloader.gui.views.downloads.context;

import java.awt.event.ActionEvent;

import jd.controlling.downloadcontroller.DownloadWatchDog;
import jd.gui.swing.jdgui.JDGui;
import jd.plugins.DownloadLink;

import org.jdownloader.actions.AppAction;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.NewTheme;

public class StopsignAction extends AppAction {

    private static final long serialVersionUID = 3332656936365114557L;

    private final Object      obj;

    public StopsignAction(Object obj) {
        this.obj = obj;
        setIconKey("stopsign");
        if (DownloadWatchDog.getInstance().isStopMark(obj)) {
            setName(_GUI._.gui_table_contextmenu_stopmark_unset());
        } else {
            setName(_GUI._.gui_table_contextmenu_stopmark_set());
        }
    }

    public void actionPerformed(ActionEvent e) {
        if (obj instanceof DownloadLink) {
            JDGui.help(_GUI._.StopsignAction_actionPerformed_help_title_(), _GUI._.StopsignAction_actionPerformed_help_msg_(), NewTheme.I().getIcon("stopsign", 32));
            DownloadWatchDog.getInstance().toggleStopMark(obj);
        } else {
            JDGui.help(_GUI._.StopsignAction_actionPerformed_help_title_package_(), _GUI._.StopsignAction_actionPerformed_help_msg_package_(), NewTheme.I().getIcon("stopsign", 32));
            DownloadWatchDog.getInstance().toggleStopMark(obj);
        }

    }

}