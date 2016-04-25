package org.jdownloader.plugins.components.youtube.choosevariantdialog;

import java.util.ArrayList;

import javax.swing.JTable;
import javax.swing.ListSelectionModel;

import org.jdownloader.plugins.components.youtube.configpanel.AbstractVariantWrapper;
import org.jdownloader.plugins.components.youtube.configpanel.VariantsMapTable;

public class CustomVariantsMapTable extends VariantsMapTable {

    public CustomVariantsMapTable(ArrayList<AbstractVariantWrapper> sorted) {
        super(new CustomVariantsMapTableModel(sorted));
        setSearchEnabled(true);
        setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        getModel().refreshSort();
    }

}
