package jd.gui.swing.jdgui.views.downloads.contextmenu;


 import org.jdownloader.gui.translate.*;
import java.awt.event.ActionEvent;
import java.util.ArrayList;

import jd.gui.UserIO;
import jd.gui.swing.jdgui.interfaces.ContextMenuAction;
import jd.plugins.FilePackage;
import jd.utils.locale.JDL;

public class PackageNameAction extends ContextMenuAction {

    private static final long serialVersionUID = -5155537516674035401L;

    private final ArrayList<FilePackage> packages;

    public PackageNameAction(ArrayList<FilePackage> packages) {
        this.packages = packages;

        init();
    }

    @Override
    protected String getIcon() {
        return "gui.images.edit";
    }

    @Override
    protected String getName() {
        return T._.gui_table_contextmenu_editpackagename() + " (" + packages.size() + ")";
    }

    public void actionPerformed(ActionEvent e) {
        String name = UserIO.getInstance().requestInputDialog(0, T._.gui_linklist_editpackagename_message(), packages.get(0).getName());
        if (name == null) return;

        for (FilePackage packagee : packages) {
            packagee.setName(name);
        }
    }

}