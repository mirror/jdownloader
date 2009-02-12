package themekeys;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.io.File;
import java.util.Collections;
import java.util.Vector;

import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableColumn;

import jd.nutils.io.JDIO;
import jd.parser.Regex;
import jd.utils.JDTheme;
import jd.utils.JDUtilities;

import org.jdesktop.swingx.JXTable;
import org.jdesktop.swingx.decorator.ColorHighlighter;
import org.jdesktop.swingx.decorator.ComponentAdapter;
import org.jdesktop.swingx.decorator.HighlightPredicate;
import org.jdesktop.swingx.decorator.HighlighterFactory;

public class ThemeKeys extends JFrame {

    public static void main(String[] args) {
        new ThemeKeys().setVisible(true);
    }

    private static final long serialVersionUID = -1906208643728258195L;

    private final boolean highlightReady = false;
    private final boolean highlightOld = true;
    private final boolean highlightMissing = true;

    private final String sourceDir = "C:\\Dokumente und Einstellungen\\Towelie\\Eigene Dateien\\Java\\jd\\src\\";

    private final int themeFile = 1;
    private final Vector<String> themes = JDTheme.getThemeIDs();
    private final File themesDir = JDUtilities.getResourceFile(JDTheme.THEME_DIR + themes.get(themeFile) + ".thm");

    private Vector<KeyInfo> themeData = new Vector<KeyInfo>();

    private JXTable table;

    public ThemeKeys() {
        initThemeData(new File(sourceDir), themesDir);

        initGUI();
    }

    private void initGUI() {
        int columnSize = 100;

        table = new JXTable(new TableModel());
        table.getTableHeader().setReorderingAllowed(false);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setAutoStartEditOnKeyStroke(false);
        table.addHighlighter(HighlighterFactory.createAlternateStriping());
        table.addHighlighter(new ColorHighlighter(HighlightPredicate.ROLLOVER_ROW, null, Color.BLUE));
        if (highlightReady) table.addHighlighter(new ColorHighlighter(new ReadyPredicate(), Color.GREEN, null));
        if (highlightOld) table.addHighlighter(new ColorHighlighter(new OldPredicate(), Color.ORANGE, null));
        if (highlightMissing) table.addHighlighter(new ColorHighlighter(new MissingPredicate(), Color.RED, null));
        setColumnWidth(table.getColumn(1), columnSize);
        setColumnWidth(table.getColumn(2), columnSize);

        setTitle(getInfoTitle());
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        add(new JScrollPane(table));
        setSize(new Dimension(600, 450));
        setResizable(false);
        setLocationRelativeTo(null);
    }

    private void setColumnWidth(TableColumn column, int size) {
        column.setMaxWidth(size);
        column.setMinWidth(size);
        column.setPreferredWidth(size);
    }

    private String getInfoTitle() {
        int old = 0;
        int missing = 0;
        for (KeyInfo ki : themeData) {
            if (!ki.inSource) ++old;
            if (!ki.inTheme) ++missing;
        }
        return "Theme-Keys (" + themes.get(themeFile) + ") [Alt: " + old + " Fehlend:" + missing + "]";
    }

    private void initThemeData(File sourceFolder, File themeFile) {
        Vector<String> sourceEntries = getSourceEntriesFromFolder(sourceFolder);
        Vector<String> themeEntries = getThemeEntriesFromFile(themeFile);

        themeData.clear();
        String key;
        for (int i = sourceEntries.size() - 1; i >= 0; --i) {
            key = sourceEntries.remove(i);
            themeData.add(new KeyInfo(key, true, themeEntries.contains(key)));
            themeEntries.remove(key);
        }
        for (int i = themeEntries.size() - 1; i >= 0; --i) {
            themeData.add(new KeyInfo(themeEntries.remove(i), false, true));
        }

        Collections.sort(themeData);
    }

