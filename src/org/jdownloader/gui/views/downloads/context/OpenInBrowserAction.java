package org.jdownloader.gui.views.downloads.context;

import java.awt.event.ActionEvent;
import java.util.List;

import jd.plugins.DownloadLink;

import org.appwork.utils.os.CrossSystem;
import org.jdownloader.actions.AppAction;
import org.jdownloader.gui.translate._GUI;

public class OpenInBrowserAction extends AppAction {

    private static final long        serialVersionUID = 7911375550836173693L;
    private static final int         MAX_LINKS        = 4;
    private final List<DownloadLink> links;

    public OpenInBrowserAction(List<DownloadLink> list) {
        this.links = list;

        setIconKey("browse");
        setName(_GUI._.gui_table_contextmenu_browselink());
    }

    @Override
    public boolean isEnabled() {
        if (links.size() > MAX_LINKS) return false;
        if (!CrossSystem.isOpenBrowserSupported()) return false;
        for (DownloadLink link : links) {
            if (link.getLinkType() == DownloadLink.LINKTYPE_NORMAL) return true;
            if (link.getLinkType() == DownloadLink.LINKTYPE_CONTAINER && link.gotBrowserUrl()) return true;
        }
        return false;
    }

    public void actionPerformed(ActionEvent e) {
        if (this.isEnabled()) { // additional security measure. Someone may call
                                // actionPerformed in the code although the
                                // action should be disabled
            for (DownloadLink link : links) {
                CrossSystem.openURLOrShowMessage(link.getBrowserUrl());
            }
        }
    }

}