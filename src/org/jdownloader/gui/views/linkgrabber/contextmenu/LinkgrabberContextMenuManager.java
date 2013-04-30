package org.jdownloader.gui.views.linkgrabber.contextmenu;

import jd.controlling.linkcrawler.CrawledLink;
import jd.controlling.linkcrawler.CrawledPackage;

import org.jdownloader.controlling.contextmenu.ActionData;
import org.jdownloader.controlling.contextmenu.BooleanLinkedMenuItemData;
import org.jdownloader.controlling.contextmenu.ContextMenuManager;
import org.jdownloader.controlling.contextmenu.MenuContainerRoot;
import org.jdownloader.controlling.contextmenu.MenuItemData;
import org.jdownloader.controlling.contextmenu.MenuItemProperty;
import org.jdownloader.controlling.contextmenu.SeperatorData;
import org.jdownloader.gui.views.downloads.action.MenuManagerAction;
import org.jdownloader.gui.views.linkgrabber.actions.AddContainerAction;
import org.jdownloader.gui.views.linkgrabber.actions.AddLinksAction;
import org.jdownloader.gui.views.linkgrabber.actions.ConfirmAction;
import org.jdownloader.settings.staticreferences.CFG_LINKGRABBER;

public class LinkgrabberContextMenuManager extends ContextMenuManager<CrawledPackage, CrawledLink> {

    private static final LinkgrabberContextMenuManager INSTANCE = new LinkgrabberContextMenuManager();

    /**
     * get the only existing instance of DownloadListContextMenuManager. This is a singleton
     * 
     * @return
     */
    public static LinkgrabberContextMenuManager getInstance() {
        return LinkgrabberContextMenuManager.INSTANCE;
    }

    /**
     * Create a new instance of DownloadListContextMenuManager. This is a singleton class. Access the only existing instance by using
     * {@link #getInstance()}.
     */

    private LinkgrabberContextMenuManager() {
        super();

    }

    private static final int VERSION = 0;

    public MenuContainerRoot createDefaultStructure() {
        MenuContainerRoot mr = new MenuContainerRoot();
        mr.setSource(VERSION);
        mr.add(new MenuItemData(new ActionData(ConfirmAction.class), MenuItemProperty.HIDE_IF_DISABLED));
        mr.add(new SeperatorData());
        mr.add(new BooleanLinkedMenuItemData(CFG_LINKGRABBER.CONTEXT_MENU_ADD_LINKS_ACTION_ALWAYS_VISIBLE, true, AddLinksAction.class));
        mr.add(new BooleanLinkedMenuItemData(CFG_LINKGRABBER.CONTEXT_MENU_ADD_LINKS_ACTION_ALWAYS_VISIBLE, true, AddContainerAction.class));
        mr.add(new SeperatorData());

        return mr;
    }

    public void show() {

        new MenuManagerAction(null).actionPerformed(null);
    }

}
