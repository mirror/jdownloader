package jd.gui.swing.jdgui.views.downloads.contextmenu;

import java.awt.event.ActionEvent;
import java.util.ArrayList;

import jd.gui.UserIO;
import jd.gui.swing.jdgui.interfaces.ContextMenuAction;
import jd.plugins.DownloadLink;

import org.jdownloader.gui.translate.T;

public class DeleteFromDiskAction extends ContextMenuAction {

    private static final long             serialVersionUID = -508777637671504774L;

    private final ArrayList<DownloadLink> links;
    private final int                     counter;

    public DeleteFromDiskAction(ArrayList<DownloadLink> links) {
        this.links = links;
        int counter = 0;
        for (DownloadLink link : links) {
            if (link.existsFile()) {
                counter++;
            }
        }
        this.counter = counter;

        init();
    }

    @Override
    protected String getIcon() {
        return "gui.images.delete";
    }

    @Override
    protected String getName() {
        return T._.gui_table_contextmenu_deletelistdisk2() + " (" + links.size() + "/" + counter + ")";
    }

    @Override
    public boolean isEnabled() {
        return counter > 0;
    }

    public void actionPerformed(ActionEvent e) {
        if (links.size() > 0 && UserIO.isOK(UserIO.getInstance().requestConfirmDialog(UserIO.DONT_SHOW_AGAIN | UserIO.DONT_SHOW_AGAIN_IGNORES_CANCEL, T._.gui_downloadlist_delete2() + " (" + T._.gui_downloadlist_delete_links(links.size()) + " /  " + T._.gui_downloadlist_delete_files(counter) + ")"))) {
            for (DownloadLink link : links) {
                link.setEnabled(false);
                link.deleteFile(true, true);
                link.getFilePackage().remove(link);
            }
        }
    }

}