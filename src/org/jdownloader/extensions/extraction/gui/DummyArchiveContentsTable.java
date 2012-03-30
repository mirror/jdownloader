package org.jdownloader.extensions.extraction.gui;

import java.awt.Color;

import jd.gui.swing.jdgui.BasicJDTable;

import org.appwork.swing.exttable.ExtOverlayRowHighlighter;
import org.appwork.swing.exttable.ExtTable;
import org.appwork.utils.ColorUtils;
import org.jdownloader.extensions.extraction.DummyArchive;
import org.jdownloader.extensions.extraction.DummyArchiveFile;

public class DummyArchiveContentsTable extends BasicJDTable<DummyArchiveFile> {

    public DummyArchiveContentsTable(DummyArchive da) {
        super(new DummyArchiveContentsTableModel(da));

        addRowHighlighter(new ExtOverlayRowHighlighter(null, ColorUtils.getAlphaInstance(Color.RED, 20)) {

            @Override
            public boolean doHighlight(ExtTable<?> extTable, int row) {
                DummyArchiveFile e = getExtTableModel().getObjectbyRow(row);
                return !e.isExists();
            }
        });
    }
}
