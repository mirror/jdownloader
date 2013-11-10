package org.jdownloader.gui.views.downloads.action;

import java.awt.event.ActionEvent;

import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;

import org.jdownloader.controlling.contextmenu.CustomizableSelectionAppAction;
import org.jdownloader.dlc.DLCFactory;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.logging.LogController;

public class CreateDLCAction extends CustomizableSelectionAppAction<FilePackage, DownloadLink> {

    private static final long serialVersionUID = 7244681674979415222L;

    public CreateDLCAction() {
        super();

        setIconKey("dlc");
        setName(_GUI._.gui_table_contextmenu_dlc());
    }

    public void actionPerformed(ActionEvent e) {

        DLCFactory plugin = new DLCFactory();
        plugin.setLogger(LogController.CL());
        plugin.createDLC(getSelection().getChildren());

    }

}