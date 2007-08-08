package jd.gui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.util.Iterator;
import java.util.Vector;
import java.util.logging.Logger;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.event.TableModelEvent;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;

import jd.gui.MainWindow.JDAction;
import jd.plugins.DownloadLink;
import jd.plugins.Plugin;
import jd.plugins.event.PluginEvent;
import jd.plugins.event.PluginListener;
/**
 * Diese Tabelle zeigt alle zur Verfügung stehenden Downloads an.
 * 
 * @author astaldo
 */
public class TabDownloadLinks extends JPanel implements PluginListener{
    private final int COL_INDEX    = 0;
    private final int COL_NAME     = 1;
    private final int COL_HOST     = 2;
    private final int COL_PROGRESS = 3;
    
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
     * Der Logger für Meldungen
     */
    private Logger logger = Plugin.getLogger();
    /**
     * Erstellt eine neue Tabelle
     */
    public TabDownloadLinks(){
        super(new BorderLayout());
        table = new JTable();
        table.setModel(internalTableModel);
        table.getColumn(table.getColumnName(COL_PROGRESS)).setCellRenderer(new ProgressBarRenderer());

        TableColumn column = null;
        for (int c = 0; c < internalTableModel.getColumnCount(); c++) {
            column = table.getColumnModel().getColumn(c);
            switch(c){
                case COL_INDEX:    column.setPreferredWidth(30);  break;
                case COL_NAME:     column.setPreferredWidth(200); break;
                case COL_HOST:     column.setPreferredWidth(100); break;
                case COL_PROGRESS: column.setPreferredWidth(150); break;
            }
        }
        
        JScrollPane scrollPane = new JScrollPane(table);
        add(scrollPane);
    }
    /**
     * Hier werden Links zu dieser Tabelle hinzugefügt.
     * 
     * @param links Ein Vector mit Downloadlinks, die alle hinzugefügt werden sollen
     */
    public void addLinks(Vector<DownloadLink> links){
        for(int i=0;i<links.size();i++){
            if(allLinks.indexOf(links.elementAt(i))==-1)
                allLinks.add(links.elementAt(i));
            else
                logger.info("download-URL already in Queue");
        }
        fireTableChanged();
    }
    /**
     * TODO Verschieben von zellen
     * 
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
            if(nextDownloadLink.isEnabled())
                return nextDownloadLink;
        }
        return null;
    }
    /**
     * Hiermit wird die Tabelle aktualisiert
     */
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
        private String labelIndex    = Utilities.getResourceString("label.tab.download.column_index");
        private String labelLink     = Utilities.getResourceString("label.tab.download.column_link");
        private String labelHost     = Utilities.getResourceString("label.tab.download.column_host");
        private String labelProgress = Utilities.getResourceString("label.tab.download.column_progress");
        @Override
        public String getColumnName(int column) {
            switch(column){
                case COL_INDEX:    return labelIndex;
                case COL_NAME:     return labelLink;
                case COL_HOST :    return labelHost;
                case COL_PROGRESS: return labelProgress;
            }
            return null;
        }
        public Class<?> getColumnClass(int columnIndex) {
            switch(columnIndex){
                case COL_INDEX:
//                    return Integer.class;
                case COL_NAME: 
                case COL_HOST: 
                    return String.class;
                case COL_PROGRESS: 
                    return JComponent.class;
            }
            return String.class;    
        }
        public int getColumnCount() {
            return 4;
        }
        public int getRowCount() {
            return allLinks.size();
        }
        public Object getValueAt(int rowIndex, int columnIndex) {
            if(rowIndex< allLinks.size()){
                DownloadLink downloadLink = allLinks.elementAt(rowIndex); 
                switch(columnIndex){
                    case COL_INDEX:    return rowIndex;
                    case COL_NAME:     return downloadLink.getName();
                    case COL_HOST:     return downloadLink.getHost();
                    case COL_PROGRESS:
                        if (downloadLink.isInProgress())
                            return downloadLink.getProgressBar();
                        else
                            return null;
                }
            }
            return null;
        }
    }
    /**
     * Diese Klasse zeichnet eine ProgressBar in eine JTable
     * 
     * @author astaldo
     */
    private class ProgressBarRenderer implements TableCellRenderer{
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column){
            return (JProgressBar)value;
        }
    }
}
