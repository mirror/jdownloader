//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team support@jdownloader.org
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
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Vector;
import java.util.Map.Entry;

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

import jd.PluginWrapper;
import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.config.MenuItem;
import jd.config.SubConfiguration;
import jd.gui.skins.simple.LocationListener;
import jd.gui.skins.simple.SimpleGUI;
import jd.gui.skins.simple.components.ChartAPI_Entity;
import jd.gui.skins.simple.components.ChartAPI_PIE;
import jd.gui.skins.simple.components.ComboBrowseFile;
import jd.gui.skins.simple.components.JDFileChooser;
import jd.nutils.io.JDFileFilter;
import jd.nutils.io.JDIO;
import jd.nutils.svn.Subversion;
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
import org.tmatesoft.svn.core.SVNException;

/**
 * Editor for jDownloader language files. Gets JDLocale.L() and JDLocale.LF()
 * entries from source and compares them to the keypairs in the language file.
 * 
 * @author eXecuTe
 * @author Greeny
 */
public class LangFileEditor extends PluginOptional implements MouseListener {

    private SubConfiguration subConfig = null;

    private static final String PROPERTY_COLORIZE_DONE = "PROPERTY_COLORIZE_DONE";
    private static final String PROPERTY_COLORIZE_MISSING = "PROPERTY_COLORIZE_MISSING";
    private static final String PROPERTY_COLORIZE_OLD = "PROPERTY_COLORIZE_OLD";
    private static final String PROPERTY_DONE_COLOR = "PROPERTY_DONE_COLOR";
    private static final String PROPERTY_MISSING_COLOR = "PROPERTY_MISSING_COLOR";
    private static final String PROPERTY_OLD_COLOR = "PROPERTY_OLD_COLOR";

    private static final String PROPERTY_SOURCE = "PROPERTY_SOURCE";

    private static final String PROPERTY_SVN_REPOSITORY = "PROPERTY_SVN_REPOSITORY";
    private static final String PROPERTY_SVN_ACCESS_ANONYMOUS = "PROPERTY_SVN_CHECKOUT_ANONYMOUS";
    private static final String PROPERTY_SVN_ACCESS_USER = "PROPERTY_SVN_CHECKOUT_USER";
    private static final String PROPERTY_SVN_ACCESS_PASS = "PROPERTY_SVN_CHECKOUT_PASS";
    private static final String PROPERTY_SVN_WORKING_COPY = "PROPERTY_SVN_WORKING_COPY";
    private static final String PROPERTY_SVN_UPDATE_ON_START = "PROPERTY_SVN_UPDATE_ON_START";

    private JFrame frame;
    private JXTable table;
    private MyTableModel tableModel;
    private File sourceFile, languageFile;
    private JPanel topFile, topFolder;
    private JComboBox cmboSelectSource;
    private ComboBrowseFile cmboSource[], cmboFile;
    private ChartAPI_PIE keyChart;
    private ChartAPI_Entity entDone, entMissing, entOld;
    private JMenu mnuFile, mnuSVN, mnuKey, mnuEntries;
    private JMenuItem mnuNew, mnuReload, mnuSave, mnuSaveAs, mnuClose;
    private JMenuItem mnuSVNSettings, mnuSVNCheckOutNow;;
    private JMenuItem mnuAdd, mnuAdopt, mnuAdoptMissing, mnuClear, mnuDelete, mnuTranslate, mnuTranslateMissing;
    private JMenuItem mnuPickDoneColor, mnuPickMissingColor, mnuPickOldColor, mnuShowDupes;
    private JCheckBoxMenuItem mnuColorizeDone, mnuColorizeMissing, mnuColorizeOld;
    private JPopupMenu mnuContextPopup;
    private JMenuItem mnuContextAdopt, mnuContextClear, mnuContextDelete, mnuContextTranslate;

    private HashMap<String, String> sourceEntries = new HashMap<String, String>();
    private Vector<String> sourcePatterns = new Vector<String>();
    private HashMap<String, String> fileEntries = new HashMap<String, String>();
    private Vector<KeyInfo> data = new Vector<KeyInfo>();
    private HashMap<String, Vector<String>> dupes = new HashMap<String, Vector<String>>();
    private String lngKey = null;
    private boolean changed = false;
    private static final JDFileFilter fileFilter = new JDFileFilter(JDLocale.L("plugins.optional.langfileeditor.fileFilter", "LanguageFiles (*.lng)"), ".lng", true);
    private boolean colorizeDone, colorizeMissing, colorizeOld;
    private Color colorDone, colorMissing, colorOld;
    private ColorHighlighter doneHighlighter, missingHighlighter, oldHighlighter;

