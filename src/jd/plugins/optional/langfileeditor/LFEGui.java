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
import jd.gui.swing.GuiRunnable;
import jd.gui.swing.SwingGui;
import jd.gui.swing.components.pieapi.ChartAPIEntity;
import jd.gui.swing.components.pieapi.PieChartAPI;
import jd.gui.swing.jdgui.interfaces.SwitchPanel;
import jd.nutils.JDFlags;
import jd.nutils.io.JDIO;
import jd.nutils.svn.ResolveHandler;
import jd.nutils.svn.Subversion;
import jd.parser.Regex;
import jd.utils.JDGeoCode;
import jd.utils.JDUtilities;
import jd.utils.locale.JDL;
import net.miginfocom.swing.MigLayout;

import org.jdesktop.swingx.JXTable;
import org.jdesktop.swingx.decorator.ColorHighlighter;
import org.jdesktop.swingx.decorator.ComponentAdapter;
import org.jdesktop.swingx.decorator.HighlightPredicate;
import org.jdesktop.swingx.search.SearchFactory;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.wc.SVNInfo;

public class LFEGui extends SwitchPanel implements ActionListener, MouseListener {

    private static final long serialVersionUID = -143452893912428555L;

    public static final String SOURCE_SVN = "svn://svn.jdownloader.org/jdownloader/trunk/src";

    public static final String LANGUAGE_SVN = "svn://svn.jdownloader.org/jdownloader/trunk/ressourcen/jd/languages";

    private static final String LOCALE_PREFIX = "plugins.optional.langfileeditor.";

    private final SubConfiguration subConfig;

    public final static String PROPERTY_SVN_ACCESS_ANONYMOUS = "PROPERTY_SVN_CHECKOUT_ANONYMOUS";
    public final static String PROPERTY_SVN_ACCESS_USER = "PROPERTY_SVN_CHECKOUT_USER";
    public final static String PROPERTY_SVN_ACCESS_PASS = "PROPERTY_SVN_CHECKOUT_PASS";

    private static final String MISSING_KEY = "~MISSING KEY/REMOVED~";

    private JXTable table;
    private MyTableModel tableModel;
    private File languageFile;
    private PieChartAPI keyChart;
    private ChartAPIEntity entDone, entMissing, entOld;
    private JMenu mnuFile, mnuLoad, mnuKey, mnuEntries;
    private JMenuItem mnuSave, mnuReload;
    private JMenuItem mnuAdd, mnuAdopt, mnuAdoptMissing, mnuClear, mnuDelete, mnuTranslate, mnuTranslateMissing;
    private JMenuItem mnuShowDupes, mnuOpenSearchDialog;
    private JPopupMenu mnuContextPopup;
    private JMenuItem mnuContextAdopt, mnuContextClear, mnuContextDelete, mnuContextTranslate;

    private HashMap<String, String> languageKeysFormFile = new HashMap<String, String>();
    private ArrayList<KeyInfo> data = new ArrayList<KeyInfo>();
    private HashMap<String, ArrayList<String>> dupes = new HashMap<String, ArrayList<String>>();
    private String lngKey = null;
    private boolean changed = false;
    private final File dirLanguages, dirWorkingCopy;

    private ColorHighlighter doneHighlighter, missingHighlighter, oldHighlighter;

    private SrcParser sourceParser;

    private long HEAD;

    public LFEGui(SubConfiguration cfg) {
        subConfig = cfg;
        this.setName(JDL.L(LOCALE_PREFIX + "title", "Language Editor"));
        dirLanguages = JDUtilities.getResourceFile("tmp/lfe/lng/");
        dirWorkingCopy = JDUtilities.getResourceFile("tmp/lfe/src/");
        dirLanguages.mkdirs();
        dirWorkingCopy.mkdirs();
        showGui();
    }

