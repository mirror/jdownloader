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
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumn;

import jd.config.CFGConfig;
import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.config.Configuration;
import jd.controlling.interaction.PackageManager;
import jd.gui.UIInterface;
import jd.gui.skins.simple.components.JLinkButton;
import jd.update.PackageData;
import jd.utils.JDLocale;

/**
 * @author JD-Team
 */
public class SubPanelRessources extends ConfigPanel implements ActionListener, PropertyChangeListener {

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
                return String.valueOf(element.getInstalledVersion());
            case 5:
                return element.isSelected();
            }
            return null;
        }

        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex) {
            return columnIndex == 2 || columnIndex == 5;
        }

        @Override
        public void setValueAt(Object value, int row, int col) {
            if (col == 5) {
                PackageData element = packageData.get(row);
                element.setSelected(!element.isSelected());
            }
        }
    }

    private static final long serialVersionUID = 1L;

    private JButton btnReset;

    private ConfigEntriesPanel cep;

    private ArrayList<PackageData> packageData = new ArrayList<PackageData>();

    private JTable table;

    private InternalTableModel tableModel;

    public SubPanelRessources(Configuration configuration, UIInterface uiinterface) {
        super(uiinterface);
        initPanel();
        load();
    }

    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == btnReset) {
            for (PackageData pkg : packageData) {
                pkg.setInstalledVersion(0);
                pkg.setUpdating(false);
                pkg.setDownloaded(false);
            }
            tableModel.fireTableDataChanged();
        }
    }

    public void propertyChange(PropertyChangeEvent evt) {
        if (evt.getSource() == cep) {
            cep.save();
        }
    }

    @Override
    public String getName() {
        return JDLocale.L("gui.config.packagemanager.name", "Paketmanager");
    }

    @Override
    public void initPanel() {
        this.setLayout(new BorderLayout());

        packageData = new PackageManager().getPackageData();
        Collections.sort(packageData, new Comparator<PackageData>() {
            public int compare(PackageData a, PackageData b) {
                return (a.getStringProperty("category") + a.getStringProperty("name")).compareToIgnoreCase(b.getStringProperty("category") + b.getStringProperty("name"));
                // return ((Integer) a.getSortID()).compareTo((Integer)
                // b.getSortID());
            }
        });

        tableModel = new InternalTableModel();
        table = new JTable(tableModel);
        table.getTableHeader().setPreferredSize(new Dimension(-1, 25));
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {

            private static final long serialVersionUID = 1L;

            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

                if (isSelected) {
                    c.setBackground(Color.LIGHT_GRAY);
                } else if (column == 0 && packageData.get(row).getInstalledVersion() != 0 && packageData.get(row).getInstalledVersion() < Integer.valueOf(packageData.get(row).getStringProperty("version"))) {
                    c.setBackground(Color.GREEN);
                } else {
                    c.setBackground(Color.WHITE);
                }

                return c;
            }

        });

        TableColumn column = null;
        for (int c = 0; c < tableModel.getColumnCount(); ++c) {
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
                column.setCellRenderer(JLinkButton.getJLinkButtonRenderer());
                column.setCellEditor(JLinkButton.getJLinkButtonEditor());
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

        btnReset = new JButton(JDLocale.L("gui.config.packagemanager.reset", "Versionsinformationen zur�cksetzen"));
        btnReset.addActionListener(this);

        JPanel bpanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 2));
        bpanel.add(btnReset);

        ConfigContainer container = new ConfigContainer(this);
        container.addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, CFGConfig.getConfig("JDU"), "SUPPORT_JD", JDLocale.L("gui.config.packagemanager.supportJD", "Support JD by downloading pumped-up-addons")).setDefaultValue(true).setInstantHelp(JDLocale.L("gui.config.packagemanager.supportJD.instanthelp", "http://wiki.jdownloader.org/index.php?title=Addon-Manager")));
        this.add(cep = new ConfigEntriesPanel(container), BorderLayout.NORTH);
        cep.addPropertyChangeListener(this);
        this.add(scrollpane);
        this.add(bpanel, BorderLayout.SOUTH);
    }

    @Override
    public void load() {
    }

    @Override
    public void save() {
        cep.save();
    }

}
