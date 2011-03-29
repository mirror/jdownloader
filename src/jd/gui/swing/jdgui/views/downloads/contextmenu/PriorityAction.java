package jd.gui.swing.jdgui.views.downloads.contextmenu;

import java.awt.event.ActionEvent;
import java.util.ArrayList;

import jd.controlling.DownloadController;
import jd.gui.swing.jdgui.interfaces.ContextMenuAction;
import jd.gui.swing.jdgui.views.downloads.DownloadTable;
import jd.plugins.DownloadLink;

public class PriorityAction extends ContextMenuAction {

    private static final long serialVersionUID = 4016589318975322111L;

    private final ArrayList<DownloadLink> links;
    private final int priority;

    public PriorityAction(ArrayList<DownloadLink> links, int priority) {
        this.links = links;
        this.priority = priority;

        init();
    }

    @Override
    protected String getIcon() {
        return "gui.images.priority" + priority;
    }

    @Override
    protected String getName() {
        return DownloadTable.PRIO_DESCS[priority + 1];
    }

    @Override
    public boolean isEnabled() {
        return links.size() != 1 || links.get(0).getPriority() != priority;
    }

    public void actionPerformed(ActionEvent e) {
        for (DownloadLink link : links) {
            link.setPriority(priority);
        }
        DownloadController.getInstance().fireDownloadLinkUpdate(links);
    }

}
