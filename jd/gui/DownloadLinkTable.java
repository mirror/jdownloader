package jd.gui;

import java.util.Vector;

import javax.swing.JTable;
import javax.swing.event.TableModelEvent;
import javax.swing.table.AbstractTableModel;

import jd.plugins.DownloadLink;
/**
 * Diese Tabelle zeigt alle zur Verfügung stehenden Downloads an.
 * 
 * @author astaldo
 */
public class DownloadLinkTable extends JTable{
    /**
     * serialVersionUID
     */
    private static final long serialVersionUID = 3033753799006526304L;
    /**
     * Dieser Vector enthält alle Downloadlinks
     */
    private Vector<DownloadLink> allLinks = new Vector<DownloadLink>();
    /**
     * Erstellt eine neue Tabelle
     */
    public DownloadLinkTable(){
        super();
        setModel(new InternalTableModel());
    }
    /**
     * Hier werden Links zu dieser Tabelle hinzugefügt.
     * 
     * @param links Ein Vector mit Downloadlinks, die alle hinzugefügt werden sollen
     */
    public void addLinks(Vector<DownloadLink> links){
        allLinks.addAll(links);
        tableChanged(new TableModelEvent(getModel()));
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
        @Override
        public String getColumnName(int column) {
            switch(column){
                case 0: return "DL Link";
                case 1: return "Host";
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
                    case 0:
                        return allLinks.elementAt(rowIndex).getName();
                    case 1:
                        return allLinks.elementAt(rowIndex).getHost();
                }
            }
            return null;
        }
    }
}
