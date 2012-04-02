package org.jdownloader.gui.views.downloads.context;

import java.awt.event.ActionEvent;
import java.util.ArrayList;

import jd.controlling.IOEQ;
import jd.controlling.downloadcontroller.DownloadWatchDog;
import jd.gui.UserIO;
import jd.plugins.DownloadLink;

import org.jdownloader.actions.AppAction;
import org.jdownloader.gui.translate._GUI;

public class ResetAction extends AppAction {

    private static final long             serialVersionUID = -5583373118359478729L;

    private final ArrayList<DownloadLink> links;

    public ResetAction(ArrayList<DownloadLink> links) {
        this.links = links;
        setIconKey("undo");
        setName(_GUI._.gui_table_contextmenu_reset());
    }

    public void actionPerformed(ActionEvent e) {
        IOEQ.add(new Runnable() {
            public void run() {
                if (UserIO.isOK(UserIO.getInstance().requestConfirmDialog(UserIO.DONT_SHOW_AGAIN | UserIO.DONT_SHOW_AGAIN_IGNORES_CANCEL, _GUI._.gui_downloadlist_reset() + " (" + _GUI._.gui_downloadlist_delete_size_packagev2(links.size()) + ")"))) {
                    for (DownloadLink link : links) {
                        if (link.getLinkStatus().isPluginActive()) {
                            /*
                             * download is still active, let DownloadWatchdog
                             * handle the reset
                             */
                            DownloadWatchDog.getInstance().resetSingleDownloadController(link.getDownloadLinkController());
                        } else {
                            /* we can do the reset ourself */
                            DownloadWatchDog.getInstance().removeIPBlockTimeout(link);
                            DownloadWatchDog.getInstance().removeTempUnavailTimeout(link);
                            link.reset();
                        }
                    }
                }
            }
        });
    }
}