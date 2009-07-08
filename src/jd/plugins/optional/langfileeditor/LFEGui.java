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

import java.awt.Color;
import java.awt.Component;
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
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map.Entry;

import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.table.AbstractTableModel;

import jd.config.SubConfiguration;
import jd.controlling.JDLogger;
import jd.controlling.ProgressController;
import jd.event.MessageEvent;
import jd.event.MessageListener;
import jd.gui.UserIO;
import jd.gui.skins.simple.JTabbedPanel;
import jd.gui.skins.simple.SimpleGUI;
import jd.gui.skins.simple.components.ChartAPIEntity;
import jd.gui.skins.simple.components.JDFileChooser;
import jd.gui.skins.simple.components.PieChartAPI;
import jd.nutils.io.JDFileFilter;
import jd.nutils.io.JDIO;
import jd.nutils.svn.Subversion;
import jd.utils.JDGeoCode;
import jd.utils.JDUtilities;
import jd.utils.locale.JDL;
import net.miginfocom.swing.MigLayout;

import org.jdesktop.swingx.JXTable;
import org.jdesktop.swingx.decorator.ColorHighlighter;
import org.jdesktop.swingx.decorator.ComponentAdapter;
import org.jdesktop.swingx.decorator.Filter;
import org.jdesktop.swingx.decorator.FilterPipeline;
import org.jdesktop.swingx.decorator.HighlightPredicate;
import org.jdesktop.swingx.decorator.PatternFilter;
import org.jdesktop.swingx.search.SearchFactory;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.wc.SVNInfo;
import org.tmatesoft.svn.core.wc.SVNRevision;

public class LFEGui extends JTabbedPanel implements ActionListener, MouseListener {

    private static final long serialVersionUID = -143452893912428555L;

    private static final String SOURCE_SVN = "svn://svn.jdownloader.org/jdownloader/trunk/src";

    private static final String LANGUAGE_SVN = "svn://svn.jdownloader.org/jdownloader/trunk/ressourcen/jd/languages";

    private static final String LOCALE_PREFIX = "plugins.optional.langfileeditor.";

    private final SubConfiguration subConfig;

    // private final String PROPERTY_SHOW_DONE = "PROPERTY_SHOW_DONE";
    // private final String PROPERTY_SHOW_MISSING = "PROPERTY_SHOW_MISSING";
    // private final String PROPERTY_SHOW_OLD = "PROPERTY_SHOW_OLD";
    // private final String PROPERTY_COLORIZE_DONE = "PROPERTY_COLORIZE_DONE";
    // private final String PROPERTY_COLORIZE_MISSING =
    // "PROPERTY_COLORIZE_MISSING";
    // private final String PROPERTY_COLORIZE_OLD = "PROPERTY_COLORIZE_OLD";
    // private final String PROPERTY_DONE_COLOR = "PROPERTY_DONE_COLOR";
    // private final String PROPERTY_MISSING_COLOR = "PROPERTY_MISSING_COLOR";
    // private final String PROPERTY_OLD_COLOR = "PROPERTY_OLD_COLOR";

    final static String PROPERTY_SVN_ACCESS_ANONYMOUS = "PROPERTY_SVN_CHECKOUT_ANONYMOUS";
    final static String PROPERTY_SVN_ACCESS_USER = "PROPERTY_SVN_CHECKOUT_USER";
    final static String PROPERTY_SVN_ACCESS_PASS = "PROPERTY_SVN_CHECKOUT_PASS";

    private JXTable table;
    private MyTableModel tableModel;
    private File languageFile;
    // private ComboBrowseFile cmboFile;
    private PieChartAPI keyChart;
    private ChartAPIEntity entDone, entMissing, entOld;
    private JMenu mnuFile, mnuKey, mnuEntries;
    private JMenuItem mnuNew, mnuSave;

    private JMenuItem mnuAdd, mnuAdopt, mnuAdoptMissing, mnuClear, mnuDelete, mnuTranslate, mnuTranslateMissing;
    private JMenuItem mnuShowDupes, mnuOpenSearchDialog;
    private JPopupMenu mnuContextPopup;
    private JMenuItem mnuContextAdopt, mnuContextClear, mnuContextDelete, mnuContextTranslate;

    // private HashMap<String, String> sourceEntries = new HashMap<String,
    // String>();
    // private ArrayList<String> sourcePatterns = new ArrayList<String>();
    private HashMap<String, String> languageKeysFormFile = new HashMap<String, String>();
    private ArrayList<KeyInfo> data = new ArrayList<KeyInfo>();
    private HashMap<String, ArrayList<String>> dupes = new HashMap<String, ArrayList<String>>();
    private String lngKey = null;
    private boolean changed = false;
    // private boolean initComplete = false;
    private boolean updatingInProgress = false;
    private final JDFileFilter fileFilter;
    private final File dirLanguages, dirWorkingCopy;

