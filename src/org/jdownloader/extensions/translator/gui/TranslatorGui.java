package org.jdownloader.extensions.translator.gui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;

import javax.swing.Icon;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import jd.gui.swing.jdgui.interfaces.SwitchPanel;
import jd.plugins.AddonPanel;
import net.miginfocom.swing.MigLayout;

import org.appwork.utils.logging.Log;
import org.jdownloader.extensions.translator.TLocale;
import org.jdownloader.extensions.translator.TranslatorExtension;
import org.jdownloader.extensions.translator.gui.actions.LoadTranslationAction;
import org.jdownloader.extensions.translator.gui.actions.MarkDefaultAction;
import org.jdownloader.extensions.translator.gui.actions.MarkOkAction;
import org.jdownloader.extensions.translator.gui.actions.NewTranslationAction;
import org.jdownloader.extensions.translator.gui.actions.TestSvnAction;
import org.jdownloader.images.NewTheme;

/**
 * Extension gui
 * 
 * @author thomas
 * 
 */
public class TranslatorGui extends AddonPanel<TranslatorExtension> implements ListSelectionListener {

    private static final String ID = "TRANSLATORGUI";
    private TranslateTableModel tableModel;
    private TranslateTable      table;
    private SwitchPanel         panel;
    private JMenu               mnuFile;
    private JMenu               mnuFileLoad;
    private JMenuItem           mnuFileSave;
    private JMenu               mnuView;
    private JCheckBoxMenuItem   mnuViewMarkOK;
    private JCheckBoxMenuItem   mnuViewMarkDef;
    private JMenu               mnuSvn;
    private JTextField          pnlSvnUser;
    private JTextField          pnlSvnPass;

    private JPanel              pnlIcon;
    private JLabel              lblIconOK;
    private JLabel              lblIconError;
    private JLabel              lblIconWarning;
    private JLabel              lblIconMissing;
    private JLabel              lblIconDefault;

    public TranslatorGui(TranslatorExtension plg) {
        super(plg);
        this.panel = new SwitchPanel(new MigLayout("ins 0,wrap 1", "[grow,fill]", "[grow,fill][]")) {

            @Override
            protected void onShow() {
            }

            @Override
            protected void onHide() {
            }
        };
        // layout all contents in panel
        this.setContent(panel);
        initComponents();

        layoutPanel();
    }

    private void layoutPanel() {
        panel.add(new JScrollPane(table), "spanx 2");
        panel.add(pnlIcon);

    }

    private void initComponents() {
        tableModel = new TranslateTableModel();
        table = new TranslateTable(tableModel);
        table.getSelectionModel().addListSelectionListener(this);

        pnlIcon = new JPanel(new MigLayout("ins 2 10 2 10", "", ""));

        lblIconOK = new JLabel("OK", NewTheme.I().getIcon("ok", 16), JLabel.LEFT);
        lblIconOK.setToolTipText("<html>Marked keys are OK.</html>");
        pnlIcon.add(lblIconOK);

        lblIconError = new JLabel("Error", NewTheme.I().getIcon("error", 16), JLabel.LEFT);
        lblIconError.setToolTipText("<html>Marked keys contain errors. Did you...<ul><li>use the right number of parameters (%s1, %s2, ...)?</li></ul></html>");
        pnlIcon.add(lblIconError);

        lblIconWarning = new JLabel("Warning", NewTheme.I().getIcon("warning", 16), JLabel.LEFT);
        lblIconWarning.setToolTipText("<html>Marked keys contain warnings. Did you...<ul><il>use about the same length as the default text for your translation?</li></ul></html>");
        pnlIcon.add(lblIconWarning);

        lblIconMissing = new JLabel("Missing", NewTheme.I().getIcon("prio_0", 16), JLabel.LEFT);
        lblIconMissing.setToolTipText("<html>Marked keys are untranslated.</html>");
        pnlIcon.add(lblIconMissing);

        lblIconDefault = new JLabel("Default", NewTheme.I().getIcon("flags/en", 16), JLabel.LEFT);
        lblIconDefault.setToolTipText("<html>Marked keys are still set to the default texts. The key might...<ul><il>be untranslated?</li><il>does not need translation?</li></ul></html>");
        pnlIcon.add(lblIconDefault);

    }

