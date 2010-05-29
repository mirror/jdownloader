package jd.gui.swing.jdgui.views.linkgrabber.contextmenu;

import java.awt.event.ActionEvent;
import java.util.ArrayList;

import jd.controlling.JDLogger;
import jd.controlling.LinkGrabberController;
import jd.gui.swing.jdgui.interfaces.ContextMenuAction;
import jd.gui.swing.jdgui.views.linkgrabber.LinkGrabberPanel;
import jd.plugins.DownloadLink;
import jd.plugins.LinkGrabberFilePackage;
import jd.utils.locale.JDL;

public class ContinueLinksAction extends ContextMenuAction {

    private static final long serialVersionUID = -3500141752693214211L;

    private final ArrayList<DownloadLink> links;

    public ContinueLinksAction(ArrayList<DownloadLink> links) {
        this.links = links;

        init();
    }

    @Override
    protected String getIcon() {
        return "gui.images.taskpanes.linkgrabber";
    }

    @Override
    protected String getName() {
        return JDL.L("gui.linkgrabberv2.lg.continueselectedlinks", "Continue with selected link(s)") + " (" + links.size() + ")";
    }

    public void actionPerformed(ActionEvent e) {
        LinkGrabberController controller = LinkGrabberController.getInstance();

        ArrayList<LinkGrabberFilePackage> packages = new ArrayList<LinkGrabberFilePackage>();
        while (links.size() > 0) {
            ArrayList<DownloadLink> links2 = new ArrayList<DownloadLink>(links);
            LinkGrabberFilePackage fp3 = controller.getFPwithLink(links.get(0));
            if (fp3 == null) {
                JDLogger.getLogger().warning("DownloadLink not controlled by LinkGrabberController!");
                links.remove(links.get(0));
                continue;
            }

            LinkGrabberFilePackage fp4 = new LinkGrabberFilePackage(fp3.getName());
            fp4.setDownloadDirectory(fp3.getDownloadDirectory());
            fp4.setPassword(fp3.getPassword());
            fp4.setPostProcessing(fp3.isPostProcessing());
            fp4.setUseSubDir(fp3.useSubDir());
            fp4.setComment(fp3.getComment());
            for (DownloadLink dl : links2) {
                if (controller.getFPwithLink(dl) == fp3) {
                    fp4.add(dl);
                    links.remove(dl);
                }
            }
            packages.add(fp4);
        }
        LinkGrabberPanel.getLinkGrabber().confirmPackages(packages);
    }

}
