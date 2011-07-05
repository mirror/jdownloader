package jd.gui.swing.jdgui.views.settings.panels.advanced;

import java.util.ArrayList;
import java.util.Iterator;

import org.appwork.utils.swing.EDTRunner;
import org.appwork.utils.swing.table.ExtTableModel;
import org.appwork.utils.swing.table.columns.ExtTextColumn;
import org.jdownloader.settings.advanced.AdvancedConfigEntry;
import org.jdownloader.settings.advanced.AdvancedConfigEventListener;
import org.jdownloader.settings.advanced.AdvancedConfigManager;

public class AdvancedTableModel extends ExtTableModel<AdvancedConfigEntry> implements AdvancedConfigEventListener {
    private static final long serialVersionUID = 1L;

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
            private static final long serialVersionUID = 1L;

            @Override
            protected String getTooltipText(AdvancedConfigEntry obj) {
                return obj.getDescription();
            }

            @Override
            public String getStringValue(AdvancedConfigEntry value) {
                return value.getKey();
            }
        });
        addColumn(new AdvancedValueColumn());
        addColumn(new ExtTextColumn<AdvancedConfigEntry>("Type") {
            private static final long serialVersionUID = 1L;

            @Override
            public int getDefaultWidth() {
                return 100;
            }

            @Override
            public String getStringValue(AdvancedConfigEntry value) {
                return value.getTypeString();
            }
        });
        addColumn(new EditColumn());
    }

    public void onAdvancedConfigUpdate() {
        fill();
    }

    public void filter(final String text) {
        new EDTRunner() {

            @Override
            protected void runInEDT() {
                ArrayList<AdvancedConfigEntry> tmp = (ArrayList<AdvancedConfigEntry>) AdvancedConfigManager.getInstance().list().clone();
                if (text != null) {
                    AdvancedConfigEntry next;
                    for (Iterator<AdvancedConfigEntry> it = tmp.iterator(); it.hasNext();) {
                        next = it.next();
                        if (!next.getKey().toLowerCase().contains(text.toLowerCase())) {
                            if (next.getDescription() == null || !next.getDescription().toLowerCase().contains(text.toLowerCase())) {
                                it.remove();
                            }
                        }
                    }
                }
                tableData = tmp;
                fireTableStructureChanged();
            }
        };
    }
}
