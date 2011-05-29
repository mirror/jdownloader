package jd.gui.swing.jdgui.views.settings.panels.advanced;

import jd.gui.swing.jdgui.views.settings.panels.components.SettingsTable;

import org.jdownloader.settings.advanced.AdvancedConfigEntry;

public class AdvancedTable extends SettingsTable<AdvancedConfigEntry> {
    private static final long serialVersionUID = 1L;

    public AdvancedTable() {
        super(new AdvancedTableModel("AdvancedTable"));
    }

    @Override
    public boolean isSearchEnabled() {
        return true;
    }

    public void filter(String text) {
        ((AdvancedTableModel) this.getExtTableModel()).filter(text);
    }

}