    public LangFileEditor(PluginWrapper wrapper) {
        super(wrapper);
        subConfig = JDUtilities.getSubConfig("ADDONS_LANGFILEEDITOR");
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

        colorizeDone = subConfig.getBooleanProperty(PROPERTY_COLORIZE_DONE, false);
        colorizeMissing = subConfig.getBooleanProperty(PROPERTY_COLORIZE_MISSING, true);
        colorizeOld = subConfig.getBooleanProperty(PROPERTY_COLORIZE_OLD, false);

        colorDone = (Color) subConfig.getProperty(PROPERTY_DONE_COLOR, Color.GREEN);
        colorMissing = (Color) subConfig.getProperty(PROPERTY_MISSING_COLOR, Color.RED);
        colorOld = (Color) subConfig.getProperty(PROPERTY_OLD_COLOR, Color.ORANGE);

        doneHighlighter = new ColorHighlighter(new DonePredicate(), colorDone, null);
        missingHighlighter = new ColorHighlighter(new MissingPredicate(), colorMissing, null);
        oldHighlighter = new ColorHighlighter(new OldPredicate(), colorOld, null);

        tableModel = new MyTableModel();
        table = new JXTable(tableModel);
        table.getTableHeader().setReorderingAllowed(false);
        table.getColumn(0).setMinWidth(200);
        table.getColumn(0).setPreferredWidth(200);
        table.addMouseListener(this);
        table.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        table.setAutoStartEditOnKeyStroke(false);
        table.addHighlighter(HighlighterFactory.createAlternateStriping());
        table.addHighlighter(new ColorHighlighter(HighlightPredicate.ROLLOVER_ROW, null, Color.BLUE));

        if (colorizeDone) table.addHighlighter(doneHighlighter);
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
        cmboSelectSource.setSelectedIndex(subConfig.getIntegerProperty(PROPERTY_SOURCE, 1));
        cmboSelectSource.addActionListener(this);

        topFolder = new JPanel(new BorderLayout(5, 5));
        topFolder.add(cmboSelectSource, BorderLayout.LINE_START);
        topFolder.add(cmboSource[cmboSelectSource.getSelectedIndex()]);

        topFile = new JPanel(new BorderLayout(5, 5));
        topFile.add(new JLabel(JDLocale.L("plugins.optional.langfileeditor.languageFile", "Language File:")), BorderLayout.LINE_START);
        topFile.add(cmboFile);

        keyChart = new ChartAPI_PIE(JDLocale.L("plugins.optional.langfileeditor.keychart", "KeyChart"), 250, 60, frame.getBackground());
        keyChart.addEntity(entDone = new ChartAPI_Entity(JDLocale.L("plugins.optional.langfileeditor.keychart.done", "Done"), 0, colorDone));
        keyChart.addEntity(entMissing = new ChartAPI_Entity(JDLocale.L("plugins.optional.langfileeditor.keychart.missing", "Missing"), 0, colorMissing));
        keyChart.addEntity(entOld = new ChartAPI_Entity(JDLocale.L("plugins.optional.langfileeditor.keychart.old", "Old"), 0, colorOld));

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

        frame.setJMenuBar(buildMenu());
        frame.setResizable(true);
        frame.pack();
        SimpleGUI.restoreWindow(null, frame);
        frame.setVisible(true);

        if (subConfig.getBooleanProperty(PROPERTY_SVN_UPDATE_ON_START, false)) updateSVN();

        sourceFile = cmboSource[cmboSelectSource.getSelectedIndex()].getCurrentPath();
        if (sourceFile != null) getSourceEntries();
        languageFile = cmboFile.getCurrentPath();
        if (languageFile == null) cmboFile.setCurrentPath(JDLocale.getLanguageFile());
        initLocaleData();
    }

    private void updateKeyChart() {
        int numMissing = 0, numOld = 0;

        for (KeyInfo entry : data) {

            if (entry.isOld()) {
                numOld++;
            } else if (entry.isMissing()) {
                numMissing++;
            }
        }

        entDone.setData(data.size() - numMissing - numOld);
        entDone.setCaption(JDLocale.L("plugins.optional.langfileeditor.keychart.done", "Done") + " [" + entDone.getData() + "]");
        entMissing.setData(numMissing);
        entMissing.setCaption(JDLocale.L("plugins.optional.langfileeditor.keychart.missing", "Missing") + " [" + entMissing.getData() + "]");
        entOld.setData(numOld);
        entOld.setCaption(JDLocale.L("plugins.optional.langfileeditor.keychart.old", "Old") + " [" + entOld.getData() + "]");
        keyChart.fetchImage();
    }

