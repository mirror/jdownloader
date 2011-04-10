package jd.gui.swing.jdgui.views.linkgrabber.contextmenu;


 import org.jdownloader.gui.translate.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.util.ArrayList;

import jd.gui.UserIO;
import jd.gui.swing.GuiRunnable;
import jd.gui.swing.jdgui.interfaces.ContextMenuAction;
import jd.plugins.LinkGrabberFilePackage;
import jd.utils.locale.JDL;

public class PackageDirectoryAction extends ContextMenuAction {

    private static final long serialVersionUID = -7890741664214058627L;

    private final ArrayList<LinkGrabberFilePackage> packages;

    public PackageDirectoryAction(ArrayList<LinkGrabberFilePackage> packages) {
        this.packages = packages;

        init();
    }

    @Override
    protected String getIcon() {
        return "gui.images.save";
    }

    @Override
    protected String getName() {
        return T._.gui_table_contextmenu_editdownloaddir() + " (" + packages.size() + ")";
    }

    public void actionPerformed(ActionEvent e) {
        new GuiRunnable<Object>() {
            @Override
            public Object runSave() {
                File[] files = UserIO.getInstance().requestFileChooser(null, null, UserIO.DIRECTORIES_ONLY, null, null, new File(packages.get(0).getDownloadDirectory()), null);
                if (files == null) return null;

                for (LinkGrabberFilePackage packagee : packages) {
                    packagee.setDownloadDirectory(files[0].getAbsolutePath());
                }
                return null;
            }
        }.start();
    }

}