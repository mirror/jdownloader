package org.jdownloader.extensions.translator.gui;

import org.appwork.utils.swing.table.ExtTableModel;
import org.appwork.utils.swing.table.columns.ExtTextColumn;

public class TranslateTableModel extends ExtTableModel<TranslatorEntry> {

    public TranslateTableModel() {
        super("TranslateTableModel");
    }

    @Override
    protected void initColumns() {
        addColumn(new ExtTextColumn<TranslatorEntry>("Key") {

            @Override
            public String getStringValue(TranslatorEntry value) {
                return value + "";
            }
        });

    }

}
