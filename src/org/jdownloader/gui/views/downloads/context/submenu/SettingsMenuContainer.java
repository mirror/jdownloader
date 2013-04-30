package org.jdownloader.gui.views.downloads.context.submenu;

import java.awt.Image;

import javax.swing.ImageIcon;
import javax.swing.JMenu;

import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;

import org.appwork.exceptions.WTFException;
import org.appwork.utils.ImageProvider.ImageProvider;
import org.jdownloader.controlling.contextmenu.MenuContainer;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.SelectionInfo;
import org.jdownloader.images.NewTheme;

public class SettingsMenuContainer extends MenuContainer {
    public SettingsMenuContainer() {
        setName(_GUI._.ContextMenuFactory_createPopup_properties_package());
        setIconKey("settings");

    }

    @Override
    public JMenu createItem(SelectionInfo<?, ?> selection) {

        JMenu subMenu = new JMenu(getName());

        if (selection.getContextPackage() instanceof FilePackage) {
            SelectionInfo<FilePackage, DownloadLink> si = (SelectionInfo<FilePackage, DownloadLink>) selection;
            if (si.isPackageContext()) {
                Image back = (si.getFirstPackage().isExpanded() ? NewTheme.I().getImage("tree_package_open", 32) : NewTheme.I().getImage("tree_package_closed", 32));
                subMenu.setIcon(new ImageIcon(ImageProvider.merge(back, NewTheme.I().getImage("settings", 14), -16, 0, 6, 6)));

            } else if (si.isLinkContext()) {
                Image back = (si.getLink().getIcon().getImage());
                subMenu.setIcon(new ImageIcon(ImageProvider.merge(back, NewTheme.I().getImage("settings", 14), 0, 0, 6, 6)));

            }
            return subMenu;
        } else {
            throw new WTFException("TODO");
        }
    }
}
