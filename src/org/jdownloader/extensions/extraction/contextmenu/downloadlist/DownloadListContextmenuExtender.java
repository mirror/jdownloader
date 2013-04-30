package org.jdownloader.extensions.extraction.contextmenu.downloadlist;

import org.jdownloader.controlling.contextmenu.ContextMenuManager;
import org.jdownloader.extensions.extraction.ExtractionExtension;
import org.jdownloader.extensions.extraction.contextmenu.downloadlist.action.AutoExtractEnabledToggleAction;
import org.jdownloader.extensions.extraction.contextmenu.downloadlist.action.CleanupAutoDeleteFilesEnabledToggleAction;
import org.jdownloader.extensions.extraction.contextmenu.downloadlist.action.CleanupAutoDeleteLinksEnabledToggleAction;
import org.jdownloader.extensions.extraction.contextmenu.downloadlist.action.ExtractArchiveNowAction;
import org.jdownloader.extensions.extraction.contextmenu.downloadlist.action.SetExtractPasswordAction;
import org.jdownloader.extensions.extraction.contextmenu.downloadlist.action.SetExtractToAction;
import org.jdownloader.extensions.extraction.contextmenu.downloadlist.action.ValidateArchivesAction;
import org.jdownloader.gui.views.downloads.contextmenumanager.ActionData;
import org.jdownloader.gui.views.downloads.contextmenumanager.AddonSubMenuLink;
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
        root.add(new MenuItemData(new ActionData(ExtractArchiveNowAction.class)));
        root.add(new MenuItemData(new ActionData(ValidateArchivesAction.class)));

        root.add(new SeperatorData());
        root.add(new MenuItemData(new ActionData(AutoExtractEnabledToggleAction.class)));
        root.add(new MenuItemData(new ActionData(SetExtractToAction.class)));
        root.add(new MenuItemData(new ActionData(SetExtractPasswordAction.class)));
        CleanupSubMenu cleanup = new CleanupSubMenu();
        root.add(cleanup);
        cleanup.add(new MenuItemData(new ActionData(CleanupAutoDeleteFilesEnabledToggleAction.class)));
        cleanup.add(new MenuItemData(new ActionData(CleanupAutoDeleteLinksEnabledToggleAction.class)));

    }

}
