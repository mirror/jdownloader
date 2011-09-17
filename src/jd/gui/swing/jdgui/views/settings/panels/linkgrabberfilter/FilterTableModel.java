package jd.gui.swing.jdgui.views.settings.panels.linkgrabberfilter;

import java.awt.Component;
import java.awt.Point;

import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.table.JTableHeader;

import net.miginfocom.swing.MigLayout;

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
            public boolean isEnabled(FilterRule obj) {
                return obj.isEnabled();
            }

            @Override
            public ExtTooltip createToolTip(Point position, FilterRule obj) {
                return createTooltip(obj);
            }

            @Override
            public String getStringValue(FilterRule value) {
                return value.getName();
            }
        });

        addColumn(new ExtTextColumn<FilterRule>(_GUI._.settings_linkgrabber_filter_columns_condition()) {
            {
                rendererField.setHorizontalAlignment(JLabel.RIGHT);
            }

            @Override
            public boolean isEnabled(FilterRule obj) {
                return obj.isEnabled();
            }

            @Override
            public ExtTooltip createToolTip(Point position, FilterRule obj) {
                return createTooltip(obj);
            }

            @Override
            public String getStringValue(FilterRule value) {
                return _GUI._.settings_linkgrabber_filter_columns_if(value.toString());
            }
        });

        addColumn(new ExtTextColumn<FilterRule>(_GUI._.settings_linkgrabber_filter_columns_then()) {
            {

                renderer.removeAll();

                renderer.setLayout(new MigLayout("ins 0", "[grow,fill]5[]", "[grow,fill]"));
                renderer.add(this.rendererField);
                renderer.add(this.rendererIcon);

            }

            @Override
            public boolean isEnabled(FilterRule obj) {
                return obj.isEnabled();
            }

            @Override
            public int getDefaultWidth() {
                return 160;
            }

            @Override
            protected boolean isDefaultResizable() {
                return false;
            }

            @Override
            public ExtTooltip createToolTip(Point position, FilterRule obj) {
                return createTooltip(obj);
            }

            @Override
            protected Icon getIcon(FilterRule value) {
                if (value.isAccept()) {
                    return NewTheme.I().getIcon("ok", 20);
                } else {
                    return NewTheme.I().getIcon("cancel", 20);
                }
            }

            @Override
            public String getStringValue(FilterRule value) {
                if (value.isAccept()) {
                    return _GUI._.FilterTableModel_initColumns_accept_link();
                } else {
                    return _GUI._.FilterTableModel_initColumns_deny_link();
                }
            }
        });

    }

    protected ExtTooltip createTooltip(FilterRule obj) {
        if (obj == null) return null;
        tooltip.updateRule(obj);
        return tooltip;
    }
}
