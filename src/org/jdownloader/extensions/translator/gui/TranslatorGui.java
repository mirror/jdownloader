package org.jdownloader.extensions.translator.gui;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.lang.reflect.Type;
import java.util.EventObject;
import java.util.Locale;

import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.Box;
import javax.swing.Icon;
import javax.swing.InputMap;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JScrollPane;
import javax.swing.KeyStroke;
import javax.swing.ListCellRenderer;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.plaf.FontUIResource;
import javax.swing.text.BadLocationException;

import jd.gui.swing.jdgui.JDGui;
import jd.gui.swing.jdgui.interfaces.SwitchPanel;
import jd.gui.swing.laf.LookAndFeelController;
import jd.nutils.encoding.Encoding;
import jd.plugins.AddonPanel;
import net.miginfocom.swing.MigLayout;

import org.appwork.swing.MigPanel;
import org.appwork.swing.components.ExtButton;
import org.appwork.swing.components.ExtTextField;
import org.appwork.swing.components.TextComponentInterface;
import org.appwork.txtresource.TranslationFactory;
import org.appwork.update.inapp.RestartController;
import org.appwork.utils.StringUtils;
import org.appwork.utils.logging.Log;
import org.appwork.utils.swing.EDTHelper;
import org.appwork.utils.swing.EDTRunner;
import org.appwork.utils.swing.dialog.ComboBoxDialog;
import org.appwork.utils.swing.dialog.Dialog;
import org.appwork.utils.swing.dialog.DialogCanceledException;
import org.appwork.utils.swing.dialog.DialogClosedException;
import org.appwork.utils.swing.dialog.InputDialog;
import org.appwork.utils.swing.dialog.ProgressDialog;
import org.appwork.utils.swing.dialog.ProgressDialog.ProgressGetter;
import org.jdownloader.actions.AppAction;
import org.jdownloader.controlling.JDRestartController;
import org.jdownloader.extensions.translator.TLocale;
import org.jdownloader.extensions.translator.TranslateEntry;
import org.jdownloader.extensions.translator.TranslatorExtension;
import org.jdownloader.extensions.translator.TranslatorExtensionEvent;
import org.jdownloader.extensions.translator.TranslatorExtensionListener;
import org.jdownloader.extensions.translator.gui.actions.NewTranslationAction;
import org.jdownloader.gui.helpdialogs.HelpDialog;
import org.jdownloader.images.NewTheme;
import org.jdownloader.logging.LogController;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.wc.SVNCommitItem;
import org.tmatesoft.svn.core.wc.SVNCommitPacket;

/**
 * Extension gui
 * 
 * @author thomas
 * 
 */
public class TranslatorGui extends AddonPanel<TranslatorExtension> implements ListSelectionListener, TranslatorExtensionListener {

    private static final String ID = "TRANSLATORGUI";
    private TranslateTableModel tableModel;
    private TranslateTable      table;

    private SwitchPanel         panel;

    private MigPanel            menuPanel;
    private JLabel              lbl;
    private Timer               ti;
    private ExtButton           logout;
    private ExtButton           load;
    private ExtButton           save;
    private ExtButton           wizard;
    private ExtButton           revert;
    private ExtButton           restart;
    private JScrollPane         sp;
    private ExtButton           upload;
    private boolean             stopEditing;
    private boolean             isWizard;
    private MigPanel            menuPanel2;
    private QuickEdit           qe;
    private SearchField         search;

    public TranslatorGui(TranslatorExtension plg) {
        super(plg);

        this.panel = new SwitchPanel(new MigLayout("ins 0,wrap 1", "[grow,fill]", "[]2[]2[grow,fill][][grow,fill][]")) {

            @Override
            protected void onShow() {

            }

            @Override
            protected void onHide() {
            }
        };
        plg.getEventSender().addListener(this, true);
        // layout all contents in panel
        this.setContent(panel);
        initComponents();
        layoutPanel();

    }

    private void layoutPanel() {

        panel.add(menuPanel);
        panel.add(menuPanel2);
        sp = new JScrollPane(table);
        search = new SearchField(table);
        qe = new QuickEdit(table);

        panel.add(sp);
        panel.add(search);
        if (getExtension().getSettings().isQuickEditBarVisible() && getExtension().getSettings().getQuickEditHeight() > 24) {
            panel.add(qe, "height " + getExtension().getSettings().getQuickEditHeight() + "!,hidemode 3");
        }

    }

