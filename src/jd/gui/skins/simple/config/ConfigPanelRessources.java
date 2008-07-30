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
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.EventObject;
import java.util.HashMap;
import java.util.Vector;

import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.event.CellEditorListener;
import javax.swing.event.TableModelEvent;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;

import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.config.Configuration;
import jd.config.SubConfiguration;
import jd.controlling.interaction.PackageManager;
import jd.gui.UIInterface;
import jd.gui.skins.simple.Link.JLinkButton;
import jd.utils.JDLocale;
import jd.utils.JDUtilities;

/**
 * @author JD-Team
 * 
 */
public class ConfigPanelRessources extends ConfigPanel implements MouseListener, ActionListener {

    private class InternalTable extends JTable {

        /**
         * 
         */
        private static final long serialVersionUID = 1L;

    }

    private class InternalTableModel extends AbstractTableModel {

        /**
         * 
         */
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
            HashMap<String, String> element = packageData.elementAt(rowIndex);

            switch (columnIndex) {
            case 0:
                return element.get("name");
            case 1:
                return element.get("category");
            case 2:
                return new JLinkButton(JDLocale.L("gui.config.packagemanager.table.info", "Info"), element.get("infourl"));
            case 3:
                return element.get("version");
            case 4:
                return getInstalledVersion(element);

            case 5:
                return element.get("selected") != null;

            }
            return null;
        }

        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex) {

            return true;
        }

        /*
         * Don't need to implement this method unless your table's data can
         * change.
         */
        @Override
        public void setValueAt(Object value, int row, int col) {
            // logger.info("Set value: " + value);
            // if ((Boolean) value) {
            // if
            // (JDUtilities.getGUI().showConfirmDialog(JDUtilities.sprintf(
            // JDLocale.L("gui.config.plugin.abg_confirm",
            // "Ich habe die AGB/TOS/FAQ von %s gelesen und erkläre mich damit
            // einverstanden!"), new String[] {
            // pluginsForHost.elementAt(row).getHost() })))
            // pluginsForHost.elementAt(row).setAGBChecked((Boolean) value);
            // }
            // else {
            // pluginsForHost.elementAt(row).setAGBChecked((Boolean) value);
            // }

            HashMap<String, String> element = packageData.elementAt(row);
            boolean v = !(element.get("selected") != null);
            config.setProperty("PACKAGE_SELECTED_" + element.get("id"), v);

            element.put("selected", v ? "true" : null);

        }
    }

    private class JLinkButtonEditor implements TableCellEditor, ActionListener {

        private JLinkButton btn;

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

            btn = (JLinkButton) value;
            btn.addActionListener(this);
            return (JLinkButton) value;
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

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    /**
     * 
     */

    // private Configuration configuration;
    private SubConfiguration config;

    private Vector<HashMap<String, String>> packageData = new Vector<HashMap<String, String>>();

    private InternalTable table;

    public ConfigPanelRessources(Configuration configuration, UIInterface uiinterface) {
        super(uiinterface);
        // this.configuration = configuration;
        initPanel();

        load();

    }

    // //Nested Classes

    public void actionPerformed(ActionEvent e) {
        for (int i = 0; i < packageData.size(); i++) {

            config.setProperty("PACKAGE_INSTALLED_VERSION_" + packageData.get(i).get("id"), 0);
        }
        table.tableChanged(new TableModelEvent(table.getModel()));

        config.setProperty("CURRENT_JDU_LIST", new ArrayList<String>());
        config.save();
    }

    public int getInstalledVersion(HashMap<String, String> element) {
        return config.getIntegerProperty("PACKAGE_INSTALLED_VERSION_" + element.get("id"), 0);

    }

    // private class MarkRenderer extends DefaultTableCellRenderer {
    // /**
    // *
    // */
    // private static final long serialVersionUID = -448800592517509052L;
    //
    // public Component getTableCellRendererComponent(JTable table, Object
    // value, boolean isSelected, boolean hasFocus, int row, int column) {
    // Component c = super.getTableCellRendererComponent(table, value,
    // isSelected, hasFocus, row, column);
    // if (!isSelected) {
    //
    // PluginForHost plugin = pluginsForHost.get(row);
    // if (plugin.getConfig().getEntries().size() == 0) {
    // c.setBackground(new Color(0, 0, 0, 10));
    // c.setForeground(new Color(0, 0, 0, 70));
    // }
    // else {
    // c.setBackground(Color.WHITE);
    // c.setForeground(Color.BLACK);
    // }
    //
    // }
    // // logger.info("jj");
    // return c;
    // }
    //
    // }

    @Override
    public String getName() {

        return JDLocale.L("gui.config.packagemanager.name", "Paketmanager");
    }

    @Override
    public void initPanel() {
        setPreferredSize(new Dimension(650, 350));
        config = JDUtilities.getSubConfig("PACKAGEMANAGER");
        packageData = new PackageManager().getPackageData();

        JDUtilities.sortHashVectorOn(packageData, "category");
        ConfigEntry ce;
        // ce = new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, config,
        // "PACKAGEMANAGER_AUTOUPDATE",
        // JDLocale.L("gui.config.packagemanager.doautoupdate", "Ausgewählte
        // Pakete automatisch aktuell halten"));
        // ce.setDefaultValue(true);
        // addGUIConfigEntry(new GUIConfigEntry(ce));
        // ce = new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, config,
        // "PACKAGEMANAGER_EXTRACT_AFTERDOWNLOAD",
        // JDLocale.L("gui.config.packagemanager.doautoupdateafterdownloads",
        // "Geladene Pakete sofort nach dem Download installieren (sonst nach
        // dem Beenden)"));
        // ce.setDefaultValue(false);
        // addGUIConfigEntry(new GUIConfigEntry(ce));
        ce = new ConfigEntry(ConfigContainer.TYPE_BUTTON, this, JDLocale.L("gui.config.packagemanager.reset", "Versionsinformationen zurücksetzen"));
        // addGUIConfigEntry(new GUIConfigEntry(ce));
        table = new InternalTable();
        table.getTableHeader().setPreferredSize(new Dimension(-1, 25));
        // table.setDragEnabled(true);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        // table.setDropMode(DropMode.INSERT_ROWS);

        InternalTableModel internalTableModel = new InternalTableModel();
        table.setModel(internalTableModel);
        // this.setPreferredSize(new Dimension(650, 350));
        // table.getColumn(table.getColumnName(0)).setCellRenderer(new
        // MarkRenderer());
        // table.getColumn(table.getColumnName(1)).setCellRenderer(new
        // MarkRenderer());
        // table.getColumn(table.getColumnName(2)).setCellRenderer(new
        // MarkRenderer());
        // table.getColumn(table.getColumnName(3)).setCellRenderer(new
        // MarkRenderer());
        table.getColumn(table.getColumnName(2)).setCellRenderer(new JLinkButtonRenderer());
        table.getColumn(table.getColumnName(2)).setCellEditor(new JLinkButtonEditor());

        TableColumn column = null;
        for (int c = 0; c < internalTableModel.getColumnCount(); c++) {
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

        // add(scrollPane);
        // list = new JList();
        table.addMouseListener(this);
        JScrollPane scrollpane = new JScrollPane(table);
        scrollpane.setPreferredSize(new Dimension(400, 200));
        JDUtilities.addToGridBag(panel, scrollpane, GridBagConstraints.RELATIVE, GridBagConstraints.RELATIVE, GridBagConstraints.REMAINDER, 1, 1, 1, insets, GridBagConstraints.BOTH, GridBagConstraints.CENTER);
        JDUtilities.addToGridBag(panel, new GUIConfigEntry(ce), GridBagConstraints.RELATIVE, GridBagConstraints.RELATIVE, GridBagConstraints.REMAINDER, 1, 1, 0, insets, GridBagConstraints.NONE, GridBagConstraints.WEST);
        add(panel, BorderLayout.CENTER);
    }

    public boolean isSelectedByUser(HashMap<String, String> elementAt) {

        return false;
    }

    @Override
    public void load() {
        loadConfigEntries();

    }

    public void mouseClicked(MouseEvent e) {

    }

    public void mouseEntered(MouseEvent e) {

    }

    public void mouseExited(MouseEvent e) {

    }

    public void mousePressed(MouseEvent e) {

    }

    public void mouseReleased(MouseEvent e) {

    }

    @Override
    public void save() {
        // logger.info("save");
        saveConfigEntries();
        config.save();
        new PackageManager().interact(this);
    }

    // private class InternalTableCellRenderer extends DefaultTableCellRenderer
    // {
    // /**
    // * serialVersionUID
    // */
    // private static final long serialVersionUID = -3912572910439565199L;
    //     
    // public Component getTableCellRendererComponent(JTable table, Object
    // value, boolean isSelected, boolean hasFocus, int row, int column) {
    // if (value instanceof JCheckBox) return
    // super.getTableCellRendererComponent(table, value, isSelected, hasFocus,
    // row, column);
    // if (value instanceof JLinkButton) return (JLinkButton) value;
    // Component c = super.getTableCellRendererComponent(table, value,
    // isSelected, hasFocus, row, column);
    // if (!isSelected) {
    //            
    // PluginForHost plugin = pluginsForHost.get(row);
    // if (plugin.getConfig().getEntries().size()==0) {
    // c.setBackground(new Color(0,0,0,10));
    // c.setForeground(new Color(0,0,0,70));
    // }else{
    // c.setBackground(Color.WHITE);
    // c.setForeground(Color.BLACK);
    // }
    //               
    //                
    // }
    // // logger.info("jj");
    // return c;
    // }
    // }
}
