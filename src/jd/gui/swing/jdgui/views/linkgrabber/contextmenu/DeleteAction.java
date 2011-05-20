package jd.gui.swing.jdgui.views.linkgrabber.contextmenu;

import java.awt.event.ActionEvent;
import java.util.ArrayList;

import jd.controlling.LinkGrabberController;
import jd.gui.UserIO;
import jd.gui.swing.jdgui.interfaces.ContextMenuAction;
import jd.plugins.DownloadLink;
import jd.plugins.LinkGrabberFilePackage;

import org.jdownloader.gui.translate._GUI;

public class DeleteAction extends ContextMenuAction {

    private static final long             serialVersionUID = -681500033424874147L;

    private final ArrayList<DownloadLink> links;

    public DeleteAction(ArrayList<DownloadLink> links) {
        this.links = links;

        init();
    }

    @Override
    protected String getIcon() {
        return "delete";
    }

    @Override
    protected String getName() {
        return _GUI._.gui_table_contextmenu_remove() + " (" + links.size() + ")";
    }

    public void actionPerformed(ActionEvent e) {
        LinkGrabberController controller = LinkGrabberController.getInstance();

        if (UserIO.isOK(UserIO.getInstance().requestConfirmDialog(UserIO.DONT_SHOW_AGAIN | UserIO.DONT_SHOW_AGAIN_IGNORES_CANCEL, _GUI._.gui_downloadlist_delete() + " (" + _GUI._.gui_downloadlist_delete_size_packagev2(links.size()) + ")"))) {
            LinkGrabberFilePackage fp;
            for (DownloadLink link : links) {
                link.setProperty("removed", true);
                fp = controller.getFPwithLink(link);
                if (fp == null) continue;
                fp.remove(link);
            }
        }
    }

}