    private JMenuBar buildMenu() {
        // File Menü
        mnuFile = new JMenu(JDLocale.L("plugins.optional.langfileeditor.file", "File"));

        mnuFile.add(mnuNew = new JMenuItem(JDLocale.L("plugins.optional.langfileeditor.new", "New")));
        mnuFile.addSeparator();
        mnuFile.add(mnuSave = new JMenuItem(JDLocale.L("plugins.optional.langfileeditor.save", "Save")));
        mnuFile.add(mnuSaveAs = new JMenuItem(JDLocale.L("plugins.optional.langfileeditor.saveAs", "Save As")));
        mnuFile.addSeparator();
        mnuFile.add(mnuReload = new JMenuItem(JDLocale.L("plugins.optional.langfileeditor.reload", "Reload")));
        mnuFile.addSeparator();
        mnuFile.add(mnuClose = new JMenuItem(JDLocale.L("plugins.optional.langfileeditor.close", "Close")));

        mnuNew.addActionListener(this);
        mnuSave.addActionListener(this);
        mnuSaveAs.addActionListener(this);
        mnuReload.addActionListener(this);
        mnuClose.addActionListener(this);

        mnuSave.setEnabled(false);
        mnuSaveAs.setEnabled(false);
        mnuReload.setEnabled(false);

        mnuNew.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_N, KeyEvent.CTRL_DOWN_MASK));
        mnuSave.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, KeyEvent.CTRL_DOWN_MASK));
        mnuReload.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_R, KeyEvent.CTRL_DOWN_MASK));

        // SVN Menü
        mnuSVN = new JMenu(JDLocale.L("plugins.optional.langfileeditor.SVN", "SVN"));

        mnuSVN.add(mnuSVNSettings = new JMenuItem(JDLocale.L("plugins.optional.langfileeditor.svn.settings", "SVN Settings")));
        mnuSVN.addSeparator();
        mnuSVN.add(mnuSVNCheckOutNow = new JMenuItem(JDLocale.L("plugins.optional.langfileeditor.svn.checkOut", "CheckOut SVN now (This may take several seconds ...)")));

        mnuSVNSettings.addActionListener(this);
        mnuSVNCheckOutNow.addActionListener(this);

        // Key Menü
        mnuKey = new JMenu(JDLocale.L("plugins.optional.langfileeditor.key", "Key"));
        mnuKey.setEnabled(false);

        mnuKey.add(mnuAdd = new JMenuItem(JDLocale.L("plugins.optional.langfileeditor.addKey", "Add Key")));
        mnuKey.add(mnuDelete = new JMenuItem(JDLocale.L("plugins.optional.langfileeditor.deleteKeys", "Delete Key(s)")));
        mnuKey.add(mnuClear = new JMenuItem(JDLocale.L("plugins.optional.langfileeditor.clearValues", "Clear Value(s)")));
        mnuKey.addSeparator();
        mnuKey.add(mnuAdopt = new JMenuItem(JDLocale.L("plugins.optional.langfileeditor.adoptDefaults", "Adopt Default(s)")));
        mnuKey.add(mnuAdoptMissing = new JMenuItem(JDLocale.L("plugins.optional.langfileeditor.adoptDefaults.missing", "Adopt Defaults of Missing Entries")));
        mnuKey.addSeparator();
        mnuKey.add(mnuTranslate = new JMenuItem(JDLocale.L("plugins.optional.langfileeditor.translate", "Translate with Google")));
        mnuKey.add(mnuTranslateMissing = new JMenuItem(JDLocale.L("plugins.optional.langfileeditor.translate.missing", "Translate Missing Entries with Google")));

        mnuAdd.addActionListener(this);
        mnuDelete.addActionListener(this);
        mnuClear.addActionListener(this);
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
        mnuEntries.add(mnuColorizeDone = new JCheckBoxMenuItem(JDLocale.L("plugins.optional.langfileeditor.colorizeDone", "Colorize Done Entries")));
        mnuEntries.add(mnuPickDoneColor = new JMenuItem(JDLocale.L("plugins.optional.langfileeditor.pickDoneColor", "Pick Color for Done Entries")));
        mnuEntries.addSeparator();
        mnuEntries.add(mnuShowDupes = new JMenuItem(JDLocale.L("plugins.optional.langfileeditor.showDupes", "Show Dupes")));

        mnuColorizeMissing.setSelected(colorizeMissing);
        mnuColorizeOld.setSelected(colorizeOld);
        mnuColorizeDone.setSelected(colorizeDone);

        mnuColorizeMissing.setIcon(JDTheme.II((mnuColorizeMissing.isSelected()) ? "gui.images.selected" : "gui.images.unselected"));
        mnuColorizeOld.setIcon(JDTheme.II((mnuColorizeOld.isSelected()) ? "gui.images.selected" : "gui.images.unselected"));
        mnuColorizeDone.setIcon(JDTheme.II((mnuColorizeDone.isSelected()) ? "gui.images.selected" : "gui.images.unselected"));

        mnuColorizeMissing.addActionListener(this);
        mnuPickMissingColor.addActionListener(this);
        mnuColorizeOld.addActionListener(this);
        mnuPickOldColor.addActionListener(this);
        mnuColorizeDone.addActionListener(this);
        mnuPickDoneColor.addActionListener(this);
        mnuShowDupes.addActionListener(this);

        mnuColorizeMissing.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F5, 0));
        mnuColorizeOld.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F6, 0));
        mnuColorizeDone.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F7, 0));
        mnuShowDupes.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_D, KeyEvent.CTRL_DOWN_MASK));

        // Menü-Bar zusammensetzen
        JMenuBar menuBar = new JMenuBar();
        menuBar.add(mnuFile);
        menuBar.add(mnuSVN);
        menuBar.add(mnuKey);
        menuBar.add(mnuEntries);

        return menuBar;
    }

    private void buildContextMenu() {
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
            if (index == subConfig.getIntegerProperty(PROPERTY_SOURCE, 1)) return;

            if (changed) {
                int res = JOptionPane.showConfirmDialog(frame, JDLocale.L("plugins.optional.langfileeditor.changed.message", "Language File changed! Save changes?"), JDLocale.L("plugins.optional.langfileeditor.changed.title", "Save changes?"), JOptionPane.YES_NO_CANCEL_OPTION);
                if (res == JOptionPane.CANCEL_OPTION) {
                    cmboSelectSource.setSelectedIndex(subConfig.getIntegerProperty(PROPERTY_SOURCE, 1));
                    return;
                } else if (res == JOptionPane.YES_OPTION) {
                    saveLanguageFile(languageFile);
                } else {
                    changed = false;
                }
            }

            if (index != subConfig.getIntegerProperty(PROPERTY_SOURCE, 1)) {
                subConfig.setProperty(PROPERTY_SOURCE, index);
                subConfig.save();

                topFolder.remove(cmboSource[1 - index]);
                topFolder.add(cmboSource[index]);

                sourceFile = cmboSource[index].getCurrentPath();
                initLocaleDataComplete();
            }

        } else if (e.getSource() == cmboSource[0] || e.getSource() == cmboSource[1]) {

            File sourceFile = cmboSource[cmboSelectSource.getSelectedIndex()].getCurrentPath();
            if (sourceFile == this.sourceFile) return;

            if (changed) {
                int res = JOptionPane.showConfirmDialog(frame, JDLocale.L("plugins.optional.langfileeditor.changed.message", "Language File changed! Save changes?"), JDLocale.L("plugins.optional.langfileeditor.changed.title", "Save changes?"), JOptionPane.YES_NO_CANCEL_OPTION);
                if (res == JOptionPane.CANCEL_OPTION) {
                    cmboSource[cmboSelectSource.getSelectedIndex()].setCurrentPath(this.sourceFile);
                    return;
                } else if (res == JOptionPane.YES_OPTION) {
                    saveLanguageFile(languageFile);
                } else {
                    changed = false;
                }
            }

            if (sourceFile != this.sourceFile && sourceFile != null) {
                this.sourceFile = sourceFile;
                initLocaleDataComplete();
            }

        } else if (e.getSource() == cmboFile) {

            File languageFile = cmboFile.getCurrentPath();
            if (languageFile == this.languageFile) return;

            if (changed) {
                int res = JOptionPane.showConfirmDialog(frame, JDLocale.L("plugins.optional.langfileeditor.changed.message", "Language File changed! Save changes?"), JDLocale.L("plugins.optional.langfileeditor.changed.title", "Save changes?"), JOptionPane.YES_NO_CANCEL_OPTION);
                if (res == JOptionPane.CANCEL_OPTION) {
                    cmboFile.setCurrentPath(this.languageFile);
                    return;
                } else if (res == JOptionPane.YES_OPTION) {
                    saveLanguageFile(this.languageFile);
                    cmboFile.setCurrentPath(languageFile);
                } else {
                    changed = false;
                }
            }

            if (languageFile != this.languageFile && languageFile != null) {
                this.languageFile = languageFile;
                initLocaleData();
            }

        } else if (e.getSource() == mnuNew) {

            if (changed) {
                int res = JOptionPane.showConfirmDialog(frame, JDLocale.L("plugins.optional.langfileeditor.changed.message", "Language File changed! Save changes?"), JDLocale.L("plugins.optional.langfileeditor.changed.title", "Save changes?"), JOptionPane.YES_NO_CANCEL_OPTION);
                if (res == JOptionPane.CANCEL_OPTION) {
                    return;
                } else if (res == JOptionPane.YES_OPTION) {
                    saveLanguageFile(languageFile);
                } else {
                    changed = false;
                }
            }

            JDFileChooser chooser = new JDFileChooser("LANGFILEEDITOR_FILE");
            chooser.setFileFilter(fileFilter);

            if (chooser.showSaveDialog(frame) == JDFileChooser.APPROVE_OPTION) {
                languageFile = chooser.getSelectedFile();
                if (!languageFile.getAbsolutePath().endsWith(".lng")) languageFile = new File(languageFile.getAbsolutePath() + ".lng");
                if (!languageFile.exists()) {
                    try {
                        languageFile.createNewFile();
                    } catch (IOException e1) {
                        e1.printStackTrace();
                    }
                }
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

        } else if (e.getSource() == mnuAdd) {

            String result = JOptionPane.showInputDialog(frame, JDLocale.L("plugins.optional.langfileeditor.addKey.message", "Type in the name of the key:"), JDLocale.L("plugins.optional.langfileeditor.addKey.title", "Add new key"), JOptionPane.INFORMATION_MESSAGE);
            data.add(new KeyInfo(result.toLowerCase(), null, null));
            tableModel.fireTableDataChanged();
            updateKeyChart();

        } else if (e.getSource() == mnuDelete || e.getSource() == mnuContextDelete) {

            deleteSelectedKeys();

        } else if (e.getSource() == mnuReload) {

            if (changed) {
                int res = JOptionPane.showConfirmDialog(frame, JDLocale.L("plugins.optional.langfileeditor.changed.message", "Language File changed! Save changes?"), JDLocale.L("plugins.optional.langfileeditor.changed.title", "Save changes?"), JOptionPane.YES_NO_CANCEL_OPTION);
                if (res == JOptionPane.CANCEL_OPTION) {
                    return;
                } else if (res == JOptionPane.YES_OPTION) {
                    saveLanguageFile(languageFile);
                } else {
                    changed = false;
                }
            }
            initLocaleDataComplete();

        } else if (e.getSource() == mnuAdopt || e.getSource() == mnuContextAdopt) {

            for (int row : getSelectedRows()) {
                tableModel.setValueAt(tableModel.getValueAt(row, 1), row, 2);
            }

        } else if (e.getSource() == mnuAdoptMissing) {

            for (int i = 0; i < tableModel.getRowCount(); ++i) {

                if (tableModel.getValueAt(i, 2).equals("")) {
                    tableModel.setValueAt(tableModel.getValueAt(i, 1), i, 2);
                }
            }

        } else if (e.getSource() == mnuClear || e.getSource() == mnuContextClear) {

            for (int row : getSelectedRows()) {
                tableModel.setValueAt("", row, 2);
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

        } else if (e.getSource() == mnuColorizeDone) {

            colorizeDone = mnuColorizeDone.isSelected();
            subConfig.setProperty(PROPERTY_COLORIZE_DONE, colorizeDone);
            subConfig.save();
            mnuColorizeDone.setIcon(JDTheme.II((mnuColorizeDone.isSelected()) ? "gui.images.selected" : "gui.images.unselected"));
            if (colorizeDone) {
                table.addHighlighter(doneHighlighter);
            } else {
                table.removeHighlighter(doneHighlighter);
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
                missingHighlighter.setBackground(colorMissing);
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
                oldHighlighter.setBackground(colorOld);
                keyChart.fetchImage();
            }

        } else if (e.getSource() == mnuPickDoneColor) {

            Color newColor = JColorChooser.showDialog(frame, JDLocale.L("plugins.optional.langfileeditor.pickDoneColor", "Pick Color for Done Entries"), colorDone);
            if (newColor != null) {
                colorDone = newColor;
                subConfig.setProperty(PROPERTY_DONE_COLOR, colorDone);
                subConfig.save();
                tableModel.fireTableDataChanged();
                entDone.setColor(colorDone);
                doneHighlighter.setBackground(colorDone);
                keyChart.fetchImage();
            }

        } else if (e.getSource() == mnuShowDupes) {

            new DupeDialog(frame, dupes);

        } else if (e.getSource() == mnuTranslate || e.getSource() == mnuContextTranslate) {

            if (getLanguageKey() == null) return;

            int[] rows = getSelectedRows();
            for (int i = rows.length - 1; i >= 0; --i) {
                translateRow(rows[i]);
            }

        } else if (e.getSource() == mnuTranslateMissing) {

            if (getLanguageKey() == null) return;

            for (int i = tableModel.getRowCount() - 1; i >= 0; --i) {
                if (tableModel.getValueAt(i, 2).equals("")) {
                    translateRow(i);
                }
            }

        } else if (e.getSource() == mnuClose) {

            frame.setVisible(false);
            frame.dispose();

        } else if (e.getSource() == mnuSVNSettings) {

            ConfigEntry ce, conditionEntry;
            ConfigContainer container = new ConfigContainer(this);
            container.addEntry(new ConfigEntry(ConfigContainer.TYPE_TEXTFIELD, subConfig, PROPERTY_SVN_REPOSITORY, JDLocale.L("plugins.optional.langfileeditor.svn.repository", "SVN Repository")).setDefaultValue("https://www.syncom.org/svn/jdownloader/trunk/src/"));
            container.addEntry(new ConfigEntry(ConfigContainer.TYPE_BROWSEFOLDER, subConfig, PROPERTY_SVN_WORKING_COPY, JDLocale.L("plugins.optional.langfileeditor.svn.workingCopy", "SVN Working Copy")).setDefaultValue(JDUtilities.getJDHomeDirectoryFromEnvironment().getAbsolutePath() + System.getProperty("file.separator") + "svn"));
            container.addEntry(new ConfigEntry(ConfigContainer.TYPE_SEPARATOR));
            container.addEntry(conditionEntry = new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, subConfig, PROPERTY_SVN_ACCESS_ANONYMOUS, JDLocale.L("plugins.optional.langfileeditor.svn.access.anonymous", "Anonymous SVN CheckOut")).setDefaultValue(true));
            container.addEntry(ce = new ConfigEntry(ConfigContainer.TYPE_TEXTFIELD, subConfig, PROPERTY_SVN_ACCESS_USER, JDLocale.L("plugins.optional.langfileeditor.svn.access.user", "SVN Username")));
            ce.setEnabledCondidtion(conditionEntry, "==", false);
            container.addEntry(ce = new ConfigEntry(ConfigContainer.TYPE_PASSWORDFIELD, subConfig, PROPERTY_SVN_ACCESS_PASS, JDLocale.L("plugins.optional.langfileeditor.svn.access.pass", "SVN Password")));
            ce.setEnabledCondidtion(conditionEntry, "==", false);
            container.addEntry(new ConfigEntry(ConfigContainer.TYPE_SEPARATOR));
            container.addEntry(new ConfigEntry(ConfigContainer.TYPE_BUTTON, new ActionListener() {

                public void actionPerformed(ActionEvent e) {
                    updateSVN();
                }

            }, JDLocale.L("plugins.optional.langfileeditor.svn.checkOut", "CheckOut SVN now (This may take several seconds ...)")));
            container.addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, subConfig, PROPERTY_SVN_UPDATE_ON_START, JDLocale.L("plugins.optional.langfileeditor.svn.checkOutOnStart", "CheckOut SVN on start")).setDefaultValue(false));
            SimpleGUI.showConfigDialog(frame, container);

        } else if (e.getSource() == mnuSVNCheckOutNow) {

            updateSVN();

        }

    }

    private void updateSVN() {
        try {
            Subversion svn;
            if (subConfig.getBooleanProperty(PROPERTY_SVN_ACCESS_ANONYMOUS, true)) {
                svn = new Subversion(subConfig.getStringProperty(PROPERTY_SVN_REPOSITORY, "https://www.syncom.org/svn/jdownloader/trunk/src/"));
            } else {
                svn = new Subversion(subConfig.getStringProperty(PROPERTY_SVN_REPOSITORY, "https://www.syncom.org/svn/jdownloader/trunk/src/"), subConfig.getStringProperty(PROPERTY_SVN_ACCESS_USER), subConfig.getStringProperty(PROPERTY_SVN_ACCESS_PASS));
            }
            String workingCopy = subConfig.getStringProperty(PROPERTY_SVN_WORKING_COPY, JDUtilities.getJDHomeDirectoryFromEnvironment().getAbsolutePath() + System.getProperty("file.separator") + "svn");
            svn.export(new File(workingCopy));
            if (sourceFile == null || !sourceFile.getAbsolutePath().equalsIgnoreCase(workingCopy)) {
                if (JOptionPane.showConfirmDialog(frame, JDLocale.L("plugins.optional.langfileeditor.svn.change.message", "Change the current source to the checked out SVN Repository?"), JDLocale.L("plugins.optional.langfileeditor.svn.change.title", "Change now?"), JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE) == JOptionPane.YES_OPTION) {
                    cmboSource[0].setText(workingCopy);
                    cmboSelectSource.setSelectedIndex(0);
                }
            } else if (sourceFile.getAbsolutePath().equalsIgnoreCase(workingCopy) && !changed) {
                initLocaleDataComplete();
            }
        } catch (SVNException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(frame, JDLocale.LF("plugins.optional.langfileeditor.svn.error.message", "An error occured while checking the SVN Repository out! Please check the SVN settings!\n%s", e.getMessage()), JDLocale.L("plugins.optional.langfileeditor.svn.error.title", "Error!"), JOptionPane.ERROR_MESSAGE);
        }
    }

    private String getLanguageKey() {
        if (lngKey == null) {
            String result = JOptionPane.showInputDialog(frame, JDLocale.L("plugins.optional.langfileeditor.translate.message", "Please insert the Language Key to provide a correct translation of Google:"), JDLocale.L("plugins.optional.langfileeditor.translate.title", "Insert Language Key"), JOptionPane.QUESTION_MESSAGE);
            if (result == null) return null;
            if (!isSupportedLanguageKey(result)) {
                JOptionPane.showMessageDialog(frame, JDLocale.L("plugins.optional.langfileeditor.translate.error.message", "The Language Key, you have entered, is not supported by Google!"), JDLocale.L("plugins.optional.langfileeditor.translate.error.title", "Wrong Language Key!"), JOptionPane.ERROR_MESSAGE);
                return null;
            }
            lngKey = result;
        }
        return lngKey;
    }

    private void translateRow(int row) {
        String def = tableModel.getValueAt(row, 1);
        if (!def.equals("")) {
            String res = JDLocale.translate(lngKey, def);
            if (res != null) tableModel.setValueAt(res, row, 2);
        }
    }

    @SuppressWarnings("unchecked")
    private void deleteSelectedKeys() {
        int[] rows = getSelectedRows();

        int len = rows.length - 1;
        String[] keys = dupes.keySet().toArray(new String[dupes.size()]);
        Object[] obj = dupes.values().toArray();
        Vector<String> values;
        for (int i = len; i >= 0; --i) {
            String temp = data.remove(rows[i]).getKey();
            data.remove(temp);
            for (int j = obj.length - 1; j >= 0; --j) {
                values = (Vector<String>) obj[j];
                values.remove(temp);
                if (values.size() == 1) dupes.remove(keys[j]);
            }
            tableModel.fireTableRowsDeleted(rows[i], rows[i]);
        }
        int newRow = Math.min(rows[len] - len, tableModel.getRowCount() - 1);
        table.getSelectionModel().setSelectionInterval(newRow, newRow);

        updateKeyChart();
        changed(true);
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
        String key = table.getStringAt(row, 0);

        for (int i = 0; i < tableModel.getRowCount(); ++i) {
            if (tableModel.getValueAt(i, 0).equalsIgnoreCase(key)) return i;
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

    private void changed(boolean b) {
        if (b != changed) {
            if (b) {
                frame.setTitle(JDLocale.L("plugins.optional.langfileeditor.title", "jDownloader - Language File Editor") + " [ * " + languageFile.getAbsolutePath() + "]");
            } else {
                frame.setTitle(JDLocale.L("plugins.optional.langfileeditor.title", "jDownloader - Language File Editor") + " [" + languageFile.getAbsolutePath() + "]");
            }
        }
        changed = b;
    }

    private void saveLanguageFile(File file) {
        StringBuilder sb = new StringBuilder();

        Collections.sort(data);

        for (KeyInfo entry : data) {
            if (!entry.isMissing()) sb.append(entry.toString() + "\n");
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
        changed(false);
        JOptionPane.showMessageDialog(frame, JDLocale.L("plugins.optional.langfileeditor.save.success.message", "LanguageFile saved successfully!"), JDLocale.L("plugins.optional.langfileeditor.save.success.title", "Save successful!"), JOptionPane.INFORMATION_MESSAGE);
    }

    private void initLocaleDataComplete() {
        getSourceEntries();
        initLocaleData();
    }

    private void initLocaleData() {
        parseLanguageFile(languageFile, fileEntries);

        HashMap<String, String> dupeHelp = new HashMap<String, String>();
        data.clear();
        dupes.clear();
        lngKey = null;

        Vector<String> values;
        String value, key, language;
        KeyInfo keyInfo;
        for (Entry<String, String> entry : sourceEntries.entrySet()) {

            key = entry.getKey();
            keyInfo = new KeyInfo(key, entry.getValue(), fileEntries.remove(key));
            data.add(keyInfo);
            if (!keyInfo.isMissing()) {

                language = keyInfo.getLanguage();
                if (dupeHelp.containsKey(language)) {
                    values = dupes.get(language);
                    if (values == null) {
                        values = new Vector<String>();
                        values.add(dupeHelp.get(language));
                        dupes.put(language, values);
                    }
                    values.add(key);
                }
                dupeHelp.put(language, key);
            }
        }

        for (Entry<String, String> entry : fileEntries.entrySet()) {

            key = entry.getKey();
            value = null;

            for (String pattern : sourcePatterns) {
                if (key.matches(pattern)) {
                    value = JDLocale.L("plugins.optional.langfileeditor.patternEntry", "<Entry matches Pattern>");
                    break;
                }
            }
            data.add(new KeyInfo(key, value, entry.getValue()));
        }

        Collections.sort(data);

        tableModel.fireTableRowsInserted(0, data.size() - 1);
        table.packAll();
        changed = false;
        if (languageFile != null) frame.setTitle(JDLocale.L("plugins.optional.langfileeditor.title", "jDownloader - Language File Editor") + " [" + languageFile.getAbsolutePath() + "]");

        updateKeyChart();
        mnuEntries.setEnabled(true);
        mnuKey.setEnabled(true);
        mnuReload.setEnabled(true);
        mnuSave.setEnabled(true);
        mnuSaveAs.setEnabled(true);
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
        parseLanguageFile(sourceFile, sourceEntries);
    }

    private void getSourceEntriesFromFolder() {
        sourceEntries.clear();
        sourcePatterns.clear();

        String[][] matches;
        for (File file : getSourceFiles(sourceFile)) {

            matches = new Regex(JDIO.getLocalFile(file), "JDLocale[\\s]*\\.L[F]?[\\s]*\\([\\s]*\"(.*?)\"[\\s]*,[\\s]*(\".*?\"|.*?)[\\s]*[,\\)]").getMatches();

            for (String[] match : matches) {

                match[0] = match[0].trim().toLowerCase();
                if (sourceEntries.containsKey(match[0])) continue;

                if (match[0].indexOf("\"") == -1) {
                    match[1] = match[1].substring(1, match[1].length() - 1);
                    sourceEntries.put(match[0], match[1]);
                } else {
                    if (match[0].contains(",")) match[0] = match[0].substring(0, match[0].indexOf(",") + 1);
                    match[0] = match[0].replaceAll("\\.", "\\\\.");
                    match[0] = match[0].replaceAll("\"(.*?)[\",]", "(.*?)");
                    sourcePatterns.add(match[0]);
                }
            }
        }
    }

    private Vector<File> getSourceFiles(File directory) {
        Vector<File> files = new Vector<File>();

        if (directory != null) {
            for (File file : directory.listFiles()) {
                if (file.isDirectory()) {
                    files.addAll(getSourceFiles(file));
                } else if (file.getName().matches(".*\\.java$")) {
                    files.add(file);
                }
            }
        }

        return files;
    }

    private void parseLanguageFile(File file, HashMap<String, String> data) {
        data.clear();

        if (file == null || !file.exists()) {
            System.out.println("JDLocale: " + file + " not found");
            return;
        }

        try {
            BufferedReader f = new BufferedReader(new InputStreamReader(new FileInputStream(file), "UTF8"));

            String line;
            String key;
            String value;
            while ((line = f.readLine()) != null) {
                if (line.startsWith("#")) continue;
                int split = line.indexOf("=");
                if (split <= 0) continue;

                key = line.substring(0, split).trim().toLowerCase();
                value = line.substring(split + 1).trim() + (line.endsWith(" ") ? " " : "");

                data.put(key, value);
            }
            f.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void mouseClicked(MouseEvent e) {
        if (e.isPopupTrigger() || e.getButton() == MouseEvent.BUTTON3) {
            int row = table.rowAtPoint(e.getPoint());
            if (!table.isRowSelected(row)) {
                table.getSelectionModel().setSelectionInterval(row, row);
            }
            if (mnuContextPopup == null) buildContextMenu();
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

    private class KeyInfo implements Comparable<KeyInfo> {

        private String key;

        private String source = "";

        private String language = "";

        public KeyInfo(String key, String source, String language) {
            this.key = key;
            this.setSource(source);
            this.setLanguage(language);
        }

        public String getKey() {
            return this.key;
        }

        public String getLanguage() {
            return this.language;
        }

        public String getSource() {
            return this.source;
        }

        public void setLanguage(String language) {
            if (language != null) this.language = language;
        }

        public void setSource(String source) {
            if (source != null) this.source = source;
        }

        public boolean isMissing() {
            return this.getLanguage().equals("");
        }

        public boolean isOld() {
            return this.getSource().equals("");
        }

        public int compareTo(KeyInfo o) {
            return this.getKey().compareToIgnoreCase(o.getKey());
        }

        @Override
        public String toString() {
            return this.getKey() + " = " + this.getLanguage();
        }

    }

    private class DonePredicate implements HighlightPredicate {
        public boolean isHighlighted(Component arg0, ComponentAdapter arg1) {
            return (!table.getValueAt(arg1.row, 1).equals("") && !table.getValueAt(arg1.row, 2).equals(""));
        }
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

        @Override
        public String getColumnName(int col) {
            return columnNames[col];
        }

        public String getValueAt(int row, int col) {
            switch (col) {
            case 0:
                return data.get(row).getKey();
            case 1:
                return data.get(row).getSource();
            case 2:
                return data.get(row).getLanguage();
            }
            return "";
        }

        @Override
        public Class<?> getColumnClass(int c) {
            return String.class;
        }

        @Override
        public boolean isCellEditable(int row, int col) {
            return true;
        }

        @Override
        public void setValueAt(Object value, int row, int col) {
            if (col == 2) {
                data.get(row).setLanguage((String) value);
                this.fireTableRowsUpdated(row, row);
                updateKeyChart();
                changed(true);
            }
        }

    }

    private class DupeDialog extends JDialog implements ActionListener {

        private static final long serialVersionUID = 1L;

        public DupeDialog(JFrame owner, HashMap<String, Vector<String>> dupes) {
            super(owner);

            setModal(true);
            setLayout(new BorderLayout());
            setTitle(JDLocale.L("plugins.optional.langfileeditor.duplicatedEntries", "Duplicated Entries") + " [" + dupes.size() + "]");

            MyDupeTableModel internalTableModel = new MyDupeTableModel(dupes);
            JXTable table = new JXTable(internalTableModel);
            table.getTableHeader().setReorderingAllowed(false);
            table.getColumnModel().getColumn(0).setPreferredWidth(50);
            table.getColumnModel().getColumn(0).setMinWidth(50);
            table.getColumnModel().getColumn(0).setMaxWidth(50);
            table.getColumnModel().getColumn(2).setMinWidth(450);
            table.addHighlighter(HighlighterFactory.createAlternateStriping());
            table.addHighlighter(new ColorHighlighter(HighlightPredicate.ROLLOVER_ROW, null, Color.BLUE));

            JButton btnClose = new JButton(JDLocale.L("plugins.optional.langfileeditor.close", "Close"));
            btnClose.addActionListener(this);
            getRootPane().setDefaultButton(btnClose);

            JPanel buttons = new JPanel(new FlowLayout());
            buttons.add(btnClose);

            add(new JScrollPane(table), BorderLayout.CENTER);
            add(buttons, BorderLayout.PAGE_END);

            setMinimumSize(new Dimension(900, 600));
            setPreferredSize(new Dimension(900, 600));
            pack();
            setLocation(JDUtilities.getCenterOfComponent(owner, this));
            setVisible(true);
        }

        public void actionPerformed(ActionEvent e) {
            dispose();
        }

        private class MyDupeTableModel extends AbstractTableModel {

            private static final long serialVersionUID = -5434313385327397539L;

            private String[] columnNames = { "*", JDLocale.L("plugins.optional.langfileeditor.string", "String"), JDLocale.L("plugins.optional.langfileeditor.keys", "Keys") };

            private HashMap<String, Vector<String>> tableData;

            private String[] keys;

            public MyDupeTableModel(HashMap<String, Vector<String>> data) {
                tableData = data;
                keys = data.keySet().toArray(new String[data.size()]);
                this.fireTableRowsInserted(0, tableData.size() - 1);
            }

            public int getColumnCount() {
                return columnNames.length;
            }

            public int getRowCount() {
                return tableData.size();
            }

            @Override
            public String getColumnName(int col) {
                return columnNames[col];
            }

            public Object getValueAt(int row, int col) {
                switch (col) {
                case 0:
                    return tableData.get(getValueAt(row, 1)).size();
                case 1:
                    return keys[row];
                case 2:
                    StringBuilder ret = new StringBuilder();
                    Vector<String> values = tableData.get(keys[row]);
                    for (String value : values) {
                        if (ret.length() > 0) ret.append(" || ");
                        ret.append(value);
                    }
                    return ret.toString();
                }
                return "";
            }

            @Override
            public Class<?> getColumnClass(int col) {
                if (col == 0) return Integer.class;
                return String.class;
            }

            @Override
            public boolean isCellEditable(int row, int col) {
                return (col == 1);
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

        menu.add(new MenuItem(MenuItem.NORMAL, getHost(), 0).setActionListener(this));

        return menu;
    }

    @Override
    public String getCoder() {
        return "Greeny";
    }

    @Override
    public String getHost() {
        return JDLocale.L("plugins.optional.langfileeditor.name", "Language File Editor");
    }

    @Override
    public String getVersion() {
        return getVersion("$Revision$");
    }

    public static int getAddonInterfaceVersion() {
        return 2;
    }
}