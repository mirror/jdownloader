package org.jdownloader.gui.views.downloads.action;

import java.awt.event.ActionEvent;

import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;

import org.jdownloader.controlling.contextmenu.CustomizableTableContextAppAction;
import org.jdownloader.dlc.DLCFactory;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.logging.LogController;

public class CreateDLCAction extends CustomizableTableContextAppAction<FilePackage, DownloadLink> {

    private static final long   serialVersionUID = 7244681674979415222L;

    private final static String NAME             = _GUI._.gui_table_contextmenu_dlc();

    public CreateDLCAction() {
        super();
        setIconKey("dlc");
        setName(NAME);
    }

    public void actionPerformed(ActionEvent e) {
        DLCFactory plugin = new DLCFactory();
        plugin.setLogger(LogController.CL());
        plugin.createDLC(getSelection().getChildren());
    }

}