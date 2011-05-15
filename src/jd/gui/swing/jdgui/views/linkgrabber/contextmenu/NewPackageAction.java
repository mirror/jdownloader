package jd.gui.swing.jdgui.views.linkgrabber.contextmenu;

import java.awt.event.ActionEvent;
import java.util.ArrayList;

import jd.controlling.LinkGrabberController;
import jd.gui.UserIO;
import jd.gui.swing.jdgui.interfaces.ContextMenuAction;
import jd.gui.swing.jdgui.views.linkgrabber.LinkgrabberSettings;
import jd.plugins.DownloadLink;
import jd.plugins.LinkGrabberFilePackage;

import org.appwork.storage.config.JsonConfig;
import org.jdownloader.gui.translate._GUI;

public class NewPackageAction extends ContextMenuAction {

    private static final long             serialVersionUID = -8544759375428602013L;

    private final ArrayList<DownloadLink> links;

    public NewPackageAction(ArrayList<DownloadLink> links) {
        this.links = links;

        init();
    }

    @Override
    protected String getIcon() {
        return "gui.images.newpackage";
    }

    @Override
    protected String getName() {
        return _GUI._.gui_table_contextmenu_newpackage() + " (" + links.size() + ")";
    }

    public void actionPerformed(ActionEvent e) {
        LinkGrabberController controller = LinkGrabberController.getInstance();

        LinkGrabberFilePackage fp = controller.getFPwithLink(links.get(0));

        String newName = UserIO.getInstance().requestInputDialog(0, _GUI._.gui_linklist_newpackage_message(), fp.getName());
        if (newName == null) return;

        LinkGrabberFilePackage nfp = new LinkGrabberFilePackage(newName, controller);
        nfp.setDownloadDirectory(fp.getDownloadDirectory());
        nfp.setPostProcessing(fp.isPostProcessing());
        nfp.setUseSubDir(fp.useSubDir());
        nfp.setComment(fp.getComment());
        for (DownloadLink dlink : links) {
            fp = controller.getFPwithLink(dlink);
            if (fp != null) dlink.addSourcePluginPassword(fp.getPassword());
        }
        nfp.addAll(links);

        if (JsonConfig.create(LinkgrabberSettings.class).isAddNewLinksOnTop()) {
            controller.addPackageAt(nfp, 0, 0);
        } else {
            controller.addPackage(nfp);
        }
    }

}