    protected void initMenu(JMenuBar menubar) {
        // Load Menu
        mnuFile = new JMenu("File");

        this.mnuFileLoad = new JMenu("Load");
        for (TLocale t : getExtension().getTranslations()) {
            mnuFileLoad.add(new LoadTranslationAction(this, t));
        }
        if (getExtension().getTranslations().size() > 0) mnuFileLoad.add(new JSeparator());
        mnuFileLoad.add(new NewTranslationAction(this));

        this.mnuFileSave = new JMenuItem("Save");
        mnuFileSave.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                save();
            }
        });
        this.mnuFileSave.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_DOWN_MASK));

        this.mnuFile.add(this.mnuFileLoad);
        this.mnuFile.addSeparator();
        this.mnuFile.add(this.mnuFileSave);

        mnuView = new JMenu("View");

        mnuView.add(this.mnuViewMarkOK = new JCheckBoxMenuItem(new MarkOkAction(this)));
        mnuView.add(this.mnuViewMarkDef = new JCheckBoxMenuItem(new MarkDefaultAction(this)));
        mnuViewMarkOK.setState(true);
        mnuViewMarkOK.setVisible(false);

        JPanel pnlSvnLogin = new JPanel(new MigLayout("wrap 2", "", "[30%][]"));
        this.pnlSvnUser = new JTextField(12);
        this.pnlSvnPass = new JTextField(12);
        pnlSvnLogin.add(new JLabel("<html><i>Username"));
        pnlSvnLogin.add(this.pnlSvnUser);
        pnlSvnLogin.add(new JLabel("<html><i>Password"));
        pnlSvnLogin.add(this.pnlSvnPass);

        JMenuItem mnuSvnTest = new JMenuItem(new TestSvnAction(this.pnlSvnUser, this.pnlSvnPass));

        mnuSvn = new JMenu("SVN");
        mnuSvn.add(pnlSvnLogin);
        mnuSvn.add(mnuSvnTest);

        // Menu-Bar zusammensetzen
        menubar.add(this.mnuFile);
        menubar.add(this.mnuView);
        menubar.add(this.mnuSvn);

        // tableModel.setMarkDefaults(mnuViewMarkDef.getState());
        // tableModel.setMarkOK(mnuViewMarkOK.getState());
    }

    protected void save() {
    }

    /**
     * is called if, and only if! the view has been closed
     */
    @Override
    protected void onDeactivated() {
        Log.L.finer("onDeactivated " + getClass().getSimpleName());
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
     * Is called if gui is visible now, and has not been visible before. For
     * example, user starte the extension, opened the view, or switched form a
     * different tab to this one
     */
    @Override
    protected void onShow() {
        Log.L.finer("Shown " + getClass().getSimpleName());
    }

    /**
     * gets called of the extensiongui is not visible any more. for example
     * because it has been closed or user switched to a different tab/view
     */
    @Override
    protected void onHide() {
        Log.L.finer("hidden " + getClass().getSimpleName());
    }

    public TLocale getLoaded() {
        return getExtension().getLoadedLocale();
    }

    public void load(TLocale locale) {

        getExtension().load(locale);
    }

    public void refresh() {
        tableModel.refresh(getExtension());
    }

    public void valueChanged(ListSelectionEvent e) {
        // ip.setEntries(tableModel.getSelectedObjects());
    }

    public void setMarkOK(boolean state) {
        lblIconOK.setEnabled(state);
        tableModel.setMarkOK(state);
        table.repaint();
    }

    public void setMarkDefault(boolean state) {
        lblIconDefault.setEnabled(state);
        tableModel.setMarkDefaults(state);
        table.repaint();
    }
}
