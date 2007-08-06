package jd.gui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.util.Iterator;
import java.util.Vector;

import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.event.TableModelEvent;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableCellRenderer;

import jd.gui.MainWindow.JDAction;
import jd.plugins.DownloadLink;
import jd.plugins.event.PluginEvent;
import jd.plugins.event.PluginListener;
/**
 * Diese Tabelle zeigt alle zur Verfügung stehenden Downloads an.
 * 
 * @author astaldo
 */
public class TabDownloadLinks extends JPanel implements PluginListener{
    /**
     * serialVersionUID
     */
    private static final long serialVersionUID = 3033753799006526304L;
    /**
     * Diese Tabelle enthält die eigentlichen DownloadLinks
     */
    private JTable             table;
    /**
     * Das interen TableModel, um die Daten anzuzeigen
     */
    private InternalTableModel internalTableModel = new InternalTableModel();
    /**
     * Dieser Vector enthält alle Downloadlinks
     */
    private Vector<DownloadLink> allLinks = new Vector<DownloadLink>();
    /**
     * Erstellt eine neue Tabelle
     */
    public TabDownloadLinks(){
        super(new BorderLayout());
        table = new JTable();
        table.setModel(internalTableModel);
        table.getColumn(table.getColumnName(2)).setCellRenderer(new ProgressBarRenderer());
        JScrollPane scrollPane = new JScrollPane(table);
        add(scrollPane);
    }
    /**
     * Hier werden Links zu dieser Tabelle hinzugefügt.
     * 
     * @param links Ein Vector mit Downloadlinks, die alle hinzugefügt werden sollen
     */
    public void addLinks(Vector<DownloadLink> links){
        allLinks.addAll(links);
        fireTableChanged();
    }
    /**
     * Hiermit werden die selektierten Zeilen innerhalb der Tabelle verschoben
     * 
     * @param direction Zeigt wie/wohin die Einträge verschoben werden sollen
     */
    public void moveItems(int direction){
        int rows[] = table.getSelectedRows();
        switch(direction){
            case JDAction.ITEMS_MOVE_TOP:
                break;
        }
    }
    /**
     * Diese Methode liefert den nächsten Download zurück.
     * 
     * @return Der nächste aktive Download
     */
    public DownloadLink getNextDownloadLink(){
        Iterator<DownloadLink> iterator = allLinks.iterator();
        DownloadLink nextDownloadLink = null;
        while(iterator.hasNext()){
            nextDownloadLink = iterator.next();
            if(nextDownloadLink.isActive())
                return nextDownloadLink;
        }
        return null;
    }
    public void fireTableChanged(){
        table.tableChanged(new TableModelEvent(table.getModel()));
    }
    public void pluginEvent(PluginEvent event) {
        DownloadLink downloadLink=null;
        switch(event.getID()){
            case PluginEvent.PLUGIN_DATA_CHANGED:
                fireTableChanged();
                break;
        }
    }

    /**
     * Dieses TableModel sorgt dafür, daß die Daten der Downloadlinks korrekt dargestellt werden
     * 
     * @author astaldo
     */
    private class InternalTableModel extends AbstractTableModel{
        /**
         * serialVersionUID
         */
        private static final long serialVersionUID = -357970066822953957L;
        private String labelLink     = Utilities.getResourceString("label.tab.download.column_link");
        private String labelHost     = Utilities.getResourceString("label.tab.download.column_host");
        private String labelProgress = Utilities.getResourceString("label.tab.download.column_progress");
        @Override
        public String getColumnName(int column) {
            switch(column){
                case 0: return labelLink;
                case 1: return labelHost;
                case 2: return labelProgress;
            }
            return null;
        }
        public Class<?> getColumnClass(int columnIndex) {
            switch(columnIndex){
                case 0: return String.class;
                case 1: return String.class;
                case 2: return JProgressBar.class;
            }
            return String.class;    
        }
        public int getColumnCount() {
            return 3;
        }
        public int getRowCount() {
            return allLinks.size();
        }
        public Object getValueAt(int rowIndex, int columnIndex) {
            if(rowIndex< allLinks.size()){
                DownloadLink downloadLink = allLinks.elementAt(rowIndex); 
                switch(columnIndex){
                    case 0: return downloadLink.getName();
                    case 1: return downloadLink.getHost();
                    case 2: return downloadLink.getProgressBar();
                }
            }
            return null;
        }
    }
    private class ProgressBarRenderer implements TableCellRenderer{
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column){
            return (JProgressBar)value;
        }
        
    }
}
