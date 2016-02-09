package org.jdownloader.gui.views.downloads.action;

import java.awt.event.ActionEvent;

import org.jdownloader.controlling.contextmenu.CustomizableTableContextAppAction;
import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.SelectionInfo;
import org.jdownloader.gui.views.SelectionInfo.PackageView;

import jd.gui.UserIO;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;

public class PackageNameAction extends CustomizableTableContextAppAction<FilePackage, DownloadLink> {

    private static final long   serialVersionUID = -5155537516674035401L;
    private final static String NAME             = _GUI.T.gui_table_contextmenu_editpackagename();

    public PackageNameAction() {
        setName(NAME);
        setIconKey(IconKey.ICON_EDIT);
    }

    public void actionPerformed(ActionEvent e) {
        final SelectionInfo<FilePackage, DownloadLink> selection = getSelection();
        final String name = UserIO.getInstance().requestInputDialog(0, _GUI.T.gui_linklist_editpackagename_message(), selection.getFirstPackage().getName());
        if (name != null) {
            for (final PackageView<FilePackage, DownloadLink> packagee : selection.getPackageViews()) {
                packagee.getPackage().setName(name);
            }
        }

    }

}