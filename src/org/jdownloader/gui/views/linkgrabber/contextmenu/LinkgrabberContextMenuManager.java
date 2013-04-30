package org.jdownloader.gui.views.linkgrabber.contextmenu;

import jd.controlling.linkcrawler.CrawledLink;
import jd.controlling.linkcrawler.CrawledPackage;

import org.jdownloader.controlling.contextmenu.ContextMenuManager;
import org.jdownloader.gui.views.downloads.context.MenuManagerAction;
import org.jdownloader.gui.views.downloads.contextmenumanager.MenuContainerRoot;

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

        return mr;
    }

    public void show() {

        new MenuManagerAction(null).actionPerformed(null);
    }

}
