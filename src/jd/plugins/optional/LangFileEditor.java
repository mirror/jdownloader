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

package jd.plugins.optional;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Vector;
import java.util.regex.Pattern;

import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JColorChooser;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.border.EmptyBorder;
import javax.swing.table.AbstractTableModel;

import jd.JDFileFilter;
import jd.PluginWrapper;
import jd.config.MenuItem;
import jd.config.SubConfiguration;
import jd.gui.skins.simple.LocationListener;
import jd.gui.skins.simple.SimpleGUI;
import jd.gui.skins.simple.components.ChartAPI_Entity;
import jd.gui.skins.simple.components.ChartAPI_PIE;
import jd.gui.skins.simple.components.ComboBrowseFile;
import jd.gui.skins.simple.components.JDFileChooser;
import jd.gui.skins.simple.components.JLinkButton;
import jd.http.Encoding;
import jd.parser.Regex;
import jd.plugins.PluginOptional;
import jd.utils.JDLocale;
import jd.utils.JDTheme;
import jd.utils.JDUtilities;

import org.jdesktop.swingx.JXTable;
import org.jdesktop.swingx.decorator.ColorHighlighter;
import org.jdesktop.swingx.decorator.ComponentAdapter;
import org.jdesktop.swingx.decorator.HighlightPredicate;
import org.jdesktop.swingx.decorator.HighlighterFactory;

/**
 * Editor for jDownloader language files. Gets JDLocale.L() and JDLocale.LF()
 * entries from source and compares them to the keypairs in the language file.
 * 
 * @author eXecuTe|Greeny
 */

public class LangFileEditor extends PluginOptional implements MouseListener {

    private SubConfiguration subConfig = JDUtilities.getSubConfig("ADDONS_LANGFILEEDITOR");
    private static final String PROPERTY_COLORIZE_MISSING = "PROPERTY_COLORIZE_MISSING";
    private static final String PROPERTY_COLORIZE_OLD = "PROPERTY_COLORIZE_OLD";
    private static final String PROPERTY_MISSING_COLOR = "PROPERTY_MISSING_COLOR";
    private static final String PROPERTY_OLD_COLOR = "PROPERTY_OLD_COLOR";
    private static final String PROPERTY_SOURCE = "PROPERTY_SOURCE";

    private JFrame frame;
    private JXTable table;
    private MyTableModel tableModel;
    private File sourceFile, languageFile;
    private JPanel topFile, topFolder;
    private JComboBox cmboSelectSource;
    private ComboBrowseFile cmboSource[], cmboFile;
    private ChartAPI_PIE keyChart;
    private ChartAPI_Entity entDone, entMissing, entOld;
    private JMenu mnuFile, mnuKey, mnuEntries;
    private JMenuItem mnuDownloadSource, mnuNew, mnuReload, mnuSave, mnuSaveAs, mnuClose;
    private JMenuItem mnuAdopt, mnuAdoptMissing, mnuClear, mnuClearAll, mnuContinueSearch, mnuDelete, mnuSearch, mnuTranslate, mnuTranslateMissing;
    private JMenuItem mnuPickMissingColor, mnuPickOldColor, mnuShowDupes;
    private JCheckBoxMenuItem mnuColorizeMissing, mnuColorizeOld;
    private JPopupMenu mnuContextPopup;
    private JMenuItem mnuContextAdopt, mnuContextClear, mnuContextDelete, mnuContextTranslate;

    private Vector<String[]> sourceEntries = new Vector<String[]>();
    private Vector<Pattern> sourcePatterns = new Vector<Pattern>();
    private Vector<String[]> fileEntries = new Vector<String[]>();
    private Vector<String[]> data = new Vector<String[]>();
    private Vector<String[]> dupes = new Vector<String[]>();
    private String lngKey = null;
    private String searchFor = "";
    private static final JDFileFilter fileFilter = new JDFileFilter(JDLocale.L("plugins.optional.langfileeditor.fileFilter", "LanguageFiles (*.lng)"), ".lng", true);
    private boolean colorizeMissing, colorizeOld;
    private Color colorMissing, colorOld;
    private ColorHighlighter missingHighlighter, oldHighlighter;

    public LangFileEditor(PluginWrapper wrapper) {
        super(wrapper);
    }

