package jd.gui.skins.simple;

import java.awt.BorderLayout;
import java.awt.Component;
import java.util.Vector;

import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.event.TableModelEvent;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableCellRenderer;

import jd.JDUtilities;
import jd.plugins.Plugin;
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
        table.setModel(new InternalTableModel());
        table.getColumn(table.getColumnName(1)).setCellRenderer(new ProgressBarRenderer());

        JScrollPane scrollPane = new JScrollPane(table);
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
            pluginProgress = new PluginProgress(event.getSource());
            pluginProgresses.add(pluginProgress);
        }
        //Hier werden die Ereignisse interpretiert
        switch(event.getEventID()){
            case PluginEvent.PLUGIN_PROGRESS_MAX:
                pluginProgress.setMaximum((Integer)event.getParameter1());
                break;
            case PluginEvent.PLUGIN_PROGRESS_INCREASE:
                pluginProgress.increaseValue();
                break;
            case PluginEvent.PLUGIN_PROGRESS_FINISH:
                break;
        }
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

        public Class<?> getColumnClass(int columnIndex) {
            switch(columnIndex){
                case 0: return String.class;
                case 1: return JProgressBar.class;
            }
            return String.class;
        }
        public int getColumnCount() {
            return 2;
        }
        public int getRowCount() {
            return pluginProgresses.size();
        }
        public Object getValueAt(int rowIndex, int columnIndex) {
            PluginProgress p = pluginProgresses.get(rowIndex);

            switch(columnIndex){
                case 0: return p.plugin.getPluginName();
                case 1: return p.progressBar;
            }
            return null;
        }
        public String getColumnName(int column) {
            switch(column){
                case 0: return labelColumnName;
                case 1: return labelColumnProgress;
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
        private Plugin       plugin;
        /**
         * Eine Fortschrittsanzeige
         */
        private JProgressBar progressBar;
        /**
         * Der aktuelle Wert
         */
        private int          value;
        public PluginProgress(Plugin plugin){
            this(plugin,0,0);
        }
        public PluginProgress(Plugin plugin, int value, int maximum){
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
