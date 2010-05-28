package jd.gui.swing.jdgui.views.downloads.contextmenu;

import java.awt.event.ActionEvent;
import java.util.ArrayList;

import jd.gui.UserIO;
import jd.gui.swing.jdgui.interfaces.ContextMenuAction;
import jd.plugins.DownloadLink;
import jd.utils.locale.JDL;

public class ResetAction extends ContextMenuAction {

    private static final long serialVersionUID = -5583373118359478729L;

    private final ArrayList<DownloadLink> links;

    public ResetAction(ArrayList<DownloadLink> links) {
        this.links = links;

        init();
    }

    @Override
    protected String getIcon() {
        return "gui.images.undo";
    }

    @Override
    protected String getName() {
        return JDL.L("gui.table.contextmenu.reset", "Reset") + " (" + links.size() + ")";
    }

    public void actionPerformed(ActionEvent e) {
        new Thread() {
            @Override
            public void run() {
                if (UserIO.isOK(UserIO.getInstance().requestConfirmDialog(UserIO.DONT_SHOW_AGAIN | UserIO.DONT_SHOW_AGAIN_IGNORES_CANCEL, JDL.L("gui.downloadlist.reset", "Reset selected downloads?") + " (" + JDL.LF("gui.downloadlist.delete.size_packagev2", "%s links", links.size()) + ")"))) {
                    for (DownloadLink link : links) {
                        link.reset();
                    }
                }
            }
        }.start();
    }

}
