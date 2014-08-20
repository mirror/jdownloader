package org.jdownloader.gui.views.components.packagetable.context.rename;

import java.util.ArrayList;

import org.appwork.swing.exttable.ExtTableModel;
import org.appwork.swing.exttable.columns.ExtTextColumn;
import org.jdownloader.gui.translate._GUI;

public class ResultTableModel extends ExtTableModel<Result> {

    public ResultTableModel(ArrayList<Result> list) {
        super("org.jdownloader.gui.views.components.packagetable.context.rename.ResultTableModel");
        addAllElements(list);
    }

    @Override
    protected void initColumns() {
        addColumn(new ExtTextColumn<Result>(_GUI._.ResultTableModel_initColumns_oldname()) {

            @Override
            public String getStringValue(Result value) {
                return value.getOldName();
            }
        });

        addColumn(new ExtTextColumn<Result>(_GUI._.ResultTableModel_initColumns_newname()) {

            @Override
            public String getStringValue(Result value) {
                return value.getNewName();
            }
        });
    }
}