    private Vector<String> getSourceEntriesFromFolder(File sourceFolder) {
        Vector<String> sourceEntries = new Vector<String>();

        String[] matches;
        for (File file : getSourceFiles(sourceFolder)) {
            matches = new Regex(JDIO.getLocalFile(file), "JDTheme[\\s]*\\.(C|II|I|V)[\\s]*\\(\"(.*?)\"[^\\)]*?\\)").getColumn(1);

            for (String match : matches) {
                match = match.trim();
                if (sourceEntries.contains(match)) continue;
                sourceEntries.add(match);
            }
        }

        Collections.sort(sourceEntries);

        return sourceEntries;
    }

    private Vector<String> getThemeEntriesFromFile(File themeFile) {
        Vector<String> themeEntries = new Vector<String>();

        if (themeFile == null || !themeFile.exists()) {
            System.out.println("Could not find " + themeFile);
            return themeEntries;
        }

        String[] file = Regex.getLines(JDIO.getLocalFile(themeFile));
        String match;
        for (String line : file) {
            match = new Regex(line, "(.*?)=(.*?)").getMatch(0);
            if (match == null) continue;
            match = match.trim();
            if (themeEntries.contains(match)) continue;
            themeEntries.add(match);
        }

        Collections.sort(themeEntries);

        return themeEntries;
    }

    private Vector<File> getSourceFiles(File directory) {
        Vector<File> files = new Vector<File>();

        for (File file : directory.listFiles()) {
            if (file.isDirectory()) {
                files.addAll(getSourceFiles(file));
            } else if (file.getName().matches(".*\\.java$")) {
                files.add(file);
            }
        }

        return files;
    }

    private class KeyInfo implements Comparable<KeyInfo> {

        private String key;

        private boolean inSource;

        private boolean inTheme;

        public KeyInfo(String key, boolean inSource, boolean inTheme) {
            this.key = key;
            this.inSource = inSource;
            this.inTheme = inTheme;
        }

        public String getKey() {
            return this.key;
        }

        public boolean isInSource() {
            return this.inSource;
        }

        public boolean isInTheme() {
            return this.inTheme;
        }

        public int compareTo(KeyInfo o) {
            return this.getKey().compareToIgnoreCase(o.getKey());
        }

        @Override
        public String toString() {
            return this.getKey() + ": " + isInSource() + " ; " + isInTheme();
        }

    }

    private class ReadyPredicate implements HighlightPredicate {

        public boolean isHighlighted(Component arg0, ComponentAdapter arg1) {
            return (Boolean) table.getValueAt(arg1.row, 1) && (Boolean) table.getValueAt(arg1.row, 2);
        }

    }

    private class OldPredicate implements HighlightPredicate {

        public boolean isHighlighted(Component arg0, ComponentAdapter arg1) {
            return !(Boolean) table.getValueAt(arg1.row, 1) && (Boolean) table.getValueAt(arg1.row, 2);
        }

    }

    private class MissingPredicate implements HighlightPredicate {

        public boolean isHighlighted(Component arg0, ComponentAdapter arg1) {
            return (Boolean) table.getValueAt(arg1.row, 1) && !(Boolean) table.getValueAt(arg1.row, 2);
        }

    }

    private class TableModel extends AbstractTableModel {

        private static final long serialVersionUID = 7678245042388067359L;

        private final String[] columns = new String[] { "Key", "In Source?", "In Theme?" };

        @Override
        public Class<?> getColumnClass(int col) {
            if (col == 0) return String.class;
            return Boolean.class;
        }

        public int getColumnCount() {
            return columns.length;
        }

        public int getRowCount() {
            return themeData.size();
        }

        @Override
        public String getColumnName(int column) {
            return columns[column];
        }

        public Object getValueAt(int row, int column) {
            switch (column) {
            case 0:
                return themeData.get(row).getKey();
            case 1:
                return themeData.get(row).isInSource();
            case 2:
                return themeData.get(row).isInTheme();
            }
            return null;
        }

    }

}
