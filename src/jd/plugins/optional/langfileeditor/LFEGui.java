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
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map.Entry;

import javax.swing.JButton;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;

import jd.config.SubConfiguration;
import jd.controlling.JDLogger;
import jd.controlling.ProgressController;
import jd.event.MessageEvent;
import jd.event.MessageListener;
import jd.gui.UserIF;
import jd.gui.UserIO;
import jd.gui.swing.GuiRunnable;
import jd.gui.swing.components.table.JDRowHighlighter;
import jd.gui.swing.components.table.JDTable;
import jd.gui.swing.jdgui.interfaces.SwitchPanel;
import jd.nutils.JDFlags;
import jd.nutils.nativeintegration.LocalBrowser;
import jd.nutils.svn.ResolveHandler;
import jd.nutils.svn.Subversion;
import jd.parser.Regex;
import jd.utils.JDGeoCode;
import jd.utils.JDUtilities;
import jd.utils.locale.JDL;
import net.miginfocom.swing.MigLayout;

import org.jdesktop.swingx.search.SearchFactory;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.wc.SVNInfo;

public class LFEGui extends SwitchPanel implements ActionListener, MouseListener {

    private static final long serialVersionUID = -143452893912428555L;

    public static final String SOURCE_SVN = "svn://svn.jdownloader.org/jdownloader/trunk/src";

    public static final String LANGUAGE_SVN = "svn://svn.jdownloader.org/jdownloader/trunk/ressourcen/jd/languages";

    private static final String LOCALE_PREFIX = "plugins.optional.langfileeditor.";

    private final SubConfiguration subConfig;

    public final static String PROPERTY_SVN_ACCESS_USER = "PROPERTY_SVN_CHECKOUT_USER";
    public final static String PROPERTY_SVN_ACCESS_PASS = "PROPERTY_SVN_CHECKOUT_PASS";

    private static final String MISSING_KEY = "~MISSING KEY/REMOVED~";

    private LFEInfoPanel infoPanel;
    private LFETableModel tableModel;
    private JDTable table;
    private File languageFile;

    private JMenuBar menubar;
    private JMenu mnuFile, mnuLoad, mnuKey, mnuTest;
    private JMenuItem mnuSave, mnuSaveLocal, mnuReload, mnuCompleteReload;
    private JMenuItem mnuAdd, mnuAdopt, mnuClear, mnuDelete, mnuDeleteOld, mnuOpenSearchDialog;
    private JMenuItem mnuCurrent, mnuKeymode;
    private JPopupMenu mnuContextPopup;
    private JMenuItem mnuContextAdopt, mnuContextClear, mnuContextDelete;

    private HashMap<String, String> languageKeysFormFile = new HashMap<String, String>();
    private HashMap<String, String> languageENKeysFormFile = new HashMap<String, String>();
    private ArrayList<KeyInfo> data = new ArrayList<KeyInfo>();
    private String lngKey = null;
    private boolean changed = false;
    private int numOld = 0;
    private final File dirLanguages, dirWorkingCopy;

    public static final Color COLOR_DONE = new Color(204, 255, 170);
    public static final Color COLOR_MISSING = new Color(221, 34, 34);
    public static final Color COLOR_OLD = Color.ORANGE;

    private SrcParser sourceParser;

    private Thread updater;

    private JButton warning;

    private LangFileEditor plugin;

    public LFEGui(LangFileEditor plugin) {
        this.plugin = plugin;
        this.subConfig = plugin.getPluginConfig();
        this.infoPanel = LFEInfoPanel.getInstance();
        this.setName(JDL.L(LOCALE_PREFIX + "title", "Language Editor"));
        dirLanguages = JDUtilities.getResourceFile("tmp/lfe/lng/");
        dirWorkingCopy = JDUtilities.getResourceFile("tmp/lfe/src/");
        dirLanguages.mkdirs();
        dirWorkingCopy.mkdirs();
        initGui();
    }

    public ArrayList<KeyInfo> getData() {
        return data;
    }

