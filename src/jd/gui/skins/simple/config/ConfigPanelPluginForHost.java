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
import java.awt.GridBagConstraints;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.dnd.DropTargetEvent;
import java.awt.dnd.DropTargetListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.EventObject;
import java.util.Vector;

import javax.swing.DefaultListSelectionModel;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.event.CellEditorListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableModelEvent;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;

import jd.config.Configuration;
import jd.gui.UIInterface;
import jd.gui.skins.simple.Link.JLinkButton;
import jd.plugins.PluginForHost;
import jd.utils.JDLocale;
import jd.utils.JDUtilities;

public class ConfigPanelPluginForHost extends ConfigPanel implements ActionListener, MouseListener, DropTargetListener {

    /**
     * 
     */
    private static final long     serialVersionUID = -5219586497809869375L;

    private JButton               btnEdit;

    private Configuration         configuration;

    private JTable                table;

    private Vector<PluginForHost> pluginsForHost;

   // private PluginForHost         currentPlugin;

    private PluginForHost         draggedPlugin;

    public ConfigPanelPluginForHost(Configuration configuration, UIInterface uiinterface) {
        super(uiinterface);
        this.configuration = configuration;

        this.pluginsForHost = JDUtilities.getPluginsForHost();

        initPanel();

        load();

    }

    /**
     * Lädt alle Informationen
     */
    public void load() {

    }

    /**
     * Speichert alle Änderungen auf der Maske
     */
    public void save() {
        // Interaction[] tmp= new Interaction[interactions.size()];
        PluginForHost plg;
        Vector<String> priority = new Vector<String>();
        for (int i = 0; i < pluginsForHost.size(); i++) {

            plg = pluginsForHost.elementAt(i);
            priority.add(plg.getHost());
            if (plg.getProperties() != null) configuration.setProperty("PluginConfig_" + plg.getPluginName(), plg.getProperties());

        }
        configuration.setProperty(Configuration.PARAM_HOST_PRIORITY, priority);

    }

    @Override
    public void initPanel() {
        setLayout(new BorderLayout());
        table = new JTable(); // new InternalTable();
        table.getTableHeader().setPreferredSize(new Dimension(-1, 25));
        table.setDragEnabled(true);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        // table.setDropMode(DropMode.INSERT_ROWS);
        new DropTarget(table, this);
        InternalTableModel internalTableModel = new InternalTableModel();
        table.setModel(internalTableModel);
        this.setPreferredSize(new Dimension(650, 350));
//        table.getColumn(table.getColumnName(0)).setCellRenderer(new MarkRenderer());
//        table.getColumn(table.getColumnName(1)).setCellRenderer(new MarkRenderer());
//        table.getColumn(table.getColumnName(2)).setCellRenderer(new MarkRenderer());
//        table.getColumn(table.getColumnName(3)).setCellRenderer(new MarkRenderer());
        table.getColumn(table.getColumnName(4)).setCellRenderer(new JLinkButtonRenderer());
        table.getColumn(table.getColumnName(4)).setCellEditor(new JLinkButtonEditor());

        TableColumn column = null;
        for (int c = 0; c < internalTableModel.getColumnCount(); c++) {
            column = table.getColumnModel().getColumn(c);
            switch (c) {
                case 0:
                    column.setPreferredWidth(30);
                    column.setMaxWidth(30);
                    column.setMinWidth(30);
                    break;
                case 1:
                    column.setPreferredWidth(250);
                    break;
                case 2:
                    column.setPreferredWidth(60);
                    column.setMinWidth(60);
                    break;
                case 3:
                    column.setPreferredWidth(250);
                    break;
                case 4:
                    column.setPreferredWidth(70);
                    column.setMaxWidth(70);
                    column.setMinWidth(70);
                    break;
                case 5:
                    column.setPreferredWidth(90);
                    column.setMaxWidth(90);
                    column.setMinWidth(90);
                    break;

            }
            logger.info(table.getColumn(table.getColumnName(4)).getCellRenderer() + "");
        }

        // add(scrollPane);
        // list = new JList();
        table.addMouseListener(this);
        JScrollPane scrollpane = new JScrollPane(table);
        scrollpane.setPreferredSize(new Dimension(400, 200));

        btnEdit = new JButton(JDLocale.L("gui.config.plugin.host.btn_settings", "Einstellungen"));
        table.getSelectionModel().addListSelectionListener(new ListSelectionListener(){
            public void valueChanged(ListSelectionEvent e) {
                  System.out.println(e.getSource());
                  DefaultListSelectionModel model = (DefaultListSelectionModel) e.getSource();
                  if (pluginsForHost.get(model.getMinSelectionIndex()).getConfig().getEntries().size() != 0) btnEdit.setEnabled(true);
                  else btnEdit.setEnabled(false);
            }
        });
        btnEdit.setEnabled(false);
        btnEdit.addActionListener(this);

        JLabel disclaimerLabel = new JLabel("<html><body align=\"justify\" color=\"red\"><b>" + JDLocale.L("gui.config.plugin.host.desc", "ACHTUNG!! Das JD Team übernimmt keine Verantwortung für die Einhaltung der AGB \r\n der Hoster. Bitte lesen Sie die AGB aufmerksam und aktivieren Sie das Plugin nur,\r\nfalls Sie sich mit diesen Einverstanden erklären!\r\nDie Reihenfolge der Plugins bestimmt die Prioritäten der automatischen Mirrorauswahl\n\rBevorzugte Hoster sollten oben stehen!"));
        // JDUtilities.addToGridBag(panel, (new JTextArea(JDLocale.L("gui.config.plugin.host.desc", "ACHTUNG!! Das JD Team übernimmt keine Verantwortung für die Einhaltung der AGB \r\n der Hoster. Bitte lesen Sie die AGB aufmerksam und aktivieren Sie das Plugin nur,\r\nfalls Sie sich mit diesen Einverstanden erklären!\r\nDie Reihenfolge der Plugins bestimmt die Prioritäten der automatischen Mirrorauswahl\n\rBevorzugte Hoster sollten oben stehen!"))), GridBagConstraints.RELATIVE, GridBagConstraints.RELATIVE, GridBagConstraints.REMAINDER, 1, 1, 0, insets, GridBagConstraints.NONE, GridBagConstraints.CENTER);
        
        JDUtilities.addToGridBag(panel, scrollpane, GridBagConstraints.RELATIVE, GridBagConstraints.RELATIVE, GridBagConstraints.REMAINDER, 1, 1, 1, insets, GridBagConstraints.BOTH, GridBagConstraints.CENTER);

        JDUtilities.addToGridBag(panel, btnEdit, GridBagConstraints.RELATIVE, GridBagConstraints.RELATIVE, GridBagConstraints.REMAINDER, 1, 1, 0, insets, GridBagConstraints.NONE, GridBagConstraints.WEST);

        // JDUtilities.addToGridBag(this, panel,0, 0, 1, 1, 1, 1, insets,
        // GridBagConstraints.BOTH, GridBagConstraints.WEST);
        add(disclaimerLabel, BorderLayout.NORTH);
        add(panel, BorderLayout.CENTER);

    }

