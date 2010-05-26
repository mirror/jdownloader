package jd.gui.swing.jdgui.views.downloads.contextmenu;

import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Set;

import jd.controlling.DownloadWatchDog;
import jd.gui.swing.jdgui.interfaces.ContextMenuAction;
import jd.plugins.DownloadLink;
import jd.plugins.LinkStatus;
import jd.utils.locale.JDL;

public class ResumeAction extends ContextMenuAction {

    private static final long serialVersionUID = 8087143123808363305L;

    private final ArrayList<DownloadLink> links;

    public ResumeAction(ArrayList<DownloadLink> links) {
        this.links = new ArrayList<DownloadLink>();
        for (DownloadLink link : links) {
            if (!link.getLinkStatus().isPluginActive() && link.getLinkStatus().isFailed()) this.links.add(link);
        }

        init();
    }

    @Override
    protected String getIcon() {
        return "gui.images.resume";
    }

    @Override
    protected String getName() {
        return JDL.L("gui.table.contextmenu.resume", "Resume") + " (" + links.size() + ")";
    }

    @Override
    public boolean isEnabled() {
        return !links.isEmpty();
    }

    public void actionPerformed(ActionEvent e) {
        for (DownloadLink link : links) {
            link.getLinkStatus().setStatus(LinkStatus.TODO);
            link.getLinkStatus().resetWaitTime();
            link.getLinkStatus().setStatusText(JDL.L("gui.linklist.status.doresume", "Wait to resume"));
        }

        Set<String> hosts = DownloadLink.getHosterList(links);
        for (String host : hosts) {
            DownloadWatchDog.getInstance().resetIPBlockWaittime(host);
            DownloadWatchDog.getInstance().resetTempUnavailWaittime(host);
        }
    }

}
