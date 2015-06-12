package org.jdownloader.gui.views.downloads.context.submenu;

import java.awt.Image;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JMenu;

import jd.controlling.linkcrawler.CrawledLink;
import jd.controlling.linkcrawler.CrawledPackage;
import jd.gui.swing.jdgui.MainTabbedPane;
import jd.gui.swing.jdgui.interfaces.View;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;

import org.appwork.utils.ImageProvider.ImageProvider;
import org.appwork.utils.images.IconIO;
import org.jdownloader.controlling.contextmenu.MenuContainer;
import org.jdownloader.controlling.contextmenu.gui.ExtMenuImpl;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.SelectionInfo;
import org.jdownloader.gui.views.downloads.DownloadsView;
import org.jdownloader.gui.views.downloads.table.DownloadsTable;
import org.jdownloader.gui.views.linkgrabber.LinkGrabberTable;
import org.jdownloader.gui.views.linkgrabber.LinkGrabberView;
import org.jdownloader.images.NewTheme;

public class SettingsMenuContainer extends MenuContainer {
    private final static String NAME     = _GUI._.ContextMenuFactory_createPopup_properties_package();

    private final static Image  SETTINGS = NewTheme.I().getImage("settings", 14);
    private final static Icon   OPEN_PACKAGE;
    private final static Icon   CLOSED_PACKAGE;
    static {
        final Image openPackage = NewTheme.I().getImage("tree_package_open", 32);
        final Image closedPackage = NewTheme.I().getImage("tree_package_closed", 32);
        OPEN_PACKAGE = new ImageIcon(ImageProvider.merge(openPackage, SETTINGS, -16, 0, 6, 6));
        CLOSED_PACKAGE = new ImageIcon(ImageProvider.merge(closedPackage, SETTINGS, -16, 0, 6, 6));
    }

    public SettingsMenuContainer() {
        setName(NAME);
        setIconKey("settings");
    }

    @Override
    public JMenu createItem() {
        final ExtMenuImpl subMenu = new ExtMenuImpl(getName());
        final View view = MainTabbedPane.getInstance().getSelectedView();
        if (view instanceof DownloadsView) {
            final SelectionInfo<FilePackage, DownloadLink> selection = DownloadsTable.getInstance().getSelectionInfo();
            if (selection.isPackageContext()) {
                if (selection.getFirstPackage().isExpanded()) {
                    subMenu.setIcon(OPEN_PACKAGE);
                } else {
                    subMenu.setIcon(CLOSED_PACKAGE);
                }
            } else if (selection.isLinkContext()) {
                final Image back = IconIO.toBufferedImage(selection.getLink().getLinkInfo().getIcon());
                subMenu.setIcon(new ImageIcon(ImageProvider.merge(back, SETTINGS, 0, 0, 6, 6)));
            }
        } else if (view instanceof LinkGrabberView) {
            final SelectionInfo<CrawledPackage, CrawledLink> selection = LinkGrabberTable.getInstance().getSelectionInfo();
            if (selection.isPackageContext()) {
                if (selection.getFirstPackage().isExpanded()) {
                    subMenu.setIcon(OPEN_PACKAGE);
                } else {
                    subMenu.setIcon(CLOSED_PACKAGE);
                }
            } else if (selection.isLinkContext()) {
                final Image back = IconIO.toBufferedImage(selection.getLink().getDownloadLink().getLinkInfo().getIcon());
                subMenu.setIcon(new ImageIcon(ImageProvider.merge(back, SETTINGS, 0, 0, 6, 6)));
            }
        }
        return subMenu;
    }
}
