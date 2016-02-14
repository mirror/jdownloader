package org.jdownloader.gui.views.downloads.context.submenu;

import org.jdownloader.controlling.contextmenu.MenuContainer;
import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.translate._GUI;

public class SettingsMenuContainer extends MenuContainer {
    private final static String NAME = _GUI.T.ContextMenuFactory_createPopup_properties_package();

    public SettingsMenuContainer() {
        setName(NAME);
        setIconKey(IconKey.ICON_SETTINGS);
    }
    //
    // @Override
    // public JMenu createItem() {
    // final ExtMenuImpl subMenu = new ExtMenuImpl(getName());
    // final View view = MainTabbedPane.getInstance().getSelectedView();
    // if (view instanceof DownloadsView) {
    // final SelectionInfo<FilePackage, DownloadLink> selection = DownloadsTable.getInstance().getSelectionInfo();
    // if (selection.isPackageContext()) {
    // if (selection.getFirstPackage().isExpanded()) {
    // subMenu.setIcon(OPEN_PACKAGE);
    // } else {
    // subMenu.setIcon(CLOSED_PACKAGE);
    // }
    // } else if (selection.isLinkContext()) {
    // final Image back = IconIO.toBufferedImage(selection.getLink().getLinkInfo().getIcon());
    // subMenu.setIcon(new ExtMergedIcon(selection.getLink().getLinkInfo().getIcon()).add(SETTINGS, 6, 6));
    //
    // }
    // } else if (view instanceof LinkGrabberView) {
    // final SelectionInfo<CrawledPackage, CrawledLink> selection = LinkGrabberTable.getInstance().getSelectionInfo();
    // if (selection.isPackageContext()) {
    // if (selection.getFirstPackage().isExpanded()) {
    // subMenu.setIcon(OPEN_PACKAGE);
    // } else {
    // subMenu.setIcon(CLOSED_PACKAGE);
    // }
    // } else if (selection.isLinkContext()) {
    //
    // subMenu.setIcon(new ExtMergedIcon(selection.getLink().getDownloadLink().getLinkInfo().getIcon()).add(SETTINGS, 6, 6));
    //
    // }
    // }
    // return subMenu;
    // }
}
