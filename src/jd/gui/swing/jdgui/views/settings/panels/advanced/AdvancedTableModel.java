package jd.gui.swing.jdgui.views.settings.panels.advanced;

import org.appwork.utils.swing.table.ExtTableModel;
import org.appwork.utils.swing.table.columns.ExtTextColumn;
import org.jdownloader.settings.advanced.AdvancedEntry;

public class AdvancedTableModel extends ExtTableModel<AdvancedEntry> {

    public AdvancedTableModel(String id) {
        super(id);
        // fill();
    }

    @Override
    protected void initColumns() {
        addColumn(new ExtTextColumn<AdvancedEntry>("Key") {

            @Override
            protected String getStringValue(AdvancedEntry value) {
                return null;
            }
        });
        addColumn(new AdvancedValueColumn());
    }

}
