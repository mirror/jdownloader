package jd.gui.skins.simple;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.util.Vector;

import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.event.TableModelEvent;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;

import jd.JDUtilities;
import jd.plugins.PluginForDecrypt;
import jd.plugins.event.PluginEvent;
import jd.plugins.event.PluginListener;
/**
 * Diese Klasse zeigt alle Fortschritte von momenten aktiven Plugins an.
 *
 * @author astaldo
 */
public class TabPluginActivity extends JPanel implements PluginListener{
    /**
     * serialVersionUID
     */
    private static final long serialVersionUID = -8537543161116653345L;
    /**
     * Die Tabelle für die Pluginaktivitäten
     */
    private JTable table;
    /**
     * Hier werden alle Fortschritte der Plugins gespeichert
     */
    private Vector<PluginProgress> pluginProgresses = new Vector<PluginProgress>();
    public TabPluginActivity(){
        setLayout(new BorderLayout());
        table = new JTable();
        InternalTableModel internalTableModel=new InternalTableModel();
        table.setModel(new InternalTableModel());
        table.getColumn(table.getColumnName(3)).setCellRenderer(new ProgressBarRenderer());
        TableColumn column = null;
        for (int c = 0; c < internalTableModel.getColumnCount(); c++) {
            column = table.getColumnModel().getColumn(c);
            switch(c){
                case 0:    column.setPreferredWidth(230);  break;
                case 1:     column.setPreferredWidth(150); break;
                case 2:     column.setPreferredWidth(200); break;
                case 3: column.setPreferredWidth(250); break;
               
                
            }
        }
       this.setVisible(false);
       JScrollPane scrollPane = new JScrollPane(table);
       scrollPane.setPreferredSize(new Dimension(800,100));
      
        add(scrollPane);
    }
   
    /**
     * Hier kann man auf Ereignisse der Plugins reagieren
     */
    public void pluginEvent(PluginEvent event) {
        PluginProgress pluginProgress = null;
        // Gibts das Plugin bereits?
        for(int i=0;i<pluginProgresses.size();i++){
            if(pluginProgresses.get(i).plugin == event.getSource()){
                pluginProgress = pluginProgresses.get(i);
                break;
            }
        }
        // Falls nicht, muß ein beschreibendes Objekt neu angelegt werden
        if(pluginProgress == null){
            pluginProgress = new PluginProgress((PluginForDecrypt)event.getSource());
            pluginProgresses.add(pluginProgress);
        }
       this.setVisible(true);
   
     
        //Hier werden die Ereignisse interpretiert
        switch(event.getEventID()){
            case PluginEvent.PLUGIN_PROGRESS_MAX:
                
                pluginProgress.setMaximum((Integer)event.getParameter1());
                break;
            case PluginEvent.PLUGIN_PROGRESS_INCREASE:
                this.setVisible(true);
                pluginProgress.increaseValue();
                break;
            case PluginEvent.PLUGIN_PROGRESS_FINISH:
                
                break;
        }
        //Prüfe ob es noch aktive gibt, und entfernt fertige
        boolean active=false;
        for(int i=pluginProgresses.size()-1;i>=0;i--){
            if(pluginProgresses.get(i).progressBar.getPercentComplete()<1.0){
                active=true;                
            }else{
                pluginProgresses.remove(i);  
            }
        }
        if(!active)this.setVisible(false);
        table.tableChanged(new TableModelEvent(table.getModel()));
    }
    /**
     * Das TableModel ist notwendig, um die Daten darzustellen
     *
     * @author astaldo
     */
    private class InternalTableModel extends AbstractTableModel{
        /**
         * serialVersionUID
         */
        private static final long serialVersionUID = 8135707376690458846L;
        /**
         * Bezeichnung der Spalte für den Pluginnamen
         */
        private String labelColumnName     = JDUtilities.getResourceString("label.tab.plugin_activity.column_plugin");
        /**
         * Bezeichnung der Spalte für die Fortschrittsanzeige
         */
        private String labelColumnProgress = JDUtilities.getResourceString("label.tab.plugin_activity.column_progress");

        private String labelColumnLink = JDUtilities.getResourceString("label.tab.plugin_activity.column_link");
        private String labelColumnStatus = JDUtilities.getResourceString("label.tab.plugin_activity.column_status");
        public Class<?> getColumnClass(int columnIndex) {
            switch(columnIndex){
                case 0: return String.class;
                case 1: return String.class;
                case 2: return String.class;
                case 3: return JProgressBar.class;
            }
            return String.class;
        }
        public int getColumnCount() {
            return 4;
        }
        public int getRowCount() {
            return pluginProgresses.size();
        }
        public Object getValueAt(int rowIndex, int columnIndex) {
            PluginProgress p = pluginProgresses.get(rowIndex);

            switch(columnIndex){
                case 0: return p.plugin.getLinkName();
                case 1: return p.plugin.getPluginName();
                case 2: return "("+p.progressBar.getValue()+"/"+p.progressBar.getMaximum()+")"+p.plugin.getStatusText();
                case 3: return p.progressBar;
            }
            return null;
        }
        public String getColumnName(int column) {
            switch(column){
                case 0: return labelColumnLink;
                case 1: return labelColumnName;
                case 2: return labelColumnStatus;
                case 3: return labelColumnProgress;
            }
            return super.getColumnName(column);
        }
    }
    /**
     * Diese Klasse sorgt lediglich dafür, die Informationen zum Fortschritt eines Plugin festzuhalten
     *
     * @author astaldo
     */
    private class PluginProgress{
        /**
         * Das Plugin, für das die Informationen gelten
         */
        private PluginForDecrypt       plugin;
        /**
         * Eine Fortschrittsanzeige
         */
        private JProgressBar progressBar;
        /**
         * Der aktuelle Wert
         */
        private int          value;
        public PluginProgress(PluginForDecrypt plugin){
            this(plugin,0,0);
        }
        public PluginProgress(PluginForDecrypt plugin, int value, int maximum){
            this.plugin = plugin;
            this.progressBar = new JProgressBar();
            this.progressBar.setMaximum(maximum);
            this.progressBar.setValue(value);
            this.progressBar.setStringPainted(true);
        }
        /**
         * Legt das Maximum der Fortschrittsanzeige fest
         *
         * @param maximum Maximum-Wert
         */
        public void setMaximum(int maximum){
            this.progressBar.setMaximum(maximum);
        }
        
      
        /**
         * Erhöht die Fortschrittsanzeige um eins
         */
        public void increaseValue(){
            this.value++;
            this.progressBar.setValue(value);
        }
    }
    /**
     * Diese Klasse zeichnet eine JProgressBar in der Tabelle
     *
     * @author astaldo
     */
    private class ProgressBarRenderer implements TableCellRenderer{
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column){
            return (JProgressBar)value;
        }

    }
}
