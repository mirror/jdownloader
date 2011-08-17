package jd.gui.swing.jdgui.views.settings.panels.advanced;

import java.util.ArrayList;
import java.util.Iterator;

import jd.controlling.IOEQ;

import org.appwork.scheduler.DelayedRunnable;
import org.appwork.swing.exttable.ExtTableModel;
import org.appwork.swing.exttable.columns.ExtTextColumn;
import org.jdownloader.settings.advanced.AdvancedConfigEntry;
import org.jdownloader.settings.advanced.AdvancedConfigManager;

public class AdvancedTableModel extends ExtTableModel<AdvancedConfigEntry> {
    private static final long serialVersionUID = 1L;
    private DelayedRunnable   delayedFilter;
    private String            text             = null;

    public AdvancedTableModel(String id) {
        super(id);

        delayedFilter = new DelayedRunnable(IOEQ.TIMINGQUEUE, 250l) {

            @Override
            public void delayedrun() {
                final ArrayList<AdvancedConfigEntry> tmp = AdvancedConfigManager.getInstance().list();
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
                _fireTableStructureChanged(tmp, true);
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

            @Override
            public boolean isHidable() {
                return false;
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

    public void filter(final String text) {
        this.text = text;
        delayedFilter.run();
    }
}
