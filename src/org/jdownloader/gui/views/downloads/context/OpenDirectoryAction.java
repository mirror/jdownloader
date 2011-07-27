package org.jdownloader.gui.views.downloads.context;

import java.awt.event.ActionEvent;
import java.io.File;

import jd.gui.swing.jdgui.interfaces.ContextMenuAction;
import jd.utils.JDUtilities;

import org.jdownloader.gui.translate._GUI;

public class OpenDirectoryAction extends ContextMenuAction {

    private static final long serialVersionUID = 3656369075540437063L;

    private final File        folder;

    public OpenDirectoryAction(File folder) {
        this.folder = folder;

        init();
    }

    @Override
    protected String getIcon() {
        return "package_open";
    }

    @Override
    protected String getName() {
        return _GUI._.gui_table_contextmenu_downloaddir();
    }

    public void actionPerformed(ActionEvent e) {
        JDUtilities.openExplorer(folder);
    }

}