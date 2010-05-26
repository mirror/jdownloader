package jd.gui.swing.jdgui.views.linkgrabber.contextmenu;

import java.awt.event.ActionEvent;
import java.util.ArrayList;

import jd.controlling.LinkGrabberController;
import jd.gui.swing.jdgui.interfaces.ContextMenuAction;
import jd.plugins.LinkGrabberFilePackage;
import jd.utils.locale.JDL;

public class DeleteOfflineAction extends ContextMenuAction {

    private static final long serialVersionUID = 5854888806467867386L;

    public DeleteOfflineAction() {
        init();
    }

    @Override
    protected String getIcon() {
        return "gui.images.remove_failed";
    }

    @Override
    protected String getName() {
        return JDL.L("gui.linkgrabberv2.lg.rmoffline", "Remove all Offline");
    }

    public void actionPerformed(ActionEvent e) {
        synchronized (LinkGrabberController.ControllerLock) {
            ArrayList<LinkGrabberFilePackage> packagess = LinkGrabberController.getInstance().getPackages();
            synchronized (packagess) {
                ArrayList<LinkGrabberFilePackage> packages = new ArrayList<LinkGrabberFilePackage>(packagess);
                packages.add(LinkGrabberController.getInstance().getFilterPackage());
                for (LinkGrabberFilePackage packagee : packages) {
                    packagee.removeOffline();
                }
            }
        }
    }

}
