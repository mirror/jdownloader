package jd.gui.swing.jdgui.views.linkgrabber.contextmenu;

import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Set;

import jd.controlling.LinkGrabberController;
import jd.gui.swing.jdgui.interfaces.ContextMenuAction;
import jd.plugins.DownloadLink;
import jd.plugins.LinkGrabberFilePackage;

import org.jdownloader.gui.translate._GUI;

public class SplitHosterAction extends ContextMenuAction {

    private static final long                       serialVersionUID = 2666013418372344530L;

    private final ArrayList<LinkGrabberFilePackage> packages;

    public SplitHosterAction(ArrayList<LinkGrabberFilePackage> packages) {
        this.packages = packages;

        init();
    }

    @Override
    protected String getIcon() {
        return "gui.images.newpackage";
    }

    @Override
    protected String getName() {
        return _GUI._.gui_linkgrabberv2_splithoster() + " (" + packages.size() + ")";
    }

    public void actionPerformed(ActionEvent e) {
        for (LinkGrabberFilePackage packagee : packages) {
            synchronized (packagee) {
                ArrayList<DownloadLink> links = new ArrayList<DownloadLink>(packagee.getDownloadLinks());
                Set<String> hosts = DownloadLink.getHosterList(links);
                for (String host : hosts) {
                    LinkGrabberFilePackage nfp = new LinkGrabberFilePackage(packagee.getName());
                    nfp.setDownloadDirectory(packagee.getDownloadDirectory());
                    nfp.setPassword(packagee.getPassword());
                    nfp.setPostProcessing(packagee.isPostProcessing());
                    nfp.setUseSubDir(packagee.useSubDir());
                    nfp.setComment(packagee.getComment());
                    for (DownloadLink dl : links) {
                        if (dl.getHost().equalsIgnoreCase(host)) {
                            nfp.add(dl);
                        }
                    }
                    LinkGrabberController.getInstance().addPackage(nfp);
                }
            }
        }
    }

}