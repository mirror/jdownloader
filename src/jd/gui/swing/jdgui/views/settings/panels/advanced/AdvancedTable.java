package jd.gui.swing.jdgui.views.settings.panels.advanced;

import jd.gui.swing.jdgui.views.settings.panels.components.SettingsTable;

import org.jdownloader.settings.advanced.AdvancedEntry;

public class AdvancedTable extends SettingsTable<AdvancedEntry> {

    public AdvancedTable() {
        super(new AdvancedTableModel("AdvancedTable"));
    }

}
