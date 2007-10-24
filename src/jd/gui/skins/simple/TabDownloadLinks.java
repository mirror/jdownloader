package jd.gui.skins.simple;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.File;
import java.util.Collections;
import java.util.Iterator;
import java.util.Vector;
import java.util.logging.Logger;

import javax.swing.JComponent;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTable;
import javax.swing.event.TableModelEvent;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;

import jd.event.ControlEvent;
import jd.event.ControlListener;
import jd.event.UIEvent;
import jd.plugins.DownloadLink;
import jd.plugins.Plugin;
import jd.plugins.event.PluginEvent;
import jd.plugins.event.PluginListener;
import jd.utils.JDUtilities;

/**
 * Diese Tabelle zeigt alle zur Verfügung stehenden Downloads an.
 * 
 * @author astaldo
 */
public class TabDownloadLinks extends JPanel implements PluginListener, ControlListener, MouseListener {
    private final int            COL_INDEX          = 0;
    private final int            COL_NAME           = 1;
    private final int            COL_HOST           = 2;
    private final int            COL_STATUS         = 3;
    private final int            COL_PROGRESS       = 4;
    private final Color          COLOR_DONE         = new Color(0, 255, 0, 20);
    private final Color          COLOR_ERROR        = new Color(255, 0, 0, 20);
    private final Color          COLOR_DISABLED     = new Color(50, 50, 50, 50);
    private final Color          COLOR_WAIT         = new Color(0, 0, 100, 20);
    /**
     * serialVersionUID
     */
    private static final long    serialVersionUID   = 3033753799006526304L;
    /**
     * Diese Tabelle enthält die eigentlichen DownloadLinks
     */
    private InternalTable        table;
    /**
     * Das interen TableModel, um die Daten anzuzeigen
     */
    private InternalTableModel   internalTableModel = new InternalTableModel();
    /**
     * Dieser Vector enthält alle Downloadlinks
     */
    private Vector<DownloadLink> allLinks           = new Vector<DownloadLink>();
    /**
     * Der Logger für Meldungen
     */
    private Logger               logger             = Plugin.getLogger();
    private JPopupMenu           popup;
    private SimpleGUI            parent;
    /**
     * Erstellt eine neue Tabelle
     * 
     * @param parent Das aufrufende Hauptfenster
     */
    public TabDownloadLinks(SimpleGUI parent) {
        super(new BorderLayout());
        this.parent = parent;
        // Set the component to show the popup menu
        table = new InternalTable();
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        table.addMouseListener(this);
        table.setModel(internalTableModel);
        // table.getColumn(table.getColumnName(COL_PROGRESS)).setCellRenderer(int);
        TableColumn column = null;
        for (int c = 0; c < internalTableModel.getColumnCount(); c++) {
            column = table.getColumnModel().getColumn(c);
            switch (c) {
                case COL_INDEX:
                    column.setPreferredWidth(25);
                    break;
                case COL_NAME:
                    column.setPreferredWidth(260);
                    break;
                case COL_HOST:
                    column.setPreferredWidth(100);
                    break;
                case COL_STATUS:
                    column.setPreferredWidth(200);
                    break;
                case COL_PROGRESS:
                    column.setPreferredWidth(250);
                    break;
            }
        }
        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setPreferredSize(new Dimension(800, 450));
        // table.setPreferredSize(new Dimension(800,450));
        add(scrollPane);
    }
    public void setDownloadLinks(DownloadLink links[]) {
        allLinks.clear();
        addLinks(links);
    }
    // Zeigt das Popup menü an
    private void showPopup(int x, int y, Vector<DownloadLink> downloadLinks) {
        new InternalPopup(table, x, y, downloadLinks);
    }
    private void checkColumnSize() {
        int minSize = 0;
        int width = 0;
        TableColumn tableColumn = table.getColumnModel().getColumn(COL_NAME);
        for (int i = 0; i < allLinks.size(); i++) {
            String name = (String) table.getValueAt(i, COL_NAME);
            width = table.getFontMetrics(table.getFont()).stringWidth(name);
            if (width > minSize) minSize = width;
        }
        width += 5;
        if (width > tableColumn.getWidth()) tableColumn.setPreferredWidth(width);
    }
    /**
     * Hier werden Links zu dieser Tabelle hinzugefügt.
     * 
     * @param links Ein Vector mit Downloadlinks, die alle hinzugefügt werden
     *            sollen
     */
    public void addLinks(DownloadLink links[]) {
        for (int i = 0; i < links.length; i++) {
            if (allLinks.indexOf(links[i]) == -1)
                allLinks.add(links[i]);
            else
                logger.info("download-URL already in Queue");
        }
        checkColumnSize();
        fireTableChanged();
    }
    /**
     * Entfernt die aktuell selektierten Links
     */
    public void removeSelectedLinks() {
        Vector<DownloadLink> linksToDelete = getSelectedObjects();
        allLinks.removeAll(linksToDelete);
        table.getSelectionModel().clearSelection();
        fireTableChanged();
    }
    /**
     * Liefert alle selektierten Links zurück
     * 
     * @return Die selektierten Links
     */
    public Vector<DownloadLink> getSelectedObjects() {
        int rows[] = table.getSelectedRows();
        Vector<DownloadLink> linksSelected = new Vector<DownloadLink>();
        for (int i = 0; i < rows.length; i++) {
            linksSelected.add(allLinks.get(rows[i]));
        }
        return linksSelected;
    }
    public int[] getIndexes(Vector<DownloadLink> selectedLinks) {
        int rows[] = new int[selectedLinks.size()];
        Vector<Integer> indexes = new Vector<Integer>();
        Iterator iterator = selectedLinks.iterator();
        while (iterator.hasNext()) {
            indexes.add(allLinks.indexOf(iterator.next()));
        }
        Collections.sort(indexes);
        for (int i = 0; i < rows.length; i++) {
            rows[i] = indexes.get(i);
        }
        return rows;
    }
    public void setSelectedDownloadLinks(Vector<DownloadLink> selectedDownloadLinks) {
        int index;
        Iterator<DownloadLink> iterator = selectedDownloadLinks.iterator();
        while (iterator.hasNext()) {
            index = allLinks.indexOf(iterator.next());
            table.getSelectionModel().addSelectionInterval(index, index);
        }
    }
    /**
     * 
     * Hiermit werden die selektierten Zeilen innerhalb der Tabelle verschoben
     * 
     * @param direction Zeigt wie/wohin die Einträge verschoben werden sollen
     */
    public void moveSelectedItems(int direction) {
        Vector<DownloadLink> selectedLinks = getSelectedObjects();
        int selectedRows[] = table.getSelectedRows();;
        table.getSelectionModel().clearSelection();
        DownloadLink tempLink;
        switch (direction) {
            case JDAction.ITEMS_MOVE_TOP:
                allLinks.removeAll(selectedLinks);
                allLinks.addAll(0, selectedLinks);
                break;
            case JDAction.ITEMS_MOVE_BOTTOM:
                allLinks.removeAll(selectedLinks);
                allLinks.addAll(allLinks.size(), selectedLinks);
                break;
            case JDAction.ITEMS_MOVE_UP:
                if(selectedRows[0]>0){
                    for(int i=0;i<selectedRows.length;i++){
                        tempLink = allLinks.get(selectedRows[i] - 1);
                        allLinks.set(selectedRows[i] - 1, allLinks.get(selectedRows[i]));
                        allLinks.set(selectedRows[i], tempLink);
                    }
                }
                break;
            case JDAction.ITEMS_MOVE_DOWN:
                if(selectedRows[selectedRows.length-1]+1<allLinks.size()){
                    for(int i=selectedRows.length-1;i>=0;i--){
                        tempLink = allLinks.get(selectedRows[i] + 1);
                        allLinks.set(selectedRows[i] + 1, allLinks.get(selectedRows[i]));
                        allLinks.set(selectedRows[i], tempLink);
                    }
                }
                break;
        }
        fireTableChanged();
        parent.fireUIEvent(new UIEvent(parent, UIEvent.UI_LINKS_CHANGED, null));
        int rows[] = getIndexes(selectedLinks);
        for (int i = 0; i < rows.length; i++) {
            table.getSelectionModel().addSelectionInterval(rows[i], rows[i]);
        }
    }
    public Vector<DownloadLink> getLinks() {
        return allLinks;
    }
    /**
     * Hiermit wird die Tabelle aktualisiert Die Markierte reihe wird nach dem
     * ändern wieder neu gesetzt
     * TODO: Selection verwaltung
     */
    public void fireTableChanged() {
        Vector<DownloadLink> selectedDownloadLinks = getSelectedObjects();
       
        table.tableChanged(new TableModelEvent(table.getModel()));
        setSelectedDownloadLinks(selectedDownloadLinks);
    }
    public void pluginEvent(PluginEvent event) {
        switch (event.getID()) {
            case PluginEvent.PLUGIN_DATA_CHANGED:
                fireTableChanged();
                break;
        }
    }
    public void controlEvent(ControlEvent event) {
        switch (event.getID()) {
            case ControlEvent.CONTROL_SINGLE_DOWNLOAD_CHANGED:
                fireTableChanged();
                break;
        }
    }
    public void mouseClicked(MouseEvent e)  { }
    public void mouseEntered(MouseEvent e)  { }
    public void mouseExited(MouseEvent e)   { }
    public void mouseReleased(MouseEvent e) { }
    public void mousePressed(MouseEvent e) {
        // TODO: isPopupTrigger() funktioniert nicht
        // logger.info("Press"+e.isPopupTrigger() );
        if (e.isPopupTrigger() || e.getButton() == MouseEvent.BUTTON3) {
            Point point = e.getPoint();
            int row = table.rowAtPoint(point);
            if (this.getSelectedObjects().indexOf(allLinks.elementAt(row)) >= 0) {
            }
            else {
                table.getSelectionModel().clearSelection();
                table.getSelectionModel().addSelectionInterval(row, row);
            }
            int x = e.getX();
            int y = e.getY();
            showPopup(x, y, this.getSelectedObjects());
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
    private class InternalPopup extends JPopupMenu implements ActionListener {
        /**
         * 
         */
        private static final long    serialVersionUID = -6561857482676777562L;
        private JMenuItem            delete;
        private JMenuItem            enable;
        private JMenuItem            info;
        private Vector<DownloadLink> downloadLinks;
        private JMenuItem            top;
        private JMenuItem            bottom;
        private JMenuItem            reset;
        private JMenuItem            openHome;
        public InternalPopup(JTable invoker, int x, int y, Vector<DownloadLink> downloadLink) {
            popup = new JPopupMenu();
            this.downloadLinks = downloadLink;
            // Create and add a menu item
            reset = new JMenuItem("Status zurücksetzen");
            delete = new JMenuItem("löschen");
            enable = new JMenuItem(downloadLink.elementAt(0).isEnabled() ? "deaktivieren" : "aktivieren");
            info = new JMenuItem("Info anzeigen");
            top = new JMenuItem("Nach oben");
            bottom = new JMenuItem("Nach unten");
            openHome = new JMenuItem("Zielverzeichnis öffen");
            delete.addActionListener(this);
            reset.addActionListener(this);
            enable.addActionListener(this);
            info.addActionListener(this);
            openHome.addActionListener(this);
            top.addActionListener(this);
            bottom.addActionListener(this);
            if (downloadLink.size() > 1) {
                info.setEnabled(false);
            }
            popup.add(info);
            popup.add(openHome);
            popup.add(new JSeparator());
            popup.add(delete);
            popup.add(enable);
            popup.add(new JSeparator());
            popup.add(top);
            popup.add(bottom);
            popup.add(new JSeparator());
            popup.add(reset);
            popup.show(table, x, y);
        }
        public void actionPerformed(ActionEvent e) {
            if (e.getSource() == openHome) {
                try {
                    logger.info("TODO: Win only");
                    JDUtilities.runCommand("explorer","\"" + new File(downloadLinks.elementAt(0).getFileOutput()).getParent() + "\"",null,0);
                }
                catch (Exception ec) {
                }
            }
            if (e.getSource() == delete) {
                removeSelectedLinks();
                parent.fireUIEvent(new UIEvent(parent, UIEvent.UI_LINKS_CHANGED, null));
            }
            if (e.getSource() == enable) {
                boolean status = downloadLinks.elementAt(0).isEnabled();
                for (int i = 0; i < downloadLinks.size(); i++) {
                    downloadLinks.elementAt(i).setEnabled(!status);
                }
                parent.fireUIEvent(new UIEvent(parent, UIEvent.UI_LINKS_CHANGED, null));
            }
            if (e.getSource() == info) {
                //                Point p=this.getLocationOnScreen();
                //                if(p==null){logger.info("NULL");
                //                }else{
                new DownloadInfo(parent.getFrame(), downloadLinks.elementAt(0));
                //            }
            }
            if (e.getSource() == top) {
                moveSelectedItems(JDAction.ITEMS_MOVE_TOP);
            }
            if (e.getSource() == bottom) {
                moveSelectedItems(JDAction.ITEMS_MOVE_BOTTOM);
            }
            if (e.getSource() == reset) {
                for (int i = 0; i < downloadLinks.size(); i++) {
                    if (!downloadLinks.elementAt(i).isInProgress()) {
                        downloadLinks.elementAt(i).setStatus(DownloadLink.STATUS_TODO);
                        downloadLinks.elementAt(i).setStatusText("");
                        downloadLinks.elementAt(i).reset();
                    }
                }
                fireTableChanged();
                parent.fireUIEvent(new UIEvent(parent, UIEvent.UI_LINKS_CHANGED, null));
            }
        }
    }
    /**
     * Dieses TableModel sorgt dafür, daß die Daten der Downloadlinks korrekt
     * dargestellt werden
     * 
     * @author astaldo
     */
    private class InternalTableModel extends AbstractTableModel {
        /**
         * serialVersionUID
         */
        private static final long    serialVersionUID = -357970066822953957L;
        /**
         * ProgressBar Vectro. Alle progressbars werden einmal angelegt und darin abgespeichert
         */
        private Vector<JProgressBar> progressBars     = new Vector<JProgressBar>();
        private String               labelIndex       = JDUtilities.getResourceString("label.tab.download.column_index");
        private String               labelLink        = JDUtilities.getResourceString("label.tab.download.column_link");
        private String               labelHost        = JDUtilities.getResourceString("label.tab.download.column_host");
        private String               labelStatus      = JDUtilities.getResourceString("label.tab.download.column_status");
        private String               labelProgress    = JDUtilities.getResourceString("label.tab.download.column_progress");
        @Override
        public String getColumnName(int column) {
            switch (column) {
                case COL_INDEX:
                    return labelIndex;
                case COL_NAME:
                    return labelLink;
                case COL_STATUS:
                    return labelStatus;
                case COL_HOST:
                    return labelHost;
                case COL_PROGRESS:
                    return labelProgress;
            }
            return null;
        }
        public Class<?> getColumnClass(int columnIndex) {
            switch (columnIndex) {
                case COL_INDEX:
                    // return Integer.class;
                case COL_NAME:
                case COL_STATUS:
                    return String.class;
                case COL_HOST:
                    return String.class;
                case COL_PROGRESS:
                    return JComponent.class;
            }
            return String.class;
        }
        public int getColumnCount() {
            return 5;
        }
        public int getRowCount() {
            return allLinks.size();
        }
        public Object getValueAt(int rowIndex, int columnIndex) {
            if (rowIndex < allLinks.size()) {
                DownloadLink downloadLink = allLinks.get(rowIndex);
                switch (columnIndex) {
                    case COL_INDEX:
                        return rowIndex;
                    case COL_NAME:
                        if (downloadLink.getFilePackage() == null) {
                            return downloadLink.getName();
                        }
                        return downloadLink.getFilePackage().getDownloadDirectoryName() + "/" + downloadLink.getName();
                    case COL_STATUS:
                        return downloadLink.getStatusText();
                    case COL_HOST:
                        return downloadLink.getHost();
                    case COL_PROGRESS:
                        if (rowIndex >= progressBars.size()) {
                            JProgressBar p = new JProgressBar(0, 1);
                            progressBars.add(rowIndex, p);
                        }
                        JProgressBar p = progressBars.elementAt(rowIndex);
                        if (downloadLink.isInProgress() && downloadLink.getRemainingWaittime() == 0 && (int) downloadLink.getDownloadCurrent() > 0 && (int) downloadLink.getDownloadCurrent() <= (int) downloadLink.getDownloadMax()) {
                            p.setMaximum((int) downloadLink.getDownloadMax());
                            p.setStringPainted(true);
                            p.setBackground(Color.WHITE);
                            p.setValue((int) downloadLink.getDownloadCurrent());
                            p.setString((int) (100 * p.getPercentComplete()) + "% (" + JDUtilities.formatBytesToMB(p.getValue()) + "/" +JDUtilities.formatBytesToMB(p.getMaximum()) + ")");
                            return p;
                        }
                        else if (downloadLink.getRemainingWaittime() > 0 && downloadLink.getWaitTime() >= downloadLink.getRemainingWaittime()) {
                            p.setMaximum(downloadLink.getWaitTime());
                            p.setBackground(new Color(255, 0, 0, 80));
                            p.setStringPainted(true);
                            p.setValue((int) downloadLink.getRemainingWaittime());
                            p.setString((int) (100 * p.getPercentComplete()) + "% (" + p.getValue() / 1000 + "/" + p.getMaximum() / 1000 + " sek)");
                            return p;
                        }
                        else
                            return null;
                }
            }
            return null;
        }
        public DownloadLink getDownloadLinkAtRow(int row) {
            return allLinks.get(row);
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
                DownloadLink dLink = allLinks.get(row);
                if (!dLink.isEnabled()) {
                    c.setBackground(COLOR_DISABLED);
                }
                else if (dLink.getRemainingWaittime() > 0) {
                    c.setBackground(COLOR_WAIT);
                }
                else if (dLink.getStatus() == DownloadLink.STATUS_DONE) {
                    c.setBackground(COLOR_DONE);
                }
                else if (dLink.getStatus() != DownloadLink.STATUS_TODO && dLink.getStatus() != DownloadLink.STATUS_ERROR_DOWNLOAD_LIMIT && dLink.getStatus() != DownloadLink.STATUS_DOWNLOAD_IN_PROGRESS) {
                    c.setBackground(COLOR_ERROR);
                }
                else {
                    c.setBackground(Color.WHITE);
                }
                if (column == 0) {
                    c.setBackground((Color) dLink.getFilePackage().getProperty("color"));
                }
            }
            //          logger.info("jj");
            return c;
        }
    }
}
