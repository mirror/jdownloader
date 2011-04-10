package jd.gui.swing.jdgui.views.downloads.contextmenu;


 import org.jdownloader.gui.translate.*;
import java.awt.event.ActionEvent;
import java.io.File;

import jd.gui.swing.jdgui.interfaces.ContextMenuAction;
import jd.utils.JDUtilities;
import jd.utils.locale.JDL;

public class OpenDirectoryAction extends ContextMenuAction {

    private static final long serialVersionUID = 3656369075540437063L;

    private final File folder;

    public OpenDirectoryAction(File folder) {
        this.folder = folder;

        init();
    }

    @Override
    protected String getIcon() {
        return "gui.images.package_opened";
    }

    @Override
    protected String getName() {
        return T._.gui_table_contextmenu_downloaddir();
    }

    public void actionPerformed(ActionEvent e) {
        JDUtilities.openExplorer(folder);
    }

}