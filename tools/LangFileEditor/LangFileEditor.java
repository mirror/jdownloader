import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.util.Collections;
import java.util.Comparator;
import java.util.Vector;
import java.util.regex.Pattern;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;
import javax.swing.table.AbstractTableModel;

import jd.parser.Regex;
import jd.utils.JDLocale;
import jd.utils.JDUtilities;

/**
 * Editor for jDownloader language files. Gets JDLocale.L() and JDLocale.LF()
 * entries from source and compares them to the keypairs in the language file.
 * 
 * @author eXecuTe|Greeny
 */

public class LangFileEditor extends JFrame implements ActionListener {

    private static final long serialVersionUID = 4958486311019858135L;
    private JTextField txtFolder, txtFile;
    private JLabel lblEntriesCount;
    private JButton btnBrowseFile, btnBrowseFolder;
    private JMenu mnuFile, mnuKey, mnuEntries;
    private JMenuItem mnuBrowseFile, mnuBrowseFolder, mnuReload, mnuSave, mnuSaveAs, mnuExit;
    private JMenuItem mnuAdd, mnuAdopt, mnuAdoptMissing, mnuClear, mnuDelete, mnuEdit, mnuTranslate, mnuTranslateMissing;
    private JMenuItem mnuSelectMissing, mnuSelectOld, mnuShowDupes;
    private JTable table;
    private MyTableModel tableModel;

    private Vector<String> oldEntries = new Vector<String>();
    private Vector<String[]> dupes = new Vector<String[]>();
    private String lngKey = null;

    private File sourceFolder, languageFile;

    public static void main(String[] args) {

        LangFileEditor editor = new LangFileEditor();

        if (args.length == 1) {
            File file = new File(args[0]);
            if (file.isDirectory()) {
                editor.sourceFolder = file;
                editor.txtFolder.setText(args[0]);
                editor.initList();
            } else if (args[0].endsWith(".lng")) {
                editor.languageFile = file;
                editor.txtFile.setText(args[0]);
                editor.initList();
            }
        }

        if (args.length == 2) {
            editor.sourceFolder = new File(args[0]);
            editor.languageFile = new File(args[1]);
            editor.txtFolder.setText(args[0]);
            editor.txtFile.setText(args[1]);
            editor.initList();
        }

    }

    public LangFileEditor() {

        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            System.out.println("Error setting native LAF: " + e);
        }

        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.setTitle("jDownloader Language File Editor");
        this.setMinimumSize(new Dimension(800, 500));
        this.setPreferredSize(new Dimension(1200, 700));
        this.setName("LANGFILEEDIT");

        tableModel = new MyTableModel();
        table = new JTable(tableModel);
        table.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        table.setRowSelectionAllowed(true);
        table.setColumnSelectionAllowed(false);

        JPanel main = new JPanel(new BorderLayout(5, 5));
        main.setBorder(new EmptyBorder(10, 10, 10, 10));
        this.setContentPane(main);

        JPanel top = new JPanel(new BorderLayout(5, 5));
        JPanel top1 = new JPanel(new BorderLayout(5, 5));
        JPanel top2 = new JPanel(new BorderLayout(5, 5));

        top.add(top1, BorderLayout.PAGE_START);
        top.add(top2, BorderLayout.PAGE_END);

        top1.add(new JLabel("Source Folder: "), BorderLayout.LINE_START);
        top1.add(txtFolder = new JTextField("<Please select the Source Folder!>"), BorderLayout.CENTER);
        top1.add(btnBrowseFolder = new JButton("Browse"), BorderLayout.EAST);
        txtFolder.setEditable(false);
        btnBrowseFolder.addActionListener(this);

        top2.add(new JLabel("Language File: "), BorderLayout.LINE_START);
        top2.add(txtFile = new JTextField("<Please select the Language File!>"), BorderLayout.CENTER);
        top2.add(btnBrowseFile = new JButton("Browse"), BorderLayout.EAST);
        txtFile.setEditable(false);
        btnBrowseFile.addActionListener(this);

        main.add(top, BorderLayout.PAGE_START);
        main.add(new JScrollPane(table), BorderLayout.CENTER);
        main.add(lblEntriesCount = new JLabel("Entries Count:"), BorderLayout.PAGE_END);

        buildMenu();

