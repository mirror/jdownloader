package org.jdownloader.gui.views.downloads.action;

import java.awt.event.ActionEvent;

import org.jdownloader.controlling.contextmenu.CustomizableTableContextAppAction;
import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.SelectionInfo;
import org.jdownloader.images.AbstractIcon;

import jd.controlling.downloadcontroller.DownloadWatchDog;
import jd.gui.swing.jdgui.JDGui;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;

public class ForceDownloadAction extends CustomizableTableContextAppAction<FilePackage, DownloadLink> {

    private static final long serialVersionUID = 7107840091963427544L;

    private final static String NAME = _GUI.T.ForceDownloadAction_ForceDownloadAction();

    public ForceDownloadAction() {
        super();
        setIconKey(IconKey.ICON_MEDIA_PLAYBACK_START_FORCED);
        setName(NAME);
    }

    @Override
    public boolean isEnabled() {
        return super.isEnabled() && DownloadWatchDog.getInstance().getStateMachine().isState(DownloadWatchDog.IDLE_STATE, DownloadWatchDog.RUNNING_STATE, DownloadWatchDog.PAUSE_STATE, DownloadWatchDog.STOPPED_STATE);
    }

    public void actionPerformed(ActionEvent e) {
        if (!isEnabled()) {
            return;
        }
        final SelectionInfo<FilePackage, DownloadLink> selection = getSelection();
        JDGui.help(_GUI.T.ForceDownloadAction_actionPerformed_help_title_(), _GUI.T.ForceDownloadAction_actionPerformed_help_msg_(), new AbstractIcon(IconKey.ICON_BOTTY_ROBOT_INFO, -1));
        DownloadWatchDog.getInstance().resume(selection.getChildren());
        DownloadWatchDog.getInstance().forceDownload(selection.getChildren());

    }

}