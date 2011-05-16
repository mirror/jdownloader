package jd.gui.swing.jdgui.views.settings.panels.advanced;

import java.util.ArrayList;

import org.appwork.utils.swing.EDTRunner;
import org.appwork.utils.swing.table.ExtTableModel;
import org.appwork.utils.swing.table.columns.ExtTextColumn;
import org.jdownloader.settings.advanced.AdvancedConfigEntry;
import org.jdownloader.settings.advanced.AdvancedConfigEventListener;
import org.jdownloader.settings.advanced.AdvancedConfigManager;

public class AdvancedTableModel extends ExtTableModel<AdvancedConfigEntry> implements AdvancedConfigEventListener {

    public AdvancedTableModel(String id) {
        super(id);
        fill();
        AdvancedConfigManager.getInstance().getEventSender().addListener(this);
    }

    private void fill() {
        new EDTRunner() {

            @Override
            protected void runInEDT() {
                ArrayList<AdvancedConfigEntry> tmp = (ArrayList<AdvancedConfigEntry>) AdvancedConfigManager.getInstance().list().clone();
                tableData = tmp;
                fireTableStructureChanged();
            }
        };

    }

    @Override
    protected void initColumns() {
        addColumn(new ExtTextColumn<AdvancedConfigEntry>("Key") {

            @Override
            protected String getStringValue(AdvancedConfigEntry value) {
                return value.getKey();
            }
        });
        addColumn(new AdvancedValueColumn());
        // addColumn(new ExtTextColumn<AdvancedConfigEntry>("Type") {
        //
        // @Override
        // protected String getStringValue(AdvancedConfigEntry value) {
        // return "[" + value.getType().getSimpleName() + "]";
        // }
        // });
        addColumn(new EditColumn());
    }

    public void onAdvancedConfigUpdate() {
        fill();
    }
}
