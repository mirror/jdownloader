package jd.gui.swing.jdgui.views.downloads.contextmenu;

import java.awt.event.ActionEvent;
import java.util.ArrayList;

import jd.gui.UserIO;
import jd.gui.swing.jdgui.interfaces.ContextMenuAction;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.utils.JDUtilities;

import org.appwork.storage.config.JsonConfig;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.settings.GeneralSettings;

public class NewPackageAction extends ContextMenuAction {

    private static final long             serialVersionUID = -8544759375428602013L;

    private final ArrayList<DownloadLink> links;

    public NewPackageAction(ArrayList<DownloadLink> links) {
        this.links = links;

        init();
    }

    @Override
    protected String getIcon() {
        return "package_new";
    }

    @Override
    protected String getName() {
        return _GUI._.gui_table_contextmenu_newpackage() + " (" + links.size() + ")";
    }

    public void actionPerformed(ActionEvent e) {
        FilePackage fp = links.get(0).getFilePackage();
        String string = UserIO.getInstance().requestInputDialog(0, _GUI._.gui_linklist_newpackage_message(), fp.getName());
        if (string == null) return;

        FilePackage nfp = FilePackage.getInstance();
        nfp.setName(string);
        nfp.setDownloadDirectory(fp.getDownloadDirectory());
        nfp.setPostProcessing(fp.isPostProcessing());
        nfp.setComment(fp.getComment());

        for (DownloadLink link : links) {
            link.addSourcePluginPassword(link.getFilePackage().getPassword());
            link.setFilePackage(nfp);
        }

        if (JsonConfig.create(GeneralSettings.class).isAddNewLinksOnTop()) {
            JDUtilities.getDownloadController().addPackageAt(nfp, 0, 0);
        } else {
            JDUtilities.getDownloadController().addPackage(nfp);
        }
    }

}