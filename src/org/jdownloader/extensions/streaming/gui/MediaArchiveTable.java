package org.jdownloader.extensions.streaming.gui;

import java.awt.Color;

import javax.swing.DropMode;
import javax.swing.JTable;

import jd.controlling.packagecontroller.AbstractNode;

import org.appwork.swing.exttable.DropHighlighter;
import org.appwork.swing.exttable.ExtColumn;
import org.jdownloader.extensions.streaming.mediaarchive.MediaFolder;
import org.jdownloader.extensions.streaming.mediaarchive.MediaItem;
import org.jdownloader.gui.views.components.packagetable.PackageControllerTable;

public class MediaArchiveTable extends PackageControllerTable<MediaFolder, MediaItem> {

    public MediaArchiveTable(MediaArchiveTableModel model) {
        super(model);
        this.addRowHighlighter(new DropHighlighter(null, new Color(27, 164, 191, 75)));
        this.setDragEnabled(true);
        this.setDropMode(DropMode.ON_OR_INSERT_ROWS);
        setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);
    }

    @Override
    public ExtColumn<AbstractNode> getExpandCollapseColumn() {
        return null;
    }

}
