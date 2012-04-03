package org.jdownloader.gui.views.downloads.context;

import java.awt.event.ActionEvent;
import java.util.ArrayList;

import jd.plugins.DownloadLink;

import org.jdownloader.actions.AppAction;
import org.jdownloader.dlc.DLCFactory;
import org.jdownloader.gui.translate._GUI;

public class CreateDLCAction extends AppAction {

    private static final long             serialVersionUID = 7244681674979415222L;

    private final ArrayList<DownloadLink> links;

    public CreateDLCAction(ArrayList<DownloadLink> links) {
        this.links = links;
        setIconKey("dlc");
        setName(_GUI._.gui_table_contextmenu_dlc());
    }

    public void actionPerformed(ActionEvent e) {

        new DLCFactory().createDLC(links);

    }

}