    private void initGui() {
        tableModel = new LFETableModel(this);
        table = new JDTable(tableModel);
        table.setEnabled(false);
        table.addMouseListener(this);
        table.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        table.setAutoStartEditOnKeyStroke(false);
        table.addJDRowHighlighter(new JDRowHighlighter(COLOR_MISSING) {

            @Override
            public boolean doHighlight(Object obj) {
                return ((KeyInfo) obj).isMissing();
            }

        });

        table.addJDRowHighlighter(new JDRowHighlighter(COLOR_OLD) {

            @Override
            public boolean doHighlight(Object obj) {
                return ((KeyInfo) obj).isOld();
            }

        });

        this.setLayout(new MigLayout("ins 0, wrap 1", "[grow, fill]", "[][grow, fill]"));
        warning = new JButton(JDL.L(LOCALE_PREFIX + "account.warning", "SVN Account missing. Click here to read more."));
        warning.setVisible(false);
        warning.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                try {
                    LocalBrowser.openURL(null, new URL("http://jdownloader.org/knowledge/wiki/development/translation/translate-jdownloader"));
                } catch (Exception e1) {
                    e1.printStackTrace();
                    UserIO.getInstance().requestMessageDialog(JDL.L("jd.plugins.optional.langfileeditor.LangFileEditor.btn.readmore", "more..."), "http://jdownloader.org/knowledge/wiki/development/translation/translate-jdownloader");
                }
            }

        });
        this.add(warning, "grow, hidemode 2");
        this.add(new JScrollPane(table), "grow");

        updater = new Thread(new Runnable() {

            public void run() {
                boolean cfgRequested = false;
                while (true) {
                    while (subConfig.getStringProperty(PROPERTY_SVN_ACCESS_USER) == null || subConfig.getStringProperty(PROPERTY_SVN_ACCESS_USER).trim().length() == 0) {
                        if (!cfgRequested) UserIF.getInstance().requestPanel(UserIF.Panels.CONFIGPANEL, plugin.getConfig());
                        cfgRequested = true;
                        try {
                            new GuiRunnable<Object>() {

                                @Override
                                public Object runSave() {
                                    warning.setVisible(true);
                                    mnuFile.setEnabled(false);
                                    return null;
                                }

                            }.start();
                            Thread.sleep(500);
                        } catch (InterruptedException e) {
                            return;
                        }
                    }

                    if (Subversion.checkLogin(SOURCE_SVN, subConfig.getStringProperty(PROPERTY_SVN_ACCESS_USER), subConfig.getStringProperty(PROPERTY_SVN_ACCESS_PASS))) {
                        break;
                    } else {
                        UserIO.getInstance().requestMessageDialog(JDL.L("jd.plugins.optional.langfileeditor.langfileeditor.badlogins", "Logins incorrect.\r\nPlease enter correct logins."));
                        subConfig.setProperty(PROPERTY_SVN_ACCESS_USER, null);
                        subConfig.setProperty(PROPERTY_SVN_ACCESS_PASS, null);
                        subConfig.save();
                    }
                }
                new GuiRunnable<Object>() {

                    @Override
                    public Object runSave() {
                        warning.setVisible(false);
                        return null;
                    }

                }.start();
                LFEGui.this.setEnabled(false);

                updateSVN(false);

                getSourceEntries();
                populateLngMenu();
                LFEGui.this.setEnabled(true);
                if (menubar != null) {
                    new GuiRunnable<Object>() {
                        @Override
                        public Object runSave() {
                            menubar.setEnabled(true);
                            return null;
                        }
                    }.start();
                }
                new GuiRunnable<Object>() {
                    @Override
                    public Object runSave() {
                        mnuFile.setEnabled(true);
                        return null;
                    }
                }.start();
            }
        });
        updater.start();
    }

    private void updateKeyChart() {
        int numMissing = 0;
        numOld = 0;

        for (KeyInfo entry : data) {
            if (entry.isOld()) {
                numOld++;
            } else if (entry.isMissing()) {
                numMissing++;
            }
        }

        infoPanel.updateInfo(data.size() - numMissing - numOld, numMissing, numOld);
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
                                            saveChanges(true);
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

        mnuContextDelete.addActionListener(this);
        mnuContextClear.addActionListener(this);
        mnuContextAdopt.addActionListener(this);
    }

    public void actionPerformed(final ActionEvent e) {
        if (e.getSource() == mnuCompleteReload || e.getSource() == mnuReload) {
            saveChanges(false);

            new Thread(new Runnable() {
                public void run() {
                    SwingUtilities.invokeLater(new Runnable() {

                        public void run() {
                            mnuKey.setEnabled(false);
                            mnuCurrent.setEnabled(false);
                            mnuSave.setEnabled(false);
                            mnuSaveLocal.setEnabled(false);
                        }

                    });
                    if (e.getSource() == mnuCompleteReload) SrcParser.deleteCache();
                    getSourceEntries();
                    updateSVN(true);
                    populateLngMenu();
                    initLocaleData();
                }
            }).start();
        } else if (e.getSource() == mnuSaveLocal) {
            saveLanguageFile(languageFile, false);
        } else if (e.getSource() == mnuSave) {
            saveLanguageFile(languageFile, true);
        } else if (e.getSource() == mnuCurrent) {
            saveLanguageFile(languageFile, false);
            startNewInstance(new String[] { "-n", "-lng", languageFile.getAbsolutePath() });
            UserIO.getInstance().requestMessageDialog("Started JDownloader using " + languageFile);
        } else if (e.getSource() == mnuKeymode) {

            startNewInstance(new String[] { "-n", "-trdebug" });
            UserIO.getInstance().requestMessageDialog("Started JDownloader in KEY DEBUG Mode");
        } else if (e.getSource() == mnuAdd) {

            String[] result = UserIO.getInstance().requestTwoTextFieldDialog(JDL.L(LOCALE_PREFIX + "addKey.title", "Add new key"), JDL.L(LOCALE_PREFIX + "addKey.message1", "Type in the name of the key:"), "", JDL.L(LOCALE_PREFIX + "addKey.message2", "Type in the translated message of the key:"), "");
            if (result == null || result[0].equals("")) return;
            result[0] = result[0].toLowerCase();
            for (KeyInfo ki : data) {
                if (ki.getKey().equals(result[0])) {
                    UserIO.getInstance().requestMessageDialog(JDL.LF(LOCALE_PREFIX + "addKey.error.message", "The key '%s' is already in use!", result[0]));
                    return;
                }
            }
            data.add(new KeyInfo(result[0].toLowerCase(), null, result[1], languageENKeysFormFile.get(result[0].toLowerCase())));
            tableModel.refreshModel();
            tableModel.fireTableDataChanged();
            updateKeyChart();

        } else if (e.getSource() == mnuDelete || e.getSource() == mnuContextDelete) {

            deleteSelectedKeys();

        } else if (e.getSource() == mnuAdopt || e.getSource() == mnuContextAdopt) {

            for (int row : getSelectedRows()) {
                data.get(row).setLanguage(data.get(row).getSource());
            }
            this.dataChanged();

        } else if (e.getSource() == mnuClear || e.getSource() == mnuContextClear) {

            for (int row : getSelectedRows()) {
                data.get(row).setLanguage("");
            }
            this.dataChanged();

        } else if (e.getSource() == mnuOpenSearchDialog) {

            SearchFactory.getInstance().showFindInput(table, table.getSearchable());

        } else if (e.getSource() == mnuDeleteOld) {

            if (numOld > 0 && JDFlags.hasAllFlags(UserIO.getInstance().requestConfirmDialog(UserIO.NO_COUNTDOWN | UserIO.DONT_SHOW_AGAIN | UserIO.DONT_SHOW_AGAIN_IGNORES_CANCEL, JDL.L(LOCALE_PREFIX + "deleteOld.title", "Delete Old Key(s)?"), JDL.LF(LOCALE_PREFIX + "deleteOld.message", "Delete all %s old Key(s)?", numOld)), UserIO.RETURN_OK)) {
                deleteOldKeys();
            }

        }

    }

    private void deleteOldKeys() {
        for (int i = data.size() - 1; i >= 0; --i) {
            if (data.get(i).isOld()) data.remove(i);
        }
        this.dataChanged();
    }

    private void startNewInstance(String[] strings) {

        ArrayList<String> jargs = new ArrayList<String>();

        jargs.add("-Xmx512m");

        jargs.add("-jar");
        jargs.add("JDownloader.jar");
        for (String a : strings) {
            jargs.add(a);
        }

        JDLogger.getLogger().info(JDUtilities.runCommand("java", jargs.toArray(new String[] {}), JDUtilities.getResourceFile(".").getAbsolutePath(), 0));

    }

    public void saveChanges(boolean upload) {
        if (!changed) return;
        String message;
        if (upload) {
            message = JDL.LF(LOCALE_PREFIX + "saveChanges.message.upload", "Save and upload your changes to %s?", this.languageFile);
        } else {
            message = JDL.LF(LOCALE_PREFIX + "saveChanges.message", "Save your changes to %s?", this.languageFile);
        }
        int ret = UserIO.getInstance().requestConfirmDialog(UserIO.NO_COUNTDOWN, JDL.L(LOCALE_PREFIX + "saveChanges", "Save changes?"), message, null, JDL.L("gui.btn_yes", "Yes"), JDL.L("gui.btn_no", "No"));
        if (JDFlags.hasAllFlags(ret, UserIO.RETURN_OK)) {
            saveLanguageFile(languageFile, upload);
        }
    }

    private void updateSVN(boolean revert) {

        if (!dirLanguages.exists()) dirLanguages.mkdirs();
        if (!dirWorkingCopy.exists()) dirWorkingCopy.mkdirs();

        final ProgressController progress = new ProgressController(JDL.L(LOCALE_PREFIX + "svn.updating", "Updating SVN: Please wait"), "gui.splash.languages");
        progress.setIndeterminate(true);
        try {
            Subversion svn = null;
            Subversion svnLanguageDir;

            svn = new Subversion(SOURCE_SVN, subConfig.getStringProperty(PROPERTY_SVN_ACCESS_USER), subConfig.getStringProperty(PROPERTY_SVN_ACCESS_PASS));
            svnLanguageDir = new Subversion(LANGUAGE_SVN, subConfig.getStringProperty(PROPERTY_SVN_ACCESS_USER), subConfig.getStringProperty(PROPERTY_SVN_ACCESS_PASS));

            // HEAD = svn.latestRevision();
            svn.getBroadcaster().addListener(new MessageListener() {

                public void onMessage(MessageEvent event) {
                    progress.setStatusText(JDL.L(LOCALE_PREFIX + "svn.updating", "Updating SVN: Please wait") + ": " + event.getMessage().replace(dirWorkingCopy.getParentFile().getAbsolutePath(), ""));
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
                UserIO.getInstance().requestMessageDialog(JDL.L(LOCALE_PREFIX + "error.title", "Error occured"), JDL.LF(LOCALE_PREFIX + "error.updatesource.message", "Error while updating source:\r\n %s", JDLogger.getStackTrace(e)));
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
                UserIO.getInstance().requestMessageDialog(JDL.L(LOCALE_PREFIX + "error.title", "Error occured"), JDL.LF(LOCALE_PREFIX + "error.updatelanguages.message", "Error while updating languages:\r\n %s", JDLogger.getStackTrace(e)));
            }
            svnLanguageDir.dispose();
            svn.dispose();
            progress.setStatusText(JDL.L(LOCALE_PREFIX + "svn.updating.ready", "Updating SVN: Complete"));
            progress.doFinalize(2 * 1000l);
        } catch (SVNException e) {
            JDLogger.exception(e);
            progress.setColor(Color.RED);
            progress.setStatusText(JDL.L(LOCALE_PREFIX + "svn.updating.error", "Updating SVN: Error!"));
            progress.doFinalize(5 * 1000l);
        }

    }

    private void deleteSelectedKeys() {
        int[] rows = getSelectedRows();
        Arrays.sort(rows);

        int len = rows.length - 1;
        for (int i = len; i >= 0; --i) {
            String temp = data.remove(rows[i]).getKey();
            data.remove(temp);
        }
        int newRow = Math.min(rows[len] - len, tableModel.getRowCount() - 1);
        table.getSelectionModel().setSelectionInterval(newRow, newRow);

        dataChanged();
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

    private void saveLanguageFile(File file, boolean upload) {
        StringBuilder sb = new StringBuilder();

        Collections.sort(data);

        if (numOld > 0 && JDFlags.hasAllFlags(UserIO.getInstance().requestConfirmDialog(UserIO.NO_COUNTDOWN | UserIO.DONT_SHOW_AGAIN | UserIO.DONT_SHOW_AGAIN_IGNORES_OK, JDL.L(LOCALE_PREFIX + "deleteOld.title", "Delete Old Key(s)?"), JDL.LF(LOCALE_PREFIX + "deleteOld.message2", "There are still %s old keys in the LanguageFile. Delete them before saving?", numOld)), UserIO.RETURN_OK)) {
            deleteOldKeys();
        }

        for (KeyInfo entry : data) {
            if (!entry.isMissing()) sb.append(entry.toString()).append('\n');
        }

        try {
            Subversion svn = new Subversion(LANGUAGE_SVN, subConfig.getStringProperty(PROPERTY_SVN_ACCESS_USER), subConfig.getStringProperty(PROPERTY_SVN_ACCESS_PASS));

            ArrayList<SVNInfo> info = svn.getInfo(file);
            if (info.get(0).getConflictWrkFile() != null) {
                svn.revert(file);
            }

            svn.dispose();
            BufferedWriter out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), "UTF8"));
            out.write(sb.toString());
            out.close();
            if (upload) {
                String message = UserIO.getInstance().requestInputDialog(0, "Enter change description", "Please enter a short description for your changes (in english).", "", null, null, null);
                if (message == null) message = "Updated language file (" + lngKey + ")";
                if (!commit(file, message, null)) {
                    UserIO.getInstance().requestMessageDialog("Could not upload changes. Please send the file " + file.getAbsolutePath() + " to support@jdownloader.org");
                    return;
                }
            }
        } catch (Exception e) {
            UserIO.getInstance().requestMessageDialog(JDL.LF(LOCALE_PREFIX + "save.error.message", "An error occured while writing the LanguageFile:\n%s", e.getMessage()));
            return;
        }

        changed = false;
        if (upload) {
            UserIO.getInstance().requestMessageDialog(JDL.L(LOCALE_PREFIX + "save.success.message", "LanguageFile saved successfully!"));
        }
        initLocaleData();
    }

    private void initLocaleData() {

        parseLanguageFile(languageFile, languageKeysFormFile);
        parseLanguageFile(new File(dirLanguages, "en.loc"), languageENKeysFormFile);
        data.clear();
        if (languageFile != null) {
            lngKey = languageFile.getName().substring(0, languageFile.getName().length() - 4);
            lngKey = JDGeoCode.parseLanguageCode(lngKey)[0];
        }
        String value, key;
        KeyInfo keyInfo;
        for (LngEntry entry : sourceParser.getEntries()) {
            key = entry.getKey();
            keyInfo = new KeyInfo(key, entry.getValue(), languageKeysFormFile.remove(key), languageENKeysFormFile.get(key));
            data.add(keyInfo);
        }

        for (Entry<String, String> entry : languageKeysFormFile.entrySet()) {
            key = entry.getKey();
            value = null;

            for (String patt : sourceParser.getPattern()) {
                if (key.matches(patt)) {
                    value = "<pattern> " + patt;
                }
            }

            data.add(new KeyInfo(key, value, entry.getValue(), languageENKeysFormFile.get(key)));
        }

        Collections.sort(data);

        tableModel.refreshModel();
        tableModel.fireTableDataChanged();
        changed = false;

        SwingUtilities.invokeLater(new Runnable() {

            public void run() {
                updateKeyChart();
                mnuKey.setEnabled(true);
                mnuCurrent.setEnabled(true);
                mnuSave.setEnabled(true);
                mnuSaveLocal.setEnabled(true);
            }

        });

    }

    private void getSourceEntries() {
        ProgressController progress = new ProgressController(JDL.L(LOCALE_PREFIX + "analyzingSource1", "Analyzing Source Folder"), "gui.splash.languages");
        progress.setIndeterminate(true);

        sourceParser = new SrcParser(this.dirWorkingCopy);
        sourceParser.getBroadcaster().addListener(progress);
        sourceParser.parse();

        JDLogger.getLogger().warning("Patternmatches are not recommened: \r\n" + sourceParser.getPattern());

        progress.setStatusText(JDL.L(LOCALE_PREFIX + "analyzingSource.ready", "Analyzing Source Folder: Complete"));
        progress.doFinalize(2 * 1000l);
    }

    private boolean commit(File file, String string, Subversion svn) {
        try {

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
                                            sb.append(key).append(" = ").append(value).append("\r\n");
                                            keys.add(key);
                                            break;
                                        } else {

                                            String newValue = selectVersion(key, value, tvalue);
                                            sb.append(key).append(" = ").append(newValue).append("\r\n");
                                            keys.add(key);
                                            break;
                                        }

                                    }
                                }

                            }

                            if (!found) {
                                String newValue = selectVersion(key, value, MISSING_KEY);
                                if (newValue == MISSING_KEY) continue;
                                sb.append(key).append(" = ").append(value).append("\r\n");
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

            svn.dispose();
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

    @Override
    public void onShow() {
    }

    @Override
    public void onHide() {
        try {
            updater.interrupt();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void initMenu(JMenuBar menubar) {
        this.menubar = menubar;
        // Load Menü
        mnuLoad = new JMenu(JDL.L(LOCALE_PREFIX + "load", "Load Language"));

        populateLngMenu();

        // File Menü
        mnuFile = new JMenu(JDL.L(LOCALE_PREFIX + "file", "File"));

        mnuFile.add(mnuLoad);
        mnuFile.addSeparator();
        mnuFile.add(mnuSaveLocal = new JMenuItem(JDL.L(LOCALE_PREFIX + "savelocale", "Save Offline")));
        mnuFile.setEnabled(false);
        mnuFile.add(mnuSave = new JMenuItem(JDL.L(LOCALE_PREFIX + "saveandupload", "Save & Upload")));
        mnuFile.addSeparator();
        mnuFile.add(mnuReload = new JMenuItem(JDL.L(LOCALE_PREFIX + "reload", "Revert/Reload")));
        mnuFile.add(mnuCompleteReload = new JMenuItem(JDL.L(LOCALE_PREFIX + "completeReload", "Complete Reload (Deletes Cache)")));

        mnuSaveLocal.addActionListener(this);
        mnuSave.addActionListener(this);
        mnuReload.addActionListener(this);
        mnuCompleteReload.addActionListener(this);

        mnuSaveLocal.setEnabled(false);
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
        mnuKey.addSeparator();
        mnuKey.add(mnuDeleteOld = new JMenuItem(JDL.L(LOCALE_PREFIX + "deleteOldKeys", "Delete Old Key(s)")));
        mnuKey.addSeparator();
        mnuKey.add(mnuOpenSearchDialog = new JMenuItem(JDL.L(LOCALE_PREFIX + "openSearchDialog", "Open Search Dialog")));

        mnuAdd.addActionListener(this);
        mnuDelete.addActionListener(this);
        mnuClear.addActionListener(this);
        mnuAdopt.addActionListener(this);
        mnuDeleteOld.addActionListener(this);
        mnuOpenSearchDialog.addActionListener(this);

        mnuDelete.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0));
        mnuOpenSearchDialog.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F, KeyEvent.CTRL_DOWN_MASK));

        // Test
        mnuTest = new JMenu(JDL.L(LOCALE_PREFIX + "test", "Test"));

        mnuTest.add(mnuCurrent = new JMenuItem(JDL.L(LOCALE_PREFIX + "startcurrent", "Test JD with current translation")));
        mnuCurrent.addActionListener(this);
        mnuTest.add(mnuKeymode = new JMenuItem(JDL.L(LOCALE_PREFIX + "startkey", "Test JD in Key mode")));
        mnuKeymode.addActionListener(this);
        mnuCurrent.setEnabled(false);

        // Menü-Bar zusammensetzen
        menubar.add(mnuFile);
        menubar.add(mnuKey);
        menubar.add(mnuTest);
        menubar.setEnabled(false);

    }

    public void dataChanged() {
        tableModel.refreshModel();
        tableModel.fireTableDataChanged();
        updateKeyChart();
        changed = true;
    }

}
