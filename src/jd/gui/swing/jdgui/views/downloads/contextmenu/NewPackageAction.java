package jd.gui.swing.jdgui.views.downloads.contextmenu;

import java.awt.event.ActionEvent;
import java.util.ArrayList;

import jd.gui.UserIO;
import jd.gui.swing.jdgui.GUIUtils;
import jd.gui.swing.jdgui.JDGuiConstants;
import jd.gui.swing.jdgui.interfaces.ContextMenuAction;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.utils.JDUtilities;
import jd.utils.locale.JDL;

public class NewPackageAction extends ContextMenuAction {

    private static final long serialVersionUID = -8544759375428602013L;

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
        return JDL.L("gui.table.contextmenu.newpackage", "Move into new Package") + " (" + links.size() + ")";
    }

    public void actionPerformed(ActionEvent e) {
        FilePackage fp = links.get(0).getFilePackage();
        String string = UserIO.getInstance().requestInputDialog(0, JDL.L("gui.linklist.newpackage.message", "Name of the new package"), fp.getName());
        if (string == null) return;

        FilePackage nfp = FilePackage.getInstance();
        nfp.setName(string);
        nfp.setDownloadDirectory(fp.getDownloadDirectory());
        nfp.setExtractAfterDownload(fp.isExtractAfterDownload());
        nfp.setComment(fp.getComment());

        for (DownloadLink link : links) {
            link.addSourcePluginPassword(link.getFilePackage().getPassword());
            link.setFilePackage(nfp);
        }

        if (GUIUtils.getConfig().getBooleanProperty(JDGuiConstants.PARAM_INSERT_NEW_LINKS_AT, false)) {
            JDUtilities.getDownloadController().addPackageAt(nfp, 0, 0);
        } else {
            JDUtilities.getDownloadController().addPackage(nfp);
        }
    }

}
