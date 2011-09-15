package jd.gui.swing.jdgui.views.settings.panels.linkgrabberfilter;

import java.awt.Component;
import java.awt.Point;

import javax.swing.JTable;
import javax.swing.table.JTableHeader;

import org.appwork.swing.components.tooltips.ExtTooltip;
import org.appwork.swing.exttable.ExtTableHeaderRenderer;
import org.appwork.swing.exttable.ExtTableModel;
import org.appwork.swing.exttable.columns.ExtCheckColumn;
import org.appwork.swing.exttable.columns.ExtTextColumn;
import org.jdownloader.controlling.filter.FilterRule;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.NewTheme;

public class FilterTableModel extends ExtTableModel<FilterRule> {

    private static final long serialVersionUID = -7756459932564776739L;
    private FilterTooltip     tooltip;

    public FilterTableModel(String id) {
        super(id);
        tooltip = new FilterTooltip();
    }

    @Override
    protected void initColumns() {

        this.addColumn(new ExtCheckColumn<FilterRule>(_GUI._.settings_linkgrabber_filter_columns_enabled()) {

            private static final long serialVersionUID = -4667150369226691276L;

            public ExtTableHeaderRenderer getHeaderRenderer(final JTableHeader jTableHeader) {

                final ExtTableHeaderRenderer ret = new ExtTableHeaderRenderer(this, jTableHeader) {

                    private static final long serialVersionUID = 3938290423337000265L;

                    @Override
                    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                        super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                        setIcon(NewTheme.I().getIcon("ok", 14));
                        setHorizontalAlignment(CENTER);
                        setText(null);
                        return this;
                    }

                };

                return ret;
            }

            @Override
            public int getMaxWidth() {
                return 30;
            }

            @Override
            public boolean isHidable() {
                return false;
            }

            @Override
            protected boolean getBooleanValue(FilterRule value) {
                return value.isEnabled();
            }

            @Override
            public boolean isEditable(FilterRule obj) {
                return true;
            }

            @Override
            public ExtTooltip createToolTip(Point position, FilterRule obj) {
                return createTooltip(obj);
            }

            @Override
            protected void setBooleanValue(boolean value, FilterRule object) {
                object.setEnabled(value);
            }
        });
        addColumn(new ExtTextColumn<FilterRule>(_GUI._.settings_linkgrabber_filter_columns_name()) {

            @Override
            public ExtTooltip createToolTip(Point position, FilterRule obj) {
                return createTooltip(obj);
            }

            @Override
            public String getStringValue(FilterRule value) {
                return value.getName();
            }
        });
    }

    protected ExtTooltip createTooltip(FilterRule obj) {
        if (obj == null) return null;
        tooltip.updateRule(obj);
        return tooltip;
    }
}
