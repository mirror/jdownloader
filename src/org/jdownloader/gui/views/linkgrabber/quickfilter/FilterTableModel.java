package org.jdownloader.gui.views.linkgrabber.quickfilter;

import java.util.ArrayList;

import javax.swing.Icon;

import net.miginfocom.swing.MigLayout;

import org.appwork.swing.exttable.ExtTableModel;
import org.appwork.swing.exttable.columns.ExtCheckColumn;
import org.appwork.swing.exttable.columns.ExtLongColumn;
import org.appwork.swing.exttable.columns.ExtTextColumn;
import org.appwork.utils.swing.EDTHelper;

public class FilterTableModel extends ExtTableModel<Filter> {

    /**
     * 
     */
    private static final long serialVersionUID = 1749243877638799385L;

    public FilterTableModel() {
        super("FilterTableModel");

    }

    public void _fireTableStructureChanged(ArrayList<Filter> newtableData, final boolean refreshSort) {
        if (refreshSort) {
            newtableData = this.refreshSort(newtableData);
        }
        final ArrayList<Filter> newdata = newtableData;
        final ArrayList<Filter> selection = new EDTHelper<ArrayList<Filter>>() {

            @Override
            public ArrayList<Filter> edtRun() {
                return FilterTableModel.this.getSelectedObjects();
            }

        }.getReturnValue();

        if (selection != null) {
            selection.retainAll(newdata);
        }

        this._replaceTableData(newdata, selection, true);
    }

    @Override
    protected void initColumns() {
        addColumn(new ExtTextColumn<Filter>("Hoster") {
            /**
             * 
             */
            private static final long serialVersionUID = 1L;

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
                return obj.isEnabled();
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
        addColumn(new ExtLongColumn<Filter>("Hoster") {
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
            public boolean isEnabled(Filter obj) {
                return obj.isEnabled();
            }

            @Override
            protected String getTooltipText(final Filter obj) {

                return obj.getDescription();
            }

            @Override
            public boolean isSortable(Filter obj) {

                return false;
            }

            @Override
            public void configureRendererComponent(Filter value, boolean isSelected, boolean hasFocus, int row, int column) {
                super.configureRendererComponent(value, isSelected, hasFocus, row, column);
                if (getLong(value) < 0) renderer.setText("");
            }

            @Override
            protected long getLong(Filter value) {
                return value.getCounter();
            }

        });
        ExtTextColumn<Filter> hosterColumn;
        addColumn(hosterColumn = new ExtTextColumn<Filter>("Hoster") {
            {
                renderer.setLayout(new MigLayout("ins 0", "[grow,fill][]", "[grow,fill]"));

            }

            @Override
            public boolean isEnabled(Filter obj) {
                return obj.isEnabled();
            }

            @Override
            protected String getTooltipText(final Filter obj) {

                return obj.getDescription();
            }

            @Override
            public boolean isSortable(Filter obj) {
                return true;
            }

            @Override
            public String getStringValue(Filter value) {
                return value.getName();
            }
        });
        addColumn(new ExtCheckColumn<Filter>("Check") {
            @Override
            public boolean isSortable(Filter obj) {
                return false;
            }

            @Override
            protected String getTooltipText(final Filter obj) {

                return obj.getDescription();
            }

            @Override
            protected boolean getBooleanValue(Filter value) {
                return value.isEnabled();
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
            public boolean isEditable(Filter obj) {
                return true;
            }

            @Override
            protected void setBooleanValue(boolean value, Filter object) {
                object.setEnabled(value);
            }
        });
        this.setSortColumn(hosterColumn);
    }

}
