//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team jdownloader@freenet.de
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

package jd.gui.skins.simple.config;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EventObject;

import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.event.CellEditorListener;
import javax.swing.event.TableModelEvent;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;

import jd.config.CFGConfig;
import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.config.Configuration;
import jd.controlling.interaction.PackageManager;
import jd.gui.UIInterface;
import jd.gui.skins.simple.Link.JLinkButton;
import jd.update.PackageData;
import jd.utils.JDLocale;
import jd.utils.JDUtilities;

/**
 * @author JD-Team
 */
public class SubPanelRessources extends ConfigPanel implements ActionListener {

    private class InternalTableModel extends AbstractTableModel {

        private static final long serialVersionUID = 1L;

        @Override
        public Class<?> getColumnClass(int columnIndex) {
            return getValueAt(0, columnIndex).getClass();
        }

        public int getColumnCount() {
            return 6;
        }

        @Override
        public String getColumnName(int column) {
            switch (column) {
            case 0:
                return JDLocale.L("gui.config.packagemanager.column_name", "Paket");
            case 1:
                return JDLocale.L("gui.config.packagemanager.column_category", "Kategorie");
            case 2:
                return JDLocale.L("gui.config.packagemanager.column_info", "Info.");
            case 3:
                return JDLocale.L("gui.config.packagemanager.column_latestVersion", "Akt. Version");
            case 4:
                return JDLocale.L("gui.config.packagemanager.column_installedVersion", "Inst. Version");
            case 5:
                return JDLocale.L("gui.config.packagemanager.column_select", "Auswählen");
            }
            return super.getColumnName(column);
        }

        public int getRowCount() {
            return packageData.size();
        }

        public Object getValueAt(int rowIndex, int columnIndex) {
            PackageData element = packageData.get(rowIndex);

            switch (columnIndex) {
            case 0:
                return element.getStringProperty("name");
            case 1:
                return element.getStringProperty("category");
            case 2:
                return new JLinkButton(JDLocale.L("gui.config.packagemanager.table.info", "Info"), element.getStringProperty("infourl"));
            case 3:
                return element.getStringProperty("version");
            case 4:
                return element.getInstalledVersion();
            case 5:
                return element.isSelected();
            }
            return null;
        }

        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex) {
            return true;
        }

        @Override
        public void setValueAt(Object value, int row, int col) {
            PackageData element = packageData.get(row);
            element.setSelected(!element.isSelected());
        }
    }

    private class JLinkButtonEditor implements TableCellEditor, ActionListener {

        private boolean stop = false;

        public void actionPerformed(ActionEvent e) {
            stop = true;
            table.tableChanged(new TableModelEvent(table.getModel()));
        }

        public void addCellEditorListener(CellEditorListener l) {
        }

        public void cancelCellEditing() {
        }

        public Object getCellEditorValue() {
            return null;
        }

        public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
            JLinkButton btn = (JLinkButton) value;
            btn.addActionListener(this);
            return btn;
        }

        public boolean isCellEditable(EventObject anEvent) {
            return true;
        }

        public void removeCellEditorListener(CellEditorListener l) {
        }

        public boolean shouldSelectCell(EventObject anEvent) {
            return false;
        }

        public boolean stopCellEditing() {
            return stop;
        }

    }

    private class JLinkButtonRenderer implements TableCellRenderer {
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            return (JLinkButton) value;
        }
    }

    private static final long serialVersionUID = 1L;

    private ArrayList<PackageData> packageData = new ArrayList<PackageData>();

    private JTable table;

    public SubPanelRessources(Configuration configuration, UIInterface uiinterface) {
        super(uiinterface);
        initPanel();
        load();
    }

    public void actionPerformed(ActionEvent e) {
        for (int i = 0; i < packageData.size(); i++) {
            packageData.get(i).setInstalledVersion(0);
            packageData.get(i).setUpdating(false);
        }
        table.tableChanged(new TableModelEvent(table.getModel()));
    }

    @Override
    public String getName() {
        return JDLocale.L("gui.config.packagemanager.name", "Paketmanager");
    }

    @Override
    public void initPanel() {
        this.setPreferredSize(new Dimension(650, 350));

        packageData = new PackageManager().getPackageData();

        Collections.sort(packageData, new Comparator<PackageData>() {

            public int compare(PackageData a, PackageData b) {
                return ((Integer) a.getSortID()).compareTo((Integer) b.getSortID());
            }

        });

        table = new JTable();
        table.getTableHeader().setPreferredSize(new Dimension(-1, 25));
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        InternalTableModel internalTableModel = new InternalTableModel();
        table.setModel(internalTableModel);
        table.getColumn(table.getColumnName(2)).setCellRenderer(new JLinkButtonRenderer());
        table.getColumn(table.getColumnName(2)).setCellEditor(new JLinkButtonEditor());

        TableColumn column = null;
        for (int c = 0; c < internalTableModel.getColumnCount(); ++c) {
            column = table.getColumnModel().getColumn(c);
            switch (c) {
            case 0:
                column.setPreferredWidth(250);
                break;
            case 1:
                column.setPreferredWidth(60);
                column.setMinWidth(60);
                break;
            case 2:
                column.setPreferredWidth(50);
                break;
            case 3:
                column.setPreferredWidth(30);
                break;
            case 4:
                column.setPreferredWidth(30);
                break;
            case 5:
                column.setPreferredWidth(60);
                column.setMaxWidth(60);
                column.setMinWidth(60);
                break;
            }
        }

        JScrollPane scrollpane = new JScrollPane(table);
        scrollpane.setPreferredSize(new Dimension(400, 200));
        JDUtilities.addToGridBag(panel, scrollpane, GridBagConstraints.RELATIVE, GridBagConstraints.RELATIVE, GridBagConstraints.REMAINDER, 1, 1, 1, insets, GridBagConstraints.BOTH, GridBagConstraints.CENTER);
        JDUtilities.addToGridBag(panel, new GUIConfigEntry(new ConfigEntry(ConfigContainer.TYPE_BUTTON, this, JDLocale.L("gui.config.packagemanager.reset", "Versionsinformationen zurücksetzen"))), GridBagConstraints.RELATIVE, GridBagConstraints.RELATIVE, GridBagConstraints.REMAINDER, 1, 1, 0, insets, GridBagConstraints.NONE, GridBagConstraints.WEST);
        this.add(panel, BorderLayout.CENTER);
    }

    @Override
    public void load() {
        loadConfigEntries();
    }

    @Override
    public void save() {
        saveConfigEntries();
        CFGConfig.getConfig("JDU").save();
    }

}
