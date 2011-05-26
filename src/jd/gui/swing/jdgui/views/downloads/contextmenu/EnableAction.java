package jd.gui.swing.jdgui.views.downloads.contextmenu;

import java.awt.event.ActionEvent;
import java.util.ArrayList;

import jd.gui.swing.jdgui.interfaces.ContextMenuAction;
import jd.plugins.DownloadLink;
import jd.utils.JDUtilities;

import org.jdownloader.gui.translate._GUI;

public class EnableAction extends ContextMenuAction {

    private static final long             serialVersionUID = 782024175742217929L;

    private final ArrayList<DownloadLink> links;

    public EnableAction(ArrayList<DownloadLink> links) {
        this.links = new ArrayList<DownloadLink>();
        for (DownloadLink link : links) {
            if (!link.isEnabled()) this.links.add(link);
        }
        init();
    }

    @Override
    protected String getIcon() {
        return "true";
    }

    @Override
    protected String getName() {
        return _GUI._.gui_table_contextmenu_enable() + " (" + links.size() + ")";
    }

    @Override
    public boolean isEnabled() {
        return !links.isEmpty();
    }

    public void actionPerformed(ActionEvent e) {
        for (DownloadLink link : links) {
            link.setEnabled(true);
        }
        JDUtilities.getDownloadController().fireStructureUpdate();
    }

}