    private boolean colorizeDone, colorizeMissing, colorizeOld, showDone, showMissing, showOld;
    private Color colorDone, colorMissing, colorOld;
    private ColorHighlighter doneHighlighter, missingHighlighter, oldHighlighter;

    private SrcParser sourceParser;

    private JMenu mnuLoad;

    private long HEAD;

    private JMenuItem mnuReload;

    public LFEGui(SubConfiguration cfg) {
        subConfig = cfg;
        fileFilter = new JDFileFilter(JDL.L(LOCALE_PREFIX + "fileFilter2", "JD Language File (*.loc) or Folder with Sourcefiles"), ".loc", true);
        // String lfeHome =
        // JDUtilities.getJDHomeDirectoryFromEnvironment().getAbsolutePath() +
        // "/plugins/lfe/";
        dirLanguages = JDUtilities.getResourceFile("tmp/lfe/lng/");
        dirWorkingCopy = JDUtilities.getResourceFile("tmp/lfe/src/");
        dirLanguages.mkdirs();
        dirWorkingCopy.mkdirs();
        showGui();
    }

    private void showGui() {
        colorizeDone = true;
        colorizeMissing = true;
        colorizeOld = true;

        showDone = true;
        showMissing = true;
        showOld = true;

        colorDone = Color.GREEN;
        colorMissing = Color.RED;
        colorOld = Color.ORANGE;

        doneHighlighter = new ColorHighlighter(new DonePredicate(), colorDone, null);
        missingHighlighter = new ColorHighlighter(new MissingPredicate(), colorMissing, null);
        oldHighlighter = new ColorHighlighter(new OldPredicate(), colorOld, null);

        tableModel = new MyTableModel();
        table = new JXTable(tableModel);
       
        table.setEnabled(false);
        FilterPipeline pipeline = new FilterPipeline(new Filter[] { new MyPatternFilter() });
        table.setFilters(pipeline);
        table.getTableHeader().setReorderingAllowed(false);
        table.getColumn(0).setMinWidth(50);
        table.getColumn(0).setMaxWidth(50);
        table.getColumn(1).setMinWidth(200);
        table.getColumn(1).setPreferredWidth(200);
        table.getColumn(3).setPreferredWidth(200);
        table.addMouseListener(this);
        table.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        table.setAutoStartEditOnKeyStroke(false);

        if (colorizeDone) table.addHighlighter(doneHighlighter);
        if (colorizeMissing) table.addHighlighter(missingHighlighter);
        if (colorizeOld) table.addHighlighter(oldHighlighter);

        // cmboSource = new ComboBrowseFile("LANGFILEEDITOR_SOURCE");
        // cmboSource.setFileSelectionMode(JDFileChooser.FILES_AND_DIRECTORIES);
        // cmboSource.setFileFilter(fileFilter);
        // cmboSource.addActionListener(this);

        // cmboFile = new ComboBrowseFile("LANGFILEEDITOR_FILE");
        // cmboFile.setFileSelectionMode(JDFileChooser.FILES_ONLY);
        // cmboFile.setFileFilter(fileFilter);
        // cmboFile.addActionListener(this);

        keyChart = new PieChartAPI("", 225, 50);
        keyChart.addEntity(entDone = new ChartAPIEntity(JDL.L(LOCALE_PREFIX + "keychart.done", "Done"), 0, colorDone));
        keyChart.addEntity(entMissing = new ChartAPIEntity(JDL.L(LOCALE_PREFIX + "keychart.missing", "Missing"), 0, colorMissing));
        keyChart.addEntity(entOld = new ChartAPIEntity(JDL.L(LOCALE_PREFIX + "keychart.old", "Old"), 0, colorOld));

        this.setLayout(new MigLayout("wrap 3", "[][grow, fill][]", "[][grow, fill][]"));
        this.add(buildMenu(), "span 3, growx, spanx");
        // this.add(new JLabel(JDLocale.L(LOCALE_PREFIX + "source",
        // "Source:")));
        // this.add(cmboSource, "growx");

        // this.add(new JLabel(JDLocale.L(LOCALE_PREFIX + "languageFile",
        // "Language File:")));
        // this.add(cmboFile, "growx");

        this.add(new JScrollPane(table), "span 3, grow, span");
        this.add(keyChart, "spany 1, w 225!, h 50!");
        // sourceFile = cmboSource.getCurrentPath();
        // languageFile = cmboFile.getCurrentPath();

        new Thread(new Runnable() {

            public void run() {

                LFEGui.this.setEnabled(false);

                /*
                 * SVN Working Copy nur dann automatisch Updaten, wenn per Jar
                 * gestartet!
                 */
                updateSVN();
                // if (languageFile == null) {
                // if (dirLanguages.exists() && new File(dirLanguages,
                // JDLocale.getLocale() + ".loc").exists()) {
                // cmboFile.setCurrentPath(new File(dirLanguages,
                // JDLocale.getLocale() + ".loc"));
                // } else {
                // cmboFile.setCurrentPath(JDLocale.getLanguageFile());
                // }
                // }

                // initComplete = true;

                // if (sourceFile != null)
                getSourceEntries();
                initLocaleData();

                LFEGui.this.setEnabled(true);

            }

        }).start();
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
        entDone.setCaption(JDL.L(LOCALE_PREFIX + "keychart.done", "Done") + " [" + entDone.getData() + "]");
        entMissing.setData(numMissing);
        entMissing.setCaption(JDL.L(LOCALE_PREFIX + "keychart.missing", "Missing") + " [" + entMissing.getData() + "]");
        entOld.setData(numOld);
        entOld.setCaption(JDL.L(LOCALE_PREFIX + "keychart.old", "Old") + " [" + entOld.getData() + "]");
        keyChart.fetchImage();
    }

