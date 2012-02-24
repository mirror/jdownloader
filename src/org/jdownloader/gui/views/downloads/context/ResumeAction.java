package org.jdownloader.gui.views.downloads.context;

import java.awt.event.ActionEvent;
import java.util.ArrayList;

import jd.controlling.IOEQ;
import jd.controlling.downloadcontroller.DownloadWatchDog;
import jd.gui.swing.jdgui.interfaces.ContextMenuAction;
import jd.plugins.DownloadLink;

import org.jdownloader.gui.translate._GUI;

public class ResumeAction extends ContextMenuAction {

    private static final long             serialVersionUID = 8087143123808363305L;

    private final ArrayList<DownloadLink> links;

    public ResumeAction(ArrayList<DownloadLink> links) {
        this.links = links;

        init();
    }

    @Override
    protected String getIcon() {
        return "resume";
    }

    @Override
    protected String getName() {
        return _GUI._.gui_table_contextmenu_resume() + " (" + links.size() + ")";
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
                        DownloadWatchDog.getInstance().removeIPBlockTimeout(link);
                        DownloadWatchDog.getInstance().removeTempUnavailTimeout(link);
                    }
                }
            }
        });
    }
}