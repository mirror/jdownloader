import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
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

import jd.gui.skins.simple.LocationListener;
import jd.parser.Regex;
import jd.utils.JDUtilities;

/**
 * 
 * Editor for jDownloader language files
 * Gets JDLocale entries from source and
 * compares them to the keypairs in the language file
 * 
 * @author eXecuTe
 * 
 */

public class LangFileEditor implements ActionListener {

    private JFrame frame;
    private JLabel lblFolder, lblFile, lblFolderValue, lblFileValue, lblEntriesCount;
    private JButton btnAdoptMissing, btnAdd, btnDelete, btnEdit, btnSave, btnBrowseFolder, btnBrowseFile, btnReload, btnAdopt, btnSelectMissing, btnClear, btnSelectOld;
    private JTable table;
    private MyTableModel tableModel;
    private Vector<String> oldEntries = new Vector<String>();

    private File sourceFolder, languageFile;

    public static void main(String[] args) {

        LangFileEditor editor = new LangFileEditor();
        editor.showGui();

        if (args.length == 2) {

            editor.sourceFolder = new File(args[0]);
            editor.languageFile = new File(args[1]);
            editor.lblFolderValue.setText(args[0]);
            editor.lblFileValue.setText(args[1]);
            editor.initList();

        }

        editor.table.setRowSelectionInterval(0, 0);

    }

    private void initList() {

        Vector<String[]> source = new Vector<String[]>();
        if (sourceFolder != null) source = getSourceEntries(sourceFolder);
        Vector<String[]> file = new Vector<String[]>();
        if (languageFile != null) file = getLanguageFileEntries(languageFile);

        Vector<String[]> data = getData(source, file);
        tableModel.setData(data);
        setInfoLabels();

    }

    private void setInfoLabels() {

        Vector<String[]> data = tableModel.getData();
        int numSource = 0, numFile = 0, numMissing = 0, numOld = 0;

        for (String[] entry : data) {

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

        lblEntriesCount.setText("Entries Count:     [Sourcecode] " + numSource + "     [Language File] " + numFile + "     [Missing] " + numMissing + "     [Not found / no Default] " + numOld);

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

        @SuppressWarnings("unchecked")
        public Class getColumnClass(int c) {
            return getValueAt(0, c).getClass();
        }

        public boolean isCellEditable(int row, int col) {

            if (col == 2) {
                return false;
            } else {
                return true;
            }

        }

        public void setValueAt(String value, int row, int col) {

            tableData.get(row)[col] = value;
            fireTableCellUpdated(row, col);

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

            tableData = newData;
            this.fireTableRowsInserted(0, tableData.size() - 1);

        }

        public Vector<String[]> getData() {

            return tableData;

        }

    }

    private void showGui() {

        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            System.out.println("Error setting native LAF: " + e);
        }

        frame = new JFrame();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setTitle("jDownloader Language File Editor");
        frame.setPreferredSize(new Dimension(1200, 700));
        frame.setName("LANGFILEEDIT");
        LocationListener listener = new LocationListener();
        frame.addComponentListener(listener);
        frame.addWindowListener(listener);

        tableModel = new MyTableModel();
        table = new JTable(tableModel);
        table.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        table.setRowSelectionAllowed(true);
        table.setColumnSelectionAllowed(false);

        btnBrowseFolder = new JButton("Browse");
        btnBrowseFile = new JButton("Browse");
        btnAdoptMissing = new JButton("Adopt Defaults of Missing Entries");
        btnAdopt = new JButton("Adopt Default(s)");
        btnAdd = new JButton("Add Key");
        btnDelete = new JButton("Delete Key(s)");
        btnEdit = new JButton("Edit Value(s)");
        btnSelectOld = new JButton("Select Probably Old Entries");
        btnSelectMissing = new JButton("Select Missing Entries");
        btnClear = new JButton("Clear Values");
        btnReload = new JButton("Reload");
        btnSave = new JButton("Save As...");

        btnBrowseFolder.addActionListener(this);
        btnBrowseFile.addActionListener(this);
        btnAdoptMissing.addActionListener(this);
        btnAdopt.addActionListener(this);
        btnAdd.addActionListener(this);
        btnDelete.addActionListener(this);
        btnEdit.addActionListener(this);
        btnSelectOld.addActionListener(this);
        btnSelectMissing.addActionListener(this);
        btnClear.addActionListener(this);
        btnReload.addActionListener(this);
        btnSave.addActionListener(this);

        lblFolder = new JLabel("Source Folder: ");
        lblFile = new JLabel("Language File: ");
        lblFolderValue = new JLabel("/.../...");
        lblFileValue = new JLabel("/.../...");
        lblEntriesCount = new JLabel("Entries Count:");

        JPanel main = new JPanel(new BorderLayout(5, 5));
        main.setBorder(new EmptyBorder(10, 10, 10, 10));
        frame.setContentPane(main);

        JPanel top = new JPanel(new BorderLayout(5, 5));
        JPanel bottom = new JPanel(new BorderLayout());

        JPanel top1 = new JPanel(new BorderLayout(5, 5));
        JPanel top2 = new JPanel(new BorderLayout(5, 5));
        JPanel infos = new JPanel(new BorderLayout(5, 5));
        JPanel buttons = new JPanel(new FlowLayout());

        top.add(top1, BorderLayout.PAGE_START);
        top.add(top2, BorderLayout.PAGE_END);

        bottom.add(infos, BorderLayout.PAGE_START);
        bottom.add(buttons, BorderLayout.LINE_END);

        top1.add(lblFolder, BorderLayout.LINE_START);
        top1.add(lblFolderValue, BorderLayout.CENTER);
        top1.add(btnBrowseFolder, BorderLayout.EAST);

        top2.add(lblFile, BorderLayout.LINE_START);
        top2.add(lblFileValue, BorderLayout.CENTER);
        top2.add(btnBrowseFile, BorderLayout.EAST);

        infos.add(lblEntriesCount, BorderLayout.LINE_START);

        buttons.add(btnSave);
        buttons.add(btnReload);
        buttons.add(btnClear);
        buttons.add(btnSelectMissing);
        buttons.add(btnSelectOld);
        buttons.add(btnAdoptMissing);
        buttons.add(btnAdd);
        buttons.add(btnDelete);
        buttons.add(btnAdopt);
        buttons.add(btnEdit);

        main.add(top, BorderLayout.PAGE_START);
        main.add(new JScrollPane(table), BorderLayout.CENTER);
        main.add(bottom, BorderLayout.PAGE_END);

        frame.setResizable(true);
        frame.pack();
        frame.setVisible(true);

    }