    private void initComponents() {
        layoutMenu();
        initTable();

    }

    protected void initTable() {
        tableModel = new TranslateTableModel(getExtension());
        if (table != null) table.getSelectionModel().removeListSelectionListener(this);
        table = new TranslateTable(getExtension(), tableModel) {

            @Override
            public boolean editCellAt(int row, int column) {
                if (stopEditing) {
                    if (isEditing() && table.getCellEditor() != null) getCellEditor().stopCellEditing();
                    return false;
                }
                if (super.editCellAt(row, column)) {
                    TranslateEntry value = tableModel.getObjectbyRow(row);
                    if (value.getDescription() != null) {
                        Dialog.I().showMessageDialog("IMPORTANT!!!\r\n\r\n" + value.getCategory() + "." + value.getKey() + "\r\n" + value.getDescription());
                    }
                    return true;
                } else {
                    return false;
                }
            }

            @Override
            public boolean editCellAt(int row, int column, EventObject e) {

                if (stopEditing) {
                    if (isEditing() && table.getCellEditor() != null) getCellEditor().stopCellEditing();
                    return false;
                }
                if (super.editCellAt(row, column, e)) {
                    TranslateEntry value = tableModel.getObjectbyRow(row);
                    if (value.getDescription() != null) {
                        Dialog.I().showMessageDialog("IMPORTANT!!!\r\n\r\n" + value.getCategory() + "." + value.getKey() + "\r\n" + value.getDescription());
                    }
                    return true;
                } else {
                    return false;
                }
            }

        };
        table.getSelectionModel().addListSelectionListener(this);
    }

