package jd.plugins.optional.langfileeditor;

import java.awt.Color;
import java.awt.Dimension;
import java.util.HashMap;
import java.util.Vector;

import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.table.AbstractTableModel;

import jd.nutils.Screen;
import jd.utils.JDLocale;

import org.jdesktop.swingx.JXTable;
import org.jdesktop.swingx.decorator.ColorHighlighter;
import org.jdesktop.swingx.decorator.HighlightPredicate;
import org.jdesktop.swingx.decorator.HighlighterFactory;

public class LFEDupeDialog extends JDialog {

    private static final long serialVersionUID = 1L;

    public static void showDialog(JFrame owner, HashMap<String, Vector<String>> dupes) {
        new LFEDupeDialog(owner, dupes).setVisible(true);
    }

    private LFEDupeDialog(JFrame owner, HashMap<String, Vector<String>> dupes) {
        super(owner);

        setModal(true);
        setTitle(JDLocale.L("plugins.optional.langfileeditor.duplicatedEntries", "Duplicated Entries") + " [" + dupes.size() + "]");
        setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);

        MyDupeTableModel internalTableModel = new MyDupeTableModel(dupes);
        JXTable table = new JXTable(internalTableModel);
        table.getTableHeader().setReorderingAllowed(false);
        table.getColumnModel().getColumn(0).setPreferredWidth(50);
        table.getColumnModel().getColumn(0).setMinWidth(50);
        table.getColumnModel().getColumn(0).setMaxWidth(50);
        table.getColumnModel().getColumn(2).setMinWidth(450);
        table.addHighlighter(HighlighterFactory.createAlternateStriping());
        table.addHighlighter(new ColorHighlighter(HighlightPredicate.ROLLOVER_ROW, null, Color.BLUE));

        add(new JScrollPane(table));

        setMinimumSize(new Dimension(900, 600));
        setPreferredSize(new Dimension(900, 600));
        pack();
        setLocation(Screen.getCenterOfComponent(owner, this));
    }

    private class MyDupeTableModel extends AbstractTableModel {

        private static final long serialVersionUID = -5434313385327397539L;

        private String[] columnNames;

        private HashMap<String, Vector<String>> tableData;

        private Vector<String> keys;

        public MyDupeTableModel(HashMap<String, Vector<String>> data) {
            columnNames = new String[] { "*", JDLocale.L("plugins.optional.langfileeditor.string", "String"), JDLocale.L("plugins.optional.langfileeditor.keys", "Keys") };
            tableData = data;
            keys = new Vector<String>(data.keySet());
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
                StringBuilder ret = new StringBuilder();
                Vector<String> values = tableData.get(keys.get(row));
                for (String value : values) {
                    if (ret.length() > 0) ret.append(" || ");
                    ret.append(value);
                }
                return ret.toString();
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