    @SuppressWarnings("unchecked")
    public void actionPerformed(ActionEvent e) {

        if (e.getSource().equals(btnBrowseFolder)) {

            JFileChooser chooser = new JFileChooser();
            chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            int value = chooser.showOpenDialog(frame);

            if (value == JFileChooser.APPROVE_OPTION && chooser.getSelectedFile().isDirectory()) {
                sourceFolder = chooser.getSelectedFile();
                lblFolderValue.setText(sourceFolder.getAbsolutePath());
                initList();
            }

        } else if (e.getSource().equals(btnBrowseFile)) {

            JFileChooser chooser = new JFileChooser();
            int value = chooser.showOpenDialog(frame);

            if (value == JFileChooser.APPROVE_OPTION) {
                languageFile = chooser.getSelectedFile();
                lblFileValue.setText(languageFile.getAbsolutePath());
                initList();
            }

        } else if (e.getSource().equals(btnSave)) {

            JFileChooser chooser = new JFileChooser();
            chooser.setDialogType(JFileChooser.SAVE_DIALOG);
            chooser.setDialogTitle("Save Language File As...");
            chooser.setCurrentDirectory(languageFile.getParentFile());
            int vlaue = chooser.showOpenDialog(frame);

            if (vlaue == JFileChooser.APPROVE_OPTION) {

                File file = chooser.getSelectedFile();
                Vector<String[]> data = tableModel.getData();
                String content = "";

                for (String[] entry : data) {

                    if (entry[2] != "") {
                        content += entry[0] + " = " + entry[2] + "\n";
                    }

                }

                try {
                    Writer out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), "UTF8"));
                    out.write(content);
                    out.close();
                } catch (UnsupportedEncodingException ex) {
                } catch (IOException ex) {
                }

