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

package org.jdownloader.extensions.langfileeditor;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
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

import jd.controlling.JDLogger;
import jd.controlling.JSonWrapper;
import jd.controlling.ProgressController;
import jd.event.MessageEvent;
import jd.event.MessageListener;
import jd.gui.UserIO;
import jd.gui.swing.GuiRunnable;
import jd.gui.swing.dialog.TwoTextFieldDialog;
import jd.gui.swing.jdgui.interfaces.SwitchPanel;
import jd.nutils.JDFlags;
import jd.nutils.svn.ResolveHandler;
import jd.nutils.svn.Subversion;
import jd.utils.JDGeoCode;
import jd.utils.JDUtilities;
import net.miginfocom.swing.MigLayout;

import org.appwork.utils.Regex;
import org.appwork.utils.os.CrossSystem;
import org.appwork.utils.swing.dialog.Dialog;
import org.appwork.utils.swing.dialog.DialogCanceledException;
import org.appwork.utils.swing.dialog.DialogClosedException;
import org.appwork.utils.swing.table.ExtRowHighlighter;
import org.appwork.utils.swing.table.ExtTable;
import org.appwork.utils.swing.table.SelectionHighlighter;
import org.jdownloader.extensions.langfileeditor.translate.T;
import org.tmatesoft.svn.core.wc.SVNInfo;

public class LFEGui extends SwitchPanel implements ActionListener {

    private static final long             serialVersionUID         = -143452893912428555L;

    public static final String            SOURCE_SVN               = "svn://svn.jdownloader.org/jdownloader/trunk/src";

    public static final String            LANGUAGE_SVN             = "svn://svn.jdownloader.org/jdownloader/trunk/ressourcen/jd/languages";

    private final JSonWrapper             subConfig;

    public final static String            PROPERTY_SVN_ACCESS_USER = "PROPERTY_SVN_CHECKOUT_USER";
    public final static String            PROPERTY_SVN_ACCESS_PASS = "PROPERTY_SVN_CHECKOUT_PASS";

    private static final String           MISSING_KEY              = "~MISSING KEY/REMOVED~";

    private final LFEInfoPanel            infoPanel;
    private LFETableModel                 tableModel;
    private ExtTable<KeyInfo>             table;
    private File                          languageFile;

    private JMenuBar                      menubar;
    private JMenu                         mnuFile, mnuLoad, mnuKey, mnuTest;
    private JMenuItem                     mnuSave, mnuSaveLocal, mnuReload, mnuCompleteReload;
    private JMenuItem                     mnuAdd, mnuAdopt, mnuClear, mnuDelete, mnuDeleteOld;
    private JMenuItem                     mnuCurrent, mnuKeymode;
    private JPopupMenu                    mnuContextPopup;
    private JMenuItem                     mnuContextAdopt, mnuContextClear, mnuContextDelete;

    private final HashMap<String, String> languageKeysFromFile     = new HashMap<String, String>();
    private final HashMap<String, String> languageENKeysFromFile   = new HashMap<String, String>();
    private final ArrayList<KeyInfo>      data                     = new ArrayList<KeyInfo>();
    private String                        lngKey                   = null;
    private boolean                       changed                  = false;
    private int                           numOld                   = 0;
    private final File                    dirLanguages, dirWorkingCopy;

    public static final Color             COLOR_DONE               = new Color(204, 255, 170, 50);
    public static final Color             COLOR_MISSING            = new Color(221, 34, 34, 50);
    public static final Color             COLOR_OLD                = new Color(255, 200, 0, 50);

    private static final Color            COLOR_SELECTED_ROW       = new Color(200, 200, 200, 80);

    private SrcParser                     sourceParser;

    private Thread                        updater;

    private JButton                       warning;

    private final LangFileEditorExtension plugin;

    public LFEGui(final LangFileEditorExtension plugin) {
        this.plugin = plugin;
        this.subConfig = plugin.getPluginConfig();
        this.infoPanel = LFEInfoPanel.getInstance();
        this.setName(T._.plugins_optional_langfileeditor_title());
        this.dirLanguages = JDUtilities.getResourceFile("tmp/lfe/lng/");
        this.dirWorkingCopy = JDUtilities.getResourceFile("tmp/lfe/src/");
        this.dirLanguages.mkdirs();
        this.dirWorkingCopy.mkdirs();
        this.initGui();
    }

