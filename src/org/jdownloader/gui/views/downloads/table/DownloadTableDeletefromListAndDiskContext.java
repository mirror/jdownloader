package org.jdownloader.gui.views.downloads.table;

import javax.swing.JMenu;

import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;

import org.jdownloader.gui.menu.MenuContext;
import org.jdownloader.gui.views.SelectionInfo;

public class DownloadTableDeletefromListAndDiskContext extends MenuContext<JMenu> {

    private SelectionInfo<FilePackage, DownloadLink> selectionInfo;

    public SelectionInfo<FilePackage, DownloadLink> getSelectionInfo() {
        return selectionInfo;
    }

    public DownloadTableDeletefromListAndDiskContext(JMenu submenu, SelectionInfo<FilePackage, DownloadLink> si) {
        super(submenu);

        this.selectionInfo = si;

    }

}
