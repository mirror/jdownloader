package jd.gui.swing.jdgui.views.downloads.contextmenu;

import java.awt.event.ActionEvent;
import java.io.File;

import jd.gui.swing.jdgui.interfaces.ContextMenuAction;

import org.appwork.utils.os.CrossSystem;
import org.jdownloader.gui.translate.T;

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
        return "gui.images.load";
    }

    @Override
    protected String getName() {
        return T._.gui_table_contextmenu_openfile();
    }

    public void actionPerformed(ActionEvent e) {
        CrossSystem.openFile(file);
    }

}