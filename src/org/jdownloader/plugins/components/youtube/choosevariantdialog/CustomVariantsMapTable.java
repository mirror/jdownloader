package org.jdownloader.plugins.components.youtube.choosevariantdialog;

import java.awt.event.MouseEvent;
import java.util.List;

import javax.swing.JPopupMenu;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;

import org.appwork.swing.exttable.ExtColumn;
import org.jdownloader.plugins.components.youtube.configpanel.AbstractVariantWrapper;
import org.jdownloader.plugins.components.youtube.configpanel.VariantsMapTable;

public class CustomVariantsMapTable extends VariantsMapTable {

    public CustomVariantsMapTable(CustomVariantsMapTableModel model) {
        super(model);
        setSearchEnabled(true);
        setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        getModel().refreshSort();
    }

    @Override
    protected JPopupMenu onContextMenu(JPopupMenu popup, AbstractVariantWrapper contextObject, List<AbstractVariantWrapper> selection, ExtColumn<AbstractVariantWrapper> column, MouseEvent mouseEvent) {
        return null;
    }

    public void addFilter(Filter typeSel) {
        ((CustomVariantsMapTableModel) getModel()).addFilter(typeSel);
    }

}
