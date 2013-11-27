package org.jdownloader.gui.views.downloads.action;

import java.awt.Image;
import java.awt.event.ActionEvent;

import javax.swing.ImageIcon;

import jd.controlling.downloadcontroller.DownloadWatchDog;
import jd.gui.swing.jdgui.JDGui;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;

import org.appwork.utils.ImageProvider.ImageProvider;
import org.jdownloader.controlling.contextmenu.CustomizableTableContextAppAction;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.NewTheme;

public class ForceDownloadAction extends CustomizableTableContextAppAction<FilePackage, DownloadLink> {

    private static final long serialVersionUID = 7107840091963427544L;

    public ForceDownloadAction() {
        super();
        Image add = NewTheme.I().getImage("media-playback-start", 20);
        Image play = NewTheme.I().getImage("prio_3", 14);
        setSmallIcon(new ImageIcon(ImageProvider.merge(add, play, -4, 0, 6, 10)));
        setName(_GUI._.ForceDownloadAction_ForceDownloadAction());
    }

    @Override
    public boolean isEnabled() {
        return super.isEnabled() && DownloadWatchDog.getInstance().getStateMachine().isState(DownloadWatchDog.IDLE_STATE, DownloadWatchDog.RUNNING_STATE, DownloadWatchDog.PAUSE_STATE, DownloadWatchDog.STOPPED_STATE);
    }

    public void actionPerformed(ActionEvent e) {
        if (!isEnabled()) return;
        JDGui.help(_GUI._.ForceDownloadAction_actionPerformed_help_title_(), _GUI._.ForceDownloadAction_actionPerformed_help_msg_(), NewTheme.I().getIcon("robot_info", -1));
        DownloadWatchDog.getInstance().resume(getSelection().getChildren());
        DownloadWatchDog.getInstance().forceDownload(getSelection().getChildren());

    }

}