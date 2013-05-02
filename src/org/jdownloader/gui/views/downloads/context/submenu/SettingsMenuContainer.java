package org.jdownloader.gui.views.downloads.context.submenu;

import java.awt.Image;

import javax.swing.ImageIcon;
import javax.swing.JMenu;

import jd.controlling.linkcrawler.CrawledLink;
import jd.plugins.DownloadLink;

import org.appwork.utils.ImageProvider.ImageProvider;
import org.jdownloader.controlling.contextmenu.MenuContainer;
import org.jdownloader.controlling.contextmenu.gui.ExtMenuImpl;
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

        ExtMenuImpl subMenu = new ExtMenuImpl(getName());

        if (selection.isPackageContext()) {
            Image back = (selection.getFirstPackage().isExpanded() ? NewTheme.I().getImage("tree_package_open", 32) : NewTheme.I().getImage("tree_package_closed", 32));
            subMenu.setIcon(new ImageIcon(ImageProvider.merge(back, NewTheme.I().getImage("settings", 14), -16, 0, 6, 6)));

        } else if (selection.isLinkContext()) {
            if (selection.getLink() instanceof DownloadLink) {
                Image back = (((DownloadLink) selection.getLink()).getIcon().getImage());
                subMenu.setIcon(new ImageIcon(ImageProvider.merge(back, NewTheme.I().getImage("settings", 14), 0, 0, 6, 6)));
            } else {
                Image back = (((CrawledLink) selection.getLink()).getDownloadLink().getIcon().getImage());
                subMenu.setIcon(new ImageIcon(ImageProvider.merge(back, NewTheme.I().getImage("settings", 14), 0, 0, 6, 6)));
            }

        }
        return subMenu;

    }
}