                JOptionPane.showMessageDialog(frame, "Saved.");

            }

        } else if (e.getSource().equals(btnEdit)) {

            int[] rows = table.getSelectedRows();

            for (int i = 0; i < rows.length; i++) {

                int j = rows[i];

                String k = tableModel.getValueAt(j, 0);
                String s = tableModel.getValueAt(j, 1);
                String f = tableModel.getValueAt(j, 2);
                EditDialog dialog = new EditDialog(frame, k, s, f);

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

        } else if (e.getSource().equals(btnDelete)) {

            int j = -1;
            int offset = 0;

            for (int i : table.getSelectedRows()) {
                tableModel.deleteRow(i - offset);
                offset++;
                j = i - offset;
            }

            if (j != -1 && j + 1 < table.getRowCount()) {
                table.getSelectionModel().setSelectionInterval(j + 1, j + 1);
            } else if (j != -1) {
                table.getSelectionModel().setSelectionInterval(j, j);
            }

            setInfoLabels();

        } else if (e.getSource().equals(btnAdd)) {

            Vector<String[]> data = tableModel.getData();
            AddDialog dialog = new AddDialog(frame);

            if (dialog.key != null && dialog.value != null && !dialog.key.equals("") && !dialog.value.equals("")) {
                data.add(new String[] { dialog.key, "", dialog.value });
            }

            Collections.sort(data, new StringArrayComparator());
            tableModel.setData(data);
            table.getSelectionModel().setSelectionInterval(0, 0);
            setInfoLabels();

        } else if (e.getSource().equals(btnAdoptMissing)) {

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

        } else if (e.getSource().equals(btnReload)) {

            initList();

        } else if (e.getSource().equals(btnAdopt)) {

            int j = -1;

            for (int i : table.getSelectedRows()) {

                String def = tableModel.getValueAt(i, 1);

                if (!def.equals("") && !def.equals("<no default value>")) {
                    tableModel.setValueAt(def, i, 2);
                }

            }

            if (j != -1 && j + 1 < table.getRowCount()) {
                table.getSelectionModel().setSelectionInterval(j + 1, j + 1);
            } else if (j != -1) {
                table.getSelectionModel().setSelectionInterval(j, j);
            }

            setInfoLabels();

        } else if (e.getSource().equals(btnClear)) {

            for (int i = 0; i < table.getRowCount(); i++) {
                tableModel.setValueAt("", i, 2);
            }

        } else if (e.getSource().equals(btnSelectMissing)) {

            table.clearSelection();
            for (int i = 0; i < table.getRowCount(); i++) {

                if (tableModel.getValueAt(i, 2).equals("")) {
                    table.getSelectionModel().addSelectionInterval(i, i);
                }

            }

        } else if (e.getSource() == btnSelectOld) {

            table.clearSelection();
            for (int i = 0; i < table.getRowCount(); i++) {

                if (oldEntries.contains(table.getValueAt(i, 0))) {
                    table.getSelectionModel().addSelectionInterval(i, i);
                }

            }

        }

    }

    @SuppressWarnings("unchecked")
    private Vector<String[]> getData(Vector<String[]> sourceEntries, Vector<String[]> fileEntries) {

        Vector<String[]> data = new Vector<String[]>();
        oldEntries.clear();

        for (String[] entry : sourceEntries) {

            String[] temp = new String[3];
            temp[0] = entry[0];
            temp[1] = entry[1];
            temp[2] = getValue(fileEntries, entry[0]);
            if (temp[2] == null) temp[2] = "";

            data.add(temp);

        }

        for (String[] entry : fileEntries) {

            if (getValue(data, entry[0]) == null) {

                String[] temp = new String[3];
                temp[0] = entry[0];
                temp[1] = "";
                temp[2] = entry[1];

                data.add(temp);
                oldEntries.add(temp[0]);

            }

        }

        Collections.sort(data, new StringArrayComparator());
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

    @SuppressWarnings("unchecked")
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

        Collections.sort(entries, new StringArrayComparator());
        return entries;

    }

    @SuppressWarnings("unchecked")
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

        Collections.sort(entries, new StringArrayComparator());
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

    @SuppressWarnings("unchecked")
    private class StringArrayComparator implements Comparator {

        public int compare(Object o1, Object o2) {

            if (o1 instanceof String[] && o2 instanceof String[]) {
                String[] s1 = (String[]) o1;
                String[] s2 = (String[]) o2;

                return s1[0].compareTo(s2[0]);

            } else
                return 1;

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

        public EditDialog(JFrame owner, String key, String sourceValue, String fileValue) {

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

            lblKey.setText("Key: " + key);
            taSourceValue.setText(sourceValue);
            taFileValue.setText(fileValue);
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

}
