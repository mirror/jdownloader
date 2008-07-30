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


package jd.gui.skins.simple;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Vector;

import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.Timer;
import javax.swing.event.TableModelEvent;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;

import jd.controlling.ProgressController;
import jd.utils.JDLocale;

/**
 * Diese Klasse zeigt alle Fortschritte von momenten aktiven Plugins an.
 * 
 * @author JD-Team
 */
public class TabProgress extends JPanel implements ActionListener {
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
         * Bezeichnung der Spalte für die Fortschrittsanzeige
         */
        private String            labelColumnProgress   = JDLocale.L("gui.tab.plugin_activity.column_progress");

        /**
         * Bezeichnung der Spalte für den Pluginnamen
         */
        private String            labelColumnStatusText = JDLocale.L("gui.tab.plugin_activity.column_plugin");

        @Override
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

        @Override
        public String getColumnName(int column) {
            switch (column) {
                case 0:
                    return labelColumnStatusText;

                case 1:
                    return labelColumnProgress;
            }
            return super.getColumnName(column);
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
                    return p.getID()+": "+p.getStatusText();
                case 1:
                    return b;

            }
            return null;
        }

        @Override
        public String toString() {
            return " col " + controllers.size();
        }
    }

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

    /**
     * serialVersionUID
     */
    private static final long          serialVersionUID = -8537543161116653345L;

    private Vector<JProgressBar>       bars;

    /**
     * Hier werden alle Fortschritte der Plugins gespeichert
     */

    private Vector<ProgressController> controllers;

    //private Logger                     logger           = JDUtilities.getLogger();

    private Timer flickerTimer;

  
    
    /**
     * Die Tabelle für die Pluginaktivitäten
     */
    private JTable                     table;


    public TabProgress() {
        controllers = new Vector<ProgressController>();
        bars = new Vector<JProgressBar>();
        setLayout(new BorderLayout());
        table = new JTable();
        table.getTableHeader().setPreferredSize(new Dimension(-1,25));
        InternalTableModel internalTableModel;
        table.setModel(internalTableModel = new InternalTableModel());
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

    public void actionPerformed(ActionEvent e) {
        this.setVisible(false);
        
    }

    public synchronized void addController(ProgressController source) {

        JProgressBar progressBar = new JProgressBar();
        progressBar.setMaximum(source.getMax());
        progressBar.setValue(source.getValue());
        progressBar.setStringPainted(true);
        bars.add(0, progressBar);
        this.controllers.add(0, source);
        updateController(source);

    }

    public synchronized Vector<ProgressController> getControllers() {

        return controllers;
    }

    public synchronized boolean hasController(ProgressController source) {
        return controllers.contains(source);
    }

    public synchronized void removeController(ProgressController source) {
        int index = controllers.indexOf(source);

        if (index >= 0) {
            bars.remove(index);
            controllers.remove(source);
            updateController(source);
        }

    }

    public synchronized void updateController(ProgressController source) {
        if (source == null) {
            table.tableChanged(new TableModelEvent(table.getModel()));
            return;
        }
        if (controllers.size() > 0) {
            if (source.isFinished()) {

            }
            else {
                this.setVisible(true);
                if(flickerTimer!=null &&flickerTimer.isRunning())
                flickerTimer.stop();
                if (controllers.indexOf(source) < bars.size()) {
                    bars.get(controllers.indexOf(source)).setMaximum(source.getMax());
                    bars.get(controllers.indexOf(source)).setValue(source.getValue());
                }

            }

        }
        else {
            this.flickerTimer= new Timer(3000,this);
            flickerTimer.setRepeats(false);
            flickerTimer.start();
            
        }

        table.tableChanged(new TableModelEvent(table.getModel()));

    }
}
