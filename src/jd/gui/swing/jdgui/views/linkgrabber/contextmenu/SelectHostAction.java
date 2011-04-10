package jd.gui.swing.jdgui.views.linkgrabber.contextmenu;

import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Set;

import jd.controlling.LinkGrabberController;
import jd.gui.swing.jdgui.interfaces.ContextMenuAction;
import jd.plugins.LinkGrabberFilePackage;

import org.jdownloader.gui.translate.T;

public class SelectHostAction extends ContextMenuAction {

    private static final long serialVersionUID = -6987398422650156384L;

    private final Set<String> hosts;

    public SelectHostAction(Set<String> hosts) {
        this.hosts = hosts;

        init();
    }

    @Override
    protected String getIcon() {
        return "gui.images.addselected";
    }

    @Override
    protected String getName() {
        return T._.gui_linkgrabberv2_onlyselectedhoster() + " (" + hosts.size() + ")";
    }

    public void actionPerformed(ActionEvent e) {
        synchronized (LinkGrabberController.ControllerLock) {
            ArrayList<LinkGrabberFilePackage> packagess = LinkGrabberController.getInstance().getPackages();
            synchronized (packagess) {
                ArrayList<LinkGrabberFilePackage> packages = new ArrayList<LinkGrabberFilePackage>(packagess);
                packages.add(LinkGrabberController.getInstance().getFilterPackage());
                for (LinkGrabberFilePackage packagee : packages) {
                    packagee.keepHostersOnly(hosts);
                }
            }
        }
    }

}