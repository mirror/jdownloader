package jd.gui.swing.jdgui.views.settings.panels.linkgrabberfilter;

import java.awt.Component;
import java.awt.Point;

import javax.swing.Icon;
import javax.swing.JTable;
import javax.swing.table.JTableHeader;

import org.appwork.swing.components.tooltips.ExtTooltip;
import org.appwork.swing.exttable.ExtTableHeaderRenderer;
import org.appwork.swing.exttable.ExtTableModel;
import org.appwork.swing.exttable.columns.ExtCheckColumn;
import org.appwork.swing.exttable.columns.ExtTextColumn;
import org.appwork.utils.event.predefined.changeevent.ChangeEvent;
import org.appwork.utils.event.predefined.changeevent.ChangeListener;
import org.jdownloader.controlling.filter.LinkFilterController;
import org.jdownloader.controlling.filter.LinkgrabberFilterRule;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.NewTheme;

public class FilterTableModel extends ExtTableModel<LinkgrabberFilterRule> implements ChangeListener {

    private static final long serialVersionUID = -7756459932564776739L;

    public FilterTableModel(String id) {
        super(id);
        LinkFilterController.getInstance().getEventSender().addListener(this, false);
    }

    @Override
    protected void initColumns() {

        this.addColumn(new ExtCheckColumn<LinkgrabberFilterRule>(_GUI._.settings_linkgrabber_filter_columns_enabled()) {

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
            protected boolean getBooleanValue(LinkgrabberFilterRule value) {
                return value.isEnabled();
            }

            @Override
            public boolean isEditable(LinkgrabberFilterRule obj) {
                return true;
            }

            @Override
            public ExtTooltip createToolTip(Point position, LinkgrabberFilterRule obj) {
                return createTooltip(obj);
            }

            @Override
            protected void setBooleanValue(boolean value, LinkgrabberFilterRule object) {
                object.setEnabled(value);
                LinkFilterController.getInstance().update();
            }
        });

        addColumn(new ExtTextColumn<LinkgrabberFilterRule>(_GUI._.settings_linkgrabber_filter_columns_name()) {

            @Override
            public boolean isEnabled(LinkgrabberFilterRule value) {
                return value.isEnabled();
            }

            protected Icon getIcon(final LinkgrabberFilterRule value) {
                String key = value.getIconKey();
                if (key == null) {
                    return null;
                } else {
                    return NewTheme.I().getIcon(key, 18);
                }
            }

            @Override
            public ExtTooltip createToolTip(Point position, LinkgrabberFilterRule obj) {
                return createTooltip(obj);
            }

            @Override
            public String getStringValue(LinkgrabberFilterRule value) {
                return value.getName();
            }
        });

        addColumn(new ExtTextColumn<LinkgrabberFilterRule>(_GUI._.settings_linkgrabber_filter_columns_condition()) {
            {
                // rendererField.setHorizontalAlignment(JLabel.RIGHT);
            }

            public ExtTableHeaderRenderer getHeaderRenderer(final JTableHeader jTableHeader) {

                final ExtTableHeaderRenderer ret = new ExtTableHeaderRenderer(this, jTableHeader) {

                    private static final long serialVersionUID = 3938290423337000265L;

                    @Override
                    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                        super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                        setIcon(NewTheme.I().getIcon("trash", 14));
                        return this;
                    }

                };

                return ret;
            }

            @Override
            public boolean isEnabled(LinkgrabberFilterRule value) {
                return value.isEnabled();
            }

            @Override
            public ExtTooltip createToolTip(Point position, LinkgrabberFilterRule obj) {
                return createTooltip(obj);
            }

            @Override
            public String getStringValue(LinkgrabberFilterRule value) {
                return _GUI._.settings_linkgrabber_filter_columns_if(value.toString());
            }
        });

    }

    protected ExtTooltip createTooltip(LinkgrabberFilterRule obj) {
        // if (obj == null) return null;
        // tooltip.updateRule(obj);
        // return tooltip;
        return null;
    }

    public void onChangeEvent(ChangeEvent event) {
        _fireTableStructureChanged(LinkFilterController.getInstance().listFilters(), true);
    }
}
