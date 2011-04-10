package jd.gui.swing.jdgui.views.downloads.contextmenu;


 import org.jdownloader.gui.translate.*;
import java.awt.event.ActionEvent;
import java.util.ArrayList;

import jd.gui.swing.jdgui.interfaces.ContextMenuAction;
import jd.plugins.DownloadLink;
import jd.utils.JDUtilities;
import jd.utils.locale.JDL;

public class DisableAction extends ContextMenuAction {

    private static final long serialVersionUID = -6491057147256278684L;

    private final ArrayList<DownloadLink> links;

    public DisableAction(ArrayList<DownloadLink> links) {
        this.links = new ArrayList<DownloadLink>();
        for (DownloadLink link : links) {
            if (link.isEnabled()) this.links.add(link);
        }

        init();
    }

    @Override
    protected String getIcon() {
        return "gui.images.bad";
    }

    @Override
    protected String getName() {
        return T._.gui_table_contextmenu_disable() + " (" + links.size() + ")";
    }

    @Override
    public boolean isEnabled() {
        return !links.isEmpty();
    }

    public void actionPerformed(ActionEvent e) {
        for (DownloadLink link : links) {
            link.setEnabled(false);
        }
        JDUtilities.getDownloadController().fireStructureUpdate();
    }

}