    protected void layoutMenu() {
        // Load Menu

        // this.mnuFileLoad = new JMenu("Load");
        // for (TLocale t : getExtension().getTranslations()) {
        // mnuFileLoad.add(new LoadTranslationAction(this, t));
        // }
        // if (getExtension().getTranslations().size() > 0) mnuFileLoad.add(new
        // JSeparator());
        // mnuFileLoad.add();

        menuPanel = new MigPanel("ins 0", "[]3[]3[]3[]3[]3[]", "[grow,fill]");

        menuPanel.add(load = new ExtButton(new AppAction() {
            {
                setName("1. Load Translation");
            }

            @Override
            public void actionPerformed(ActionEvent e) {
                final ListCellRenderer org = new JComboBox().getRenderer();
                try {

                    TLocale pre = getExtension().getTLocaleByID(getExtension().getSettings().getLastLoaded());
                    if (pre == null) {
                        pre = new TLocale(TranslationFactory.getDesiredLocale().toString());
                    }
                    final ComboBoxDialog d = new ComboBoxDialog(0, "Choose Translation", "Please choose the Translation you want to modify, or create a new one", getExtension().getTranslations().toArray(new TLocale[] {}), pre == null ? 0 : getExtension().getTranslations().indexOf(pre), NewTheme.I().getIcon("language", 32), null, null, null);
                    d.setLeftActions(new NewTranslationAction(TranslatorGui.this) {

                        @Override
                        public void actionPerformed(ActionEvent e) {
                            super.actionPerformed(e);
                            d.dispose();

                            String variant = null;
                            String country = null;
                            try {
                                final String lng = (String) Dialog.getInstance().showComboDialog(0, "Choose Language ID", "Choose correct Language", Locale.getISOLanguages(), null, null, null, null, new ListCellRenderer() {

                                    @Override
                                    public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                                        return org.getListCellRendererComponent(list, new Locale((String) value).getDisplayLanguage(), index, isSelected, cellHasFocus);

                                    }

                                });
                                try {
                                    country = (String) Dialog.getInstance().showComboDialog(0, "Choose Country", "Choose correct Country", Locale.getISOCountries(), null, null, null, "No Special Country variant", new ListCellRenderer() {

                                        @Override
                                        public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                                            return org.getListCellRendererComponent(list, new Locale(lng, (String) value).getDisplayName(), index, isSelected, cellHasFocus);

                                        }

                                    });
                                } catch (DialogClosedException e1) {
                                    e1.printStackTrace();
                                } catch (DialogCanceledException e1) {
                                    e1.printStackTrace();
                                }
                                try {
                                    variant = Dialog.getInstance().showInputDialog(0, "Anything Special?", "If this is a special variant, please enter a Variant ID", "incomplete", null, null, "Nothing Special");

                                    if (variant != null) {
                                        variant = variant.replaceAll("[^a-zA-Z0-9]", "");
                                    }

                                } catch (DialogClosedException e1) {
                                    e1.printStackTrace();
                                } catch (DialogCanceledException e1) {
                                    e1.printStackTrace();
                                }
                                StringBuilder id = new StringBuilder();
                                id.append(lng);

                                if (!StringUtils.isEmpty(country)) {
                                    id.append("_");
                                    id.append(country);
                                }
                                if (!StringUtils.isEmpty(variant)) {
                                    if (StringUtils.isEmpty(country)) {
                                        id.append("_");
                                    }
                                    id.append("_");
                                    id.append(variant);
                                }
                                load(new TLocale(id.toString()));
                            } catch (DialogClosedException e1) {
                                e1.printStackTrace();
                            } catch (DialogCanceledException e1) {
                                e1.printStackTrace();
                            }

                        }

                    });
                    int sel = Dialog.getInstance().showDialog(d);
                    if (sel >= 0) {
                        load(getExtension().getTranslations().get(sel));

                    }
                } catch (DialogClosedException e1) {
                    e1.printStackTrace();
                } catch (DialogCanceledException e1) {
                    e1.printStackTrace();
                }
            }
        }));

        menuPanel.add(save = new ExtButton(new AppAction() {
            {
                setName("2. Save locally");
                setAccelerator("CTRL+S");
            }

            @Override
            public void actionPerformed(ActionEvent e2) {
                ProgressGetter pg = new ProgressDialog.ProgressGetter() {

                    @Override
                    public void run() throws Exception {

                        try {
                            if (!getExtension().hasChanges()) {
                                Dialog.getInstance().showMessageDialog("Nothing has Changed");
                                return;
                            }
                            stopEditing(true);
                            getExtension().write();
                            Dialog.getInstance().showMessageDialog("Save Succesful.\r\nDo not forget to click [Upload] when you stop translating.");
                            return;
                        } catch (Throwable e) {
                            Dialog.getInstance().showExceptionDialog("An error occured", "Could not save the changes", e);
                        }

                    }

                    @Override
                    public String getString() {
                        return null;
                    }

                    @Override
                    public int getProgress() {
                        return -1;
                    }

                    @Override
                    public String getLabelString() {
                        return null;
                    }
                };

                try {
                    Dialog.getInstance().showDialog(new ProgressDialog(pg, Dialog.BUTTONS_HIDE_CANCEL, "Saving", "Please wait.", null, null, null) {

                        @Override
                        public Dimension getPreferredSize() {
                            return new Dimension(200, 40);
                        }

                    });
                } catch (DialogClosedException e) {
                    e.printStackTrace();
                } catch (DialogCanceledException e) {
                    e.printStackTrace();
                }
            }
        }));
        menuPanel.add(upload = new ExtButton(new AppAction() {
            {
                setName("3. Upload Changes");
                setAccelerator("CTRL+u");
            }

            @Override
            public void actionPerformed(ActionEvent e) {

                ProgressGetter pg = new ProgressDialog.ProgressGetter() {

                    @Override
                    public void run() throws Exception {

                        try {
                            stopEditing(true);
                            SVNCommitPacket commit = getExtension().save();
                            if (commit == null) return;
                            if (commit.getCommitItems().length == 0) {
                                Dialog.getInstance().showMessageDialog("Nothing has Changed");
                                return;
                            }
                            StringBuilder sb = new StringBuilder();
                            for (SVNCommitItem ci : commit.getCommitItems()) {
                                if (commit.isCommitItemSkipped(ci)) continue;
                                if (sb.length() > 0) sb.append("\r\n");
                                sb.append(ci.getFile());
                            }
                            Dialog.getInstance().showMessageDialog("Save Succesful\r\n" + sb.toString());
                            return;
                        } catch (Throwable e) {
                            Dialog.getInstance().showExceptionDialog("An error occured", "Could not save and upload the changes", e);
                        }

                    }

                    @Override
                    public String getString() {
                        return null;
                    }

                    @Override
                    public int getProgress() {
                        return -1;
                    }

                    @Override
                    public String getLabelString() {
                        return null;
                    }
                };

                try {
                    Dialog.getInstance().showDialog(new ProgressDialog(pg, Dialog.BUTTONS_HIDE_CANCEL, "Saving", "Please wait.", null, null, null) {

                        @Override
                        public Dimension getPreferredSize() {
                            return new Dimension(200, 40);
                        }

                    });
                } catch (DialogClosedException e1) {
                    e1.printStackTrace();
                } catch (DialogCanceledException e1) {
                    e1.printStackTrace();
                }
            }
        }));

        isWizard = false;
        menuPanel.add(wizard = new ExtButton(new AppAction() {
            {
                setName("Wizard");

            }

            @Override
            public void actionPerformed(ActionEvent e) {
                isWizard = true;
                try {

                    while (true) {
                        boolean nothingtoDo = true;
                        try {

                            for (final TranslateEntry value : getExtension().getTranslationEntries()) {
                                if (value.isOK()) continue;
                                nothingtoDo = false;
                                if (stopEditing) break;
                                String ret = "<style>td.a{font-style:italic;}</style><table valign=top>";
                                ret += "<tr><td class=a>Key:</td><td>" + value.getCategory() + "." + value.getKey() + "</td></tr>";

                                ret += "<tr><td class=a>Location:</td><td>" + value.getFullKey() + "</td></tr>";

                                ret += "<tr><td class=a>Original:</td><td><b>" + Encoding.cdataEncode(value.getDirect()) + "</b></td></tr>";
                                if (value.isMissing()) {
                                    ret += "<tr><td class=a><font color='#ff0000' >Error:</font></td><td class=a><font color='#ff0000' >Not translated yet</font></td></tr>";

                                }
                                if (value.isDefault()) {
                                    ret += "<tr><td class=a><font color='#339900' >Warning:</font></td><td class=a><font color='#339900' >The translation equals the english default language.</font></td></tr>";
                                }

                                if (value.isParameterInvalid()) {
                                    ret += "<tr><td class=a><font color='#ff0000' >Error:</font></td><td class=a><font color='#ff0000' >Parameter Wildcards (%s*) do not match.</font></td></tr>";
                                }

                                Type[] parameters = value.getParameters();
                                ret += "<tr><td class=a>Parameters:</td>";
                                if (parameters.length == 0) {
                                    ret += "<td>none</td></tr>";
                                } else {
                                    ret += "<td>";
                                    int i = 1;
                                    for (Type t : parameters) {
                                        ret += "   %s" + i + " (" + t + ")<br>";
                                        i++;
                                    }
                                    ret += "</td>";
                                    ret += "</tr>";
                                }

                                ret += "</table>";

                                // ConfirmDialog d = new
                                // ConfirmDialog(Dialog.STYLE_HTML, "", ret, null, null,
                                // null);
                                try {
                                    while (true) {

                                        if (value.getDescription() != null) {
                                            Dialog.I().showMessageDialog("IMPORTANT!!!\r\n\r\n" + value.getCategory() + "." + value.getKey() + "\r\n" + value.getDescription());
                                        }
                                        final InputDialog d = new InputDialog(Dialog.STYLE_HTML, "Progress " + getExtension().getPercent() + "%", ret, null, null, "Next", "Cancel") {
                                            protected TextComponentInterface getSmallInputComponent() {

                                                final ExtTextField ttx = new ExtTextField();

                                                // private static final String
                                                // TEXT_SUBMIT = "text-submit";
                                                // private static final String
                                                // INSERT_BREAK = "insert-break";

                                                InputMap input = ttx.getInputMap();
                                                KeyStroke enter = KeyStroke.getKeyStroke("ENTER");
                                                KeyStroke shiftEnter = KeyStroke.getKeyStroke("shift ENTER");
                                                input.put(shiftEnter, "INSERT_BREAK"); // input.get(enter))
                                                                                       // =
                                                                                       // "insert-break"
                                                input.put(enter, "TEXT_SUBMIT");

                                                ActionMap actions = ttx.getActionMap();
                                                actions.put("TEXT_SUBMIT", new AbstractAction() {
                                                    @Override
                                                    public void actionPerformed(ActionEvent e) {
                                                        System.out.println("INSRT");
                                                        try {
                                                            Point point = ttx.getCaret().getMagicCaretPosition();
                                                            SwingUtilities.convertPointToScreen(point, ttx);

                                                            ttx.getDocument().insertString(ttx.getCaretPosition(), "\\r\\n", null);
                                                            HelpDialog.show(point, "TRANSLETOR_USE_NEWLINE", Dialog.STYLE_SHOW_DO_NOT_DISPLAY_AGAIN, "NewLine", "Press <Enter> to insert a Newline (\\r\\n). Press <CTRL ENTER> to Confirm  translation. Press <TAB> to confirm and move to next line.", NewTheme.I().getIcon("help", 32));

                                                        } catch (BadLocationException e1) {
                                                            e1.printStackTrace();
                                                        }
                                                    }
                                                });

                                                ttx.setClearHelpTextOnFocus(false);
                                                ttx.addKeyListener(this);
                                                ttx.addMouseListener(this);
                                                ttx.setHelpText("Please translate: " + value.getDirect());
                                                ttx.addActionListener(new ActionListener() {

                                                    @Override
                                                    public void actionPerformed(ActionEvent e) {
                                                        setReturnmask(true);
                                                    }
                                                });
                                                return ttx;

                                            }

                                            @Override
                                            protected int getPreferredWidth() {
                                                return JDGui.getInstance().getMainFrame().getWidth();
                                            }

                                        };
                                        d.setLeftActions(new AppAction() {
                                            {
                                                setName("Skip");
                                            }

                                            @Override
                                            public void actionPerformed(ActionEvent e) {
                                                d.dispose();
                                            }

                                        });

                                        String newTranslation = Dialog.getInstance().showDialog(d);

                                        if (newTranslation == null) {
                                            // Skip
                                            break;
                                        }
                                        value.setTranslation(newTranslation);
                                        if (value.isOK() || value.isDefault()) break;

                                    }
                                } catch (DialogClosedException e1) {
                                    e1.printStackTrace();

                                } catch (DialogCanceledException e1) {
                                    return;
                                }

                            }
                        } finally {
                            isWizard = false;
                        }
                        if (nothingtoDo && !stopEditing) break;
                    }
                } finally {

                    Dialog.getInstance().showMessageDialog("Wizard ended");
                }
            }
        }));
        menuPanel.add(Box.createHorizontalGlue(), "growx,pushx");
        menuPanel2 = new MigPanel("ins 0", "[grow,fill]3[]3[]3[]", "[grow,fill]");
        menuPanel2.add(lbl = new JLabel(), "aligny center,pushx,growx");

        menuPanel2.add(revert = new ExtButton(new AppAction() {
            {
                setName("Revert");
                setSmallIcon(NewTheme.I().getIcon("undo", 18));
                setTooltipText("Revert all your changes");

            }

            @Override
            public void actionPerformed(ActionEvent e) {

                try {

                    Dialog.I().showConfirmDialog(0, "Revert all Changes?", "All your changes will be lost. Continue anyway?");
                    ProgressGetter pg = new ProgressDialog.ProgressGetter() {

                        @Override
                        public void run() throws Exception {
                            try {
                                getExtension().revert();
                            } catch (SVNException e) {
                                Dialog.getInstance().showExceptionDialog("Error occured", "You got logged out", e);
                                getExtension().doLogout();
                            } catch (Throwable e) {
                                Dialog.getInstance().showExceptionDialog("Error", e.getLocalizedMessage(), e);
                            }
                        }

                        @Override
                        public String getString() {
                            return null;
                        }

                        @Override
                        public int getProgress() {
                            return -1;
                        }

                        @Override
                        public String getLabelString() {
                            return null;
                        }
                    };

                    try {
                        Dialog.getInstance().showDialog(new ProgressDialog(pg, Dialog.BUTTONS_HIDE_CANCEL, "Reverting", "Please wait. Reverting all Changes", null, null, null) {

                            @Override
                            public Dimension getPreferredSize() {
                                return new Dimension(200, 40);
                            }

                        });
                    } catch (DialogClosedException e2) {
                        e2.printStackTrace();
                    } catch (DialogCanceledException e2) {
                        e2.printStackTrace();
                    }

                } catch (DialogClosedException e1) {
                    e1.printStackTrace();
                } catch (DialogCanceledException e1) {
                    e1.printStackTrace();
                }
            }
        }));
        // bt.setRolloverEffectEnabled(true);

        menuPanel2.add(restart = new ExtButton(new AppAction() {
            {
                setName("Restart");
                setSmallIcon(NewTheme.I().getIcon("restart", 18));
                setTooltipText("Restart JDownloader to test the translation.");

            }

            @Override
            public void actionPerformed(ActionEvent e) {
                RestartController.getInstance().getAdditionalParameters().add("-translatortest");
                RestartController.getInstance().getAdditionalParameters().add(getExtension().getLoadedLocale().getId());
                JDRestartController.getInstance().directRestart(true);

            }
        }));
        menuPanel2.add(logout = new ExtButton(new AppAction() {
            {
                setName("Logout");
                setSmallIcon(NewTheme.I().getIcon("logout", 18));

            }

            @Override
            public void actionPerformed(ActionEvent e) {
                if (getExtension().isLoggedIn()) {
                    try {
                        getExtension().doLogout();
                    } catch (InterruptedException e1) {
                        e1.printStackTrace();
                    }

                } else {
                    try {
                        getExtension().doLogin();
                    } catch (InterruptedException e1) {
                        e1.printStackTrace();
                    }
                }

            }
        }));

        ti = new Timer(300, new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                if (getExtension().getLoadedLocale() != null && getExtension().getTranslationEntries() != null) {
                    int[] sel = table.getSelectedRows();

                    if (sel.length > 0) {
                        lbl.setText("Translation: " + getExtension().getLoadedLocale() + " - " + getExtension().getPercent() + "% translated (" + getExtension().getOK() + "/" + getExtension().getTranslationEntries().size() + ") " + "Selected: " + sel.length);

                    } else {
                        lbl.setText("Translation: " + getExtension().getLoadedLocale() + " - " + getExtension().getPercent() + "% translated (" + getExtension().getOK() + "/" + getExtension().getTranslationEntries().size() + ")");
                    }
                } else {
                    lbl.setText("Please Log In & Load a Language");
                }
            }
        });
        ti.setRepeats(true);

        // menubar.add(this.mnuView);

        // tableModel.setMarkDefaults(mnuViewMarkDef.getState());
        // tableModel.setMarkOK(mnuViewMarkOK.getState());

    }

    /**
     * is called if, and only if! the view has been closed
     */
    @Override
    protected void onDeactivated() {
        Log.L.finer("onDeactivated " + getClass().getSimpleName());
        if (!getExtension().getSettings().isRememberLoginsEnabled()) {
            try {
                getExtension().doLogout();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * is called, if the gui has been opened.
     */
    @Override
    protected void onActivated() {
        Log.L.finer("onActivated " + getClass().getSimpleName());

    }

    @Override
    public Icon getIcon() {
        return this.getExtension().getIcon(16);
    }

    @Override
    public String getID() {
        return ID;
    }

    @Override
    public String getTitle() {
        return "Translator";
    }

    @Override
    public String getTooltip() {
        return "Translator - Edit JDownloader Translation";
    }

    /**
     * Is called if gui is visible now, and has not been visible before. For example, user starte the extension, opened the view, or
     * switched form a different tab to this one
     */
    @Override
    protected void onShow() {
        ti.start();
        JDRestartController.getInstance().setSilentRestartAllowed(false);
        Log.L.finer("Shown " + getClass().getSimpleName());
        if (getExtension().isLoggedIn()) return;
        ProgressGetter pg = new ProgressDialog.ProgressGetter() {

            @Override
            public void run() throws Exception {
                try {
                    getExtension().doLogin();
                } catch (Throwable e) {
                    Dialog.getInstance().showExceptionDialog("Error", e.getLocalizedMessage(), e);
                }
            }

            @Override
            public String getString() {
                return null;
            }

            @Override
            public int getProgress() {
                return -1;
            }

            @Override
            public String getLabelString() {
                return null;
            }
        };

        try {
            Dialog.getInstance().showDialog(new ProgressDialog(pg, Dialog.BUTTONS_HIDE_CANCEL, "Login", "Please wait.", null, null, null) {

                @Override
                public Dimension getPreferredSize() {
                    return new Dimension(200, 40);
                }

            });
        } catch (DialogClosedException e) {
            e.printStackTrace();
        } catch (DialogCanceledException e) {
            e.printStackTrace();
        }

    }

    /**
     * gets called of the extensiongui is not visible any more. for example because it has been closed or user switched to a different
     * tab/view
     */
    @Override
    protected void onHide() {
        Log.L.finer("hidden " + getClass().getSimpleName());
        ti.stop();
        JDRestartController.getInstance().setSilentRestartAllowed(true);
    }

    public TLocale getLoaded() {
        return getExtension().getLoadedLocale();
    }

    public void load(final TLocale locale) {

        ProgressGetter pg = new ProgressDialog.ProgressGetter() {

            @Override
            public void run() throws Exception {
                try {

                    stopEditing(true);
                    if (getExtension().getLoadedLocale() != null) {
                        try {
                            if (getExtension().hasChanges()) {

                                Dialog.I().showConfirmDialog(0, "Save " + getExtension().getLoadedLocale().getLocale().getDisplayName(), "Do you want to save your Changes on the " + getExtension().getLoadedLocale().getLocale().getDisplayName() + " Translation?");
                                getExtension().write();
                            }
                        } catch (Throwable e) {
                            e.printStackTrace();
                        }
                    }

                    getExtension().load(locale, false, true);
                } catch (SVNException e) {
                    Dialog.getInstance().showExceptionDialog("Error occured", "You got logged out", e);
                    getExtension().doLogout();
                } catch (Throwable e) {
                    Dialog.getInstance().showExceptionDialog("Error", e.getLocalizedMessage(), e);
                }
            }

            @Override
            public String getString() {
                return null;
            }

            @Override
            public int getProgress() {
                return -1;
            }

            @Override
            public String getLabelString() {
                return null;
            }
        };

        try {
            Dialog.getInstance().showDialog(new ProgressDialog(pg, Dialog.BUTTONS_HIDE_CANCEL, "Load Language", "Please wait. Loading " + locale, null, null, null) {

                @Override
                public Dimension getPreferredSize() {
                    return new Dimension(200, 40);
                }

            });
        } catch (DialogClosedException e) {
            e.printStackTrace();
        } catch (DialogCanceledException e) {
            e.printStackTrace();
        }

    }

    public void refresh() {
        new EDTRunner() {

            @Override
            protected void runInEDT() {
                String desiredFont = getExtension().getFontname();
                if (LookAndFeelController.getInstance().isSynthetica()) {
                    try {
                        if (desiredFont != null && !desiredFont.equals(de.javasoft.plaf.synthetica.SyntheticaLookAndFeel.getFontName())) {
                            // switch fontname. create ne table to use the new font
                            Font newFont = (Font) (new FontUIResource(desiredFont, 0, de.javasoft.plaf.synthetica.SyntheticaLookAndFeel.getFontSize()));
                            de.javasoft.plaf.synthetica.SyntheticaLookAndFeel.setFont(newFont, false);
                            initTable();
                            sp.getViewport().setView(table);
                        }
                    } catch (final Throwable e) {
                        LogController.CL().log(e);
                    }
                }

                tableModel.refresh(getExtension());
                if (getExtension().getLoadedLocale() != null) {
                    restart.setEnabled(true);
                    restart.setText("Restart in " + getExtension().getLoadedLocale().getLocale().getDisplayName(getExtension().getLoadedLocale().getLocale()));
                } else {
                    restart.setEnabled(false);
                }

            }
        };

    }

    public void valueChanged(ListSelectionEvent e) {
        // ip.setEntries(tableModel.getSelectedObjects());
    }

    @Override
    public void onLngRefresh(TranslatorExtensionEvent event) {
        new EDTRunner() {

            @Override
            protected void runInEDT() {
                refresh();

            }
        };
    }

    @Override
    public void onLogInOrOut() {
        new EDTRunner() {

            @Override
            protected void runInEDT() {
                if (!getExtension().isLoggedIn()) {
                    refresh();
                    logout.setText("Login");
                    load.setEnabled(false);
                    save.setEnabled(false);
                    revert.setEnabled(false);
                    restart.setEnabled(false);
                    wizard.setEnabled(false);
                    upload.setEnabled(false);

                } else {
                    logout.setText("Logout");

                    load.setEnabled(true);
                    save.setEnabled(true);
                    revert.setEnabled(true);
                    wizard.setEnabled(true);
                    restart.setEnabled(true);
                    upload.setEnabled(true);
                }

            }
        };
    }

    public void stopEditing(boolean requestStop) throws InterruptedException {
        if (requestStop) {
            new EDTRunner() {

                @Override
                protected void runInEDT() {
                    if (table.getCellEditor() != null) {
                        table.getCellEditor().stopCellEditing();
                    }
                }
            };

        }
        stopEditing = true;
        try {
            Log.L.info("Wait for editstop");

            while (new EDTHelper<Boolean>() {

                @Override
                public Boolean edtRun() {
                    return table.isEditing();
                }
            }.getReturnValue() || isWizard) {
                Thread.sleep(10);
            }
        } finally {
            stopEditing = false;
            Log.L.info("Editing has stopped");
        }
    }

}
