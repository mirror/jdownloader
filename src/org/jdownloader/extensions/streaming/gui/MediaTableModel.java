package org.jdownloader.extensions.streaming.gui;

import org.appwork.swing.exttable.ExtTableModel;
import org.jdownloader.extensions.streaming.mediaarchive.MediaItem;
import org.jdownloader.extensions.streaming.mediaarchive.MediaListController;
import org.jdownloader.extensions.streaming.mediaarchive.MediaListListener;

public abstract class MediaTableModel<T extends MediaItem, E extends MediaListController<T>> extends ExtTableModel<T> implements MediaListListener {

    /**
     * 
     */
    private static final long serialVersionUID = 6166865781525160042L;
    private E                 listController;

    @Override
    public void onContentChanged(MediaListController<?> caller) {
        _fireTableStructureChanged(listController.getList(), true);
    }

    @Override
    protected void initColumns() {
    }

    public MediaTableModel(String id, E listController) {
        super(id);
        this.listController = listController;
        listController.getEventSender().addListener(this, true);
    }

}
