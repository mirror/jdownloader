package org.jdownloader.gui.views.downloads.context;

import java.awt.event.ActionEvent;
import java.io.File;

import jd.gui.swing.jdgui.interfaces.ContextMenuAction;

import org.appwork.utils.os.CrossSystem;
import org.jdownloader.gui.translate._GUI;

public class OpenFileAction extends ContextMenuAction {

    private static final long serialVersionUID = 1901008532686173167L;

    private final File        file;

    public OpenFileAction(File file) {
        this.file = file;
        init();
    }

    @Override
    public boolean isEnabled() {
        return file != null && file.exists();
    }

    @Override
    protected String getIcon() {
        return "load";
    }

    @Override
    protected String getName() {
        return _GUI._.gui_table_contextmenu_openfile();
    }

    public void actionPerformed(ActionEvent e) {
        CrossSystem.openFile(file);
    }

}