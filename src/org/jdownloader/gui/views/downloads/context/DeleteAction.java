package org.jdownloader.gui.views.downloads.context;

import java.awt.event.ActionEvent;
import java.util.ArrayList;

import jd.gui.UserIO;
import jd.gui.swing.jdgui.interfaces.ContextMenuAction;
import jd.plugins.DownloadLink;

import org.jdownloader.gui.translate._GUI;

public class DeleteAction extends ContextMenuAction {

    private static final long             serialVersionUID = -5721724901676405104L;

    private final ArrayList<DownloadLink> links;

    private boolean                       force            = false;

    public DeleteAction(ArrayList<DownloadLink> links) {
        this.links = links;
        init();
    }

    public DeleteAction(ArrayList<DownloadLink> links, boolean force) {
        this.links = links;
        this.force = force;
        init();
    }

    @Override
    protected String getIcon() {
        return "delete";
    }

    @Override
    protected String getName() {
        return _GUI._.gui_table_contextmenu_deletelist2() + " (" + links.size() + ")";
    }

    public void actionPerformed(ActionEvent e) {
        if (!isEnabled()) return;
        if (force || UserIO.isOK(UserIO.getInstance().requestConfirmDialog(UserIO.DONT_SHOW_AGAIN | UserIO.DONT_SHOW_AGAIN_IGNORES_CANCEL, _GUI._.gui_downloadlist_delete() + " (" + _GUI._.gui_downloadlist_delete_size_packagev2(links.size()) + ")"))) {
            for (DownloadLink link : links) {
                link.setEnabled(false);
                link.deleteFile(true, false);
                link.getFilePackage().remove(link);
            }
        }
    }

    @Override
    public boolean isEnabled() {
        if (links != null && links.size() > 0) return true;
        return false;
    }

}