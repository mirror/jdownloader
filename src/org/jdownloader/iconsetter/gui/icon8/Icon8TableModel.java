package org.jdownloader.iconsetter.gui.icon8;

import java.awt.Component;
import java.util.ArrayList;

import javax.swing.Icon;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;

import org.appwork.swing.exttable.ExtTableModel;
import org.appwork.swing.exttable.columns.ExtTextColumn;
import org.jdownloader.iconsetter.gui.Icon8Resource;

public class Icon8TableModel extends ExtTableModel<Icon8Resource> {

    private ArrayList<Icon8Resource> iconsList;
    private Icon8Dialog              icon8Dialog;

    public Icon8TableModel(Icon8Dialog icon8Dialog, ArrayList<Icon8Resource> iconsList) {
        super("Icon8TableModel");
        this.iconsList = iconsList;
        this.icon8Dialog = icon8Dialog;

        super.init("Icon8TableModel");
        _fireTableStructureChanged(iconsList, true);
    }

    @Override
    protected void init(String id) {

    }

    @Override
    protected void initColumns() {
        ExtTextColumn<Icon8Resource> sortOn;
        addColumn(sortOn = new ExtTextColumn<Icon8Resource>("Name") {

            @Override
            public String getStringValue(Icon8Resource value) {
                return value.getName();
            }
        });
        addColumn(new ExtTextColumn<Icon8Resource>("Platform") {

            @Override
            public String getStringValue(Icon8Resource value) {
                return value.getPlatform();
            }
        });
        addColumn(new ExtTextColumn<Icon8Resource>("Icon") {
            private int minWidth;

            @Override
            protected Icon getIcon(Icon8Resource value) {
                return icon8Dialog.getIcon(value, 32);

            }

            @Override
            public int getDefaultWidth() {
                return getMinWidth();
            }

            @Override
            public int getMaxWidth() {
                return getMinWidth();
            }

            @Override
            public int getMinWidth() {
                if (this.minWidth > 0) {
                    return this.minWidth;
                }
                // derive default width from the preferred header width
                TableColumn tableColumn = this.getTableColumn();
                TableCellRenderer renderer = tableColumn.getHeaderRenderer();
                if (renderer == null) {
                    renderer = getTable().getTableHeader().getDefaultRenderer();
                }
                Component component = renderer.getTableCellRendererComponent(getTable(), this.getName(), false, false, -1, 2);

                this.minWidth = Math.max(36, component.getPreferredSize().width);
                return this.minWidth;
            }

            @Override
            public String getStringValue(Icon8Resource value) {
                return null;
            }
        });
        sortColumn = sortOn;
    }

}
