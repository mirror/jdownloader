package org.jdownloader.gui.views.downloads.table;

import java.awt.Color;

import javax.swing.DropMode;
import javax.swing.ListSelectionModel;

import jd.plugins.PackageLinkNode;

import org.appwork.utils.swing.table.AlternateHighlighter;
import org.appwork.utils.swing.table.ExtTable;
import org.appwork.utils.swing.table.ExtTableModel;
import org.appwork.utils.swing.table.SelectionHighlighter;

public class DownloadsTable extends ExtTable<PackageLinkNode> {

    private static final long serialVersionUID = 8843600834248098174L;

    public DownloadsTable(final ExtTableModel<PackageLinkNode> tableModel) {
        super(tableModel);
        this.setShowVerticalLines(false);
        this.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        this.setDragEnabled(true);
        this.setDropMode(DropMode.INSERT_ROWS);
        this.addRowHighlighter(new SelectionHighlighter(null, new Color(0x30, 0x30, 0x30, 50)));
        this.addRowHighlighter(new AlternateHighlighter(null, new Color(0x30, 0x30, 0x30, 5)));
    }

}
