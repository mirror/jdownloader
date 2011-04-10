package jd.gui.swing.jdgui.views.downloads.contextmenu;


 import org.jdownloader.gui.translate.*;
import java.awt.event.ActionEvent;
import java.util.ArrayList;

import jd.gui.swing.jdgui.interfaces.ContextMenuAction;
import jd.plugins.DownloadLink;
import jd.utils.locale.JDL;

import org.appwork.utils.os.CrossSystem;

public class OpenInBrowserAction extends ContextMenuAction {

    private static final long             serialVersionUID = 7911375550836173693L;

    private final ArrayList<DownloadLink> links;

    public OpenInBrowserAction(ArrayList<DownloadLink> links) {
        this.links = links;

        init();
    }

    @Override
    protected String getIcon() {
        return "gui.images.browse";
    }

    @Override
    protected String getName() {
        return T._.gui_table_contextmenu_browselink();
    }

    @Override
    public boolean isEnabled() {
        return links.size() == 1 && links.get(0).getLinkType() == DownloadLink.LINKTYPE_NORMAL && CrossSystem.isOpenBrowserSupported();
    }

    public void actionPerformed(ActionEvent e) {
        CrossSystem.openURLOrShowMessage(links.get(0).getBrowserUrl());
    }

}