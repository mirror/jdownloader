package jd.gui.swing.jdgui.views.downloads.contextmenu;

import java.awt.event.ActionEvent;
import java.util.ArrayList;

import jd.controlling.DownloadWatchDog;
import jd.gui.swing.jdgui.interfaces.ContextMenuAction;
import jd.plugins.DownloadLink;
import jd.utils.locale.JDL;

public class ForceDownloadAction extends ContextMenuAction {

    private static final long serialVersionUID = 7107840091963427544L;

    private final ArrayList<DownloadLink> links;

    public ForceDownloadAction(ArrayList<DownloadLink> links) {
        this.links = new ArrayList<DownloadLink>();
        for (DownloadLink link : links) {
            if (!link.getLinkStatus().isPluginActive()) this.links.add(link);
        }

        init();
    }

    @Override
    protected String getIcon() {
        return "gui.images.next";
    }

    @Override
    protected String getName() {
        return JDL.L("gui.table.contextmenu.tryforce", "Force download") + " (" + links.size() + ")";
    }

    @Override
    public boolean isEnabled() {
        return !links.isEmpty() && DownloadWatchDog.getInstance().getDownloadStatus() != DownloadWatchDog.STATE.STOPPING;
    }

    public void actionPerformed(ActionEvent e) {
        DownloadWatchDog.getInstance().forceDownload(links);
    }

}
