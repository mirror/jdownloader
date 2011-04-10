package jd.gui.swing.jdgui.views.downloads.contextmenu;


 import org.jdownloader.gui.translate.*;
import java.awt.event.ActionEvent;
import java.util.ArrayList;

import jd.gui.swing.jdgui.interfaces.ContextMenuAction;
import jd.plugins.DownloadLink;
import jd.plugins.LinkStatus;
import jd.utils.locale.JDL;

public class StopAction extends ContextMenuAction {

    private static final long serialVersionUID = -7844169547770293809L;

    private final ArrayList<DownloadLink> links;

    public StopAction(ArrayList<DownloadLink> links) {
        this.links = new ArrayList<DownloadLink>();
        for (DownloadLink link : links) {
            if (link.getLinkStatus().hasStatus(LinkStatus.DOWNLOADINTERFACE_IN_PROGRESS)) this.links.add(link);
        }

        init();
    }

    @Override
    protected String getIcon() {
        return "gui.images.stopsign";
    }

    @Override
    protected String getName() {
        return T._.jd_gui_swing_jdgui_views_downloads_contextmenu_StopAction_name() + " (" + links.size() + ")";
    }

    @Override
    public boolean isEnabled() {
        return !links.isEmpty();
    }

    public void actionPerformed(ActionEvent e) {
        // TODO: Stop link!
    }

}