    public void actionPerformed(final ActionEvent e) {
        if (e.getSource() == this.mnuCompleteReload || e.getSource() == this.mnuReload) {
            this.saveChanges(false);

            new Thread(new Runnable() {
                public void run() {
                    SwingUtilities.invokeLater(new Runnable() {

                        public void run() {
                            LFEGui.this.mnuKey.setEnabled(false);
                            LFEGui.this.mnuCurrent.setEnabled(false);
                            LFEGui.this.mnuSave.setEnabled(false);
                            LFEGui.this.mnuSaveLocal.setEnabled(false);
                        }

                    });
                    if (e.getSource() == LFEGui.this.mnuCompleteReload) {
                        SrcParser.deleteCache();
                    }
                    LFEGui.this.getSourceEntries();
                    LFEGui.this.updateSVN(true);
                    LFEGui.this.populateLngMenu();
                    LFEGui.this.initLocaleData();
                }
            }).start();
        } else if (e.getSource() == this.mnuSaveLocal) {
            this.saveLanguageFile(this.languageFile, false);
        } else if (e.getSource() == this.mnuSave) {
            this.saveLanguageFile(this.languageFile, true);
        } else if (e.getSource() == this.mnuCurrent) {
            this.saveLanguageFile(this.languageFile, false);
            this.startNewInstance(new String[] { "-n", "-lng", this.languageFile.getAbsolutePath() });
            UserIO.getInstance().requestMessageDialog("Started JDownloader using " + this.languageFile);
        } else if (e.getSource() == this.mnuKeymode) {

            this.startNewInstance(new String[] { "-n", "-trdebug" });
            UserIO.getInstance().requestMessageDialog("Started JDownloader in KEY DEBUG Mode");
        } else if (e.getSource() == this.mnuAdd) {

            String[] result;
            try {
                result = Dialog.getInstance().showDialog(new TwoTextFieldDialog(T._.plugins_optional_langfileeditor_addKey_title(), T._.plugins_optional_langfileeditor_addKey_message1(), "", T._.plugins_optional_langfileeditor_addKey_message2(), ""));
                if (result == null || result[0].equals("")) { return; }
                result[0] = result[0].toLowerCase();
                for (final KeyInfo ki : this.data) {
                    if (ki.getKey().equals(result[0])) {
                        UserIO.getInstance().requestMessageDialog(T._.plugins_optional_langfileeditor_addKey_error_message(result[0]));
                        return;
                    }
                }
                this.data.add(new KeyInfo(result[0].toLowerCase(), null, result[1], this.languageENKeysFromFile.get(result[0].toLowerCase())));
                this.tableModel.refreshData();
                this.updateKeyChart();
            } catch (DialogClosedException e1) {
                e1.printStackTrace();
            } catch (DialogCanceledException e1) {
                e1.printStackTrace();
            }

        } else if (e.getSource() == this.mnuDelete || e.getSource() == this.mnuContextDelete) {

            this.deleteSelectedKeys();

        } else if (e.getSource() == this.mnuAdopt || e.getSource() == this.mnuContextAdopt) {

            for (final KeyInfo ki : this.getSelectedRows()) {
                ki.setLanguage(ki.getSource());
            }
            this.dataChanged();

        } else if (e.getSource() == this.mnuClear || e.getSource() == this.mnuContextClear) {

            for (final KeyInfo ki : this.getSelectedRows()) {
                ki.setLanguage("");
            }
            this.dataChanged();

        } else if (e.getSource() == this.mnuDeleteOld) {

            if (this.numOld > 0 && JDFlags.hasAllFlags(UserIO.getInstance().requestConfirmDialog(UserIO.NO_COUNTDOWN | UserIO.DONT_SHOW_AGAIN | UserIO.DONT_SHOW_AGAIN_IGNORES_CANCEL, T._.plugins_optional_langfileeditor_deleteOld_title(), T._.plugins_optional_langfileeditor_deleteOld_message(this.numOld)), UserIO.RETURN_OK)) {
                this.deleteOldKeys();
            }

        }

    }

