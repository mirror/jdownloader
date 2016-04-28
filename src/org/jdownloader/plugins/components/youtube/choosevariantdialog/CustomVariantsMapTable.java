package org.jdownloader.plugins.components.youtube.choosevariantdialog;

import javax.swing.JTable;
import javax.swing.ListSelectionModel;

import org.jdownloader.plugins.components.youtube.configpanel.VariantsMapTable;

public class CustomVariantsMapTable extends VariantsMapTable {

    public CustomVariantsMapTable(CustomVariantsMapTableModel model) {
        super(model);
        setSearchEnabled(true);
        setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        getModel().refreshSort();
    }

    public void addFilter(Filter typeSel) {
        ((CustomVariantsMapTableModel) getModel()).addFilter(typeSel);
    }

}
