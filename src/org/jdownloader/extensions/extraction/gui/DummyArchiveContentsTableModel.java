package org.jdownloader.extensions.extraction.gui;

import org.appwork.swing.exttable.ExtTableModel;
import org.appwork.swing.exttable.columns.ExtCheckColumn;
import org.appwork.swing.exttable.columns.ExtTextColumn;
import org.jdownloader.extensions.antireconnect.translate.T;
import org.jdownloader.extensions.extraction.DummyArchive;
import org.jdownloader.extensions.extraction.DummyArchiveFile;

public class DummyArchiveContentsTableModel extends ExtTableModel<DummyArchiveFile> {

    public DummyArchiveContentsTableModel(DummyArchive da) {
        super("DummyArchiveContentsTableModel");

        getTableData().addAll(da.getList());

    }

    @Override
    protected void initColumns() {
        addColumn(new ExtTextColumn<DummyArchiveFile>(T._.filename()) {

            @Override
            public String getStringValue(DummyArchiveFile value) {
                return value.getName();
            }
        });

        addColumn(new ExtCheckColumn<DummyArchiveFile>(T._.exists()) {

            @Override
            protected boolean getBooleanValue(DummyArchiveFile value) {
                return value.isExists();
            }

            @Override
            protected void setBooleanValue(boolean value, DummyArchiveFile object) {
            }
        });
    }

}
