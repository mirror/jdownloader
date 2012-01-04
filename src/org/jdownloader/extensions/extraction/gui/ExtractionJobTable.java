package org.jdownloader.extensions.extraction.gui;

import jd.gui.swing.jdgui.BasicJDTable;

import org.jdownloader.extensions.extraction.ExtractionController;

public class ExtractionJobTable extends BasicJDTable<ExtractionController> {

    public ExtractionJobTable(ExtractionJobTableModel extractionJobTableModel) {
        super(extractionJobTableModel);
        this.setBackground(null);
        setOpaque(false);

    }

}
