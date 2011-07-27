package org.jdownloader.gui.views.downloads.context;

import java.awt.event.ActionEvent;
import java.util.ArrayList;

import jd.gui.UserIO;
import jd.gui.swing.jdgui.interfaces.ContextMenuAction;
import jd.plugins.DownloadLink;

import org.jdownloader.gui.translate._GUI;

public class SetPasswordAction extends ContextMenuAction {

    private static final long             serialVersionUID = -6673856992749946616L;

    private final ArrayList<DownloadLink> links;

    public SetPasswordAction(ArrayList<DownloadLink> links) {
        this.links = links;

        init();
    }

    @Override
    protected String getIcon() {
        return "password";
    }

    @Override
    protected String getName() {
        return _GUI._.gui_table_contextmenu_setdlpw() + " (" + links.size() + ")";
    }

    public void actionPerformed(ActionEvent e) {
        String pw = UserIO.getInstance().requestInputDialog(0, _GUI._.gui_linklist_setpw_message(), null);
        for (DownloadLink link : links) {
            link.setProperty("pass", pw);
        }
    }

}