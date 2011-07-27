package org.jdownloader.gui.views.downloads.context;

import java.awt.event.ActionEvent;
import java.util.ArrayList;

import javax.swing.JMenu;

import jd.controlling.DownloadController;
import jd.gui.swing.jdgui.interfaces.ContextMenuAction;
import jd.gui.swing.jdgui.views.downloads.DownloadTable;
import jd.plugins.DownloadLink;

import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.NewTheme;

public class PriorityAction extends ContextMenuAction {

    private static final long             serialVersionUID = 4016589318975322111L;

    private final ArrayList<DownloadLink> links;
    private final int                     priority;

    public PriorityAction(ArrayList<DownloadLink> links, int priority) {
        this.links = links;
        this.priority = priority;

        init();
    }

    @Override
    protected String getIcon() {
        return "prio_" + priority;
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

    public static JMenu createPrioMenu(final ArrayList<DownloadLink> links) {
        final JMenu prioPopup = new JMenu(_GUI._.gui_table_contextmenu_priority() + " (" + links.size() + ")");
        prioPopup.setIcon(NewTheme.I().getIcon("prio_0", 16));

        prioPopup.add(new PriorityAction(links, 3));
        prioPopup.add(new PriorityAction(links, 2));
        prioPopup.add(new PriorityAction(links, 1));
        prioPopup.add(new PriorityAction(links, 0));
        prioPopup.add(new PriorityAction(links, -1));

        return prioPopup;
    }

}
