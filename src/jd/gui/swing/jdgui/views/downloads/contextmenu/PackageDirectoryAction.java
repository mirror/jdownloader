package jd.gui.swing.jdgui.views.downloads.contextmenu;

import java.awt.event.ActionEvent;
import java.io.File;
import java.util.ArrayList;

import jd.gui.UserIO;
import jd.gui.swing.GuiRunnable;
import jd.gui.swing.jdgui.interfaces.ContextMenuAction;
import jd.plugins.FilePackage;

import org.jdownloader.gui.translate._GUI;

public class PackageDirectoryAction extends ContextMenuAction {

    private static final long            serialVersionUID = -4322266872775860673L;

    private final ArrayList<FilePackage> packages;

    public PackageDirectoryAction(ArrayList<FilePackage> packages) {
        this.packages = packages;

        init();
    }

    @Override
    protected String getIcon() {
        return "gui.images.save";
    }

    @Override
    protected String getName() {
        return _GUI._.gui_table_contextmenu_editdownloaddir() + " (" + packages.size() + ")";
    }

    public void actionPerformed(ActionEvent e) {
        new GuiRunnable<Object>() {
            @Override
            public Object runSave() {
                File[] files = UserIO.getInstance().requestFileChooser(null, null, UserIO.DIRECTORIES_ONLY, null, null, new File(packages.get(0).getDownloadDirectory()), null);
                if (files == null) return null;

                for (FilePackage packagee : packages) {
                    packagee.setDownloadDirectory(files[0].getAbsolutePath());
                }
                return null;
            }
        }.start();
    }

}