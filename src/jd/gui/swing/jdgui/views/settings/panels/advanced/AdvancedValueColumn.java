package jd.gui.swing.jdgui.views.settings.panels.advanced;

import javax.swing.JComponent;

import org.appwork.utils.swing.table.columns.ExtComponentColumn;
import org.jdownloader.settings.advanced.AdvancedEntry;

public class AdvancedValueColumn extends ExtComponentColumn<AdvancedEntry> {

    public AdvancedValueColumn() {
        super("Value");
    }

    @Override
    protected JComponent getEditorComponent(AdvancedEntry value, boolean isSelected, int row, int column) {
        return null;
    }

    @Override
    protected JComponent getRendererComponent(AdvancedEntry value, boolean isSelected, int row, int column) {
        return null;
    }

}
