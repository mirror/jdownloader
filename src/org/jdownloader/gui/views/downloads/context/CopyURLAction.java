package org.jdownloader.gui.views.downloads.context;

import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.HashSet;

import jd.controlling.ClipboardHandler;
import jd.gui.swing.jdgui.interfaces.ContextMenuAction;
import jd.plugins.DownloadLink;

import org.jdownloader.gui.translate._GUI;

public class CopyURLAction extends ContextMenuAction {

    private static final long             serialVersionUID = -8775747188751533463L;

    private final ArrayList<DownloadLink> links;

    public CopyURLAction(ArrayList<DownloadLink> links) {
        this.links = new ArrayList<DownloadLink>();
        for (DownloadLink link : links) {
            if (link.getLinkType() == DownloadLink.LINKTYPE_NORMAL) this.links.add(link);
        }

        init();
    }

    @Override
    protected String getIcon() {
        return "cut";
    }

    @Override
    protected String getName() {
        return _GUI._.gui_table_contextmenu_copyLink() + " (" + links.size() + ")";
    }

    @Override
    public boolean isEnabled() {
        return !links.isEmpty();
    }

    public void actionPerformed(ActionEvent e) {
        StringBuilder sb = new StringBuilder();
        HashSet<String> list = new HashSet<String>();
        for (DownloadLink link : links) {
            String url = link.getBrowserUrl();
            if (!list.contains(url)) {
                if (list.size() > 0) sb.append("\r\n");
                list.add(url);
                sb.append(url);
            }
        }
        ClipboardHandler.getClipboard().copyTextToClipboard(sb.toString());
    }

}