package org.jdownloader.extensions.extraction.gui;

import java.awt.Color;

import jd.gui.swing.jdgui.BasicJDTable;

import org.appwork.swing.exttable.ExtOverlayRowHighlighter;
import org.appwork.swing.exttable.ExtTable;
import org.appwork.utils.ColorUtils;
import org.jdownloader.extensions.extraction.DummyArchive;
import org.jdownloader.extensions.extraction.DummyArchiveFile;
import org.jdownloader.extensions.extraction.bindings.crawledlink.CrawledLinkArchiveFile;

public class DummyArchiveContentsTable extends BasicJDTable<DummyArchiveFile> {

    public DummyArchiveContentsTable(DummyArchive da) {
        super(new DummyArchiveContentsTableModel(da));

        boolean linkgrabber = false;
        for (DummyArchiveFile daf : da.getList()) {
            if (daf.getArchiveFile() != null && daf.getArchiveFile() instanceof CrawledLinkArchiveFile) {
                linkgrabber = true;
                break;

            }
        }

        // getModel().setColumnVisible(((DummyArchiveContentsTableModel)
        // getModel()).getLocal(), !linkgrabber);
        getModel().setColumnVisible(((DummyArchiveContentsTableModel) getModel()).getLinkStatus(), linkgrabber);
        addRowHighlighter(new ExtOverlayRowHighlighter(null, ColorUtils.getAlphaInstance(Color.RED, 20)) {

            @Override
            public boolean doHighlight(ExtTable<?> extTable, int row) {
                DummyArchiveFile e = getModel().getObjectbyRow(row);
                return e.isMissing();
            }
        });
        addRowHighlighter(new ExtOverlayRowHighlighter(null, ColorUtils.getAlphaInstance(Color.ORANGE, 20)) {

            @Override
            public boolean doHighlight(ExtTable<?> extTable, int row) {
                DummyArchiveFile e = getModel().getObjectbyRow(row);
                return e.isIncomplete() && !e.isLocalFileAvailable();
            }
        });

        addRowHighlighter(new ExtOverlayRowHighlighter(null, ColorUtils.getAlphaInstance(Color.GREEN, 20)) {

            @Override
            public boolean doHighlight(ExtTable<?> extTable, int row) {
                DummyArchiveFile e = getModel().getObjectbyRow(row);
                return e.isLocalFileAvailable();
            }
        });
    }
}
