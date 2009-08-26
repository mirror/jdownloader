//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team support@jdownloader.org
//
//    This program is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.
//
//    This program is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with this program.  If not, see <http://www.gnu.org/licenses/>.

package jd.gui.swing.jdgui.settings.panels.gui;

import java.awt.Component;
import java.util.ArrayList;
import java.util.Iterator;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;

import jd.config.Configuration;
import jd.config.ConfigEntry.PropertyType;
import jd.gui.swing.jdgui.actions.ActionController;
import jd.gui.swing.jdgui.actions.ToolBarAction;
import jd.gui.swing.jdgui.settings.ConfigPanel;
import jd.utils.JDTheme;
import jd.utils.locale.JDL;
import net.miginfocom.swing.MigLayout;

import org.jdesktop.swingx.renderer.DefaultTableRenderer;

public class ToolbarController extends ConfigPanel {

    private static final long serialVersionUID = -7024581410075950497L;

    public String getBreadcrum() {
        return JDL.L(this.getClass().getName() + ".breadcrum", this.getClass().getSimpleName());
    }

    public static String getTitle() {
        return JDL.L(JDL_PREFIX + ".toolbarController.title", "Toolbar Manager");
    }

    private static final String JDL_PREFIX = "jd.gui.swing.jdgui.settings.panels.gui.ToolbarController.";

    private class InternalTableModel extends AbstractTableModel {

        private static final long serialVersionUID = 1155282457354673850L;

        @Override
        public Class<?> getColumnClass(int columnIndex) {
            if (columnIndex == 0) return Boolean.class;
            return ToolBarAction.class;
        }

        public int getColumnCount() {
            return 4;
        }

        @Override
        public String getColumnName(int column) {
            switch (column) {
            case 0:
                return JDL.L(JDL_PREFIX + ".column.use", "Use");
            case 1:
                return JDL.L(JDL_PREFIX + ".column.name", "Name");
            case 2:
                return JDL.L(JDL_PREFIX + ".column.desc", "Description");
            case 3:
                return JDL.L(JDL_PREFIX + ".column.icon", "Icon");
            }
            return super.getColumnName(column);
        }

        public int getRowCount() {
            return actions.size();
        }

        public Object getValueAt(final int rowIndex, final int columnIndex) {
            if (columnIndex == 0) return actions.get(rowIndex).isVisible();
            return actions.get(rowIndex);
        }

        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex) {
            return columnIndex == 0;
        }

        @Override
        public void setValueAt(Object value, int row, int col) {
            if (col == 0) {
                actions.get(row).setVisible((Boolean) value);
            }
        }
    }

    private JTable table;

    private InternalTableModel tableModel;

    private ArrayList<ToolBarAction> actions;

    public ToolbarController(Configuration configuration) {
        super();
        setActions(ActionController.getActions());
        initPanel();
        load();
    }

    public void onShow() {
        super.onShow();
        setActions(ActionController.getActions());
    }

    /**
     * filters the available actions
     * 
     * @param actions2
     */
    private void setActions(ArrayList<ToolBarAction> actions2) {
        for (Iterator<ToolBarAction> it = actions2.iterator(); it.hasNext();) {
            ToolBarAction a = it.next();
            if (a.getID().equals("action.opendlfolder")) continue;

            if (a.getID().equals("toolbar.separator")) {
                it.remove();
                continue;
            } else if (a.getID().equals("toolbar.separator")) {
                it.remove();
                continue;
            } else if (a.getID().equals("toolbar.control.start")) {
                it.remove();
                continue;
            } else if (a.getID().equals("toolbar.control.stop")) {
                it.remove();
                continue;
            } else if (!a.getID().startsWith("toolbar.") && !a.getID().startsWith("action.downloadview")) {
                it.remove();
                continue;
            }
        }
        this.actions = actions2;

    }

    @Override
    public void initPanel() {
        tableModel = new InternalTableModel();
        table = new JTable(tableModel) {
            private static final long serialVersionUID = -7914266013067863393L;

            @Override
            public TableCellRenderer getCellRenderer(int row, int col) {
                if (col == 0) return super.getCellRenderer(row, col);
                return new TableRenderer();
            }
        };

        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        table.getTableHeader().setReorderingAllowed(false);

        TableColumn column = null;

        for (int c = 0; c < tableModel.getColumnCount(); c++) {
            column = table.getColumnModel().getColumn(c);
            switch (c) {
            case 0:
                column.setPreferredWidth(40);
                column.setMaxWidth(40);
                break;

            case 3:
                column.setPreferredWidth(40);
                column.setMaxWidth(40);
                break;

            }
        }

        panel.setLayout(new MigLayout("ins 5,wrap 1", "[fill,grow]", "[fill,grow][]"));
        panel.add(new JScrollPane(table));

        JTabbedPane tabbed = new JTabbedPane();
        tabbed.setOpaque(false);
        tabbed.add(getBreadcrum(), panel);

        this.add(tabbed);
    }

    @Override
    public void load() {
    }

    @Override
    public void save() {
    }

    @Override
    public PropertyType hasChanges() {
        return PropertyType.NONE;
    }

    class TableRenderer extends DefaultTableRenderer {

        private static final long serialVersionUID = 1L;

        private Component co;
        private JCheckBox checkbox;
        private JLabel label;

        public TableRenderer() {
            checkbox = new JCheckBox();

            label = new JLabel();
        }

        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            co = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            ToolBarAction action = (ToolBarAction) value;
            switch (column) {
            case 0:
                checkbox.setSelected(action.isVisible());
                return checkbox;
            case 1:
                label.setIcon(null);
                label.setText(action.getTitle());
                return label;
            case 2:
                label.setIcon(null);
                label.setText(action.getTooltipText());
                return label;
            case 3:
                label.setIcon(JDTheme.II(action.getValue(ToolBarAction.IMAGE_KEY) + "", 16, 16));
                label.setText("");
                return label;

            }
            return co;
        }

    }
}
