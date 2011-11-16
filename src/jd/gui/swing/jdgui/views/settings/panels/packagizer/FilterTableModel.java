package jd.gui.swing.jdgui.views.settings.panels.packagizer;

import java.awt.Component;

import javax.swing.Icon;
import javax.swing.JTable;
import javax.swing.table.JTableHeader;

import org.appwork.swing.exttable.ExtTableHeaderRenderer;
import org.appwork.swing.exttable.ExtTableModel;
import org.appwork.swing.exttable.columns.ExtCheckColumn;
import org.appwork.swing.exttable.columns.ExtSpinnerColumn;
import org.appwork.swing.exttable.columns.ExtTextColumn;
import org.appwork.utils.event.predefined.changeevent.ChangeEvent;
import org.appwork.utils.event.predefined.changeevent.ChangeListener;
import org.appwork.utils.swing.EDTRunner;
import org.jdownloader.controlling.packagizer.PackagizerController;
import org.jdownloader.controlling.packagizer.PackagizerRule;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.NewTheme;

public class FilterTableModel extends ExtTableModel<PackagizerRule> implements ChangeListener {

    private static final long                serialVersionUID = -7756459932564776739L;
    private ExtSpinnerColumn<PackagizerRule> prio;

    public FilterTableModel(String id) {
        super(id);
        PackagizerController.getInstance().getEventSender().addListener(this, false);

    }

    @Override
    protected void initColumns() {

        this.addColumn(new ExtCheckColumn<PackagizerRule>(_GUI._.settings_linkgrabber_filter_columns_enabled()) {

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
            protected boolean getBooleanValue(PackagizerRule value) {
                return value.isEnabled();
            }

            @Override
            public boolean isEditable(PackagizerRule obj) {

                return true;
            }

            @Override
            public boolean isSortable(final PackagizerRule obj) {
                return false;
            }

            @Override
            protected void setBooleanValue(boolean value, PackagizerRule object) {
                object.setEnabled(value);
                PackagizerController.getInstance().update();
            }
        });
        addColumn(prio = new ExtSpinnerColumn<PackagizerRule>(_GUI._.settings_linkgrabber_filter_columns_exepriority()) {

            @Override
            public boolean isEnabled(PackagizerRule obj) {
                return obj.isEnabled();
            }

            @Override
            public boolean isSortable(final PackagizerRule obj) {
                return false;
            }

            @Override
            protected String getTooltipText(PackagizerRule obj) {
                return _GUI._.FilterTableModel_getTooltipText_prio_();
            }

            @Override
            public int getDefaultWidth() {
                return 60;
            }

            // @Override
            // protected String getNextSortIdentifier() {
            // return super.getNextSortIdentifier();
            // }

            @Override
            protected boolean isDefaultResizable() {

                return false;
            }

            @Override
            public boolean isEditable(final PackagizerRule obj) {
                return true;
            }

            @Override
            public boolean isPaintWidthLockIcon() {
                return false;
            }

            @Override
            protected Number getNumber(PackagizerRule value) {
                return value.getOrder();
            }

            @Override
            protected void setNumberValue(Number value, PackagizerRule object) {
                object.setOrder(value.intValue());
                refreshSort(getTableData());
            }

            @Override
            public String getStringValue(PackagizerRule value) {
                return value.getOrder() + "";
            }
        });
        addColumn(new ExtTextColumn<PackagizerRule>(_GUI._.settings_linkgrabber_filter_columns_name()) {

            @Override
            public boolean isEnabled(PackagizerRule obj) {
                return obj.isEnabled();
            }

            @Override
            public boolean isSortable(final PackagizerRule obj) {
                return false;
            }

            protected Icon getIcon(final PackagizerRule value) {
                String key = value.getIconKey();
                if (key == null) {
                    return null;
                } else {
                    return NewTheme.I().getIcon(key, 18);
                }
            }

            @Override
            public String getStringValue(PackagizerRule value) {
                return value.getName();
            }
        });
        setSortColumn(prio);
    }

    public void onChangeEvent(ChangeEvent event) {
        new EDTRunner() {

            @Override
            protected void runInEDT() {
                _fireTableStructureChanged(PackagizerController.getInstance().list(), true);
            }
        };

    }
}
