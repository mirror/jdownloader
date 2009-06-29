//    jDownloader - Downloadmanager
//    Copyright (C) 2009  JD-Team support@jdownloader.org
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

package jd.plugins.optional.langfileeditor;

import java.awt.Dimension;
import java.util.ArrayList;
import java.util.HashMap;

import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.table.AbstractTableModel;

import jd.nutils.Screen;
import jd.utils.locale.JDL;

import org.jdesktop.swingx.JXTable;

public class LFEDupeDialog extends JDialog {

    private static final long serialVersionUID = 1L;

    public static void showDialog(JFrame owner, HashMap<String, ArrayList<String>> dupes) {
        new LFEDupeDialog(owner, dupes).setVisible(true);
    }

    private LFEDupeDialog(JFrame owner, HashMap<String, ArrayList<String>> dupes) {
        super(owner);

        setModal(true);
        setTitle(JDL.L("plugins.optional.langfileeditor.duplicatedEntries", "Duplicated Entries") + " [" + dupes.size() + "]");
        setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);

        MyDupeTableModel internalTableModel = new MyDupeTableModel(dupes);
        JXTable table = new JXTable(internalTableModel);
        table.getTableHeader().setReorderingAllowed(false);
        table.getColumnModel().getColumn(0).setPreferredWidth(50);
        table.getColumnModel().getColumn(0).setMinWidth(50);
        table.getColumnModel().getColumn(0).setMaxWidth(50);
        table.getColumnModel().getColumn(2).setMinWidth(450);

        add(new JScrollPane(table));

        setMinimumSize(new Dimension(900, 600));
        setPreferredSize(new Dimension(900, 600));
        pack();
        setLocation(Screen.getCenterOfComponent(owner, this));
    }

    private class MyDupeTableModel extends AbstractTableModel {

        private static final long serialVersionUID = -5434313385327397539L;

        private String[] columnNames;

        private HashMap<String, ArrayList<String>> tableData;

        private ArrayList<String> keys;

        public MyDupeTableModel(HashMap<String, ArrayList<String>> data) {
            columnNames = new String[] { "*", JDL.L("plugins.optional.langfileeditor.string", "String"), JDL.L("plugins.optional.langfileeditor.keys", "Keys") };
            tableData = data;
            keys = new ArrayList<String>(data.keySet());
        }

        public int getColumnCount() {
            return columnNames.length;
        }

        public int getRowCount() {
            return tableData.size();
        }

        // @Override
        public String getColumnName(int col) {
            return columnNames[col];
        }

        public Object getValueAt(int row, int col) {
            switch (col) {
            case 0:
                return tableData.get(keys.get(row)).size();
            case 1:
                return keys.get(row);
            case 2:
                return tableData.get(keys.get(row)).toString();
            }
            return "";
        }

        // @Override
        public Class<?> getColumnClass(int col) {
            if (col == 0) return Integer.class;
            return String.class;
        }

        // @Override
        public boolean isCellEditable(int row, int col) {
            return (col == 1);
        }

    }

}