        this.setResizable(true);
        this.pack();
        this.setVisible(true);

    }

    private void initList() {

        Vector<String[]> source = new Vector<String[]>();
        if (sourceFolder != null) source = getSourceEntries(sourceFolder);
        Vector<String[]> file = new Vector<String[]>();
        if (languageFile != null) file = getLanguageFileEntries(languageFile);

        tableModel.setData(getData(source, file));
        mnuEntries.setEnabled(true);
        mnuKey.setEnabled(true);
        mnuReload.setEnabled(true);
        mnuSave.setEnabled(true);
        mnuSaveAs.setEnabled(true);
        setInfoLabels();

    }

    private void setInfoLabels() {

        int numSource = 0, numFile = 0, numMissing = 0, numOld = 0;

        for (String[] entry : tableModel.getData()) {

            if (entry[1] != "" && entry[2] != "") {
                numSource++;
                numFile++;
            } else if (entry[1].equals("")) {
                numFile++;
                numOld++;
            } else if (entry[2].equals("")) {
                numSource++;
                numMissing++;
            }

        }

        lblEntriesCount.setText("Entries Count:     [Sourcecode] " + numSource + "     [Language File] " + numFile + "     [Missing] " + numMissing + "     [Not found / no Default] " + numOld + "     [Probably old] " + oldEntries.size() + "     [Probably dupes] " + dupes.size());

    }

    private void buildMenu() {
        // File Menü
        mnuFile = new JMenu("File");

        mnuFile.add(mnuBrowseFolder = new JMenuItem("Browse Source Folder"));
        mnuFile.add(mnuBrowseFile = new JMenuItem("Browse Language File"));
        mnuFile.addSeparator();
        mnuFile.add(mnuReload = new JMenuItem("Reload"));
        mnuFile.addSeparator();
        mnuFile.add(mnuSave = new JMenuItem("Save"));
        mnuFile.add(mnuSaveAs = new JMenuItem("Save As"));
        mnuFile.addSeparator();
        mnuFile.add(mnuExit = new JMenuItem("Exit"));

        mnuBrowseFolder.addActionListener(this);
        mnuBrowseFile.addActionListener(this);
        mnuReload.addActionListener(this);
        mnuReload.setEnabled(false);
        mnuSave.addActionListener(this);
        mnuSave.setEnabled(false);
        mnuSaveAs.addActionListener(this);
        mnuSaveAs.setEnabled(false);
        mnuExit.addActionListener(this);

        // Key Menü
        mnuKey = new JMenu("Key");
        mnuKey.setEnabled(false);

        mnuKey.add(mnuAdd = new JMenuItem("Add Key"));
        mnuKey.add(mnuDelete = new JMenuItem("Delete Key(s)"));
        mnuKey.addSeparator();
        mnuKey.add(mnuEdit = new JMenuItem("Edit Value(s)"));
        mnuKey.add(mnuClear = new JMenuItem("Clear Values"));
        mnuKey.addSeparator();
        mnuKey.add(mnuAdopt = new JMenuItem("Adopt Default(s)"));
        mnuKey.add(mnuAdoptMissing = new JMenuItem("Adopt Defaults of Missing Entries"));
        mnuKey.add(mnuTranslate = new JMenuItem("Translate with Google"));
        mnuKey.add(mnuTranslateMissing = new JMenuItem("Translate Missing Entries with Google"));

        mnuAdd.addActionListener(this);
        mnuDelete.addActionListener(this);
        mnuEdit.addActionListener(this);
        mnuClear.addActionListener(this);
        mnuAdopt.addActionListener(this);
        mnuAdoptMissing.addActionListener(this);
        mnuTranslate.addActionListener(this);
        mnuTranslateMissing.addActionListener(this);

        // Entries Menü
        mnuEntries = new JMenu("Entries");
        mnuEntries.setEnabled(false);

        mnuEntries.add(mnuSelectMissing = new JMenuItem("Select Missing Entries"));
        mnuEntries.add(mnuSelectOld = new JMenuItem("Select Old Entries"));
        mnuEntries.add(mnuShowDupes = new JMenuItem("Show Dupes"));

        mnuSelectMissing.addActionListener(this);
        mnuSelectOld.addActionListener(this);
        mnuShowDupes.addActionListener(this);

        JMenuBar menuBar = new JMenuBar();
        menuBar.add(mnuFile);
        menuBar.add(mnuKey);
        menuBar.add(mnuEntries);
        setJMenuBar(menuBar);
    }

    public void actionPerformed(ActionEvent e) {

        if (e.getSource() == btnBrowseFolder || e.getSource() == mnuBrowseFolder) {

            JFileChooser chooser = new JFileChooser(sourceFolder);
            chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

            if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION && chooser.getSelectedFile().isDirectory()) {
                sourceFolder = chooser.getSelectedFile();
                txtFolder.setText(sourceFolder.getAbsolutePath());
                initList();
            }

        } else if (e.getSource() == btnBrowseFile || e.getSource() == mnuBrowseFile) {

            JFileChooser chooser = new JFileChooser((languageFile != null) ? languageFile.getAbsolutePath() : null);

            if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                languageFile = chooser.getSelectedFile();
                txtFile.setText(languageFile.getAbsolutePath());
                initList();
            }

        } else if (e.getSource() == mnuSave) {

            saveLanguageFile(languageFile);

        } else if (e.getSource() == mnuSaveAs) {

            JFileChooser chooser = new JFileChooser();
            chooser.setDialogType(JFileChooser.SAVE_DIALOG);
            chooser.setDialogTitle("Save Language File As...");
            chooser.setCurrentDirectory(languageFile.getParentFile());
            int value = chooser.showOpenDialog(this);

            if (value == JFileChooser.APPROVE_OPTION) saveLanguageFile(chooser.getSelectedFile());

        } else if (e.getSource() == mnuEdit) {

            int[] rows = table.getSelectedRows();

            for (int i = 0; i < rows.length; i++) {

                int j = rows[i];

                EditDialog dialog = new EditDialog(this, tableModel.getData().get(j));

                if (dialog.value != null) {

                    tableModel.setValueAt(dialog.value, j, 2);

                    if (i + 1 == rows.length) {
                        if (j + 1 < table.getRowCount()) {
                            table.getSelectionModel().setSelectionInterval(j + 1, j + 1);
                        } else {
                            table.getSelectionModel().setSelectionInterval(j, j);
                        }
                    }

                } else {
                    break;
                }

                setInfoLabels();

            }

        } else if (e.getSource() == mnuDelete) {

            int[] rows = table.getSelectedRows();
            for (int i = rows.length - 1; i >= 0; --i) {
                tableModel.deleteRow(rows[i]);
            }

            setInfoLabels();

        } else if (e.getSource() == mnuAdd) {

            Vector<String[]> data = tableModel.getData();
            AddDialog dialog = new AddDialog(this);

            if (dialog.key != null && dialog.value != null && !dialog.key.equals("") && !dialog.value.equals("")) {
                data.add(new String[] { dialog.key, "", dialog.value });
            }

            tableModel.setData(data);
            table.getSelectionModel().setSelectionInterval(0, 0);
            setInfoLabels();

        } else if (e.getSource() == mnuAdoptMissing) {

            Vector<String[]> data = tableModel.getData();

            for (int i = 0; i < data.size(); i++) {

                if (data.get(i)[2] == "") {

                    String def = data.get(i)[1];
                    if (!def.equals("") && !def.equals("<no default value>")) {
                        tableModel.setValueAt(def, i, 2);
                    }

                }

            }

            setInfoLabels();

        } else if (e.getSource() == mnuReload) {

            initList();

        } else if (e.getSource() == mnuAdopt) {

            for (int i : table.getSelectedRows()) {

                String def = tableModel.getValueAt(i, 1);
                if (!def.equals("") && !def.equals("<no default value>")) {
                    tableModel.setValueAt(def, i, 2);
                }

            }

            setInfoLabels();

        } else if (e.getSource() == mnuClear) {

            for (int i = 0; i < table.getRowCount(); i++) {
                tableModel.setValueAt("", i, 2);
            }

        } else if (e.getSource() == mnuSelectMissing) {

            table.clearSelection();
            for (int i = 0; i < table.getRowCount(); i++) {

                if (tableModel.getValueAt(i, 2).equals("")) {
                    table.getSelectionModel().addSelectionInterval(i, i);
                }

            }

        } else if (e.getSource() == mnuSelectOld) {

            table.clearSelection();
            for (int i = 0; i < table.getRowCount(); i++) {

                if (oldEntries.contains(table.getValueAt(i, 0))) {
                    table.getSelectionModel().addSelectionInterval(i, i);
                }

            }

        } else if (e.getSource() == mnuShowDupes) {

            new DupeDialog(this, dupes);

        } else if (e.getSource() == mnuTranslateMissing) {

            if (lngKey == null) {
                LanguageDialog dialog = new LanguageDialog(this);
                if (dialog.key != null && !dialog.key.equals("")) {
                    lngKey = dialog.key;
                } else {
                    return;
                }
            }

            Vector<String[]> data = tableModel.getData();

            for (int i = 0; i < data.size(); i++) {

                if (data.get(i)[2] == "") {

                    String def = data.get(i)[1];

                    if (!def.equals("") && !def.equals("<no default value>")) {
                        System.out.println("Working on " + data.get(i)[0] + ":");
                        String result = JDLocale.translate(lngKey, def);
                        System.out.println("Default entry is \"" + def + "\" and google returned \"" + result + "\"");
                        tableModel.setValueAt(result, i, 2);
                    }

                }

            }

            setInfoLabels();

        } else if (e.getSource() == mnuTranslate) {

            if (lngKey == null) {
                LanguageDialog dialog = new LanguageDialog(this);
                if (dialog.key != null && !dialog.key.equals("")) {
                    lngKey = dialog.key;
                } else {
                    return;
                }
            }

            for (int i : table.getSelectedRows()) {

                String def = tableModel.getValueAt(i, 1);

                if (!def.equals("") && !def.equals("<no default value>")) {
                    tableModel.setValueAt(JDLocale.translate(lngKey, def), i, 2);
                }

            }

            setInfoLabels();

        } else if (e.getSource() == mnuExit) {

            setVisible(false);
            dispose();

        }

    }

    private void saveLanguageFile(File file) {
        StringBuilder sb = new StringBuilder();

        for (String[] entry : tableModel.getData()) {
            if (entry[2] != "") sb.append(entry[0] + " = " + entry[2] + "\n");
        }

        try {
            Writer out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), "UTF8"));
            out.write(sb.toString());
            out.close();
        } catch (UnsupportedEncodingException ex) {
        } catch (IOException ex) {
        }

        JOptionPane.showMessageDialog(this, "LanguageFile saved successfully.");
    }

    private Vector<String[]> getData(Vector<String[]> sourceEntries, Vector<String[]> fileEntries) {

        String tmp;
        Vector<String[]> data = new Vector<String[]>();
        Vector<String[]> dupeHelp = new Vector<String[]>();
        oldEntries.clear();
        dupes.clear();
        lngKey = null;

        for (String[] entry : sourceEntries) {

            String[] temp = new String[3];
            temp[0] = entry[0];
            temp[1] = entry[1];
            temp[2] = getValue(fileEntries, entry[0]);
            if (temp[2] == null) temp[2] = "";

            data.add(temp);
            if (temp[2] != "") {
                tmp = getValue(dupeHelp, temp[2]);
                if (tmp != null) dupes.add(new String[] { temp[2], temp[0], tmp });
                dupeHelp.add(new String[] { temp[2], temp[0] });
            }

        }

        for (String[] entry : fileEntries) {

            if (getValue(data, entry[0]) == null) {

                String[] temp = new String[3];
                temp[0] = entry[0];
                temp[1] = "";
                temp[2] = entry[1];

                data.add(temp);
                oldEntries.add(temp[0]);
                if (temp[2] != "") {
                    tmp = getValue(dupeHelp, temp[2]);
                    if (tmp != null) dupes.add(new String[] { temp[2], temp[0], tmp });
                    dupeHelp.add(new String[] { temp[2], temp[0] });
                }

            }

        }

        return data;

    }

    private String getValue(Vector<String[]> vector, String key) {

        String result = null;

        for (String[] entry : vector) {

            if (entry[0].equals(key)) {
                result = entry[1];
                break;
            }

        }

        return result;

    }

    private Vector<String[]> getLanguageFileEntries(File file) {

        String content = JDUtilities.getLocalFile(file);
        Vector<String[]> entries = new Vector<String[]>();
        Vector<String> keys = new Vector<String>();

        String[][] matches = new Regex(Pattern.compile("(.*?)=(.*?)[\\r]?\\n").matcher(content)).getMatches();

        for (String[] match : matches) {

            if (match[0].endsWith(" ")) match[0] = match[0].substring(0, match[0].length() - 1);
            if (match[1].startsWith(" ")) match[1] = match[1].substring(1);
            if (!keys.contains(match[0]) && !match[0].equals("") && !match[1].equals("")) {

                keys.add(match[0]);
                String[] temp = new String[] { match[0], match[1] };
                entries.add(temp);

            }

        }

        return entries;

    }

    private Vector<String[]> getSourceEntries(File dir) {

        Vector<String> fileContents = getFileContents(dir, "java");
        Vector<String[]> entries = new Vector<String[]>();
        Vector<String> keys = new Vector<String>();

        for (String file : fileContents) {

            String pattern1 = "JDLocale\\.L" + "[\\n\\s]*?\\([\\n\\s]*?" + "\"\\s*?(.*?)\\s*?\"" + "[\\n\\s]*?(,[\\n\\s]*?" + "\"\\s*?(.*?)\\s*?\"" + "[\\n\\s]*?)*\\)";
            String pattern2 = "JDLocale\\.LF" + "[\\n\\s]*?\\([\\n\\s]*?" + "\"\\s*?(.*?)\\s*?\"" + "[\\n\\s]*?,[\\n\\s]*?" + "\"\\s*?(.*?)\\s*?\"" + "[\\n\\s]*?,";

            String[][] matches1 = new Regex(Pattern.compile(pattern1).matcher(file)).getMatches();
            String[][] matches2 = new Regex(Pattern.compile(pattern2).matcher(file)).getMatches();

            for (String[] match : matches1) {

                if (!keys.contains(match[0].trim())) {

                    keys.add(match[0].trim());
                    String k = match[0].trim();
                    String v = "<no default value>";

                    try {
                        v = match[2].trim();
                    } catch (Exception e) {
                    }

                    String[] temp = new String[] { k, v };
                    entries.add(temp);

                }

            }

            for (String[] match : matches2) {

                if (!keys.contains(match[0].trim())) {

                    keys.add(match[0].trim());
                    String k = match[0].trim();
                    String v = "<no default value>";

                    try {
                        v = match[1].trim();
                    } catch (Exception e) {
                    }

                    String[] temp = new String[] { k, v };
                    entries.add(temp);

                }

            }

        }

        return entries;

    }

    private Vector<String> getFileContents(File directory, String filter) {

        Vector<String> fileContents = new Vector<String>();
        File[] entries = directory.listFiles();

        for (int i = 0; i < entries.length; i++) {

            if (entries[i].isDirectory()) {

                fileContents.addAll(getFileContents(entries[i], filter));

            } else if (entries[i].isFile()) {

                String extension = JDUtilities.getFileExtension(entries[i]);

                if (extension.equals(filter)) {
                    fileContents.add(JDUtilities.getLocalFile(entries[i]));
                }

            }

        }

        return fileContents;

    }

    class MyTableModel extends AbstractTableModel {

        private static final long serialVersionUID = -5434313385327397539L;
        private String[] columnNames = { "Key", "Source Value", "Language File Value" };
        private Vector<String[]> tableData = new Vector<String[]>();

        public int getColumnCount() {
            return columnNames.length;
        }

        public int getRowCount() {
            return tableData.size();
        }

        public String getColumnName(int col) {
            return columnNames[col];
        }

        public String getValueAt(int row, int col) {
            return tableData.get(row)[col];
        }

        public Class<?> getColumnClass(int c) {
            return String.class;
        }

        public boolean isCellEditable(int row, int col) {
            if (col == 2) return true;
            return false;
        }

        public void setValueAt(String value, int row, int col) {
            tableData.get(row)[col] = value;
            this.fireTableCellUpdated(row, col);
        }

        public void addRow(String[] value) {
            tableData.add(value);
            this.fireTableRowsInserted(tableData.size() - 1, tableData.size() - 1);
        }

        public void deleteRow(int index) {
            tableData.remove(index);
            this.fireTableRowsDeleted(index, index);
        }

        public void setData(Vector<String[]> newData) {
            System.out.println(newData.get(newData.size() - 1)[0]);
            Collections.sort(newData, new StringArrayComparator());
            System.out.println(newData.get(newData.size() - 1)[0]);
            tableData = newData;
            this.fireTableRowsInserted(0, tableData.size() - 1);
        }

        public Vector<String[]> getData() {
            return tableData;
        }

    }

    private class StringArrayComparator implements Comparator<String[]> {

        public int compare(String[] s1, String[] s2) {

            return s1[0].compareTo(s2[0]);

        }

    }

    private class DupeDialog extends JDialog implements ActionListener {

        private static final long serialVersionUID = 1L;

        public DupeDialog(JFrame owner, Vector<String[]> dupes) {

            super();

            JPanel panel = new JPanel();
            setContentPane(panel);

            setModal(true);
            setLayout(new GridBagLayout());
            setTitle("Duplicated Entries");

            MyDupeTableModel tableModel = new MyDupeTableModel();
            JTable table = new JTable(tableModel);
            tableModel.setData(dupes);
            JScrollPane scroll = new JScrollPane(table);
            scroll.setPreferredSize(new Dimension(900, 350));

            JButton btnClose = new JButton("Close");
            btnClose.addActionListener(this);
            getRootPane().setDefaultButton(btnClose);

            JDUtilities.addToGridBag(this, scroll, 1, 1, 1, 1, 1, 1, new Insets(5, 5, 5, 5), GridBagConstraints.BOTH, GridBagConstraints.CENTER);
            JDUtilities.addToGridBag(this, btnClose, 1, 2, 1, 1, 1, 1, new Insets(5, 5, 5, 5), GridBagConstraints.NONE, GridBagConstraints.CENTER);

            int n = 10;
            panel.setBorder(new EmptyBorder(n, n, n, n));

            pack();
            setLocation(JDUtilities.getCenterOfComponent(owner, this));
            setVisible(true);

        }

        public void actionPerformed(ActionEvent e) {

            dispose();

        }

        class MyDupeTableModel extends AbstractTableModel {

            private static final long serialVersionUID = -5434313385327397539L;
            private String[] columnNames = { "String", "First Key", "Secondary Key" };
            private Vector<String[]> tableData = new Vector<String[]>();

            public int getColumnCount() {
                return columnNames.length;
            }

            public int getRowCount() {
                return tableData.size();
            }

            public String getColumnName(int col) {
                return columnNames[col];
            }

            public String getValueAt(int row, int col) {
                return tableData.get(row)[col];
            }

            public Class<?> getColumnClass(int c) {
                return String.class;
            }

            public boolean isCellEditable(int row, int col) {
                return false;
            }

            public void setData(Vector<String[]> newData) {
                tableData = newData;
                this.fireTableRowsInserted(0, tableData.size() - 1);
            }

        }

    }

    private class EditDialog extends JDialog implements ActionListener {

        private static final long serialVersionUID = 1L;

        private JButton btnOK = new JButton("OK");
        private JButton btnCancel = new JButton("Cancel");
        private JButton btnAdopt = new JButton("Adopt Default Value");
        private JFrame owner;
        private JTextArea taSourceValue = new JTextArea(5, 20);
        private JTextArea taFileValue = new JTextArea(5, 20);

        public String value;

        public EditDialog(JFrame owner, String[] entry) {

            super(owner);
            this.owner = owner;

            setModal(true);
            setLayout(new BorderLayout(5, 5));
            setTitle("Edit Value");
            getRootPane().setDefaultButton(btnOK);

            JLabel lblKey = new JLabel("");

            btnOK.addActionListener(this);
            btnCancel.addActionListener(this);
            btnAdopt.addActionListener(this);

            lblKey.setText("Key: " + entry[0]);
            taSourceValue.setText(entry[1]);
            taFileValue.setText(entry[2]);
            taSourceValue.setEditable(false);

            JPanel main = new JPanel(new BorderLayout(5, 5));
            main.setBorder(new EmptyBorder(10, 10, 10, 10));
            JPanel fields = new JPanel(new BorderLayout(5, 5));
            JPanel buttons1 = new JPanel(new BorderLayout(5, 5));
            JPanel buttons2 = new JPanel(new FlowLayout());

            main.add(lblKey, BorderLayout.PAGE_START);
            main.add(fields, BorderLayout.CENTER);
            main.add(buttons1, BorderLayout.PAGE_END);

            fields.add(new JScrollPane(taSourceValue), BorderLayout.PAGE_START);
            fields.add(new JScrollPane(taFileValue), BorderLayout.PAGE_END);

            buttons1.add(buttons2, BorderLayout.LINE_END);
            buttons2.add(btnAdopt);
            buttons2.add(btnOK);
            buttons2.add(btnCancel);

            setContentPane(main);
            pack();
            setLocation(JDUtilities.getCenterOfComponent(owner, this));
            setVisible(true);

        }

        public void actionPerformed(ActionEvent e) {

            if (e.getSource() == btnOK) {

                value = taFileValue.getText();
                dispose();
                owner.setVisible(true);

            } else if (e.getSource() == btnCancel) {

                value = null;
                dispose();
                owner.setVisible(true);

            } else if (e.getSource() == btnAdopt) {

                taFileValue.setText(taSourceValue.getText());

            }

        }

    }

    private class AddDialog extends JDialog implements ActionListener {

        private static final long serialVersionUID = 1L;

        private JButton btnOK = new JButton("OK");
        private JButton btnCancel = new JButton("Cancel");
        private JFrame owner;
        private JTextField tfKey = new JTextField(20);
        private JTextArea taValue = new JTextArea(5, 20);

        public String value;
        public String key;

        public AddDialog(JFrame owner) {

            super(owner);
            this.owner = owner;

            setModal(true);
            setLayout(new BorderLayout(5, 5));
            setTitle("Edit Value");
            getRootPane().setDefaultButton(btnOK);

            btnOK.addActionListener(this);
            btnCancel.addActionListener(this);

            JLabel lblKey = new JLabel("Key:");

            JPanel main = new JPanel(new BorderLayout(5, 5));
            main.setBorder(new EmptyBorder(10, 10, 10, 10));
            JPanel keyPanel = new JPanel(new BorderLayout(5, 5));
            JPanel buttons1 = new JPanel(new BorderLayout(5, 5));
            JPanel buttons2 = new JPanel(new FlowLayout());

            main.add(keyPanel, BorderLayout.PAGE_START);
            main.add(new JScrollPane(taValue), BorderLayout.CENTER);
            main.add(buttons1, BorderLayout.PAGE_END);

            keyPanel.add(lblKey, BorderLayout.LINE_START);
            keyPanel.add(tfKey, BorderLayout.CENTER);

            buttons1.add(buttons2, BorderLayout.LINE_END);
            buttons2.add(btnOK);
            buttons2.add(btnCancel);

            setContentPane(main);
            pack();
            setLocation(JDUtilities.getCenterOfComponent(owner, this));
            setVisible(true);

        }

        public void actionPerformed(ActionEvent e) {

            if (e.getSource() == btnOK) {

                value = taValue.getText().trim();
                key = tfKey.getText().trim();
                dispose();
                owner.setVisible(true);

            } else if (e.getSource() == btnCancel) {

                value = null;
                dispose();
                owner.setVisible(true);

            }

        }

    }

    private class LanguageDialog extends JDialog implements ActionListener {

        private static final long serialVersionUID = 1L;

        private JButton btnOK = new JButton("OK");
        private JButton btnCancel = new JButton("Cancel");
        private JFrame owner;
        private JTextField txtLanguageKey = new JTextField(2);

        public String value;
        public String key;

        public LanguageDialog(JFrame owner) {

            super(owner);
            this.owner = owner;

            setModal(true);
            setLayout(new BorderLayout(5, 5));
            setTitle("Insert Language Key");
            getRootPane().setDefaultButton(btnOK);

            btnOK.addActionListener(this);
            btnCancel.addActionListener(this);

            JPanel main = new JPanel(new BorderLayout(5, 5));
            main.setBorder(new EmptyBorder(10, 10, 10, 10));
            JPanel keyPanel = new JPanel(new BorderLayout(5, 5));
            JPanel buttons1 = new JPanel(new BorderLayout(5, 5));
            JPanel buttons2 = new JPanel(new FlowLayout());

            main.add(keyPanel, BorderLayout.PAGE_START);
            main.add(buttons1, BorderLayout.PAGE_END);

            keyPanel.add(new JLabel("Please insert the Language Key to provide a correct translation of Google:"), BorderLayout.LINE_START);
            keyPanel.add(txtLanguageKey, BorderLayout.CENTER);

            buttons1.add(buttons2, BorderLayout.LINE_END);
            buttons2.add(btnOK);
            buttons2.add(btnCancel);

            setContentPane(main);
            pack();
            setLocation(JDUtilities.getCenterOfComponent(owner, this));
            setVisible(true);

        }

        public void actionPerformed(ActionEvent e) {

            if (e.getSource() == btnOK) {

                key = txtLanguageKey.getText();
                dispose();
                owner.setVisible(true);

            } else if (e.getSource() == btnCancel) {

                key = null;
                dispose();
                owner.setVisible(true);

            }

        }

    }

}
