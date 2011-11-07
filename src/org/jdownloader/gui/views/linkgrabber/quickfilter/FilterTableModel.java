package org.jdownloader.gui.views.linkgrabber.quickfilter;

import javax.swing.Icon;

import jd.controlling.packagecontroller.AbstractPackageChildrenNode;
import jd.controlling.packagecontroller.AbstractPackageNode;
import net.miginfocom.swing.MigLayout;

import org.appwork.swing.exttable.ExtTableModel;
import org.appwork.swing.exttable.columns.ExtCheckColumn;
import org.appwork.swing.exttable.columns.ExtLongColumn;
import org.appwork.swing.exttable.columns.ExtTextColumn;

public class FilterTableModel<E extends AbstractPackageNode<V, E>, V extends AbstractPackageChildrenNode<E>> extends ExtTableModel<Filter<E, V>> {

    /**
     * 
     */
    private static final long serialVersionUID = 1749243877638799385L;

    public FilterTableModel() {
        super("FilterTableModel");

    }

    @Override
    protected void initColumns() {
        addColumn(new ExtTextColumn<Filter<E, V>>("Hoster") {
            // {
            // renderer.setLayout(new MigLayout("ins 0", "[grow,fill][]",
            // "[]"));
            //
            // }
            @Override
            protected int getMaxWidth() {
                return 18;
            }

            @Override
            public int getMinWidth() {
                return getMaxWidth();
            }

            @Override
            public int getDefaultWidth() {
                return getMaxWidth();
            }

            @Override
            public boolean isEnabled(Filter obj) {
                return !obj.isEnabled();
            }

            @Override
            protected Icon getIcon(Filter value) {
                return value.getIcon();
            }

            @Override
            public boolean isSortable(Filter obj) {
                return false;
            }

            @Override
            public String getStringValue(Filter value) {
                return "";
            }
        });
        addColumn(new ExtLongColumn<Filter<E, V>>("Hoster") {
            // {
            // renderer.setLayout(new MigLayout("ins 0", "[grow,fill][]",
            // "[]"));
            //
            // }
            @Override
            protected int getMaxWidth() {
                return 40;
            }

            @Override
            public int getMinWidth() {
                return getMaxWidth();
            }

            @Override
            public int getDefaultWidth() {
                return getMaxWidth();
            }

            @Override
            public boolean isEnabled(Filter<E, V> obj) {
                return !obj.isEnabled();
            }

            @Override
            protected String getTooltipText(final Filter<E, V> obj) {

                return obj.getDescription();
            }

            @Override
            public boolean isSortable(Filter<E, V> obj) {

                return false;
            }

            @Override
            public void configureRendererComponent(Filter<E, V> value, boolean isSelected, boolean hasFocus, int row, int column) {
                super.configureRendererComponent(value, isSelected, hasFocus, row, column);
                if (getLong(value) < 0) renderer.setText("");
            }

            @Override
            protected long getLong(Filter<E, V> value) {
                return value.getCounter();
            }

        });
        ExtTextColumn<Filter<E, V>> hosterColumn;
        addColumn(hosterColumn = new ExtTextColumn<Filter<E, V>>("Hoster") {
            {
                renderer.setLayout(new MigLayout("ins 0", "[grow,fill][]", "[]"));

            }

            @Override
            public boolean isEnabled(Filter<E, V> obj) {
                return !obj.isEnabled();
            }

            @Override
            protected String getTooltipText(final Filter<E, V> obj) {

                return obj.getDescription();
            }

            @Override
            public boolean isSortable(Filter<E, V> obj) {
                return true;
            }

            @Override
            public String getStringValue(Filter<E, V> value) {
                return value.getName();
            }
        });
        addColumn(new ExtCheckColumn<Filter<E, V>>("Check") {
            @Override
            public boolean isSortable(Filter<E, V> obj) {
                return false;
            }

            @Override
            protected String getTooltipText(final Filter<E, V> obj) {

                return obj.getDescription();
            }

            @Override
            protected boolean getBooleanValue(Filter<E, V> value) {
                return !value.isEnabled();
            }

            @Override
            protected int getMaxWidth() {
                return 30;
            }

            @Override
            public int getMinWidth() {
                return getMaxWidth();
            }

            @Override
            public int getDefaultWidth() {
                return getMaxWidth();
            }

            @Override
            public boolean isEditable(Filter<E, V> obj) {
                return true;
            }

            @Override
            protected void setBooleanValue(boolean value, Filter<E, V> object) {
                object.setEnabled(value);
            }
        });
        this.setSortColumn(hosterColumn);
    }

}
