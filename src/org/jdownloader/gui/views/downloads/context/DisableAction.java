package org.jdownloader.gui.views.downloads.context;

import java.awt.event.ActionEvent;
import java.util.ArrayList;

import jd.gui.swing.jdgui.interfaces.ContextMenuAction;
import jd.plugins.DownloadLink;
import jd.utils.JDUtilities;

import org.jdownloader.gui.translate._GUI;

public class DisableAction extends ContextMenuAction {

    private static final long             serialVersionUID = -6491057147256278684L;

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
        return "false";
    }

    @Override
    protected String getName() {
        return _GUI._.gui_table_contextmenu_disable() + " (" + links.size() + ")";
    }

    @Override
    public boolean isEnabled() {
        return !links.isEmpty();
    }

    public void actionPerformed(ActionEvent e) {
        for (DownloadLink link : links) {
            link.setEnabled(false);
        }
        JDUtilities.getDownloadController().fireDataUpdate();
    }

}