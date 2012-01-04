package org.jdownloader.extensions.extraction.gui;

import java.awt.Color;

import jd.gui.swing.laf.LookAndFeelController;

import org.appwork.swing.exttable.ExtTableModel;
import org.appwork.swing.exttable.columns.ExtCircleProgressColumn;
import org.appwork.swing.exttable.columns.ExtFileSizeColumn;
import org.appwork.swing.exttable.columns.ExtTextColumn;
import org.jdownloader.extensions.extraction.ExtractionController;

public class ExtractionJobTableModel extends ExtTableModel<ExtractionController> {

    private Color textColor;

    public ExtractionJobTableModel() {
        super("ExtractionJobTableModel");
        textColor = new Color(LookAndFeelController.getInstance().getLAFOptions().getTooltipForegroundColor());
    }

    @Override
    protected void initColumns() {

        addColumn(new ExtTextColumn<ExtractionController>("Name") {
            {

            }

            @Override
            protected String getTooltipText(ExtractionController value) {
                return value.getArchiv().getName();
            }

            public void resetRenderer() {
                super.resetRenderer();
                this.rendererField.setForeground(textColor);

            }

            @Override
            public int getDefaultWidth() {
                return 80;
            }

            @Override
            public String getStringValue(ExtractionController value) {
                return value.getArchiv().getName();
            }
        });

        addColumn(new ExtFileSizeColumn<ExtractionController>("Size") {

            public void resetRenderer() {
                super.resetRenderer();
                this.renderer.setForeground(textColor);
            }

            @Override
            public int getDefaultWidth() {
                return 60;
            }

            @Override
            protected long getBytes(ExtractionController o2) {
                return o2.getArchiv().getSize();
            }

        });

        addColumn(new ExtCircleProgressColumn<ExtractionController>("Progress") {
            {
                determinatedRenderer.setForeground(textColor);

            }

            @Override
            protected String getString(ExtractionController value) {
                return "";
            }

            @Override
            public void resetRenderer() {
                super.resetRenderer();
                determinatedRenderer.setForeground(textColor);

            }

            @Override
            public int getDefaultWidth() {
                return 50;
            }

            @Override
            protected long getMax(ExtractionController value) {
                return value.getArchiv().getSize();
            }

            @Override
            protected long getValue(ExtractionController value) {
                return value.getArchiv().getExtracted();
            }
        });
    }

}
