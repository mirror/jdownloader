package jd.gui.swing.jdgui.views.linkgrabber.contextmenu;

import java.awt.event.ActionEvent;
import java.io.File;
import java.util.ArrayList;

import jd.gui.swing.GuiRunnable;
import jd.gui.swing.SwingGui;
import jd.gui.swing.components.JDFileChooser;
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
        return JDL.L("gui.table.contextmenu.editdownloaddir", "Edit Directory") + " (" + packages.size() + ")";
    }

    public void actionPerformed(ActionEvent e) {
        new GuiRunnable<Object>() {
            @Override
            public Object runSave() {
                JDFileChooser fc = new JDFileChooser();
                fc.setApproveButtonText(JDL.L("gui.btn_ok", "OK"));
                fc.setFileSelectionMode(JDFileChooser.DIRECTORIES_ONLY);
                fc.setCurrentDirectory(new File(packages.get(0).getDownloadDirectory()));
                if (fc.showOpenDialog(SwingGui.getInstance().getMainFrame()) == JDFileChooser.APPROVE_OPTION) {
                    File ret = fc.getSelectedFile();
                    if (ret == null) return null;

                    for (LinkGrabberFilePackage packagee : packages) {
                        packagee.setDownloadDirectory(ret.getAbsolutePath());
                    }
                }
                return null;
            }
        }.start();
    }

}
