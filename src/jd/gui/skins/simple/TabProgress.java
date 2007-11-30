package jd.gui.skins.simple;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.util.Vector;
import java.util.logging.Logger;

import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.event.TableModelEvent;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;

import jd.controlling.ProgressController;
import jd.utils.JDUtilities;

/**
 * Diese Klasse zeigt alle Fortschritte von momenten aktiven Plugins an.
 * 
 * @author coalado
 */
public class TabProgress extends JPanel {
    /**
     * serialVersionUID
     */
    private static final long          serialVersionUID = -8537543161116653345L;

    /**
     * Die Tabelle für die Pluginaktivitäten
     */
    private JTable                     table;

    /**
     * Hier werden alle Fortschritte der Plugins gespeichert
     */

    private Vector<ProgressController> controllers;

    private Vector<JProgressBar>       bars;

    private Logger                     logger           = JDUtilities.getLogger();

    public TabProgress() {
        controllers = new Vector<ProgressController>();
        bars = new Vector<JProgressBar>();
        setLayout(new BorderLayout());
        table = new JTable();
        InternalTableModel internalTableModel;
        table.setModel(internalTableModel=new InternalTableModel());
        table.getColumn(table.getColumnName(1)).setCellRenderer(new ProgressBarRenderer());
        TableColumn column = null;
         for (int c = 0; c < internalTableModel.getColumnCount(); c++) {
         column = table.getColumnModel().getColumn(c);
         switch (c) {
         case 0:
         column.setPreferredWidth(600);
         break;
                   
         case 1:
         column.setPreferredWidth(230);
         break;
        
         }
         }
       this.setVisible(false);
        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setPreferredSize(new Dimension(800, 100));

        add(scrollPane);
    }

    //
    // /**
    // * Hier kann man auf Ereignisse der Plugins reagieren
    // */
    // public void pluginEvent(PluginEvent event) {
    // PluginProgress pluginProgress = null;
    // // Gibts das Plugin bereits?
    // for (int i = 0; i < pluginProgresses.size(); i++) {
    // if (pluginProgresses.get(i).plugin == event.getSource()) {
    // pluginProgress = pluginProgresses.get(i);
    // break;
    // }
    // }
    // // Falls nicht, muß ein beschreibendes Objekt neu angelegt werden
    // if (pluginProgress == null) {
    // pluginProgress = new PluginProgress((Plugin) event.getSource());
    // pluginProgresses.add(pluginProgress);
    // }
    // this.setVisible(true);
    //
    // // Hier werden die Ereignisse interpretiert
    // switch (event.getEventID()) {
    // case PluginEvent.PLUGIN_PROGRESS_MAX:
    //
    // pluginProgress.setMaximum((Integer) event.getParameter1());
    // break;
    // case PluginEvent.PLUGIN_PROGRESS_INCREASE:
    //
    // pluginProgress.increaseValue();
    // break;
    // case PluginEvent.PLUGIN_PROGRESS_FINISH:
    // pluginProgress.setMaximum(-1);
    // boolean active = false;
    // for (int i = pluginProgresses.size() - 1; i >= 0; i--) {
    // if (pluginProgresses.get(i).progressBar.getPercentComplete() < 1.0) {
    // active = true;
    // }
    // else {
    // try {
    // Thread.sleep(100);
    // }
    // catch (InterruptedException e) {
    // }
    // pluginProgresses.remove(i);
    //
    // }
    // }
    // if (!active) {
    //
    // setVisible(false);
    //
    // }
    // break;
    // }
    // // Prüfe ob es noch aktive gibt, und entfernt fertige
    //
    // table.tableChanged(new TableModelEvent(table.getModel()));
    // }

    /**
     * Das TableModel ist notwendig, um die Daten darzustellen
     * 
     * @author astaldo
     */
    private class InternalTableModel extends AbstractTableModel {
        /**
         * serialVersionUID
         */
        private static final long serialVersionUID      = 8135707376690458846L;

        /**
         * Bezeichnung der Spalte für den Pluginnamen
         */
        private String            labelColumnStatusText = JDUtilities.getResourceString("label.tab.plugin_activity.column_plugin");