    private int getSelectedInteractionIndex() {
        return table.getSelectedRow();
    }

    @Override
    public String getName() {

        return JDLocale.L("gui.config.plugin.host.name", "Host Plugins");
    }

    private void openPopupPanel(ConfigPanel config) {
        JPanel panel = new JPanel(new BorderLayout());

        // InteractionTrigger[] triggers = InteractionTrigger.getAllTrigger();

        PluginForHost plugin = this.getSelectedPlugin();
       // currentPlugin = plugin;
        if (plugin == null) return;

        JPanel topPanel = new JPanel();
        panel.add(topPanel, BorderLayout.NORTH);
        panel.add(config, BorderLayout.CENTER);
        ConfigurationPopup pop = new ConfigurationPopup(JDUtilities.getParentFrame(this), config, panel, uiinterface, configuration);
        pop.setLocation(JDUtilities.getCenterOfComponent(this, pop));
        pop.setVisible(true);
    }

    private PluginForHost getSelectedPlugin() {
        int index = getSelectedInteractionIndex();
        if (index < 0) return null;
        return this.pluginsForHost.elementAt(index);
    }

    public void actionPerformed(ActionEvent e) {

        if (e.getSource() == btnEdit) {
            editEntry();

        }

    }

    private void editEntry() {
        PluginForHost plugin = getSelectedPlugin();

        if (plugin != null && plugin.getConfig().getEntries().size() > 0) {

            openPopupPanel(new ConfigPanelPlugin(configuration, uiinterface, plugin));

        }

    }

    public void mouseClicked(MouseEvent e) {
        if (e.getClickCount() > 1) {
            editEntry();
        }

    }

    public void mouseEntered(MouseEvent e) {

    }

    public void mouseExited(MouseEvent e) {

    }

    public void mousePressed(MouseEvent e) {

    }

    public void mouseReleased(MouseEvent e) {

    }

    private class InternalTableModel extends AbstractTableModel {

        /**
         * 
         */
        private static final long serialVersionUID = 1155282457354673850L;

        public Class<?> getColumnClass(int columnIndex) {
            return getValueAt(0, columnIndex).getClass();

        }

        public boolean isCellEditable(int rowIndex, int columnIndex) {

            return columnIndex > 2;
        }

        public int getColumnCount() {

            return 6;
        }

        public int getRowCount() {
            return pluginsForHost.size();
        }

        public Object getValueAt(int rowIndex, int columnIndex) {

            switch (columnIndex) {
                case 0:
                    return rowIndex;
                case 1:
                    return pluginsForHost.elementAt(rowIndex).getPluginName();
                case 2:
                    return pluginsForHost.elementAt(rowIndex).getVersion();
                case 3:
                    return pluginsForHost.elementAt(rowIndex).getCoder();
                case 4:
                    return new JLinkButton(JDLocale.L("gui.config.plugin.host.readAGB", "AGB"), pluginsForHost.elementAt(rowIndex).getAGBLink());
                case 5:
                    return pluginsForHost.elementAt(rowIndex).isAGBChecked();

            }
            return null;
        }