    private void showGui() {

        frame = new JFrame();
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.setTitle(JDLocale.L("plugins.optional.langfileeditor.title", "jDownloader - Language File Editor"));
        frame.setIconImage(JDTheme.I("gui.images.jd_logo"));
        frame.setMinimumSize(new Dimension(800, 500));
        frame.setPreferredSize(new Dimension(1200, 700));
        frame.setName("LANGFILEEDIT");
        frame.addWindowListener(new LocationListener());

        colorizeMissing = subConfig.getBooleanProperty(PROPERTY_COLORIZE_MISSING, true);
        colorizeOld = subConfig.getBooleanProperty(PROPERTY_COLORIZE_OLD, false);

        colorMissing = (Color) subConfig.getProperty(PROPERTY_MISSING_COLOR, Color.RED);
        colorOld = (Color) subConfig.getProperty(PROPERTY_OLD_COLOR, Color.ORANGE);

        missingHighlighter = new ColorHighlighter(new MissingPredicate(), colorMissing, null);
        oldHighlighter = new ColorHighlighter(new OldPredicate(), colorOld, null);

        tableModel = new MyTableModel();
        table = new JXTable(tableModel);
        table.getTableHeader().addMouseListener(this);
        table.getTableHeader().setReorderingAllowed(false);
        table.addMouseListener(this);
        table.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        table.setAutoStartEditOnKeyStroke(false);
        table.addHighlighter(HighlighterFactory.createAlternateStriping());
        table.addHighlighter(new ColorHighlighter(HighlightPredicate.ROLLOVER_ROW, null, Color.BLUE));
        if (colorizeMissing) table.addHighlighter(missingHighlighter);
        if (colorizeOld) table.addHighlighter(oldHighlighter);

        cmboSource = new ComboBrowseFile[2];

        cmboSource[0] = new ComboBrowseFile("LANGFILEEDITOR_SOURCEFOLDER");
        cmboSource[0].setFileSelectionMode(JDFileChooser.DIRECTORIES_ONLY);
        cmboSource[0].addActionListener(this);

        cmboSource[1] = new ComboBrowseFile("LANGFILEEDITOR_SOURCEFILE");
        cmboSource[1].setFileSelectionMode(JDFileChooser.FILES_ONLY);
        cmboSource[1].setFileFilter(fileFilter);
        cmboSource[1].addActionListener(this);

        cmboFile = new ComboBrowseFile("LANGFILEEDITOR_FILE");
        cmboFile.setFileSelectionMode(JDFileChooser.FILES_ONLY);
        cmboFile.setFileFilter(fileFilter);
        cmboFile.addActionListener(this);

        cmboSelectSource = new JComboBox(new String[] { JDLocale.L("plugins.optional.langfileeditor.sourceFolder", "Source Folder:"), JDLocale.L("plugins.optional.langfileeditor.sourceFile", "Source File:") });
        cmboSelectSource.setSelectedIndex(subConfig.getIntegerProperty(PROPERTY_SOURCE, 0));
        cmboSelectSource.addActionListener(this);

        topFolder = new JPanel(new BorderLayout(5, 5));
        topFolder.add(cmboSelectSource, BorderLayout.LINE_START);
        topFolder.add(cmboSource[cmboSelectSource.getSelectedIndex()]);

        topFile = new JPanel(new BorderLayout(5, 5));
        topFile.add(new JLabel(JDLocale.L("plugins.optional.langfileeditor.languageFile", "Language File:")), BorderLayout.LINE_START);
        topFile.add(cmboFile);

        keyChart = new ChartAPI_PIE(JDLocale.L("plugins.optional.langfileeditor.keychart", "KeyChart"), 250, 60, frame.getBackground());
        keyChart.addEntity(entDone = new ChartAPI_Entity(JDLocale.L("plugins.optional.langfileeditor.keychart.done", "Done"), 0, Color.GREEN));
        keyChart.addEntity(entMissing = new ChartAPI_Entity(JDLocale.L("plugins.optional.langfileeditor.keychart.missing", "Missing"), 0, colorMissing));
        keyChart.addEntity(entOld = new ChartAPI_Entity(JDLocale.L("plugins.optional.langfileeditor.keychart.old", "Probably Old"), 0, colorOld));

        JPanel topLeft = new JPanel(new BorderLayout(5, 5));
        topLeft.add(topFolder, BorderLayout.PAGE_START);
        topLeft.add(topFile, BorderLayout.PAGE_END);

        JPanel top = new JPanel(new BorderLayout(5, 5));
        top.add(topLeft, BorderLayout.CENTER);
        top.add(keyChart, BorderLayout.LINE_END);

        JPanel main = new JPanel(new BorderLayout(5, 5));
        main.setBorder(new EmptyBorder(10, 10, 10, 10));
        frame.setContentPane(main);
        main.add(top, BorderLayout.PAGE_START);
        main.add(new JScrollPane(table));

        buildMenu();

        frame.setResizable(true);
        frame.pack();
        SimpleGUI.restoreWindow(null, frame);
        frame.setVisible(true);

        sourceFile = cmboSource[cmboSelectSource.getSelectedIndex()].getCurrentPath();
        if (sourceFile != null) getSourceEntries();
        languageFile = cmboFile.getCurrentPath();
        if (languageFile == null) cmboFile.setCurrentPath(JDLocale.getLanguageFile());
        getLanguageFileEntries();
        initLocaleData();

    }

    private void setInfoLabels() {

        int numMissing = 0, numOld = 0;

        for (String[] entry : data) {

            if (entry[1].equals("")) {
                numOld++;
            } else if (entry[2].equals("")) {
                numMissing++;
            }

        }

        entDone.setData(data.size() - numMissing - numOld);
        entDone.setCaption(JDLocale.L("plugins.optional.langfileeditor.keychart.done", "Done") + " [" + entDone.getData() + "]");
        entMissing.setData(numMissing);
        entMissing.setCaption(JDLocale.L("plugins.optional.langfileeditor.keychart.missing", "Missing") + " [" + entMissing.getData() + "]");
        entOld.setData(numOld);
        entOld.setCaption(JDLocale.L("plugins.optional.langfileeditor.keychart.old", "Probably Old") + " [" + entOld.getData() + "]");
        keyChart.fetchImage();
    }

