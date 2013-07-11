package org.jdownloader.gui.views.downloads.action;

import java.awt.event.ActionEvent;

import jd.controlling.downloadcontroller.DownloadWatchDog;
import jd.gui.swing.jdgui.JDGui;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;

import org.jdownloader.actions.SelectionAppAction;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.SelectionInfo;
import org.jdownloader.images.NewTheme;

public class StopsignAction extends SelectionAppAction<FilePackage, DownloadLink> {

    private static final long serialVersionUID = 3332656936365114557L;

    @Override
    public void setSelection(SelectionInfo<FilePackage, DownloadLink> selection) {
        super.setSelection(selection);
        if (getSelection() != null) {
            if (DownloadWatchDog.getInstance().isStopMark(getSelection().getRawContext())) {
                setName(_GUI._.gui_table_contextmenu_stopmark_unset());
            } else {
                setName(_GUI._.gui_table_contextmenu_stopmark_set());
            }
        } else {
            setName(_GUI._.gui_table_contextmenu_stopmark());
        }
    }

    public StopsignAction(SelectionInfo<FilePackage, DownloadLink> si) {
        super(si);

        setIconKey("stopsign");

    }

    public void actionPerformed(ActionEvent e) {
        if (getSelection().isLinkContext()) {
            JDGui.help(_GUI._.StopsignAction_actionPerformed_help_title_(), _GUI._.StopsignAction_actionPerformed_help_msg_(), NewTheme.I().getIcon("stopsign", 32));
            DownloadWatchDog.getInstance().toggleStopMark(getSelection().getRawContext());
        } else {
            JDGui.help(_GUI._.StopsignAction_actionPerformed_help_title_package_(), _GUI._.StopsignAction_actionPerformed_help_msg_package_(), NewTheme.I().getIcon("stopsign", 32));
            DownloadWatchDog.getInstance().toggleStopMark(getSelection().getRawContext());
        }

    }

}