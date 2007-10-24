package jd.gui.skins.simple.config;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.File;
import java.util.Vector;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.event.TableModelEvent;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;

import jd.config.Configuration;
import jd.gui.UIInterface;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForSearch;
import jd.plugins.UserPlugin;
import jd.utils.JDUtilities;

/**
 * @author coalado
 *
 */
public class ConfigPanelUserPlugins extends ConfigPanel implements ActionListener, MouseListener {

    /**
     * 
     */
    private static final long serialVersionUID = 5794208138046480006L;

    /**
     * 
     */
  

    private JButton                  btnEdit;

    private JTable                   table;

    private File[] availablePlugins;
    private Configuration configuration;

    private JButton enableDisable;

    private Boolean[] enabledList;


    public ConfigPanelUserPlugins(Configuration configuration, UIInterface uiinterface) {
        super(uiinterface);
        this.configuration=configuration;
        this.availablePlugins = JDUtilities.getAvailableUserPlugins();
        if(availablePlugins==null){
            logger.warning("Keine optionalen Plugins gefunden.");
            availablePlugins= new File[0];
        }
        this.enabledList=new Boolean[availablePlugins.length];
        for(int i=0; i<availablePlugins.length;i++){
            enabledList[i]=configuration.getBooleanProperty(getConfigParamKey(availablePlugins[i]), false);
        }
        initPanel(); 

        load();

    }
    private String getConfigParamKey(File jar){
        return "OPTIONAL_PLUGIN_"+jar.getName();
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

//        for (int i = 0; i < availablePlugins.length; i++) {
//           table.getValueAt(0., column)
//            if (plg.getProperties() != null) configuration.setProperty("PluginConfig_" + plg.getPluginName(), plg.getProperties());
//        }

    }

    @Override
    public void initPanel() {
        setLayout(new BorderLayout());
        table = new InternalTable();
        InternalTableModel internalTableModel = new InternalTableModel();
      
        table.setModel(new InternalTableModel());
        this.setPreferredSize(new Dimension(700, 350));

        TableColumn column = null;
        for (int c = 0; c < internalTableModel.getColumnCount(); c++) {
            column = table.getColumnModel().getColumn(c);
            switch (c) {

                case 0:
                    column.setPreferredWidth(30);
                    break;
                case 1:
                    column.setPreferredWidth(70);
                    break;
                case 2:
                    column.setPreferredWidth(600);
                    break;

            }
        }
        
        // add(scrollPane);
        // list = new JList();
        table.addMouseListener(this);
        JScrollPane scrollpane = new JScrollPane(table);
        scrollpane.setPreferredSize(new Dimension(400, 200));

        btnEdit = new JButton("Einstellungen");
       
        btnEdit.addActionListener(this);
        enableDisable = new JButton("An/Aus");
        
        enableDisable.addActionListener(this);
        JDUtilities.addToGridBag(panel, scrollpane, 0, 0, 3, 1, 1, 1, insets, GridBagConstraints.BOTH, GridBagConstraints.CENTER);

        JDUtilities.addToGridBag(panel, btnEdit, 0, 1, 1, 1, 0, 1, insets, GridBagConstraints.NONE, GridBagConstraints.WEST);
        JDUtilities.addToGridBag(panel, enableDisable, 1, 1, 1, 1, 0, 1, insets, GridBagConstraints.NONE, GridBagConstraints.WEST);

        // JDUtilities.addToGridBag(this, panel,0, 0, 1, 1, 1, 1, insets,
        // GridBagConstraints.BOTH, GridBagConstraints.WEST);
        add(panel, BorderLayout.CENTER);

    }


    @Override
    public String getName() {

        return "Optional Plugins";
    }

    private void openPopupPanel(ConfigPanel config) {
//        JPanel panel = new JPanel(new BorderLayout());
//
////        InteractionTrigger[] triggers = InteractionTrigger.getAllTrigger();
//
//        PluginForSearch plugin = this.getSelectedPlugin();
////        currentPlugin = plugin;
//        if (plugin == null) return;
//
//        JPanel topPanel = new JPanel();
//        panel.add(topPanel, BorderLayout.NORTH);
//        panel.add(config, BorderLayout.CENTER);
//        ConfigurationPopup pop = new ConfigurationPopup(JDUtilities.getParentFrame(this), config, panel, uiinterface, configuration);
//        pop.setLocation(JDUtilities.getCenterOfComponent(this, pop));
//        pop.setVisible(true);
    }

    public void fireTableChanged() {
        int rowIndex=table.getSelectedRow();
       
        table.tableChanged(new TableModelEvent(table.getModel()));
        table.getSelectionModel().addSelectionInterval(rowIndex, rowIndex);
    }

    public void actionPerformed(ActionEvent e) {

        if (e.getSource() == btnEdit) {
            editEntry();

        }
        
        if (e.getSource() == enableDisable) {
            int rowIndex=table.getSelectedRow();
            boolean b=configuration.getBooleanProperty(getConfigParamKey(availablePlugins[rowIndex]), false);
            configuration.setProperty(getConfigParamKey(availablePlugins[rowIndex]), !b);
            fireTableChanged();
        }

    }

    private void editEntry() {
//        PluginForSearch plugin = getSelectedPlugin();
//
//        if (plugin != null && plugin.getConfig().getEntries().size() > 0) {
//
//            openPopupPanel(new ConfigPanelPlugin(configuration, uiinterface, plugin));
//
//        }

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
            switch (columnIndex) {
                case 0:
                    return String.class;
                case 1:
                    return String.class;
                case 2:
                    return String.class;

            }
            return String.class;
        }

        public int getColumnCount() {
            return 3;
        }

        public int getRowCount() {
            return availablePlugins.length;
        }

        public Object getValueAt(int rowIndex, int columnIndex) {

            switch (columnIndex) {
                case 0:
                    return rowIndex+"";
                case 1:
                    return configuration.getBooleanProperty(getConfigParamKey(availablePlugins[rowIndex]), false)?"An":"Aus";
                case 2:
                    return availablePlugins[rowIndex].getName().substring(0,availablePlugins[rowIndex].getName().length()-4);

            }
            return null;
        }

        public String getColumnName(int column) {
            switch (column) {
                case 0:
                    return "ID";
                case 1:
                    return "Status";
                case 2:
                    return "Plugin";

            }
            return super.getColumnName(column);
        }
    }
    
    private class InternalTable extends JTable {
        /**
         * serialVersionUID
         */
        private static final long         serialVersionUID          = 4424930948374806098L;

        private InternalTableCellRenderer internalTableCellRenderer = new InternalTableCellRenderer();
        @Override
        public TableCellRenderer getCellRenderer(int arg0, int arg1) {
            return internalTableCellRenderer;
        }
        
        
   
    }

    
    
    private class InternalTableCellRenderer extends DefaultTableCellRenderer {
        /**
         * serialVersionUID
         */
        private static final long serialVersionUID = -3912572910439565199L;

        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            if (value instanceof JProgressBar) return (JProgressBar) value;
           
            Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            if (!isSelected) {
            boolean b=configuration.getBooleanProperty(getConfigParamKey(availablePlugins[row]), false);
                if (!b) {
                    c.setBackground(new Color(255,0,0,10));
                    c.setForeground(new Color(0,0,0,70));
                   
                }else{
                   
                    c.setBackground(Color.WHITE);
                  c.setForeground(Color.BLACK);
                }
             
                
            }
//          logger.info("jj");
            return c;
        }

    
    }
}
