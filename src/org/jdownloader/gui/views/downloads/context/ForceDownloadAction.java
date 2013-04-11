package org.jdownloader.gui.views.downloads.context;

import java.awt.Image;
import java.awt.event.ActionEvent;
import java.io.File;

import javax.swing.ImageIcon;

import jd.controlling.downloadcontroller.DownloadWatchDog;
import jd.gui.swing.jdgui.JDGui;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;

import org.appwork.utils.Files;
import org.appwork.utils.Files.AbstractHandler;
import org.appwork.utils.ImageProvider.ImageProvider;
import org.jdownloader.actions.AppAction;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.SelectionInfo;
import org.jdownloader.images.NewTheme;

public class ForceDownloadAction extends AppAction {

    private static final long                              serialVersionUID = 7107840091963427544L;

    private final SelectionInfo<FilePackage, DownloadLink> si;

    public ForceDownloadAction(final SelectionInfo<FilePackage, DownloadLink> si) {
        this.si = si;
        Image add = NewTheme.I().getImage("media-playback-start", 20);
        Image play = NewTheme.I().getImage("prio_3", 14);
        setSmallIcon(new ImageIcon(ImageProvider.merge(add, play, -4, 0, 6, 10)));
        setName(_GUI._.gui_table_contextmenu_tryforcethisdownload());
    }

    @Override
    public boolean isEnabled() {
        return !si.isEmpty() && DownloadWatchDog.getInstance().getStateMachine().isState(DownloadWatchDog.IDLE_STATE, DownloadWatchDog.RUNNING_STATE, DownloadWatchDog.PAUSE_STATE, DownloadWatchDog.STOPPED_STATE);
    }

    public void actionPerformed(ActionEvent e) {
        if (!isEnabled()) return;
        JDGui.help(_GUI._.ForceDownloadAction_actionPerformed_help_title_(), _GUI._.ForceDownloadAction_actionPerformed_help_msg_(), NewTheme.I().getIcon("robot_info", -1));
        DownloadWatchDog.getInstance().forceDownload(si.getSelectedChildren());
    }

    public static void main(String[] args) {
        long t = System.currentTimeMillis();
        Files.walkThroughStructure(new AbstractHandler<RuntimeException>() {
            private long i;
            {
                i = 0l;
            }

            @Override
            public void onFile(final File f) throws RuntimeException {
                i++;
            }

        }, new File("C:\\Users\\Thomas\\.awupdsys\\avlintern\\source"));

        System.out.println(System.currentTimeMillis() - t);
    }
}