    private void buildContextMenu() {
        // Context Menu
        this.mnuContextPopup = new JPopupMenu();

        this.mnuContextPopup.add(this.mnuContextDelete = new JMenuItem(T._.plugins_optional_langfileeditor_deleteKeys()));
        this.mnuContextPopup.add(this.mnuContextClear = new JMenuItem(T._.plugins_optional_langfileeditor_clearValues()));
        this.mnuContextPopup.addSeparator();
        this.mnuContextPopup.add(this.mnuContextAdopt = new JMenuItem(T._.plugins_optional_langfileeditor_adoptDefaults()));

        this.mnuContextDelete.addActionListener(this);
        this.mnuContextClear.addActionListener(this);
        this.mnuContextAdopt.addActionListener(this);
    }

    private boolean commit(final File file, final String string, Subversion svn) {
        try {

            if (svn == null) {
                svn = new Subversion(LFEGui.LANGUAGE_SVN, this.subConfig.getStringProperty(LFEGui.PROPERTY_SVN_ACCESS_USER), this.subConfig.getStringProperty(LFEGui.PROPERTY_SVN_ACCESS_PASS));
            }
            svn.update(file, null);
            try {
                svn.resolveConflicts(file, new ResolveHandler() {

                    public String resolveConflict(final SVNInfo info, final File file, final String contents, final int startMine, final int endMine, final int startTheirs, final int endTheirs) {
                        final String[] mine = Regex.getLines(contents.substring(startMine, endMine).trim());
                        final String[] theirs = Regex.getLines(contents.substring(startTheirs, endTheirs).trim());
                        final StringBuilder sb = new StringBuilder();
                        final ArrayList<String> keys = new ArrayList<String>();
                        for (final String m : mine) {
                            final int index = m.indexOf("=");
                            final String key = m.substring(0, index).trim();
                            final String value = m.substring(index + 1).trim();
                            boolean found = false;

                            for (final String t : theirs) {

                                if (t.startsWith(key)) {
                                    final int tindex = t.indexOf("=");
                                    final String tkey = t.substring(0, tindex).trim();
                                    final String tvalue = t.substring(tindex + 1).trim();
                                    if (key.equalsIgnoreCase(tkey)) {
                                        found = true;
                                        if (value.equals(tvalue)) {
                                            sb.append(key).append(" = ").append(value).append("\r\n");
                                            keys.add(key);
                                            break;
                                        } else {

                                            final String newValue = this.selectVersion(key, value, tvalue);
                                            sb.append(key).append(" = ").append(newValue).append("\r\n");
                                            keys.add(key);
                                            break;
                                        }

                                    }
                                }

                            }

                            if (!found) {
                                final String newValue = this.selectVersion(key, value, LFEGui.MISSING_KEY);
                                if (newValue == LFEGui.MISSING_KEY) {
                                    continue;
                                }
                                sb.append(key).append(" = ").append(value).append("\r\n");
                                keys.add(key);
                                continue;
                            }

                        }

                        for (final String t : theirs) {
                            final int tindex = t.indexOf("=");
                            if (tindex < 0) {
                                continue;
                            }
                            final String tkey = t.substring(0, tindex).trim();
                            final String tvalue = t.substring(tindex + 1).trim();
                            if (!keys.contains(tkey)) {
                                final String newValue = this.selectVersion(tkey, LFEGui.MISSING_KEY, tvalue);
                                if (newValue == LFEGui.MISSING_KEY) {
                                    continue;
                                }

                                sb.append(tkey + " = " + tvalue + "\r\n");
                                keys.add(tkey);
                            }

                        }

                        return sb.toString().trim();
                    }

                    private String selectVersion(final String key, final String value, final String tvalue) {
                        final String html = "<h1>Key: " + key + "</h1><h2>Translation A</h2>" + value + "<h2>Translation B</h2>" + tvalue + "<br><br>Select the better translation. A or B:";
                        final int ret = UserIO.getInstance().requestConfirmDialog(UserIO.STYLE_HTML | UserIO.STYLE_LARGE | UserIO.NO_COUNTDOWN, "Conflicts occured!", html, null, "A", "B");
                        if (JDFlags.hasAllFlags(ret, UserIO.RETURN_CANCEL)) { return tvalue; }
                        return value;

                    }

                });
            } catch (final Exception e) {
                e.printStackTrace();
            }
            svn.commit(file, string);

            svn.dispose();
            return true;
        } catch (final Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public void dataChanged() {
        this.tableModel.refreshData();
        this.updateKeyChart();
        this.changed = true;
    }

    private void deleteOldKeys() {
        for (int i = this.data.size() - 1; i >= 0; --i) {
            if (this.data.get(i).isOld()) {
                this.data.remove(i);
            }
        }
        this.dataChanged();
    }

    private void deleteSelectedKeys() {
        final int sel = this.table.getSelectedRow();

        this.data.removeAll(getSelectedRows());

        final int newRow = Math.min(sel, this.tableModel.getRowCount() - 1);
        this.table.getSelectionModel().setSelectionInterval(newRow, newRow);

        this.dataChanged();
    }

    public ArrayList<KeyInfo> getData() {
        return this.data;
    }

    private ArrayList<KeyInfo> getSelectedRows() {
        return this.table.getExtTableModel().getSelectedObjects();
    }

    private void getSourceEntries() {
        final ProgressController progress = new ProgressController(T._.plugins_optional_langfileeditor_analyzingSource1(), "gui.splash.languages");
        progress.setIndeterminate(true);

        this.sourceParser = new SrcParser(this.dirWorkingCopy);
        this.sourceParser.getBroadcaster().addListener(progress);
        this.sourceParser.parse();

        JDLogger.getLogger().warning("Patternmatches are not recommened: \r\n" + this.sourceParser.getPattern());

        progress.setStatusText(T._.plugins_optional_langfileeditor_analyzingSource_ready());
        progress.doFinalize(2 * 1000l);
    }

    private void initGui() {
        this.tableModel = new LFETableModel(this);
        this.table = new ExtTable<KeyInfo>(this.tableModel, "lfe") {

            private static final long serialVersionUID = 7054804074534585633L;

            @Override
            protected JPopupMenu onContextMenu(final JPopupMenu popup, final KeyInfo contextObject, final ArrayList<KeyInfo> selection) {
                if (LFEGui.this.mnuContextPopup == null) {
                    LFEGui.this.buildContextMenu();
                }
                return LFEGui.this.mnuContextPopup;
            }

        };
        this.table.setSearchEnabled(true);
        this.table.setEnabled(false);
        this.table.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        this.table.addRowHighlighter(new ExtRowHighlighter(null, LFEGui.COLOR_MISSING) {

            @Override
            public boolean doHighlight(final ExtTable<?> extTable, final int row) {
                return ((KeyInfo) extTable.getExtTableModel().getValueAt(row, 0)).isMissing();
            }

        });
        this.table.addRowHighlighter(new ExtRowHighlighter(null, LFEGui.COLOR_OLD) {

            @Override
            public boolean doHighlight(final ExtTable<?> extTable, final int row) {
                return ((KeyInfo) extTable.getExtTableModel().getValueAt(row, 0)).isOld();
            }

        });
        this.table.addRowHighlighter(new SelectionHighlighter(null, LFEGui.COLOR_SELECTED_ROW));

        this.warning = new JButton(T._.plugins_optional_langfileeditor_account_warning());
        this.warning.setVisible(false);
        this.warning.addActionListener(new ActionListener() {

            public void actionPerformed(final ActionEvent e) {
                try {
                    CrossSystem.openURLOrShowMessage("http://jdownloader.org/knowledge/wiki/development/translation/translate-jdownloader");
                } catch (final Exception e1) {
                    e1.printStackTrace();
                    UserIO.getInstance().requestMessageDialog(T._.jd_plugins_optional_langfileeditor_LangFileEditor_btn_readmore(), "http://jdownloader.org/knowledge/wiki/development/translation/translate-jdownloader");
                }
            }

        });

        this.setLayout(new MigLayout("ins 0, wrap 1", "[grow, fill]", "[][grow, fill]"));
        this.add(this.warning, "grow, hidemode 2");
        this.add(new JScrollPane(this.table), "grow");

        this.updater = new Thread(new Runnable() {

            public void run() {
                boolean cfgRequested = false;
                while (true) {
                    while (LFEGui.this.subConfig.getStringProperty(LFEGui.PROPERTY_SVN_ACCESS_USER) == null || LFEGui.this.subConfig.getStringProperty(LFEGui.PROPERTY_SVN_ACCESS_USER).trim().length() == 0) {
                        if (!cfgRequested) {
                            // UserIF.getInstance().requestPanel(UserIF.Panels.CONFIGPANEL,
                            // LFEGui.this.plugin.getSettings());
                            throw new RuntimeException("TODO");
                        }
                        cfgRequested = true;
                        try {
                            new GuiRunnable<Object>() {

                                @Override
                                public Object runSave() {
                                    LFEGui.this.warning.setVisible(true);
                                    LFEGui.this.mnuFile.setEnabled(false);
                                    return null;
                                }

                            }.start();
                            Thread.sleep(500);
                        } catch (final InterruptedException e) {
                            return;
                        }
                    }

                    if (Subversion.checkLogin(LFEGui.SOURCE_SVN, LFEGui.this.subConfig.getStringProperty(LFEGui.PROPERTY_SVN_ACCESS_USER), LFEGui.this.subConfig.getStringProperty(LFEGui.PROPERTY_SVN_ACCESS_PASS))) {
                        break;
                    } else {
                        UserIO.getInstance().requestMessageDialog(T._.jd_plugins_optional_langfileeditor_langfileeditor_badlogins());
                        LFEGui.this.subConfig.setProperty(LFEGui.PROPERTY_SVN_ACCESS_USER, null);
                        LFEGui.this.subConfig.setProperty(LFEGui.PROPERTY_SVN_ACCESS_PASS, null);
                        LFEGui.this.subConfig.save();
                    }
                }
                new GuiRunnable<Object>() {

                    @Override
                    public Object runSave() {
                        LFEGui.this.warning.setVisible(false);
                        return null;
                    }

                }.start();
                LFEGui.this.setEnabled(false);

                LFEGui.this.updateSVN(false);

                LFEGui.this.getSourceEntries();
                LFEGui.this.populateLngMenu();
                LFEGui.this.setEnabled(true);
                if (LFEGui.this.menubar != null) {
                    new GuiRunnable<Object>() {
                        @Override
                        public Object runSave() {
                            LFEGui.this.menubar.setEnabled(true);
                            return null;
                        }
                    }.start();
                }
                new GuiRunnable<Object>() {
                    @Override
                    public Object runSave() {
                        LFEGui.this.mnuFile.setEnabled(true);
                        return null;
                    }
                }.start();
            }
        });
        this.updater.start();
    }

    private void initLocaleData() {
        parseLanguageFile(this.languageFile, this.languageKeysFromFile);
        parseLanguageFile(new File(this.dirLanguages, "en.loc"), this.languageENKeysFromFile);

        this.data.clear();
        if (this.languageFile != null) {
            this.lngKey = this.languageFile.getName().substring(0, this.languageFile.getName().length() - 4);
            this.lngKey = JDGeoCode.parseLanguageCode(this.lngKey)[0];
        }

        String value, key;
        KeyInfo keyInfo;
        for (final LngEntry entry : this.sourceParser.getEntries()) {
            key = entry.getKey();
            keyInfo = new KeyInfo(key, entry.getValue(), this.languageKeysFromFile.remove(key), this.languageENKeysFromFile.get(key));
            this.data.add(keyInfo);
        }

        for (final Entry<String, String> entry : this.languageKeysFromFile.entrySet()) {
            key = entry.getKey();
            value = null;

            for (final String patt : this.sourceParser.getPattern()) {
                if (key.matches(patt)) {
                    value = "<pattern> " + patt;
                }
            }

            this.data.add(new KeyInfo(key, value, entry.getValue(), this.languageENKeysFromFile.get(key)));
        }

        Collections.sort(this.data);

        this.tableModel.refreshData();
        this.changed = false;

        SwingUtilities.invokeLater(new Runnable() {

            public void run() {
                LFEGui.this.updateKeyChart();
                LFEGui.this.mnuKey.setEnabled(true);
                LFEGui.this.mnuCurrent.setEnabled(true);
                LFEGui.this.mnuSave.setEnabled(true);
                LFEGui.this.mnuSaveLocal.setEnabled(true);
            }

        });

    }

    public void initMenu(final JMenuBar menubar) {
        this.menubar = menubar;
        // Load Menu
        this.mnuLoad = new JMenu(T._.plugins_optional_langfileeditor_load());

        this.populateLngMenu();

        // File Menu
        this.mnuFile = new JMenu(T._.plugins_optional_langfileeditor_file());

        this.mnuFile.add(this.mnuLoad);
        this.mnuFile.addSeparator();
        this.mnuFile.add(this.mnuSaveLocal = new JMenuItem(T._.plugins_optional_langfileeditor_savelocale()));
        this.mnuFile.setEnabled(false);
        this.mnuFile.add(this.mnuSave = new JMenuItem(T._.plugins_optional_langfileeditor_saveandupload()));
        this.mnuFile.addSeparator();
        this.mnuFile.add(this.mnuReload = new JMenuItem(T._.plugins_optional_langfileeditor_reload()));
        this.mnuFile.add(this.mnuCompleteReload = new JMenuItem(T._.plugins_optional_langfileeditor_completeReload()));

        this.mnuSaveLocal.addActionListener(this);
        this.mnuSave.addActionListener(this);
        this.mnuReload.addActionListener(this);
        this.mnuCompleteReload.addActionListener(this);

        this.mnuSaveLocal.setEnabled(false);
        this.mnuSave.setEnabled(false);

        this.mnuSave.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_DOWN_MASK));

        // Key Menu
        this.mnuKey = new JMenu(T._.plugins_optional_langfileeditor_key());
        this.mnuKey.setEnabled(false);

        this.mnuKey.add(this.mnuAdd = new JMenuItem(T._.plugins_optional_langfileeditor_addKey()));
        this.mnuKey.add(this.mnuDelete = new JMenuItem(T._.plugins_optional_langfileeditor_deleteKeys()));
        this.mnuKey.add(this.mnuClear = new JMenuItem(T._.plugins_optional_langfileeditor_clearValues()));
        this.mnuKey.addSeparator();
        this.mnuKey.add(this.mnuAdopt = new JMenuItem(T._.plugins_optional_langfileeditor_adoptDefaults()));
        this.mnuKey.addSeparator();
        this.mnuKey.add(this.mnuDeleteOld = new JMenuItem(T._.plugins_optional_langfileeditor_deleteOldKeys()));

        this.mnuAdd.addActionListener(this);
        this.mnuDelete.addActionListener(this);
        this.mnuClear.addActionListener(this);
        this.mnuAdopt.addActionListener(this);
        this.mnuDeleteOld.addActionListener(this);

        this.mnuDelete.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0));

        // Test
        this.mnuTest = new JMenu(T._.plugins_optional_langfileeditor_test());

        this.mnuTest.add(this.mnuCurrent = new JMenuItem(T._.plugins_optional_langfileeditor_startcurrent()));
        this.mnuCurrent.addActionListener(this);
        this.mnuTest.add(this.mnuKeymode = new JMenuItem(T._.plugins_optional_langfileeditor_startkey()));
        this.mnuKeymode.addActionListener(this);
        this.mnuCurrent.setEnabled(false);

        // Menu-Bar zusammensetzen
        menubar.add(this.mnuFile);
        menubar.add(this.mnuKey);
        menubar.add(this.mnuTest);
        menubar.setEnabled(false);
    }

    @Override
    public void onHide() {
        try {
            this.updater.interrupt();
        } catch (final Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onShow() {
    }

    public static void parseLanguageFile(final File file, final HashMap<String, String> data) {
        data.clear();

        if (file == null || !file.exists()) {
            System.out.println("JDLocale: " + file + " not found");
            return;
        }

        try {
            final BufferedReader f = new BufferedReader(new InputStreamReader(new FileInputStream(file), "UTF8"));

            String line;
            String key;
            String value;
            while ((line = f.readLine()) != null) {
                if (line.startsWith("#")) {
                    continue;
                }
                final int split = line.indexOf("=");
                if (split <= 0) {
                    continue;
                }

                key = line.substring(0, split).trim().toLowerCase();
                value = line.substring(split + 1).trim() + (line.endsWith(" ") ? " " : "");

                data.put(key, value);
            }
            f.close();
        } catch (final IOException e) {
            JDLogger.exception(e);
        }
    }

    private void populateLngMenu() {
        new GuiRunnable<Object>() {

            @Override
            public Object runSave() {
                LFEGui.this.mnuLoad.removeAll();
                for (final File f : LFEGui.this.dirLanguages.listFiles()) {
                    if (f.getName().endsWith(".loc")) {
                        try {
                            final String language = JDGeoCode.toLonger(f.getName().substring(0, f.getName().length() - 4));
                            if (language != null) {
                                final JMenuItem mi = new JMenuItem(language);
                                mi.addActionListener(new ActionListener() {

                                    public void actionPerformed(final ActionEvent e) {
                                        if (LFEGui.this.languageFile != null) {
                                            LFEGui.this.saveChanges(true);
                                        }
                                        LFEGui.this.languageFile = new File(LFEGui.this.dirLanguages, JDGeoCode.longToShort(e.getActionCommand()) + ".loc");
                                        LFEGui.this.initLocaleData();
                                        LFEGui.this.table.setEnabled(true);
                                    }

                                });
                                LFEGui.this.mnuLoad.add(mi);
                            }
                        } catch (final Exception e) {
                            System.out.println(f);
                            e.printStackTrace();
                        }
                    }
                }
                return null;
            }

        }.waitForEDT();

    }

    public void saveChanges(final boolean upload) {
        if (!this.changed) { return; }
        String message;
        if (upload) {
            message = T._.plugins_optional_langfileeditor_saveChanges_message_upload(this.languageFile);
        } else {
            message = T._.plugins_optional_langfileeditor_saveChanges_message(this.languageFile);
        }
        final int ret = UserIO.getInstance().requestConfirmDialog(UserIO.NO_COUNTDOWN, T._.plugins_optional_langfileeditor_saveChanges(), message, null, T._.gui_btn_yes(), T._.gui_btn_no());
        if (JDFlags.hasAllFlags(ret, UserIO.RETURN_OK)) {
            this.saveLanguageFile(this.languageFile, upload);
        }
    }

    private void saveLanguageFile(final File file, final boolean upload) {
        final StringBuilder sb = new StringBuilder();

        Collections.sort(this.data);

        if (this.numOld > 0 && JDFlags.hasAllFlags(UserIO.getInstance().requestConfirmDialog(UserIO.NO_COUNTDOWN | UserIO.DONT_SHOW_AGAIN | UserIO.DONT_SHOW_AGAIN_IGNORES_OK, T._.plugins_optional_langfileeditor_deleteOld_title(), T._.plugins_optional_langfileeditor_deleteOld_message2(this.numOld)), UserIO.RETURN_OK)) {
            this.deleteOldKeys();
        }

        for (final KeyInfo entry : this.data) {
            if (!entry.isMissing()) {
                sb.append(entry.toString()).append('\n');
            }
        }

        try {
            final Subversion svn = new Subversion(LFEGui.LANGUAGE_SVN, this.subConfig.getStringProperty(LFEGui.PROPERTY_SVN_ACCESS_USER), this.subConfig.getStringProperty(LFEGui.PROPERTY_SVN_ACCESS_PASS));

            final ArrayList<SVNInfo> info = svn.getInfo(file);
            if (info.get(0).getConflictWrkFile() != null) {
                svn.revert(file);
            }

            svn.dispose();
            final BufferedWriter out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), "UTF8"));
            out.write(sb.toString());
            out.close();
            if (upload) {
                String message = UserIO.getInstance().requestInputDialog(0, "Enter change description", "Please enter a short description for your changes (in english).", "", null, null, null);
                if ((message == null) || (message.trim() == "")) {
                    message = "Updated language file (" + this.lngKey + ")";
                }
                if (!this.commit(file, message, null)) {
                    UserIO.getInstance().requestMessageDialog("Could not upload changes. Please send the file " + file.getAbsolutePath() + " to support@jdownloader.org");
                    return;
                }
            }
        } catch (final Exception e) {
            UserIO.getInstance().requestMessageDialog(T._.plugins_optional_langfileeditor_save_error_message(e.getMessage()));
            return;
        }

        this.changed = false;
        if (upload) {
            UserIO.getInstance().requestMessageDialog(T._.plugins_optional_langfileeditor_save_success_message());
        }
        this.initLocaleData();
    }

    private void startNewInstance(final String[] strings) {

        final ArrayList<String> jargs = new ArrayList<String>();

        jargs.add("-Xmx512m");

        jargs.add("-jar");
        jargs.add("JDownloader.jar");
        for (final String a : strings) {
            jargs.add(a);
        }

        JDLogger.getLogger().info(JDUtilities.runCommand("java", jargs.toArray(new String[] {}), JDUtilities.getResourceFile(".").getAbsolutePath(), 0));

    }

    private void updateKeyChart() {
        int numMissing = 0;
        this.numOld = 0;

        for (final KeyInfo entry : this.data) {
            if (entry.isOld()) {
                this.numOld++;
            } else if (entry.isMissing()) {
                numMissing++;
            }
        }

        this.infoPanel.updateInfo(this.data.size() - numMissing - this.numOld, numMissing, this.numOld);
    }

    private void updateSVN(final boolean revert) {

        if (!this.dirLanguages.exists()) {
            this.dirLanguages.mkdirs();
        }
        if (!this.dirWorkingCopy.exists()) {
            this.dirWorkingCopy.mkdirs();
        }

        final ProgressController progress = new ProgressController(T._.plugins_optional_langfileeditor_svn_updating(), "gui.splash.languages");
        progress.setIndeterminate(true);
        try {
            Subversion svn = new Subversion(LFEGui.SOURCE_SVN, this.subConfig.getStringProperty(LFEGui.PROPERTY_SVN_ACCESS_USER), this.subConfig.getStringProperty(LFEGui.PROPERTY_SVN_ACCESS_PASS));
            Subversion svnLanguageDir = new Subversion(LFEGui.LANGUAGE_SVN, this.subConfig.getStringProperty(LFEGui.PROPERTY_SVN_ACCESS_USER), this.subConfig.getStringProperty(LFEGui.PROPERTY_SVN_ACCESS_PASS));

            // HEAD = svn.latestRevision();
            svn.getBroadcaster().addListener(new MessageListener() {

                public void onMessage(final MessageEvent event) {
                    progress.setStatusText(T._.plugins_optional_langfileeditor_svn_updating() + ": " + event.getMessage().replace(LFEGui.this.dirWorkingCopy.getParentFile().getAbsolutePath(), ""));
                }

            });
            try {
                svn.revert(this.dirWorkingCopy);
            } catch (final Exception e) {
                JDLogger.exception(e);
            }
            try {
                svn.update(this.dirWorkingCopy, null);
            } catch (final Exception e) {
                JDLogger.exception(e);
                UserIO.getInstance().requestMessageDialog(T._.plugins_optional_langfileeditor_error_title(), T._.plugins_optional_langfileeditor_error_updatesource_message(JDLogger.getStackTrace(e)));
            }
            if (revert) {
                try {
                    svnLanguageDir.revert(this.dirLanguages);
                } catch (final Exception e) {
                    JDLogger.exception(e);
                }
            }
            try {
                svnLanguageDir.update(this.dirLanguages, null);
            } catch (final Exception e) {
                JDLogger.exception(e);
                UserIO.getInstance().requestMessageDialog(T._.plugins_optional_langfileeditor_error_title(), T._.plugins_optional_langfileeditor_error_updatelanguages_message(JDLogger.getStackTrace(e)));
            }
            svnLanguageDir.dispose();
            svn.dispose();
            progress.setStatusText(T._.plugins_optional_langfileeditor_svn_updating_ready());
            progress.doFinalize(2 * 1000l);
        } catch (final Exception e) {
            JDLogger.exception(e);
            progress.setColor(Color.RED);
            progress.setStatusText(T._.plugins_optional_langfileeditor_svn_updating_error());
            progress.doFinalize(5 * 1000l);
        }

    }

}