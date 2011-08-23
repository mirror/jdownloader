package org.jdownloader.gui.views.linkgrabber;

import javax.swing.Icon;

import net.miginfocom.swing.MigLayout;

import org.appwork.swing.exttable.ExtTableModel;
import org.appwork.swing.exttable.columns.ExtCheckColumn;
import org.appwork.swing.exttable.columns.ExtTextColumn;

public class FilterTableModel extends ExtTableModel<Filter> {

    public FilterTableModel() {
        super("FilterTableModel");
    }

    @Override
    protected void initColumns() {
        addColumn(new ExtTextColumn<Filter>("Hoster") {
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
        addColumn(new ExtTextColumn<Filter>("Hoster") {
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
            public boolean isSortable(Filter obj) {
                return false;
            }

            @Override
            public String getStringValue(Filter value) {
                return value.getInfo();
            }
        });
        addColumn(new ExtTextColumn<Filter>("Hoster") {
            {
                renderer.setLayout(new MigLayout("ins 0", "[grow,fill][]", "[]"));

            }

            @Override
            public boolean isEnabled(Filter obj) {
                return obj.isEnabled();
            }

            @Override
            public boolean isSortable(Filter obj) {
                return true;
            }

            @Override
            public String getStringValue(Filter value) {
                return value.getHoster();
            }
        });
        addColumn(new ExtCheckColumn<Filter>("Check") {
            @Override
            public boolean isSortable(Filter obj) {
                return false;
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
    }

}
