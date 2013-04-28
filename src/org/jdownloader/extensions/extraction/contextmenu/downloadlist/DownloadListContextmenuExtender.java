package org.jdownloader.extensions.extraction.contextmenu.downloadlist;

import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;

import org.jdownloader.extensions.extraction.ExtractionExtension;
import org.jdownloader.gui.views.downloads.contextmenumanager.AddonSubMenuLink;
import org.jdownloader.gui.views.downloads.contextmenumanager.ContextMenuManager;
import org.jdownloader.gui.views.downloads.contextmenumanager.MenuContainerRoot;
import org.jdownloader.gui.views.downloads.contextmenumanager.MenuExtenderHandler;

public class DownloadListContextmenuExtender implements MenuExtenderHandler<FilePackage, DownloadLink> {

    private ExtractionExtension extension;

    public ExtractionExtension getExtension() {
        return extension;
    }

    public void setExtension(ExtractionExtension extension) {
        this.extension = extension;
    }

    public DownloadListContextmenuExtender(ExtractionExtension extractionExtension) {
        this.extension = extractionExtension;
    }

    @Override
    public void updateMenuModel(ContextMenuManager manager, MenuContainerRoot mr) {
        int addonLinkIndex = 0;
        for (int i = 0; i < mr.getItems().size(); i++) {
            if (mr.getItems().get(i) instanceof AddonSubMenuLink) {
                addonLinkIndex = i;
                break;
            }
        }

        mr.getItems().add(addonLinkIndex, new ExtractionExtensionMenuLink());
        // ArchiveSubMenuContainer sub = new ArchiveSubMenuContainer();
        // sub.add(new MenuItemData(new ActionData(ArchiveSubmenuAction.class)));
        // mr.getItems().add(3, new MenuItemData(new ActionData(CopyOfArchiveSubmenuAction.class)));
        // mr.add(sub);
    }

    // @Override
    // public void extend(JComponent root, ExtensionContextMenuItem<?> inst, SelectionInfo<FilePackage, DownloadLink> selection,
    // MenuContainerRoot menuData) {
    // System.out.println(1);
    // try {
    // if (!inst.showItem(selection)) return;
    // if (inst instanceof ArchiveSubMenu) {
    //
    // extension.onExtendPopupMenu(new DownloadTableContext(root, (SelectionInfo<FilePackage, DownloadLink>) selection,
    // selection.getContextColumn()));
    // }
    //
    // } catch (Exception e) {
    // throw new RuntimeException(e);
    // }
    // }

}
