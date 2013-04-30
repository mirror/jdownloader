package org.jdownloader.gui.views.downloads.action;

import java.awt.Image;
import java.awt.event.ActionEvent;
import java.util.ArrayList;

import javax.swing.ImageIcon;

import jd.controlling.downloadcontroller.DownloadWatchDog;
import jd.plugins.DownloadLink;

import org.appwork.utils.ImageProvider.ImageProvider;
import org.jdownloader.actions.AppAction;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.NewTheme;

public class SuperPriorityDownloadAction extends AppAction {

    private static final long             serialVersionUID = 7107840091963427544L;

    private final java.util.List<DownloadLink> links;

    public SuperPriorityDownloadAction(final java.util.List<DownloadLink> links) {
        this.links = links;
        Image add = NewTheme.I().getImage("media-playback-start", 20);
        Image play = NewTheme.I().getImage("prio_1", 14);
        setSmallIcon(new ImageIcon(ImageProvider.merge(add, play, -4, 0, 6, 10)));
        setName(_GUI._.gui_table_contextmenu_SuperPriorityDownloadAction());
    }

    @Override
    public boolean isEnabled() {
        return !links.isEmpty() && DownloadWatchDog.getInstance().getStateMachine().isState(DownloadWatchDog.IDLE_STATE, DownloadWatchDog.RUNNING_STATE, DownloadWatchDog.STOPPED_STATE);
    }

    public void actionPerformed(ActionEvent e) {

        for (DownloadLink link : links) {
            link.setPriority(3);
        }

    }

}