        /**
         * Bezeichnung der Spalte für die Fortschrittsanzeige
         */
        private String            labelColumnProgress   = JDUtilities.getResourceString("label.tab.plugin_activity.column_progress");

        public String toString() {
            return " col " + controllers.size();
        }

        public Class<?> getColumnClass(int columnIndex) {
            switch (columnIndex) {

                case 0:
                    return String.class;
                case 1:
                    return JProgressBar.class;
            }
            return String.class;
        }

        public int getColumnCount() {
            return 2;
        }

        public int getRowCount() {
            
            return controllers.size();
        }

        public Object getValueAt(int rowIndex, int columnIndex) {

            if (controllers.size() <= rowIndex) return null;
            ProgressController p = controllers.get(rowIndex);
            if (bars.size() <= rowIndex) return null;
            JProgressBar b = bars.get(rowIndex);
            switch (columnIndex) {
                case 0:
                    return p.getStatusText();
                case 1:
                    return b;

            }
            return null;
        }

        public String getColumnName(int column) {
            switch (column) {
                case 0:
                    return labelColumnStatusText;

                case 1:
                    return labelColumnProgress;
            }
            return super.getColumnName(column);
        }
    }

    // /**
    // * Diese Klasse sorgt lediglich dafür, die Informationen zum Fortschritt
    // * eines Plugin festzuhalten
    // *
    // * @author astaldo
    // */
    // private class PluginProgress {
    // /**
    // * Das Plugin, für das die Informationen gelten
    // */
    // private Plugin plugin;
    //
    // /**
    // * Eine Fortschrittsanzeige
    // */
    // private JProgressBar progressBar;
    //
    // /**
    // * Der aktuelle Wert
    // */
    // private int value;
    //
    // public PluginProgress(Plugin plugin) {
    // this(plugin, 0, 0);
    // }
    //
    // public PluginProgress(Plugin plugin, int value, int maximum) {
    // this.plugin = plugin;
    // this.progressBar = new JProgressBar();
    // this.progressBar.setMaximum(maximum);
    // this.progressBar.setValue(value);
    // this.progressBar.setStringPainted(true);
    // }
    //
    // /**
    // * Legt das Maximum der Fortschrittsanzeige fest
    // *
    // * @param maximum Maximum-Wert
    // */
    // public void setMaximum(int maximum) {
    // if(maximum<0)maximum=progressBar.getMaximum();
    // this.progressBar.setMaximum(maximum);
    // }
    //
    // /**
    // * Erhöht die Fortschrittsanzeige um eins
    // */
    // public void increaseValue() {
    // this.value++;
    // this.progressBar.setValue(value);
    // }
    // }

    /**
     * Diese Klasse zeichnet eine JProgressBar in der Tabelle
     * 
     * @author astaldo
     */
    private class ProgressBarRenderer implements TableCellRenderer {
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            return (JProgressBar) value;
        }

    }

    public boolean hasController(ProgressController source) {
        return controllers.contains(source);
    }

    public void addController(ProgressController source) {
        this.controllers.add(0,source);

       
        JProgressBar progressBar = new JProgressBar();
        progressBar.setMaximum(source.getMax());
        progressBar.setValue(source.getValue());
        progressBar.setStringPainted(true);
        bars.add(0,progressBar);
        updateController(source);

    }

    public void removeController(ProgressController source) {
       int index=controllers.indexOf(source);
       
      bars.remove(index);
        controllers.remove(source);
        updateController(source);
   

    }

    public void updateController(ProgressController source) {
 if(source==null){
     table.tableChanged(new TableModelEvent(table.getModel()));
     return;
 }
        if (controllers.size() > 0) {
            if (source.isFinished()) {

            }
            else {
                this.setVisible(true);

                bars.get(controllers.indexOf(source)).setMaximum(source.getMax());
                bars.get(controllers.indexOf(source)).setValue(source.getValue());

            }

        }
        else {
            this.setVisible(false);
        }

        table.tableChanged(new TableModelEvent(table.getModel()));

    }

    public Vector<ProgressController> getControllers() {

        return controllers;
    }
}
