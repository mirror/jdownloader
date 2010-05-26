package jd.gui.swing.jdgui.views.linkgrabber.contextmenu;

import java.awt.event.ActionEvent;
import java.util.ArrayList;

import jd.gui.swing.jdgui.interfaces.ContextMenuAction;
import jd.gui.swing.jdgui.views.linkgrabber.LinkGrabberPanel;
import jd.plugins.DownloadLink;
import jd.utils.locale.JDL;

public class CheckStatusAction extends ContextMenuAction {

    private static final long serialVersionUID = -5858369633927419005L;

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
        LinkGrabberPanel.getLinkGrabber().recheckLinks(links);
    }

}
