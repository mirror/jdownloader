package org.jdownloader.gui.views.downloads.context;

import java.awt.event.ActionEvent;
import java.util.ArrayList;

import jd.controlling.downloadcontroller.DownloadController;
import jd.gui.UserIO;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;

import org.appwork.exceptions.WTFException;
import org.appwork.storage.config.JsonConfig;
import org.jdownloader.actions.AppAction;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.settings.GeneralSettings;

public class NewPackageAction extends AppAction {

    private static final long             serialVersionUID = -8544759375428602013L;

    private final ArrayList<DownloadLink> links;

    public NewPackageAction(ArrayList<DownloadLink> links) {
        this.links = links;
        setIconKey("package_new");
        setName(_GUI._.gui_table_contextmenu_newpackage());
    }

    public void actionPerformed(ActionEvent e) {
        if (true) {
            /* not finished yet */
            return;
        }
        FilePackage fp = links.get(0).getFilePackage();
        String string = UserIO.getInstance().requestInputDialog(0, _GUI._.gui_linklist_newpackage_message(), fp.getName());
        if (string == null) return;

        FilePackage nfp = FilePackage.getInstance();
        nfp.setName(string);
        nfp.setDownloadDirectory(fp.getDownloadDirectory());
        nfp.setPostProcessing(fp.isPostProcessing());
        nfp.setComment(fp.getComment());

        for (DownloadLink link : links) {
            /* TODO: speed optimize */
            link.getFilePackage().remove(link);
        }
        if (true) throw new WTFException("FINISH ME");
        // TODO:Daniel
        if (JsonConfig.create(GeneralSettings.class).isAddNewLinksOnTop()) {
            DownloadController.getInstance().addmovePackageAt(fp, 0);
        } else {
            DownloadController.getInstance().addmovePackageAt(fp, -1);
        }
    }

}