    private void showGui() {
        doneHighlighter = new ColorHighlighter(new DonePredicate(), Color.GREEN, null);
        missingHighlighter = new ColorHighlighter(new MissingPredicate(), Color.RED, null);
        oldHighlighter = new ColorHighlighter(new OldPredicate(), Color.ORANGE, null);

        tableModel = new MyTableModel();
        table = new JXTable(tableModel);

        table.setEnabled(false);
        table.getTableHeader().setReorderingAllowed(false);
        table.getColumn(0).setMinWidth(50);
        table.getColumn(0).setMaxWidth(50);
        table.getColumn(1).setMinWidth(200);
        table.getColumn(1).setPreferredWidth(200);
        table.getColumn(3).setPreferredWidth(200);
        table.addMouseListener(this);
        table.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        table.setAutoStartEditOnKeyStroke(false);

        table.addHighlighter(doneHighlighter);
        table.addHighlighter(missingHighlighter);
        table.addHighlighter(oldHighlighter);
        keyChart = new PieChartAPI("", 225, 50);
        keyChart.addEntity(entDone = new ChartAPIEntity(JDL.L(LOCALE_PREFIX + "keychart.done", "Done"), 0, Color.GREEN));
        keyChart.addEntity(entMissing = new ChartAPIEntity(JDL.L(LOCALE_PREFIX + "keychart.missing", "Missing"), 0, Color.RED));
        keyChart.addEntity(entOld = new ChartAPIEntity(JDL.L(LOCALE_PREFIX + "keychart.old", "Old"), 0, Color.ORANGE));

        this.setLayout(new MigLayout("wrap 3", "[grow, fill]", "[grow, fill][]"));
      
        this.add(new JScrollPane(table), "grow, spanx");
        this.add(keyChart, "w 225!, h 50!");

        new Thread(new Runnable() {

            public void run() {
                LFEGui.this.setEnabled(false);

                updateSVN(false);

                getSourceEntries();
                populateLngMenu();
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

  
    private void populateLngMenu() {
        new GuiRunnable<Object>() {

            @Override
            public Object runSave() {
                mnuLoad.removeAll();
                for (File f : dirLanguages.listFiles()) {
                    if (f.getName().endsWith(".loc")) {
                        try {
                            String language = JDGeoCode.toLonger(f.getName().substring(0, f.getName().length() - 4));
                            if (language != null) {
                                JMenuItem mi = new JMenuItem(language);
                                mi.addActionListener(new ActionListener() {

                                    public void actionPerformed(ActionEvent e) {
                                        if (languageFile != null) {
                                            saveChanges();
                                        }
                                        languageFile = new File(dirLanguages, JDGeoCode.longToShort(e.getActionCommand()) + ".loc");
                                        initLocaleData();
                                        table.setEnabled(true);
                                    }

                                });
                                mnuLoad.add(mi);
                            }
                        } catch (Exception e) {
                            System.out.println(f);
                            e.printStackTrace();
                        }
                    }
                }
                return null;
            }

        }.waitForEDT();

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
        if (e.getSource() == mnuReload) {
            saveChanges();

         
            new Thread(new Runnable() {
                public void run() {
                    try {
                        getSourceEntries();

                        updateSVN(true);
                        populateLngMenu();
                        initLocaleData();
                    } finally {
                    
                    }
                }
            }).start();

        } else if (e.getSource() == mnuSave) {

            saveLanguageFile(languageFile);

        } else if (e.getSource() == mnuAdd) {

            String[] result = UserIO.getInstance().requestTwoTextFieldDialog(
                                                                            JDL.L(LOCALE_PREFIX + "addKey.title", "Add new key"),
                                                                            JDL.L(LOCALE_PREFIX + "addKey.message1", "Type in the name of the key:"),
                                                                            JDL.L(LOCALE_PREFIX + "addKey.message2", "Type in the translated message of the key:"),
                                                                            "",
                                                                            "");
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
                    tableModel.setValueAt(tableModel.getValueAt(i, 2), i, 3);
                }
            }

        } else if (e.getSource() == mnuClear || e.getSource() == mnuContextClear) {

            for (int row : getSelectedRows()) {
                tableModel.setValueAt("", row, 3);
            }

        } else if (e.getSource() == mnuShowDupes) {

            LFEDupeDialog.showDialog(SwingGui.getInstance().getMainFrame(), dupes);

        } else if (e.getSource() == mnuOpenSearchDialog) {

            SearchFactory.getInstance().showFindInput(table, table.getSearchable());

        } else if (e.getSource() == mnuTranslate || e.getSource() == mnuContextTranslate) {

            if (lngKey == null) return;

            int[] rows = getSelectedRows();

            for (int i = rows.length - 1; i >= 0; --i) {
                translateRow(rows[i]);
            }

        } else if (e.getSource() == mnuTranslateMissing) {

            if (lngKey == null) return;

            for (int i = tableModel.getRowCount() - 1; i >= 0; --i) {
                if (tableModel.getValueAt(i, 2).equals("")) {
                    translateRow(i);
                }
            }

        }

    }

    private void saveChanges() {
        if (!changed) return;
        int ret = UserIO.getInstance().requestConfirmDialog(UserIO.NO_COUNTDOWN, "Save changes?", "Save your changes to " + this.languageFile + "?", null, null, null);
        if (JDFlags.hasAllFlags(ret, UserIO.RETURN_OK)) {
            saveLanguageFile(languageFile);
        }
    }

    private void updateSVN(boolean revert) {
      

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
                    progress
                            .setStatusText(JDL.L(LOCALE_PREFIX + "svn.updating", "Updating SVN: Please wait") + ": " + event.getMessage().replace(dirWorkingCopy.getParentFile().getAbsolutePath(), ""));

                }

            });
            try {
                svnLanguageDir.revert(dirWorkingCopy);
            } catch (Exception e) {

                JDLogger.exception(e);
            }
            try {
                svn.update(this.dirWorkingCopy, null);
            } catch (Exception e) {

                JDLogger.exception(e);
                UserIO.getInstance().requestMessageDialog(
                                                          JDL.L(LOCALE_PREFIX + "error.title", "Error occured"),
                                                          JDL.LF(LOCALE_PREFIX + "error.updatesource.message", "Error while updating source:\r\n %s", JDLogger.getStackTrace(e)));
            }
            if (revert) {
                try {
                    svnLanguageDir.revert(dirLanguages);
                } catch (Exception e) {

                    JDLogger.exception(e);
                }

            }
            try {
                svnLanguageDir.update(dirLanguages, null);
            } catch (Exception e) {
                JDLogger.exception(e);
                UserIO.getInstance().requestMessageDialog(
                                                          JDL.L(LOCALE_PREFIX + "error.title", "Error occured"),
                                                          JDL.LF(LOCALE_PREFIX + "error.updatelanguages.message", "Error while updating languages:\r\n %s", JDLogger.getStackTrace(e)));

            }
            progress.setStatusText(JDL.L(LOCALE_PREFIX + "svn.updating.ready", "Updating SVN: Complete"));
            progress.doFinalize(2 * 1000l);
        } catch (SVNException e) {
            JDLogger.exception(e);
            progress.setColor(Color.RED);
            progress.setStatusText(JDL.L(LOCALE_PREFIX + "svn.updating.error", "Updating SVN: Error!"));
            progress.doFinalize(5 * 1000l);
        }

    

    }

    private void translateRow(int row) {
        String def = tableModel.getValueAt(row, 2);
        if (!def.equals("")) {
            String res = JDL.translate(lngKey, def);
            if (res != null) tableModel.setValueAt(res, row, 3);
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
            Subversion svn;
            if (subConfig.getBooleanProperty(PROPERTY_SVN_ACCESS_ANONYMOUS, true)) {
                svn = new Subversion(LANGUAGE_SVN);

            } else {
                svn = new Subversion(LANGUAGE_SVN, subConfig.getStringProperty(PROPERTY_SVN_ACCESS_USER), subConfig.getStringProperty(PROPERTY_SVN_ACCESS_PASS));
            }

            ArrayList<SVNInfo> info = svn.getInfo(file);
            if (info.get(0).getConflictWrkFile() != null) {
                svn.revert(file);
            }
            BufferedWriter out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), "UTF8"));
            out.write(sb.toString());
            out.close();

            String message = UserIO.getInstance().requestInputDialog(0, "Enter change description", "Please enter a short description for your changes (in english).", "", null, null, null);
            if (message == null) message = "Updated language file";
            if (!commit(file, message, null)) {

                UserIO.getInstance().requestMessageDialog("Could not upload changes. Please send the file " + file.getAbsolutePath() + " to support@jdownloader.org");

                return;
            }
        } catch (Exception e) {
            UserIO.getInstance().requestMessageDialog(JDL.LF(LOCALE_PREFIX + "save.error.message", "An error occured while writing the LanguageFile:\n%s", e.getMessage()));
            return;
        }

        changed = false;
        UserIO.getInstance().requestMessageDialog(JDL.L(LOCALE_PREFIX + "save.success.message", "LanguageFile saved successfully!"));
        initLocaleData();
    }

    private void initLocaleData() {
      
        parseLanguageFile(languageFile, languageKeysFormFile);

        HashMap<String, String> dupeHelp = new HashMap<String, String>();
        data.clear();
        dupes.clear();
        if (languageFile != null) {
            lngKey = languageFile.getName().substring(0, languageFile.getName().length() - 4);
            lngKey = JDGeoCode.parseLanguageCode(lngKey)[0];
        }
        ArrayList<String> values;
        String value, key, language;
        KeyInfo keyInfo;
        for (LngEntry entry : sourceParser.getEntries()) {
            key = entry.getKey();
            keyInfo = new KeyInfo(key, entry.getValue(), languageKeysFormFile.remove(key));
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


    }

    private void getSourceEntries() {
        
        getSourceEntriesFromFolder();
        
    }

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

        } catch (SVNException e) {
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
            e.printStackTrace();
        }
        progress.setStatusText(JDL.L(LOCALE_PREFIX + "analyzingSource.ready", "Analyzing Source Folder: Complete"));
        progress.doFinalize(2 * 1000l);
    }

    private boolean commit(File file, String string, Subversion svn) {
        try {
            if (subConfig.getBooleanProperty(PROPERTY_SVN_ACCESS_ANONYMOUS, true)) return false;
            if (svn == null) {
                svn = new Subversion(LANGUAGE_SVN, subConfig.getStringProperty(PROPERTY_SVN_ACCESS_USER), subConfig.getStringProperty(PROPERTY_SVN_ACCESS_PASS));
            }
            svn.update(file, null);
            try {
                svn.resolveConflicts(file, new ResolveHandler() {

                    public String resolveConflict(SVNInfo info, File file, String contents, int startMine, int endMine, int startTheirs, int endTheirs) {
                        String[] mine = Regex.getLines(contents.substring(startMine, endMine).trim());
                        String[] theirs = Regex.getLines(contents.substring(startTheirs, endTheirs).trim());
                        StringBuilder sb = new StringBuilder();
                        ArrayList<String> keys = new ArrayList<String>();
                        for (String m : mine) {
                            int index = m.indexOf("=");
                            String key = m.substring(0, index).trim();
                            String value = m.substring(index + 1).trim();
                            boolean found = false;

                            for (String t : theirs) {

                                if (t.startsWith(key)) {
                                    int tindex = t.indexOf("=");
                                    String tkey = t.substring(0, tindex).trim();
                                    String tvalue = t.substring(tindex + 1).trim();
                                    if (key.equalsIgnoreCase(tkey)) {
                                        found = true;
                                        if (value.equals(tvalue)) {
                                            sb.append(key + " = " + value + "\r\n");
                                            keys.add(key);
                                            break;
                                        } else {

                                            String newValue = selectVersion(key, value, tvalue);
                                            sb.append(key + " = " + newValue + "\r\n");
                                            keys.add(key);
                                            break;
                                        }

                                    }
                                }

                            }

                            if (!found) {
                                String newValue = selectVersion(key, value, MISSING_KEY);
                                if (newValue == MISSING_KEY) continue;
                                sb.append(key + " = " + value + "\r\n");
                                keys.add(key);
                                continue;
                            }

                        }

                        for (String t : theirs) {
                            int tindex = t.indexOf("=");
                            if (tindex < 0) {
                                continue;
                            }
                            String tkey = t.substring(0, tindex).trim();
                            String tvalue = t.substring(tindex + 1).trim();
                            if (!keys.contains(tkey)) {
                                String newValue = selectVersion(tkey, MISSING_KEY, tvalue);
                                if (newValue == MISSING_KEY) continue;

                                sb.append(tkey + " = " + tvalue + "\r\n");
                                keys.add(tkey);
                            }

                        }

                        return sb.toString().trim();
                    }

                    private String selectVersion(String key, String value, String tvalue) {
                        String html = "<h1>Key: " + key + "</h1><h2>Translation A</h2>" + value + "<h2>Translation B</h2>" + tvalue + "<br><br>Select the better translation. A or B:";
                        int ret = UserIO.getInstance().requestConfirmDialog(UserIO.STYLE_HTML | UserIO.STYLE_LARGE | UserIO.NO_COUNTDOWN, "Conflicts occured!", html, null, "A", "B");
                        if (JDFlags.hasAllFlags(ret, UserIO.RETURN_CANCEL)) return tvalue;
                        return value;

                    }

                });
            } catch (Exception e) {
                e.printStackTrace();
            }
            svn.commit(file, string);
            return true;
        } catch (SVNException e) {
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

    private class MyTableModel extends AbstractTableModel {

        private static final long serialVersionUID = -5434313385327397539L;

        private String[] columnNames = {
                JDL.L(LOCALE_PREFIX + "id", "ID"),
                JDL.L(LOCALE_PREFIX + "key", "Key"),
                JDL.L(LOCALE_PREFIX + "sourceValue", "Default Value"),
                JDL.L(LOCALE_PREFIX + "languageFileValue", "Language File Value")
        };

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
    public void onShow() {
    }

    @Override
    public void onHide() {
        saveChanges();
    }

    public void initMenu(JMenuBar menubar) {

        // Load Menü
        mnuLoad = new JMenu(JDL.L(LOCALE_PREFIX + "load", "Load Language"));

        populateLngMenu();

        // File Menü
        mnuFile = new JMenu(JDL.L(LOCALE_PREFIX + "file", "File"));

        mnuFile.add(mnuLoad);
        mnuFile.addSeparator();
        mnuFile.add(mnuSave = new JMenuItem(JDL.L(LOCALE_PREFIX + "save", "Save")));
        mnuFile.addSeparator();
        mnuFile.add(mnuReload = new JMenuItem(JDL.L(LOCALE_PREFIX + "reload", "Revert/Reload")));

        mnuReload.addActionListener(this);

        mnuSave.addActionListener(this);

        mnuSave.setEnabled(false);

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

        mnuEntries.add(mnuShowDupes = new JMenuItem(JDL.L(LOCALE_PREFIX + "showDupes", "Show Dupes")));
        mnuEntries.addSeparator();
        mnuEntries.add(mnuOpenSearchDialog = new JMenuItem(JDL.L(LOCALE_PREFIX + "openSearchDialog", "Open Search Dialog")));

        mnuShowDupes.addActionListener(this);
        mnuOpenSearchDialog.addActionListener(this);

        mnuShowDupes.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_D, KeyEvent.CTRL_DOWN_MASK));
        mnuOpenSearchDialog.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F, KeyEvent.CTRL_DOWN_MASK));

        // Menü-Bar zusammensetzen
      
        menubar.add(mnuFile);
        menubar.add(mnuKey);
        menubar.add(mnuEntries);

    
        
    }

}
