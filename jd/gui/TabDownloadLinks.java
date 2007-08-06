package jd.gui;

import java.awt.BorderLayout;
import java.util.Vector;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.event.TableModelEvent;
import javax.swing.table.AbstractTableModel;

import jd.gui.MainWindow.JDAction;
import jd.plugins.DownloadLink;
/**
 * Diese Tabelle zeigt alle zur Verfügung stehenden Downloads an.
 * 
 * @author astaldo
 */
public class TabDownloadLinks extends JPanel{
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
    public void fireTableChanged(){
        table.tableChanged(new TableModelEvent(table.getModel()));
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
        private String labelLink = Utilities.getResourceString("label.tab.download.column_link");
        private String labelHost = Utilities.getResourceString("label.tab.download.column_host");
        @Override
        public String getColumnName(int column) {
            switch(column){
                case 0: return labelLink;
                case 1: return labelHost;
            }
            return null;
        }
        public int getColumnCount() {
            return 2;
        }
        public int getRowCount() {
            return allLinks.size();
        }
        public Object getValueAt(int rowIndex, int columnIndex) {
            if(rowIndex< allLinks.size()){
                switch(columnIndex){
                    case 0: return allLinks.elementAt(rowIndex).getName();
                    case 1: return allLinks.elementAt(rowIndex).getHost();
                }
            }
            return null;
        }
    }
}
