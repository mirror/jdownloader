package org.jdownloader.gui.views.downloads.action;

import java.awt.event.ActionEvent;

import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;

import org.appwork.exceptions.WTFException;
import org.jdownloader.actions.CachableInterface;
import org.jdownloader.actions.AbstractSelectionContextAction;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.SelectionInfo;

public class NewPackageAction extends AbstractSelectionContextAction<FilePackage, DownloadLink> implements CachableInterface {

    private static final long serialVersionUID = -8544759375428602013L;

    public NewPackageAction(SelectionInfo<FilePackage, DownloadLink> si) {
        super(si);
        setIconKey("package_new");
        setName(_GUI._.gui_table_contextmenu_newpackage());
    }

    public void actionPerformed(ActionEvent e) {
        if (!isEnabled()) return;
        throw new WTFException("fixme 22023");

    }

    @Override
    public void setData(String data) {
    }

}