package org.jdownloader.gui.views.downloads.table;

import java.awt.event.MouseEvent;

import javax.swing.JPopupMenu;

import org.appwork.swing.exttable.ExtColumn;
import org.jdownloader.gui.views.downloads.contextmenumanager.MenuManagerDownloadTableContext;

import jd.controlling.packagecontroller.AbstractNode;

public class DownloadTableContextMenuFactory {
    private static final DownloadTableContextMenuFactory INSTANCE = new DownloadTableContextMenuFactory();

    /**
     * get the only existing instance of DownloadTableContextMenuFactory. This is a singleton
     *
     * @return
     */
    public static DownloadTableContextMenuFactory getInstance() {
        return DownloadTableContextMenuFactory.INSTANCE;
    }

    private MenuManagerDownloadTableContext manager;

    /**
     * Create a new instance of DownloadTableContextMenuFactory. This is a singleton class. Access the only existing instance by using
     * {@link #getInstance()}.
     */
    private DownloadTableContextMenuFactory() {
        manager = MenuManagerDownloadTableContext.getInstance();

    }

    public JPopupMenu create(DownloadsTable downloadsTable, JPopupMenu popup, AbstractNode contextObject, java.util.List<AbstractNode> selection, ExtColumn<AbstractNode> column, MouseEvent ev) {

        /* Properties */

        return manager.build();

    }

}
