package org.jdownloader.gui.views.downloads.context;

import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.HashSet;

import jd.controlling.ClipboardMonitoring;
import jd.gui.swing.jdgui.interfaces.ContextMenuAction;
import jd.plugins.DownloadLink;

import org.appwork.exceptions.WTFException;
import org.appwork.utils.logging.Log;
import org.jdownloader.gui.translate._GUI;

public class CopyPasswordAction extends ContextMenuAction {
    private static final long             serialVersionUID = -6747711277011715259L;

    private final ArrayList<DownloadLink> links;
    private final String                  password;

    public CopyPasswordAction(ArrayList<DownloadLink> links) {
        this.links = links;
        this.password = getPasswordSelectedLinks(links);

        init();
    }

    @Override
    protected String getIcon() {
        return "copy";
    }

    @Override
    protected String getName() {
        return _GUI._.gui_table_contextmenu_copyPassword() + " (" + links.size() + ")";
    }

    @Override
    public boolean isEnabled() {
        return password.length() != 0;
    }

    public void actionPerformed(ActionEvent e) {
        ClipboardMonitoring.getINSTANCE().setCurrentContent(password);
    }

    public static String getPasswordSelectedLinks(ArrayList<DownloadLink> links) {
        HashSet<String> list = new HashSet<String>();
        StringBuilder sb = new StringBuilder("");
        String pw;
        for (DownloadLink link : links) {
            Log.exception(new WTFException("TODO"));
        }
        return sb.toString();
    }

}