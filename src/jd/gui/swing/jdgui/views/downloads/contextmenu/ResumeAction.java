package jd.gui.swing.jdgui.views.downloads.contextmenu;

import java.awt.event.ActionEvent;
import java.util.ArrayList;

import jd.controlling.DownloadWatchDog;
import jd.controlling.IOEQ;
import jd.gui.swing.jdgui.interfaces.ContextMenuAction;
import jd.plugins.DownloadLink;

import org.jdownloader.gui.translate.T;

public class ResumeAction extends ContextMenuAction {

    private static final long             serialVersionUID = 8087143123808363305L;

    private final ArrayList<DownloadLink> links;

    public ResumeAction(ArrayList<DownloadLink> links) {
        this.links = links;

        init();
    }

    @Override
    protected String getIcon() {
        return "gui.images.resume";
    }

    @Override
    protected String getName() {
        return T._.gui_table_contextmenu_resume() + " (" + links.size() + ")";
    }

    @Override
    public boolean isEnabled() {
        return !links.isEmpty();
    }

    public void actionPerformed(ActionEvent e) {
        IOEQ.add(new Runnable() {
            public void run() {
                for (DownloadLink link : links) {
                    if (!link.getLinkStatus().isPluginActive() && link.getLinkStatus().isFailed()) {
                        link.getLinkStatus().reset(true);
                        String host = link.getHost();
                        DownloadWatchDog.getInstance().resetIPBlockWaittime(host);
                        DownloadWatchDog.getInstance().resetTempUnavailWaittime(host);
                    }
                }
            }
        });
    }
}