        /*
         * Don't need to implement this method unless your table's data can
         * change.
         */
        public void setValueAt(Object value, int row, int col) {
            logger.info("Set value: " + value);
            if ((Boolean) value) {
                String msg=String.format(JDLocale.L("gui.config.plugin.abg_confirm", "Ich habe die AGB/TOS/FAQ von %s gelesen und erkläre mich damit einverstanden!"), pluginsForHost.elementAt(row).getHost() );
                
                if(JOptionPane.showConfirmDialog(ConfigurationDialog.DIALOG,msg)==JOptionPane.OK_OPTION) pluginsForHost.elementAt(row).setAGBChecked((Boolean) value);
            }
            else {
                pluginsForHost.elementAt(row).setAGBChecked((Boolean) value);
            }
        }

        public String getColumnName(int column) {
            switch (column) {
                case 0:
                    return JDLocale.L("gui.config.plugin.host.column_id2", "ID");
                case 1:
                    return JDLocale.L("gui.config.plugin.host.column_host2", "Host");
                case 2:
                    return JDLocale.L("gui.config.plugin.host.column_version2", "Version");
                case 3:
                    return JDLocale.L("gui.config.plugin.host.column_coder2", "Ersteller");
                case 4:
                    return JDLocale.L("gui.config.plugin.host.column_agb2", "AGB");
                case 5:
                    return JDLocale.L("gui.config.plugin.host.column_agbChecked2", "akzeptieren");

            }
            return super.getColumnName(column);
        }
    }

    private class InternalTable extends JTable {
        /**
         * serialVersionUID
         */
        private static final long serialVersionUID = 4424930948374806098L;

        // private InternalTableCellRenderer internalTableCellRenderer = new
        // InternalTableCellRenderer();
        // @Override
        // public TableCellRenderer getCellRenderer(int arg0, int arg1) {
        // return internalTableCellRenderer;
        // }

    }

    private class MarkRenderer extends DefaultTableCellRenderer {
        /**
		 * 
		 */
		private static final long serialVersionUID = -448800592517509052L;

		public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            if (!isSelected) {

                PluginForHost plugin = pluginsForHost.get(row);
                if (plugin.getConfig().getEntries().size() == 0) {
                    c.setBackground(new Color(0, 0, 0, 10));
                    c.setForeground(new Color(0, 0, 0, 70));
                }
                else {
                    c.setBackground(Color.WHITE);
                    c.setForeground(Color.BLACK);
                }

            }
            // logger.info("jj");
            return c;
        }

    }

    private class JLinkButtonRenderer implements TableCellRenderer {
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            return (JLinkButton) value;
        }

    }

    private class JLinkButtonEditor implements TableCellEditor, ActionListener {

        private JLinkButton btn;

        private boolean     stop = false;

        public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
            // TODO Auto-generated method stub

            this.btn = (JLinkButton) value;
            btn.addActionListener(this);
            return (JLinkButton) value;
        }

        public void addCellEditorListener(CellEditorListener l) {

        }

        public void cancelCellEditing() {

        }

        public Object getCellEditorValue() {

            return null;
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

        public void actionPerformed(ActionEvent e) {

            this.stop = true;
            table.tableChanged(new TableModelEvent(table.getModel()));
            // this.stopCellEditing();

        }

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

    public void dragEnter(DropTargetDragEvent e) {
        int[] draggedRows = table.getSelectedRows();
        this.draggedPlugin = pluginsForHost.get(draggedRows[0]);

    }

    public void dragExit(DropTargetEvent dte) {
    // TODO Auto-generated method stub

    }

    public void dragOver(DropTargetDragEvent e) {
        // TODO Auto-generated method stub
        int id = table.rowAtPoint(e.getLocation());

        // table.setSelectionModel(newModel)
        // table.getSelectionModel().clearSelection();
        // table.getSelectionModel().addSelectionInterval(id, id);
        pluginsForHost.remove(draggedPlugin);
        pluginsForHost.add(id, draggedPlugin);
        table.tableChanged(new TableModelEvent(table.getModel()));
        table.getSelectionModel().clearSelection();
        table.getSelectionModel().addSelectionInterval(id, id);
    }

    public void drop(DropTargetDropEvent e) {

        logger.info("insert at " + table.rowAtPoint(e.getLocation()));
        try {

            // e.dropComplete(true);
        }
        catch (Exception exc) {
            // e.rejectDrop();
            exc.printStackTrace();
        }

    }

    public void dropActionChanged(DropTargetDragEvent dtde) {
    // TODO Auto-generated method stub

    }
    // private class CheckBoxRenderer implements TableCellRenderer {
    // public Component getTableCellRendererComponent(JTable table, Object
    // value, boolean isSelected, boolean hasFocus, int row, int column) {
    // logger.info("BLABLA"+value);
    // return (JCheckBox) value;
    // }
    //
    // }
    //    
    // private class LinkButtonRenderer implements TableCellRenderer {
    // public Component getTableCellRendererComponent(JTable table, Object
    // value, boolean isSelected, boolean hasFocus, int row, int column) {
    // return (JLinkButton) value;
    // }
    //
    // }

}
