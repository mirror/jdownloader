package jd.gui.swing.jdgui.views.downloads.contextmenu;

import java.awt.event.ActionEvent;
import java.util.ArrayList;

import jd.controlling.LinkCheck;
import jd.gui.swing.jdgui.interfaces.ContextMenuAction;
import jd.plugins.DownloadLink;
import jd.utils.locale.JDL;

public class CheckStatusAction extends ContextMenuAction {

    private static final long serialVersionUID = 6821943398259956694L;

    private final ArrayList<DownloadLink> links;

    public CheckStatusAction(ArrayList<DownloadLink> links) {
        this.links = links;

        init();
    }

    @Override
    protected String getIcon() {
        return "gui.images.config.network_local";
    }

    @Override
    protected String getName() {
        return JDL.L("gui.table.contextmenu.check", "Check Online Status") + " (" + links.size() + ")";
    }

    public void actionPerformed(ActionEvent e) {
        LinkCheck.getLinkChecker().checkLinks(links, true);
    }

}