    private void buildMenu() {
        // File Menü
        mnuFile = new JMenu(JDLocale.L("plugins.optional.langfileeditor.file", "File"));

        mnuFile.add(mnuNew = new JMenuItem(JDLocale.L("plugins.optional.langfileeditor.new", "New")));
        mnuFile.addSeparator();
        mnuFile.add(mnuSave = new JMenuItem(JDLocale.L("plugins.optional.langfileeditor.save", "Save")));
        mnuFile.add(mnuSaveAs = new JMenuItem(JDLocale.L("plugins.optional.langfileeditor.saveAs", "Save As")));
        mnuFile.addSeparator();
        mnuFile.add(mnuReload = new JMenuItem(JDLocale.L("plugins.optional.langfileeditor.reload", "Reload")));
        mnuFile.addSeparator();
        mnuFile.add(mnuDownloadSource = new JMenuItem(JDLocale.L("plugins.optional.langfileeditor.download", "Download SourceCode")));
        mnuFile.addSeparator();
        mnuFile.add(mnuClose = new JMenuItem(JDLocale.L("plugins.optional.langfileeditor.close", "Close")));

        mnuNew.addActionListener(this);
        mnuSave.addActionListener(this);
        mnuSaveAs.addActionListener(this);
        mnuReload.addActionListener(this);
        mnuDownloadSource.addActionListener(this);
        mnuClose.addActionListener(this);

        mnuSave.setEnabled(false);
        mnuSaveAs.setEnabled(false);
        mnuReload.setEnabled(false);

        mnuNew.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_N, KeyEvent.CTRL_DOWN_MASK));
        mnuSave.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, KeyEvent.CTRL_DOWN_MASK));
        mnuReload.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_R, KeyEvent.CTRL_DOWN_MASK));

        // Key Menü
        mnuKey = new JMenu(JDLocale.L("plugins.optional.langfileeditor.key", "Key"));
        mnuKey.setEnabled(false);

        mnuKey.add(mnuDelete = new JMenuItem(JDLocale.L("plugins.optional.langfileeditor.deleteKeys", "Delete Key(s)")));
        mnuKey.addSeparator();
        mnuKey.add(mnuClear = new JMenuItem(JDLocale.L("plugins.optional.langfileeditor.clearValues", "Clear Value(s)")));
        mnuKey.add(mnuClearAll = new JMenuItem(JDLocale.L("plugins.optional.langfileeditor.clearAllValues", "Clear All Values")));
        mnuKey.addSeparator();
        mnuKey.add(mnuAdopt = new JMenuItem(JDLocale.L("plugins.optional.langfileeditor.adoptDefaults", "Adopt Default(s)")));
        mnuKey.add(mnuAdoptMissing = new JMenuItem(JDLocale.L("plugins.optional.langfileeditor.adoptDefaults.missing", "Adopt Defaults of Missing Entries")));
        mnuKey.addSeparator();
        mnuKey.add(mnuTranslate = new JMenuItem(JDLocale.L("plugins.optional.langfileeditor.translate", "Translate with Google")));
        mnuKey.add(mnuTranslateMissing = new JMenuItem(JDLocale.L("plugins.optional.langfileeditor.translate.missing", "Translate Missing Entries with Google")));

        mnuDelete.addActionListener(this);
        mnuClear.addActionListener(this);
        mnuClearAll.addActionListener(this);
        mnuAdopt.addActionListener(this);
        mnuAdoptMissing.addActionListener(this);
        mnuTranslate.addActionListener(this);
        mnuTranslateMissing.addActionListener(this);

        mnuDelete.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0));

        // Entries Menü
        mnuEntries = new JMenu(JDLocale.L("plugins.optional.langfileeditor.entries", "Entries"));
        mnuEntries.setEnabled(false);

        mnuEntries.add(mnuColorizeMissing = new JCheckBoxMenuItem(JDLocale.L("plugins.optional.langfileeditor.colorizeMissing", "Colorize Missing Entries")));
        mnuEntries.add(mnuPickMissingColor = new JMenuItem(JDLocale.L("plugins.optional.langfileeditor.pickMissingColor", "Pick Color for Missing Entries")));
        mnuEntries.addSeparator();
        mnuEntries.add(mnuColorizeOld = new JCheckBoxMenuItem(JDLocale.L("plugins.optional.langfileeditor.colorizeOld", "Colorize Old Entries")));
        mnuEntries.add(mnuPickOldColor = new JMenuItem(JDLocale.L("plugins.optional.langfileeditor.pickOldColor", "Pick Color for Old Entries")));
        mnuEntries.addSeparator();
        mnuEntries.add(mnuShowDupes = new JMenuItem(JDLocale.L("plugins.optional.langfileeditor.showDupes", "Show Dupes")));
        mnuEntries.addSeparator();
        mnuEntries.add(mnuSearch = new JMenuItem(JDLocale.L("plugins.optional.langfileeditor.search", "Search")));
        mnuEntries.add(mnuContinueSearch = new JMenuItem(JDLocale.L("plugins.optional.langfileeditor.continueSearch", "Continue Search")));

        mnuColorizeMissing.setSelected(colorizeMissing);
        mnuColorizeOld.setSelected(colorizeOld);

        mnuColorizeMissing.setIcon(JDTheme.II((mnuColorizeMissing.isSelected()) ? "gui.images.selected" : "gui.images.unselected"));
        mnuColorizeOld.setIcon(JDTheme.II((mnuColorizeOld.isSelected()) ? "gui.images.selected" : "gui.images.unselected"));

        mnuColorizeMissing.addActionListener(this);
        mnuPickMissingColor.addActionListener(this);
        mnuColorizeOld.addActionListener(this);
        mnuPickOldColor.addActionListener(this);
        mnuShowDupes.addActionListener(this);
        mnuSearch.addActionListener(this);
        mnuContinueSearch.addActionListener(this);

        mnuSearch.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F3, 0));
        mnuContinueSearch.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F4, 0));

        JMenuBar menuBar = new JMenuBar();
        menuBar.add(mnuFile);
        menuBar.add(mnuKey);
        menuBar.add(mnuEntries);
        frame.setJMenuBar(menuBar);

        // Context Menü
        mnuContextPopup = new JPopupMenu();

        mnuContextPopup.add(mnuContextDelete = new JMenuItem(JDLocale.L("plugins.optional.langfileeditor.deleteKeys", "Delete Key(s)")));
        mnuContextPopup.add(mnuContextClear = new JMenuItem(JDLocale.L("plugins.optional.langfileeditor.clearValues", "Clear Value(s)")));
        mnuContextPopup.addSeparator();
        mnuContextPopup.add(mnuContextAdopt = new JMenuItem(JDLocale.L("plugins.optional.langfileeditor.adoptDefaults", "Adopt Default(s)")));
        mnuContextPopup.add(mnuContextTranslate = new JMenuItem(JDLocale.L("plugins.optional.langfileeditor.translate", "Translate with Google")));

        mnuContextDelete.addActionListener(this);
        mnuContextClear.addActionListener(this);
        mnuContextAdopt.addActionListener(this);
        mnuContextTranslate.addActionListener(this);

    }

    @Override
    public void actionPerformed(ActionEvent e) {

        if (e.getSource() instanceof MenuItem && ((MenuItem) e.getSource()).getActionID() == 0) {

            if (frame == null || !frame.isVisible()) {
                showGui();
            } else {
                frame.toFront();
            }

        } else if (e.getSource() == cmboSelectSource) {

            int index = cmboSelectSource.getSelectedIndex();

            if (index != subConfig.getIntegerProperty(PROPERTY_SOURCE, 0)) {
                subConfig.setProperty(PROPERTY_SOURCE, index);
                subConfig.save();

                topFolder.remove(cmboSource[1 - index]);
                topFolder.add(cmboSource[index]);

                sourceFile = cmboSource[index].getCurrentPath();
                getSourceEntries();
                initLocaleData();
            }

        } else if (e.getSource() == cmboSource[0] || e.getSource() == cmboSource[1]) {

            File sourceFile = cmboSource[cmboSelectSource.getSelectedIndex()].getCurrentPath();
            if (sourceFile != this.sourceFile && sourceFile != null) {
                this.sourceFile = sourceFile;
                getSourceEntries();
                initLocaleData();
            }

        } else if (e.getSource() == cmboFile) {

            File languageFile = cmboFile.getCurrentPath();
            if (languageFile != this.languageFile && languageFile != null) {
                this.languageFile = languageFile;
                getLanguageFileEntries();
                initLocaleData();
            }

        } else if (e.getSource() == mnuNew) {

            JDFileChooser chooser = new JDFileChooser("LANGFILEEDITOR_FILE");
            chooser.setFileFilter(fileFilter);

            if (chooser.showSaveDialog(frame) == JDFileChooser.APPROVE_OPTION) {
                languageFile = chooser.getSelectedFile();
                if (!languageFile.getAbsolutePath().endsWith(".lng")) languageFile = new File(languageFile.getAbsolutePath() + ".lng");
                cmboFile.setCurrentPath(languageFile);
            }

        } else if (e.getSource() == mnuSave) {

            saveLanguageFile(languageFile);

        } else if (e.getSource() == mnuSaveAs) {

            JDFileChooser chooser = new JDFileChooser("LANGFILEEDITOR_FILE");
            chooser.setFileFilter(fileFilter);

            if (chooser.showSaveDialog(frame) == JDFileChooser.APPROVE_OPTION) {
                languageFile = chooser.getSelectedFile();
                if (!languageFile.getAbsolutePath().endsWith(".lng")) languageFile = new File(languageFile.getAbsolutePath() + ".lng");
                saveLanguageFile(languageFile);
            }

        } else if (e.getSource() == mnuDelete || e.getSource() == mnuContextDelete) {

            deleteSelectedKeys();

        } else if (e.getSource() == mnuAdoptMissing) {

            for (int i = 0; i < data.size(); i++) {

                if (tableModel.getValueAt(i, 2).equals("")) {
                    tableModel.setValueAt(tableModel.getValueAt(i, 1), i, 2);
                }

            }

        } else if (e.getSource() == mnuReload) {

            getSourceEntries();
            getLanguageFileEntries();
            initLocaleData();

        } else if (e.getSource() == mnuAdopt || e.getSource() == mnuContextAdopt) {

            for (int i : getSelectedRows()) {
                String def = tableModel.getValueAt(i, 1);
                if (!def.equals("")) tableModel.setValueAt(def, i, 2);
            }

        } else if (e.getSource() == mnuClear || e.getSource() == mnuContextClear) {

            for (int i : getSelectedRows()) {
                tableModel.setValueAt("", i, 2);
            }

            setInfoLabels();

        } else if (e.getSource() == mnuClearAll) {

            for (int i = 0; i < table.getRowCount(); i++) {
                tableModel.setValueAt("", i, 2);
            }

        } else if (e.getSource() == mnuColorizeMissing) {

            colorizeMissing = mnuColorizeMissing.isSelected();
            subConfig.setProperty(PROPERTY_COLORIZE_MISSING, colorizeMissing);
            subConfig.save();
            mnuColorizeMissing.setIcon(JDTheme.II((mnuColorizeMissing.isSelected()) ? "gui.images.selected" : "gui.images.unselected"));
            if (colorizeMissing) {
                table.addHighlighter(missingHighlighter);
            } else {
                table.removeHighlighter(missingHighlighter);
            }
            tableModel.fireTableDataChanged();

        } else if (e.getSource() == mnuColorizeOld) {

            colorizeOld = mnuColorizeOld.isSelected();
            subConfig.setProperty(PROPERTY_COLORIZE_OLD, colorizeOld);
            subConfig.save();
            mnuColorizeOld.setIcon(JDTheme.II((mnuColorizeOld.isSelected()) ? "gui.images.selected" : "gui.images.unselected"));
            if (colorizeOld) {
                table.addHighlighter(oldHighlighter);
            } else {
                table.removeHighlighter(oldHighlighter);
            }
            tableModel.fireTableDataChanged();

        } else if (e.getSource() == mnuPickMissingColor) {

            Color newColor = JColorChooser.showDialog(frame, JDLocale.L("plugins.optional.langfileeditor.pickMissingColor", "Pick Color for Missing Entries"), colorMissing);
            if (newColor != null) {
                colorMissing = newColor;
                subConfig.setProperty(PROPERTY_MISSING_COLOR, colorMissing);
                subConfig.save();
                tableModel.fireTableDataChanged();
                entMissing.setColor(colorMissing);
                keyChart.fetchImage();
            }

        } else if (e.getSource() == mnuPickOldColor) {

            Color newColor = JColorChooser.showDialog(frame, JDLocale.L("plugins.optional.langfileeditor.pickOldColor", "Pick Color for Old Entries"), colorOld);
            if (newColor != null) {
                colorOld = newColor;
                subConfig.setProperty(PROPERTY_OLD_COLOR, colorOld);
                subConfig.save();
                tableModel.fireTableDataChanged();
                entOld.setColor(colorOld);
                keyChart.fetchImage();
            }

        } else if (e.getSource() == mnuShowDupes) {

            new DupeDialog(frame, dupes);

        } else if (e.getSource() == mnuTranslateMissing) {

            if (lngKey == null) {
                String result = JOptionPane.showInputDialog(frame, JDLocale.L("plugins.optional.langfileeditor.translate.message", "Please insert the Language Key to provide a correct translation of Google:"), JDLocale.L("plugins.optional.langfileeditor.translate.title", "Insert Language Key"), JOptionPane.QUESTION_MESSAGE);
                if (result == null) return;
                if (!isSupportedLanguageKey(result)) {
                    JOptionPane.showMessageDialog(frame, JDLocale.L("plugins.optional.langfileeditor.translate.error.message", "The Language Key, you have entered, is not supported by Google!"), JDLocale.L("plugins.optional.langfileeditor.translate.error.title", "Wrong Language Key!"), JOptionPane.ERROR_MESSAGE);
                    return;
                }
                lngKey = result;
            }

            for (int i = 0; i < data.size(); i++) {

                if (tableModel.getValueAt(i, 2).equals("")) {

                    String def = data.get(i)[1];

                    if (!def.equals("")) {
                        logger.finer("Working on " + tableModel.getValueAt(i, 0) + ":");
                        String result = JDLocale.translate(lngKey, def);
                        logger.finer("Google translated \"" + def + "\" to \"" + result + "\" with LanguageKey " + lngKey);
                        tableModel.setValueAt(result, i, 2);
                    }

                }

            }

        } else if (e.getSource() == mnuTranslate || e.getSource() == mnuContextTranslate) {

            if (lngKey == null) {
                String result = JOptionPane.showInputDialog(frame, JDLocale.L("plugins.optional.langfileeditor.translate.message", "Please insert the Language Key to provide a correct translation of Google:"), JDLocale.L("plugins.optional.langfileeditor.translate.title", "Insert Language Key"), JOptionPane.QUESTION_MESSAGE);
                if (result == null) return;
                if (!isSupportedLanguageKey(result)) {
                    JOptionPane.showMessageDialog(frame, JDLocale.L("plugins.optional.langfileeditor.translate.error.message", "The Language Key, you have entered, is not supported by Google!"), JDLocale.L("plugins.optional.langfileeditor.translate.error.title", "Wrong Language Key!"), JOptionPane.ERROR_MESSAGE);
                    return;
                }
                lngKey = result;
            }

            for (int i : getSelectedRows()) {

                String def = tableModel.getValueAt(i, 1);

                if (!def.equals("")) {
                    logger.finer("Working on " + tableModel.getValueAt(i, 0) + ":");
                    String result = JDLocale.translate(lngKey, def);
                    logger.finer("Google translated \"" + def + "\" to \"" + result + "\" with LanguageKey " + lngKey);
                    tableModel.setValueAt(result, i, 2);
                }

            }

        } else if (e.getSource() == mnuClose) {

            frame.setVisible(false);
            frame.dispose();

        } else if (e.getSource() == mnuDownloadSource) {

            String svnUrl = "https://www.syncom.org/svn/jdownloader/";
            if (JOptionPane.showConfirmDialog(frame, JDLocale.LF("plugins.optional.langfileeditor.downloadSource", "The SourceCode can be obtained via SVN.\nThe SVN repository is located at %s\nPress OK to open the repository!", svnUrl), null, JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION) {
                try {
                    JLinkButton.openURL(svnUrl);
                } catch (Exception e1) {
                    e1.printStackTrace();
                }
            }

        } else if (e.getSource() == mnuSearch) {

            searchFor = JOptionPane.showInputDialog(frame, JDLocale.L("plugins.optional.langfileeditor.searchFor", "String to search for: (Startposition is the first selected row)"), searchFor);
            search();

        } else if (e.getSource() == mnuContinueSearch) {

            search();

        }

    }

    private void search() {
        if (!searchFor.equals("")) {
            int begin = table.getSelectedRow();
            if (begin == table.getRowCount()) begin = -1;
            if (table.getSearchable().search(searchFor, begin) == -1) {
                table.getSearchable().search(searchFor, -1);
            }
        }
    }

    private void deleteSelectedKeys() {
        int[] rows = getSelectedRows();

        int len = rows.length - 1;
        for (int i = len; i >= 0; --i) {
            int cur = rows[i];
            String temp = data.remove(cur)[0];
            for (int j = dupes.size() - 1; j >= 0; --j) {
                if (dupes.get(j)[1].equals(temp) || dupes.get(j)[2].equals(temp)) dupes.remove(j);
            }
            tableModel.fireTableRowsDeleted(cur, cur);
        }
        int newRow = Math.min(rows[len] - len, data.size() - 1);
        table.getSelectionModel().setSelectionInterval(newRow, newRow);

        setInfoLabels();
    }

    private int[] getSelectedRows() {
        int[] rows = table.getSelectedRows();
        int[] ret = new int[rows.length];

        for (int i = 0; i < rows.length; ++i) {
            ret[i] = getOriginalIndex(rows[i]);
        }

        return ret;
    }

    private int getOriginalIndex(int row) {
        String key = table.getValueAt(row, 0).toString();

        for (int i = 0; i < data.size(); ++i) {
            if (data.get(i)[0].equalsIgnoreCase(key)) return i;
        }

        return -1;
    }

    private boolean isSupportedLanguageKey(String lngKey) {
        String[] lngKeys = new String[] { "da", "de", "fi", "fr", "el", "hi", "it", "ja", "ko", "hr", "nl", "no", "pl", "pt", "ro", "ru", "sv", "es", "cs", "en", "ar" };

        for (String element : lngKeys) {
            if (element.equals(lngKey)) return true;
        }
        return false;
    }

    private void saveLanguageFile(File file) {
        StringBuilder sb = new StringBuilder();

        for (String[] entry : data) {
            if (!entry[2].equals("")) sb.append(entry[0] + " = " + entry[2] + "\n");
        }

        try {
            BufferedWriter out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), "UTF8"));
            out.write(sb.toString());
            out.close();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(frame, JDLocale.LF("plugins.optional.langfileeditor.save.error.message", "An error occured while writing the LanguageFile:\n%s", e.getMessage()), JDLocale.L("plugins.optional.langfileeditor.save.error.title", "Error!"), JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (languageFile.getAbsolutePath() != cmboFile.getText()) cmboFile.setCurrentPath(languageFile);
        JOptionPane.showMessageDialog(frame, JDLocale.L("plugins.optional.langfileeditor.save.success.message", "LanguageFile saved successfully!"), JDLocale.L("plugins.optional.langfileeditor.save.success.title", "Save successful!"), JOptionPane.INFORMATION_MESSAGE);
    }

    private void initLocaleData() {

        Vector<String[]> dupeHelp = new Vector<String[]>();
        data.clear();
        dupes.clear();
        lngKey = null;

        String value;
        String[] temp;
        for (String[] entry : sourceEntries) {

            temp = new String[] { entry[0], entry[1], getValue(fileEntries, entry[0]) };
            if (temp[2] == null) temp[2] = "";

            data.add(temp);
            if (!temp[2].equals("")) {

                value = getValue(dupeHelp, temp[2]);
                if (value != null) dupes.add(new String[] { temp[2], temp[0], value });
                dupeHelp.add(new String[] { temp[2], temp[0] });

            }

        }

        boolean breakIt;
        for (String[] entry : fileEntries) {

            if (getValue(data, entry[0]) == null) {

                breakIt = false;
                for (Pattern pattern : sourcePatterns) {

                    if (pattern.matcher(entry[0]).matches()) {

                        data.add(new String[] { entry[0], JDLocale.L("plugins.optional.langfileeditor.patternEntry", "<Entry matches Pattern>"), entry[1] });
                        breakIt = true;
                        break;

                    }

                }

                if (entry[0].startsWith("$") && entry[0].endsWith("$")) {
                    data.add(new String[] { entry[0], JDLocale.L("plugins.optional.langfileeditor.languageFileInfo", "<LanguageFile Information Entry>"), entry[1] });
                    breakIt = true;
                }

                if (breakIt) continue;
                data.add(new String[] { entry[0], "", entry[1] });

            }

        }

        Collections.sort(data, new Comparator<String[]>() {
            public int compare(String[] a, String[] b) {
                return a[0].compareToIgnoreCase(b[0]);
            }
        });
        tableModel.fireTableRowsInserted(0, data.size() - 1);
        if (languageFile != null) frame.setTitle(JDLocale.L("plugins.optional.langfileeditor.title", "jDownloader - Language File Editor") + " [" + languageFile.getAbsolutePath() + "]");

        setInfoLabels();
        mnuEntries.setEnabled(true);
        mnuKey.setEnabled(true);
        mnuReload.setEnabled(true);
        mnuSave.setEnabled(true);
        mnuSaveAs.setEnabled(true);
    }

    private String getValue(Vector<String[]> vector, String key) {

        for (String[] entry : vector) {
            if (entry[0].equalsIgnoreCase(key)) return entry[1];
        }

        return null;

    }

    private void getLanguageFileEntries() {

        getLanguageFileEntries(languageFile, fileEntries);

    }

    private void getLanguageFileEntries(File file, Vector<String[]> data) {

        data.clear();
        Vector<String> keys = new Vector<String>();

        String[] lines = Regex.getLines(JDUtilities.getLocalFile(file));

        for (String line : lines) {
            String[] match = new Regex(line, Pattern.compile("^(.*?)[\\s]*?=[\\s]*?(.*?)$")).getRow(0);

            match[0] = match[0].trim().toLowerCase();
            match[1] = match[1].trim() + ((match[1].endsWith(" ")) ? " " : "");
            if (!keys.contains(match[0]) && !match[0].equals("") && !match[1].equals("")) {

                match[0] = Encoding.UTF8Decode(match[0]);
                match[1] = Encoding.UTF8Decode(match[1]);
                keys.add(match[0]);
                data.add(new String[] { match[0], match[1] });

            }

        }

    }

    private void getSourceEntries() {

        if (cmboSelectSource.getSelectedIndex() == 0) {
            getSourceEntriesFromFolder();
        } else {
            getSourceEntriesFromFile();
        }

    }

    private void getSourceEntriesFromFile() {

        sourcePatterns.clear();
        getLanguageFileEntries(sourceFile, sourceEntries);

    }

    private void getSourceEntriesFromFolder() {

        sourceEntries.clear();
        sourcePatterns.clear();
        Vector<String> keys = new Vector<String>();

        String[][] matches;
        for (File file : getSourceFiles(sourceFile)) {

            matches = new Regex(JDUtilities.getLocalFile(file), Pattern.compile("JDLocale[\\s]*\\.L[F]?[\\s]*\\([\\s]*\"(.*?)\"[\\s]*,[\\s]*(\".*?\"|.*?)[\\s]*[,\\)]")).getMatches();

            for (String[] match : matches) {

                match[0] = match[0].trim().toLowerCase();
                if (keys.contains(match[0])) continue;

                if (match[0].indexOf("\"") == -1) {

                    match[0] = Encoding.UTF8Decode(match[0]);
                    match[1] = Encoding.UTF8Decode(match[1].substring(1, match[1].length() - 1));
                    keys.add(match[0]);
                    sourceEntries.add(new String[] { match[0], match[1] });

                } else {

                    if (match[0].contains(",")) match[0] = match[0].substring(0, match[0].indexOf(",") + 1);
                    match[0] = match[0].replaceAll("\\.", "\\\\.");
                    match[0] = match[0].replaceAll("\"(.*?)[\",]", "(.*?)");

                    if (keys.contains(match[0])) continue;

                    keys.add(Encoding.UTF8Decode(match[0]));
                    sourcePatterns.add(Pattern.compile(match[0], Pattern.CASE_INSENSITIVE));

                }

            }

        }

    }

    private Vector<File> getSourceFiles(File directory) {

        Vector<File> fileContents = new Vector<File>();

        for (File entry : directory.listFiles()) {

            if (entry.isDirectory()) {

                fileContents.addAll(getSourceFiles(entry));

            } else if (entry.isFile()) {

                if (JDUtilities.getFileExtension(entry).equals("java")) fileContents.add(entry);

            }

        }

        return fileContents;

    }

    public void mouseClicked(MouseEvent e) {
        if (e.isPopupTrigger() || e.getButton() == MouseEvent.BUTTON3) {
            int row = table.rowAtPoint(e.getPoint());
            if (!table.isRowSelected(row)) {
                table.getSelectionModel().setSelectionInterval(row, row);
            }
            mnuContextPopup.show(table, e.getX(), e.getY());
        }
    }

    public void mouseEntered(MouseEvent e) {
    }

    public void mouseExited(MouseEvent e) {
    }

    public void mousePressed(MouseEvent e) {
    }

    public void mouseReleased(MouseEvent e) {
    }

    private class MissingPredicate implements HighlightPredicate {

        public boolean isHighlighted(Component arg0, ComponentAdapter arg1) {
            return (table.getValueAt(arg1.row, 2).equals(""));
        }

    }

    private class OldPredicate implements HighlightPredicate {

        public boolean isHighlighted(Component arg0, ComponentAdapter arg1) {
            return (table.getValueAt(arg1.row, 1).equals(""));
        }

    }

    private class MyTableModel extends AbstractTableModel {

        private static final long serialVersionUID = -5434313385327397539L;

        private String[] columnNames = { JDLocale.L("plugins.optional.langfileeditor.key", "Key"), JDLocale.L("plugins.optional.langfileeditor.sourceValue", "Source Value"), JDLocale.L("plugins.optional.langfileeditor.languageFileValue", "Language File Value") };

        public int getColumnCount() {
            return columnNames.length;
        }

        public int getRowCount() {
            return data.size();
        }

        public String getColumnName(int col) {
            return columnNames[col];
        }

        public String getValueAt(int row, int col) {
            return data.get(row)[col];
        }

        public Class<?> getColumnClass(int c) {
            return String.class;
        }

        public boolean isCellEditable(int row, int col) {
            return (col == 2);
        }

        public void setValueAt(Object value, int row, int col) {
            data.get(row)[col] = (String) value;
            this.fireTableRowsUpdated(row, row);
            setInfoLabels();
        }

    }

    private class DupeDialog extends JDialog implements ActionListener {

        private static final long serialVersionUID = 1L;

        public DupeDialog(JFrame owner, Vector<String[]> dupes) {

            super(owner);

            setModal(true);
            setLayout(new BorderLayout());
            setTitle(JDLocale.L("plugins.optional.langfileeditor.duplicatedEntries", "Duplicated Entries") + " [" + dupes.size() + "]");

            JXTable table = new JXTable(new MyDupeTableModel(dupes));
            table.getTableHeader().setReorderingAllowed(false);
            table.addHighlighter(HighlighterFactory.createAlternateStriping());
            table.addHighlighter(new ColorHighlighter(HighlightPredicate.ROLLOVER_ROW, null, Color.BLUE));
            JScrollPane scroll = new JScrollPane(table);
            scroll.setPreferredSize(new Dimension(900, 350));

            JButton btnClose = new JButton(JDLocale.L("plugins.optional.langfileeditor.close", "Close"));
            btnClose.addActionListener(this);
            getRootPane().setDefaultButton(btnClose);

            JPanel buttons = new JPanel(new FlowLayout());
            buttons.add(btnClose);

            add(scroll, BorderLayout.CENTER);
            add(buttons, BorderLayout.PAGE_END);

            pack();
            setLocation(JDUtilities.getCenterOfComponent(owner, this));
            setVisible(true);

        }

        public void actionPerformed(ActionEvent e) {

            dispose();

        }

        class MyDupeTableModel extends AbstractTableModel {

            private static final long serialVersionUID = -5434313385327397539L;

            private String[] columnNames = { JDLocale.L("plugins.optional.langfileeditor.string", "String"), JDLocale.L("plugins.optional.langfileeditor.firstKey", "First Key"), JDLocale.L("plugins.optional.langfileeditor.secondKey", "Second Key") };

            private Vector<String[]> tableData;

            public MyDupeTableModel(Vector<String[]> data) {
                tableData = data;
                this.fireTableRowsInserted(0, tableData.size() - 1);
            }

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

        }

    }

    @Override
    public String getRequirements() {
        return "JRE 1.5+";
    }

    @Override
    public boolean initAddon() {
        return true;
    }

    @Override
    public void onExit() {
    }

    @Override
    public ArrayList<MenuItem> createMenuitems() {
        ArrayList<MenuItem> menu = new ArrayList<MenuItem>();

        menu.add(new MenuItem(MenuItem.NORMAL, JDLocale.L("plugins.optional.langfileeditor.show_gui", "Show GUI"), 0).setActionListener(this));

        return menu;
    }

    @Override
    public String getHost() {
        return JDLocale.L("plugins.optional.langfileeditor.name", "JDLangFileEditor");
    }

    @Override
    public String getVersion() {
        return getVersion("$Revision$");
    }

    public static int getAddonInterfaceVersion() {
        return 2;
    }

}