    private JMenuBar buildMenu() {
        // File Menü
        mnuFile = new JMenu(JDL.L(LOCALE_PREFIX + "file", "File"));
      
//        mnuFile.add(mnuNew = new JMenuItem(JDL.L(LOCALE_PREFIX + "new", "New")));
        mnuFile.add(mnuLoad = new JMenu(JDL.L(LOCALE_PREFIX + "load", "Load Language")));
        for (File f : dirLanguages.listFiles()) {
            if (f.getName().endsWith(".loc")) {
                String language = JDGeoCode.toLonger(f.getName().substring(0, f.getName().length() - 4));
                if (language != null) {
                    JMenuItem mi;
                    mnuLoad.add(mi = new JMenuItem(language));
                    mi.addActionListener(new ActionListener() {

                        public void actionPerformed(ActionEvent e) {
                            languageFile = new File(dirLanguages, JDGeoCode.longToShort(e.getActionCommand()) + ".loc");
                            initLocaleData();
                            table.setEnabled(true);

                        }

                    });
                }
            }

        }
        mnuFile.addSeparator();
        mnuFile.add(mnuSave = new JMenuItem(JDL.L(LOCALE_PREFIX + "save", "Save")));
        mnuFile.addSeparator();
        mnuFile.add(mnuReload = new JMenuItem(JDL.L(LOCALE_PREFIX + "reload", "Revert/Reload")));
        // mnuFile.add(mnuSaveAs = new JMenuItem(JDL.L(LOCALE_PREFIX + "saveAs",
        // "Save As")));
        // mnuFile.addSeparator();
        // mnuFile.add(mnuReload = new JMenuItem(JDL.L(LOCALE_PREFIX + "reload",
        // "Reload")));
        mnuReload.addActionListener(this);
//        mnuNew.addActionListener(this);
        mnuSave.addActionListener(this);

        // mnuReload.addActionListener(this);

        mnuSave.setEnabled(false);

//        mnuNew.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_N, KeyEvent.CTRL_DOWN_MASK));
        mnuSave.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, KeyEvent.CTRL_DOWN_MASK));

        // Key Menü
        mnuKey = new JMenu(JDL.L(LOCALE_PREFIX + "key", "Key"));
        mnuKey.setEnabled(false);

        mnuKey.add(mnuAdd = new JMenuItem(JDL.L(LOCALE_PREFIX + "addKey", "Add Key")));
        mnuKey.add(mnuDelete = new JMenuItem(JDL.L(LOCALE_PREFIX + "deleteKeys", "Delete Key(s)")));
        mnuKey.add(mnuClear = new JMenuItem(JDL.L(LOCALE_PREFIX + "clearValues", "Clear Value(s)")));
        mnuKey.addSeparator();
        mnuKey.add(mnuAdopt = new JMenuItem(JDL.L(LOCALE_PREFIX + "adoptDefaults", "Adopt Default(s)")));
        mnuKey.add(mnuAdoptMissing = new JMenuItem(JDL.L(LOCALE_PREFIX + "adoptDefaults.missing", "Adopt Defaults of Missing Entries")));
        mnuKey.addSeparator();
        mnuKey.add(mnuTranslate = new JMenuItem(JDL.L(LOCALE_PREFIX + "translate", "Translate with Google")));
        mnuKey.add(mnuTranslateMissing = new JMenuItem(JDL.L(LOCALE_PREFIX + "translate.missing", "Translate Missing Entries with Google")));

        mnuAdd.addActionListener(this);
        mnuDelete.addActionListener(this);
        mnuClear.addActionListener(this);
        mnuAdopt.addActionListener(this);
        mnuAdoptMissing.addActionListener(this);
        mnuTranslate.addActionListener(this);
        mnuTranslateMissing.addActionListener(this);

        mnuDelete.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0));

        // Entries Menü
        mnuEntries = new JMenu(JDL.L(LOCALE_PREFIX + "entries", "Entries"));
        mnuEntries.setEnabled(false);

        mnuEntries.addSeparator();
        mnuEntries.add(mnuShowDupes = new JMenuItem(JDL.L(LOCALE_PREFIX + "showDupes", "Show Dupes")));
        mnuEntries.addSeparator();
        mnuEntries.add(mnuOpenSearchDialog = new JMenuItem(JDL.L(LOCALE_PREFIX + "openSearchDialog", "Open Search Dialog")));

        mnuShowDupes.addActionListener(this);
        mnuOpenSearchDialog.addActionListener(this);

        mnuShowDupes.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_D, KeyEvent.CTRL_DOWN_MASK));
        mnuOpenSearchDialog.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F, KeyEvent.CTRL_DOWN_MASK));

        // Menü-Bar zusammensetzen
        JMenuBar menuBar = new JMenuBar();
        menuBar.add(mnuFile);

        menuBar.add(mnuKey);
        menuBar.add(mnuEntries);

        return menuBar;
    }

    private void buildContextMenu() {
        // Context Menü
        mnuContextPopup = new JPopupMenu();

        mnuContextPopup.add(mnuContextDelete = new JMenuItem(JDL.L(LOCALE_PREFIX + "deleteKeys", "Delete Key(s)")));
        mnuContextPopup.add(mnuContextClear = new JMenuItem(JDL.L(LOCALE_PREFIX + "clearValues", "Clear Value(s)")));
        mnuContextPopup.addSeparator();
        mnuContextPopup.add(mnuContextAdopt = new JMenuItem(JDL.L(LOCALE_PREFIX + "adoptDefaults", "Adopt Default(s)")));
        mnuContextPopup.add(mnuContextTranslate = new JMenuItem(JDL.L(LOCALE_PREFIX + "translate", "Translate with Google")));

        mnuContextDelete.addActionListener(this);
        mnuContextClear.addActionListener(this);
        mnuContextAdopt.addActionListener(this);
        mnuContextTranslate.addActionListener(this);
    }

    public void actionPerformed(ActionEvent e) {
        // if (e.getSource() == cmboSource) {
        //
        // if (!initComplete) return;
        // File sourceFile = cmboSource.getCurrentPath();
        // if (sourceFile == this.sourceFile) return;
        //
        // if (!saveChanges(this.sourceFile, true, null)) return;
        //
        // if (sourceFile != this.sourceFile && sourceFile != null) {
        // this.sourceFile = sourceFile;
        // initLocaleDataComplete();
        // }
        //
        // } else
        // if (e.getSource() == cmboFile) {

        // File languageFile = cmboFile.getCurrentPath();
        // if (languageFile == this.languageFile) return;
        //
        // if
        // (!languageFile.getAbsolutePath().startsWith(this.dirLanguages.getAbsolutePath()))
        // {
        // UserIO.getInstance().requestMessageDialog(JDLocale.LF(LOCALE_PREFIX +
        // "wrongLanguageFile",
        // "With the selected LanguageFile you are unable to let the LanguageFileEditor commit your changes to the SVN! Please change to a LanguageFile from the folder %s",
        // dirLanguages.getAbsolutePath()));
        // }
        //
        // if (!saveChanges(this.languageFile, false, languageFile)) return;
        //
        // if (languageFile != this.languageFile && languageFile != null) {
        // this.languageFile = languageFile;
        // initLocaleData();
        // }

        // } else
        
        if (e.getSource() == this.mnuReload) {
            initLocaleDataComplete();
            updateSVNinThread();
            
        }else if (e.getSource() == mnuNew) {

            if (!saveChanges()) return;

            JDFileChooser chooser = new JDFileChooser("LANGFILEEDITOR_FILE");
            chooser.setFileFilter(fileFilter);
            if (languageFile != null) chooser.setCurrentDirectory(languageFile.getParentFile());

            if (chooser.showSaveDialog(this) == JDFileChooser.APPROVE_OPTION) {
                languageFile = chooser.getSelectedFile();
                if (!languageFile.getAbsolutePath().endsWith(".loc")) languageFile = new File(languageFile.getAbsolutePath() + ".loc");
                if (!languageFile.exists()) {
                    try {
                        languageFile.createNewFile();
                    } catch (IOException e1) {
                        JDLogger.exception(e1);
                    }
                }
                // cmboFile.setCurrentPath(languageFile);

                initLocaleDataComplete();
            }

        } else if (e.getSource() == mnuSave) {

            saveLanguageFile(languageFile);

        } else if (e.getSource() == mnuAdd) {

            String[] result = SimpleGUI.CURRENTGUI.showTwoTextFieldDialog(JDL.L(LOCALE_PREFIX + "addKey.title", "Add new key"), JDL.L(LOCALE_PREFIX + "addKey.message1", "Type in the name of the key:"), JDL.L(LOCALE_PREFIX + "addKey.message2", "Type in the translated message of the key:"), "", "");
            if (result == null || result[0].equals("")) return;
            result[0] = result[0].toLowerCase();
            for (KeyInfo ki : data) {
                if (ki.getKey().equals(result[0])) {
                    UserIO.getInstance().requestMessageDialog(JDL.LF(LOCALE_PREFIX + "addKey.error.message", "The key '%s' is already in use!", result[0]));
                    return;
                }
            }
            data.add(new KeyInfo(result[0].toLowerCase(), null, result[1]));
            tableModel.fireTableDataChanged();
            updateKeyChart();

        } else if (e.getSource() == mnuDelete || e.getSource() == mnuContextDelete) {

            deleteSelectedKeys();

        } else if (e.getSource() == mnuAdopt || e.getSource() == mnuContextAdopt) {

            for (int row : getSelectedRows()) {
                tableModel.setValueAt(tableModel.getValueAt(row, 2), row, 3);
            }

        } else if (e.getSource() == mnuAdoptMissing) {

            for (int i = 0; i < tableModel.getRowCount(); ++i) {

                if (tableModel.getValueAt(i, 3).equals("")) {
                    tableModel.setValueAt(tableModel.getValueAt(i, 2), i,3);
                }
            }

        } else if (e.getSource() == mnuClear || e.getSource() == mnuContextClear) {

            for (int row : getSelectedRows()) {
                tableModel.setValueAt("", row,3);
            }

        } else if (e.getSource() == mnuShowDupes) {

            LFEDupeDialog.showDialog(SimpleGUI.CURRENTGUI, dupes);

        } else if (e.getSource() == mnuOpenSearchDialog) {

            SearchFactory.getInstance().showFindInput(table, table.getSearchable());

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


        }

    }

    private boolean saveChanges() {
        if (changed) {
            int res = JOptionPane.showConfirmDialog(this, JDL.L(LOCALE_PREFIX + "changed.message", "Language File changed! Save changes?"), JDL.L(LOCALE_PREFIX + "changed.title", "Save changes?"), JOptionPane.YES_NO_CANCEL_OPTION);
            if (res == JOptionPane.CANCEL_OPTION) {

                return false;
            } else if (res == JOptionPane.YES_OPTION) {
                saveLanguageFile(languageFile);

            } else {
                changed = false;
            }
        }

        return true;
    }

    private void updateSVNinThread() {
        if (updatingInProgress) return;
        new Thread(new Runnable() {

            public void run() {
                updateSVN();
            }

        }).start();
    }

    private void updateSVN() {
        SimpleGUI.CURRENTGUI.setWaiting(true);
        updatingInProgress = true;

        if (!dirLanguages.exists()) dirLanguages.mkdirs();
        if (!dirWorkingCopy.exists()) dirWorkingCopy.mkdirs();

        final ProgressController progress = new ProgressController(JDL.L(LOCALE_PREFIX + "svn.updating", "Updating SVN: Please wait"));
        progress.setIndeterminate(true);
        try {
            Subversion svn = null;
            Subversion svnLanguageDir;
            if (subConfig.getBooleanProperty(PROPERTY_SVN_ACCESS_ANONYMOUS, true)) {
                svn = new Subversion(SOURCE_SVN);
                svnLanguageDir = new Subversion(LANGUAGE_SVN);
            } else {
                svn = new Subversion(SOURCE_SVN, subConfig.getStringProperty(PROPERTY_SVN_ACCESS_USER), subConfig.getStringProperty(PROPERTY_SVN_ACCESS_PASS));
                svnLanguageDir = new Subversion(LANGUAGE_SVN, subConfig.getStringProperty(PROPERTY_SVN_ACCESS_USER), subConfig.getStringProperty(PROPERTY_SVN_ACCESS_PASS));
            }
            HEAD = svn.latestRevision();
            svn.getBroadcaster().addListener(new MessageListener() {

                public void onMessage(MessageEvent event) {
                    progress.setStatusText(JDL.L(LOCALE_PREFIX + "svn.updating", "Updating SVN: Please wait") + ": " + event.getMessage().replace(dirWorkingCopy.getParentFile().getAbsolutePath(), ""));

                }

            });
            try {
                svn.update(this.dirWorkingCopy, null);
            } catch (Exception e) {

                JDLogger.exception(e);
                UserIO.getInstance().requestMessageDialog(JDL.L(LOCALE_PREFIX + "error.title", "Error occured"), JDL.LF(LOCALE_PREFIX + "error.updatesource.message", "Error while updating source:\r\n %s", JDLogger.getStackTrace(e)));
            }
            try {
                svnLanguageDir.update(dirLanguages, null);
            } catch (Exception e) {
                JDLogger.exception(e);
                UserIO.getInstance().requestMessageDialog(JDL.L(LOCALE_PREFIX + "error.title", "Error occured"), JDL.LF(LOCALE_PREFIX + "error.updatelanguages.message", "Error while updating languages:\r\n %s", JDLogger.getStackTrace(e)));

            }
            progress.setStatusText(JDL.L(LOCALE_PREFIX + "svn.updating.ready", "Updating SVN: Complete"));
            progress.finalize(2 * 1000l);
        } catch (SVNException e) {
            JDLogger.exception(e);
            progress.setColor(Color.RED);
            progress.setStatusText(JDL.L(LOCALE_PREFIX + "svn.updating.error", "Updating SVN: Error!"));
            progress.finalize(5 * 1000l);
        }
        updatingInProgress = false;
        SimpleGUI.CURRENTGUI.setWaiting(false);

        // if (sourceFile == null || !sourceFile.equals(dirWorkingCopy)) {
        // if (!initComplete) sourceFile = dirWorkingCopy;
        // cmboSource.setCurrentPath(dirWorkingCopy);
        // } else if (sourceFile.equals(dirWorkingCopy) && !changed) {
        // if (initComplete) initLocaleDataComplete();
        // }
    }

    private String getLanguageKey() {
        if (lngKey == null) {
            String[] localeKeys = new String[] { "da", "de", "fi", "fr", "el", "hi", "it", "ja", "ko", "hr", "nl", "no", "pl", "pt", "ro", "ru", "sv", "es", "cs", "en", "ar" };
            Object newKey = JOptionPane.showInputDialog(this, JDL.L("plugins.optional.langfileeditor.translatedialog.message", "Choose Languagekey:"), JDL.L("plugins.optional.langfileeditor.translatedialog.title", "Languagekey"), JOptionPane.QUESTION_MESSAGE, null, localeKeys, null);
            lngKey = (newKey == null) ? null : newKey.toString();
        }
        return lngKey;
    }

    private void translateRow(int row) {
        String def = tableModel.getValueAt(row, 1);
        if (!def.equals("")) {
            String res = JDL.translate(lngKey, def);
            if (res != null) tableModel.setValueAt(res, row, 2);
        }
    }

    private void deleteSelectedKeys() {
        int[] rows = getSelectedRows();
        Arrays.sort(rows);

        int len = rows.length - 1;
        ArrayList<String> keys = new ArrayList<String>(dupes.keySet());
        ArrayList<ArrayList<String>> obj = new ArrayList<ArrayList<String>>(dupes.values());
        ArrayList<String> values;
        for (int i = len; i >= 0; --i) {
            System.out.println(rows[i]);
            String temp = data.remove(rows[i]).getKey();
            data.remove(temp);
            for (int j = obj.size() - 1; j >= 0; --j) {
                values = obj.get(j);
                values.remove(temp);
                if (values.size() == 1) dupes.remove(keys.get(j));
            }
            tableModel.fireTableRowsDeleted(rows[i], rows[i]);
        }
        int newRow = Math.min(rows[len] - len, tableModel.getRowCount() - 1);
        table.getSelectionModel().setSelectionInterval(newRow, newRow);

        updateKeyChart();
        changed = true;
    }

    private int[] getSelectedRows() {
        int[] rows = table.getSelectedRows();
        int[] ret = new int[rows.length];

        for (int i = 0; i < rows.length; ++i) {
            ret[i] = table.convertRowIndexToModel(rows[i]);
        }
        Arrays.sort(ret);
        return ret;
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
            String message = UserIO.getInstance().requestInputDialog(0, "Enter change description", "Please enjter a short description for your changes (in english).", "", null, null, null);
            if (message == null) message = "Updated language file";
            if (!commit(file, message, null)) {

                UserIO.getInstance().requestMessageDialog("Could not upload changes. Please send the file " + file.getAbsolutePath() + " to support@jdownloader.org");

            }
        } catch (Exception e) {
            UserIO.getInstance().requestMessageDialog(JDL.LF(LOCALE_PREFIX + "save.error.message", "An error occured while writing the LanguageFile:\n%s", e.getMessage()));
            return;
        }

        // if (languageFile.getAbsolutePath() != cmboFile.getText())
        // cmboFile.setCurrentPath(languageFile);
        changed = false;
        UserIO.getInstance().requestMessageDialog(JDL.L(LOCALE_PREFIX + "save.success.message", "LanguageFile saved successfully!"));
    }

    private void initLocaleDataComplete() {
        getSourceEntries();
        initLocaleData();
    }

    private void initLocaleData() {
        SimpleGUI.CURRENTGUI.setWaiting(true);
        parseLanguageFile(languageFile, languageKeysFormFile);

        HashMap<String, String> dupeHelp = new HashMap<String, String>();
        data.clear();
        dupes.clear();
        lngKey = null;

        ArrayList<String> values;
        String value, key, language;
        KeyInfo keyInfo;
        for (LngEntry entry : sourceParser.getEntries()) {
            key = entry.getKey();
            keyInfo = new KeyInfo(key, entry.getValue(), languageKeysFormFile.remove(key));
            if (key.equalsIgnoreCase("$Version$")) keyInfo.setLanguage("$Revision$");
            data.add(keyInfo);
            if (!keyInfo.isMissing()) {

                language = keyInfo.getLanguage();
                if (dupeHelp.containsKey(language)) {
                    values = dupes.get(language);
                    if (values == null) {
                        values = new ArrayList<String>();
                        values.add(dupeHelp.get(language));
                        dupes.put(language, values);
                    }
                    values.add(key);
                }
                dupeHelp.put(language, key);
            }
        }

        for (Entry<String, String> entry : languageKeysFormFile.entrySet()) {
            key = entry.getKey();
            value = null;

            for (String patt : sourceParser.getPattern()) {

                if (key.matches(patt)) {
                    value = "<pattern> " + patt;
                }
            }

            data.add(new KeyInfo(key, value, entry.getValue()));
        }

        Collections.sort(data);

        tableModel.fireTableRowsInserted(0, data.size() - 1);
        table.packAll();
        changed = false;

        SwingUtilities.invokeLater(new Runnable() {

            public void run() {
                updateKeyChart();
                mnuEntries.setEnabled(true);
                mnuKey.setEnabled(true);

                mnuSave.setEnabled(true);

            }

        });

        SimpleGUI.CURRENTGUI.setWaiting(false);
    }

    private void getSourceEntries() {
        SimpleGUI.CURRENTGUI.setWaiting(true);
        // if (sourceFile.isDirectory()) {

        getSourceEntriesFromFolder();
        // } else {
        // getSourceEntriesFromFile();
        // }
        SimpleGUI.CURRENTGUI.setWaiting(false);
    }

    // private void getSourceEntriesFromFile() {
    // sourcePatterns.clear();
    // // parseLanguageFile(sourceFile, sourceEntries);
    // }

    private void getSourceEntriesFromFolder() {

        ProgressController progress = new ProgressController(JDL.L(LOCALE_PREFIX + "analyzingSource1", "Analyzing Source Folder"));
        progress.setIndeterminate(true);
        Subversion svn = null;
        try {

            if (subConfig.getBooleanProperty(PROPERTY_SVN_ACCESS_ANONYMOUS, true)) {
                svn = new Subversion(SOURCE_SVN);

            } else {
                svn = new Subversion(SOURCE_SVN, subConfig.getStringProperty(PROPERTY_SVN_ACCESS_USER), subConfig.getStringProperty(PROPERTY_SVN_ACCESS_PASS));
            }

            SVNInfo info = svn.getWCClient().doInfo(new File(dirLanguages, "keys.def"), SVNRevision.HEAD);
            if (info.getCommittedRevision().getNumber() == HEAD) {
                sourceParser = new SrcParser(this.dirWorkingCopy);
                sourceParser.getBroadcaster().addListener(progress);
                sourceParser.parseDefault(new File(dirLanguages, "keys.def"));
                progress.setStatusText(JDL.L(LOCALE_PREFIX + "analyzingSource.ready", "Analyzing Source Folder: Complete"));
                progress.finalize(2 * 1000l);
                return;
            }
        } catch (SVNException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        sourceParser = new SrcParser(this.dirWorkingCopy);
        sourceParser.getBroadcaster().addListener(progress);
        sourceParser.parse();

        JDLogger.getLogger().warning("Patternmatches are not recommened: \r\n" + sourceParser.getPattern());

        try {

            if (svn != null) {

                if (!subConfig.getBooleanProperty(PROPERTY_SVN_ACCESS_ANONYMOUS, true)) {

                    StringBuilder sb = new StringBuilder();
                    sb.append("#Rev" + HEAD);
                    for (LngEntry lng : sourceParser.getEntries()) {
                        sb.append("\r\n" + lng.getKey() + " = " + lng.getValue());

                    }

                    for (String pat : sourceParser.getPattern()) {
                        sb.append("\r\n#pattern: " + pat);
                    }
                    JDIO.writeLocalFile(new File(dirLanguages, "keys.def"), sb.toString());

                    svn = new Subversion(LANGUAGE_SVN, subConfig.getStringProperty(PROPERTY_SVN_ACCESS_USER), subConfig.getStringProperty(PROPERTY_SVN_ACCESS_PASS));
                    commit(new File(dirLanguages, "keys.def"), "parsed latest Source at Revision " + HEAD, svn);

                }
            }
        } catch (SVNException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        progress.setStatusText(JDL.L(LOCALE_PREFIX + "analyzingSource.ready", "Analyzing Source Folder: Complete"));
        progress.finalize(2 * 1000l);
    }

    private boolean commit(File file, String string, Subversion svn) {
        try {
            if (subConfig.getBooleanProperty(PROPERTY_SVN_ACCESS_ANONYMOUS, true)) return false;
            if (svn == null) {

                svn = new Subversion(LANGUAGE_SVN, subConfig.getStringProperty(PROPERTY_SVN_ACCESS_USER), subConfig.getStringProperty(PROPERTY_SVN_ACCESS_PASS));
            }
            svn.update(file, null);
            svn.commit(file, string);
            return true;
        } catch (SVNException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return false;
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
            JDLogger.exception(e);
        }
    }

    public void mouseClicked(MouseEvent e) {
        if (e.getButton() == MouseEvent.BUTTON3) {
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

        private final String key;

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
            return (!table.getValueAt(arg1.row, 2).equals("") && !table.getValueAt(arg1.row, 3).equals(""));
        }
    }

    private class MissingPredicate implements HighlightPredicate {
        public boolean isHighlighted(Component arg0, ComponentAdapter arg1) {
            return (table.getValueAt(arg1.row, 3).equals(""));
        }
    }

    private class OldPredicate implements HighlightPredicate {
        public boolean isHighlighted(Component arg0, ComponentAdapter arg1) {
            return (table.getValueAt(arg1.row, 2).equals(""));
        }
    }

    private class MyPatternFilter extends PatternFilter {

        @Override
        public boolean test(int row) {
            boolean result = true;
            return result;
        }

    }

    private class MyTableModel extends AbstractTableModel {

        private static final long serialVersionUID = -5434313385327397539L;

        private String[] columnNames = { JDL.L(LOCALE_PREFIX + "id", "ID"), JDL.L(LOCALE_PREFIX + "key", "Key"), JDL.L(LOCALE_PREFIX + "sourceValue", "Default Value"), JDL.L(LOCALE_PREFIX + "languageFileValue", "Language File Value") };

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
                return getType(data.get(row)) + row + "";
            case 1:
                return data.get(row).getKey();
            case 2:
                return data.get(row).getSource();
            case 3:
                return data.get(row).getLanguage();
            }
            return "";
        }

        private String getType(KeyInfo keyInfo) {
            if (keyInfo.isMissing()) return "M";
            if (keyInfo.getSource() == null || keyInfo.getSource().equals("")) return "O";
            if (keyInfo.getLanguage() != null && keyInfo.getLanguage().trim().length() > 0) return "D";
            return " ";
        }

        @Override
        public Class<?> getColumnClass(int c) {
            return String.class;
        }

        @Override
        public boolean isCellEditable(int row, int col) {
            if (table.getValueAt(row, 1).toString().equalsIgnoreCase("$Version$")) return false;
            return true;
        }

        @Override
        public void setValueAt(Object value, int row, int col) {
            if (col == 3) {
                data.get(row).setLanguage((String) value);
                this.fireTableRowsUpdated(row, row);
                updateKeyChart();
                changed = true;
            }
        }

    }

    @Override
    public void onDisplay() {
    }

    @Override
    public void onHide() {
        if (changed && JOptionPane.showConfirmDialog(this, JDL.L(LOCALE_PREFIX + "changed.message", "Language File changed! Save changes?"), JDL.L(LOCALE_PREFIX + "changed.title", "Save changes?"), JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
            saveLanguageFile(languageFile);
        }
    }

    @Override
    public boolean needsViewport() {
        return false;
    }

}
