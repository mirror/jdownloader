package org.jdownloader.extensions.extraction.gui;

import jd.gui.swing.jdgui.BasicJDTable;

import org.jdownloader.extensions.extraction.ExtractionController;

public class ExtractionJobTable extends BasicJDTable<ExtractionController> {

    /**
	 * 
	 */
    private static final long serialVersionUID = -2198672161538215628L;

    public ExtractionJobTable(ExtractionJobTableModel extractionJobTableModel) {
        super(extractionJobTableModel);
        this.setBackground(null);
        setOpaque(false);
        this.setShowVerticalLines(false);
        this.setShowGrid(false);
        this.setShowHorizontalLines(false);
    }

}
