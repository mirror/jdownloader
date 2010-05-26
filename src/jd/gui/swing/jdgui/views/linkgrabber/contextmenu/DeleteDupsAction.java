package jd.gui.swing.jdgui.views.linkgrabber.contextmenu;

import java.awt.event.ActionEvent;
import java.util.ArrayList;

import jd.controlling.LinkGrabberController;
import jd.gui.swing.jdgui.interfaces.ContextMenuAction;
import jd.plugins.LinkGrabberFilePackage;
import jd.plugins.LinkStatus;
import jd.utils.locale.JDL;

public class DeleteDupsAction extends ContextMenuAction {

    private static final long serialVersionUID = 108351717558746494L;

    public DeleteDupsAction() {
        init();
    }

    @Override
    protected String getIcon() {
        return "gui.images.remove_dupes";
    }

    @Override
    protected String getName() {
        return JDL.L("gui.linkgrabberv2.lg.rmdups", "Remove all Duplicates");
    }

    public void actionPerformed(ActionEvent e) {
        synchronized (LinkGrabberController.ControllerLock) {
            ArrayList<LinkGrabberFilePackage> packagess = LinkGrabberController.getInstance().getPackages();
            synchronized (packagess) {
                ArrayList<LinkGrabberFilePackage> packages = new ArrayList<LinkGrabberFilePackage>(packagess);
                packages.add(LinkGrabberController.getInstance().getFilterPackage());
                for (LinkGrabberFilePackage packagee : packages) {
                    packagee.remove(packagee.getLinksListbyStatus(LinkStatus.ERROR_ALREADYEXISTS));
                }
            }
        }
    }

}
