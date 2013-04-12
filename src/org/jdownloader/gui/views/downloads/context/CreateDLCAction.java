package org.jdownloader.gui.views.downloads.context;

import java.awt.event.ActionEvent;

import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;

import org.jdownloader.actions.AppAction;
import org.jdownloader.dlc.DLCFactory;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.SelectionInfo;
import org.jdownloader.logging.LogController;

public class CreateDLCAction extends AppAction {

    private static final long                              serialVersionUID = 7244681674979415222L;

    private final SelectionInfo<FilePackage, DownloadLink> links;

    public CreateDLCAction(SelectionInfo<FilePackage, DownloadLink> si) {
        this.links = si;
        setIconKey("dlc");
        setName(_GUI._.gui_table_contextmenu_dlc());
    }

    public void actionPerformed(ActionEvent e) {

        DLCFactory plugin = new DLCFactory();
        plugin.setLogger(LogController.CL());
        plugin.createDLC(links.getChildren());

    }

}