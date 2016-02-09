package org.jdownloader.iconsetter.gui;

import java.awt.Component;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import javax.swing.Icon;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;

import org.appwork.swing.exttable.ExtTableModel;
import org.appwork.swing.exttable.columns.ExtTextColumn;
import org.jdownloader.iconsetter.IconResource;
import org.jdownloader.iconsetter.ResourceSet;

public class SetTableModel extends ExtTableModel<IconResource> {
    private ResourceSet standard;
    private ResourceSet selected;

    public SetTableModel(ResourceSet standard, ResourceSet selected) {
        super("SetTableModel");
        this.standard = standard;
        this.selected = selected;
        ArrayList<IconResource> list = new ArrayList<IconResource>();
        list.addAll(standard.getIcons());
        Collections.sort(list, new Comparator<IconResource>() {

            @Override
            public int compare(IconResource o1, IconResource o2) {
                return o1.getPath().compareTo(o2.getPath());
            }
        });
        setTableData(list);
        super.init("SetTableModel");
    }

    @Override
    protected void init(String id) {

    }

    @Override
    protected void initColumns() {
        addColumn(new ExtTextColumn<IconResource>("Path") {

            @Override
            public String getStringValue(IconResource value) {
                return value.getPath().replace("org/jdownloader/images", "");
            }
        });
        addColumn(new ExtTextColumn<IconResource>("Standard") {
            private int minWidth;

            @Override
            protected Icon getIcon(IconResource value) {
                return value.getIcon("standard", 32);

            }

            @Override
            public boolean isResizable() {
                return false;
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
            public String getStringValue(IconResource value) {
                return null;
            }
        });
        addColumn(new ExtTextColumn<IconResource>(selected.getName()) {
            private int minWidth;

            @Override
            protected Icon getIcon(IconResource value) {
                return value.getIcon(selected.getName(), 32);

            }

            @Override
            public boolean isResizable() {
                return false;
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
            public String getStringValue(IconResource value) {
                return null;
            }
        });
    }

}
