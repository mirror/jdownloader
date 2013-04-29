package org.jdownloader.extensions.extraction.contextmenu.downloadlist;

import org.jdownloader.extensions.extraction.ExtractionExtension;
import org.jdownloader.gui.views.downloads.contextmenumanager.ActionData;
import org.jdownloader.gui.views.downloads.contextmenumanager.AddonSubMenuLink;
import org.jdownloader.gui.views.downloads.contextmenumanager.ContextMenuManager;
import org.jdownloader.gui.views.downloads.contextmenumanager.MenuContainerRoot;
import org.jdownloader.gui.views.downloads.contextmenumanager.MenuExtenderHandler;
import org.jdownloader.gui.views.downloads.contextmenumanager.MenuItemData;
import org.jdownloader.gui.views.downloads.contextmenumanager.SeperatorData;

public class DownloadListContextmenuExtender implements MenuExtenderHandler {

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

        ArchivesSubMenu root;
        mr.getItems().add(addonLinkIndex, root = new ArchivesSubMenu());
        root.add(new MenuItemData(new ActionData(ExtractNowAction.class)));
        root.add(new MenuItemData(new ActionData(ValidateArchivesAction.class)));

        root.add(new SeperatorData());
        root.add(new MenuItemData(new ActionData(AutoExtractAction.class)));
        root.add(new MenuItemData(new ActionData(SetExtractToAction.class)));
        root.add(new MenuItemData(new ActionData(SetExtractPasswordAction.class)));
        ArchivesCleanupSubMenu cleanup = new ArchivesCleanupSubMenu();
        root.add(cleanup);
        cleanup.add(new MenuItemData(new ActionData(CleanupAutoDeleteFilesAction.class)));
        cleanup.add(new MenuItemData(new ActionData(CleanupAutoDeleteLinksAction.class)));

        root.add(new SeperatorData());
        root.add(new ExtractionExtensionMenuLink());
    }

}
