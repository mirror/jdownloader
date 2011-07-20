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
import javax.swing.KeyStroke;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import jd.controlling.IOEQ;
import jd.gui.swing.jdgui.interfaces.SwitchPanel;
import jd.nutils.svn.Subversion;
import jd.plugins.AddonPanel;
import net.miginfocom.swing.MigLayout;

import org.appwork.utils.logging.Log;
import org.appwork.utils.swing.dialog.Dialog;
import org.appwork.utils.swing.dialog.DialogCanceledException;
import org.appwork.utils.swing.dialog.DialogClosedException;
import org.appwork.utils.swing.dialog.LoginDialog;
import org.appwork.utils.swing.dialog.LoginDialog.LoginData;
import org.jdownloader.extensions.translator.TLocale;
import org.jdownloader.extensions.translator.TranslatorExtension;
import org.jdownloader.extensions.translator.gui.actions.LoadTranslationAction;
import org.jdownloader.extensions.translator.gui.actions.LoginSvnAction;
import org.jdownloader.extensions.translator.gui.actions.MarkDefaultAction;
import org.jdownloader.extensions.translator.gui.actions.MarkOkAction;
import org.jdownloader.extensions.translator.gui.actions.NewTranslationAction;
import org.jdownloader.extensions.translator.gui.actions.TestSvnAction;
import org.jdownloader.extensions.translator.gui.actions.UpdateSVNAction;
import org.jdownloader.images.NewTheme;
import org.tmatesoft.svn.core.SVNException;

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
    private JMenuItem           mnuSvnTest;
    private JMenuItem           mnuSvnUpdate;
    private JMenuItem           mnuSvnLogin;
    private JLabel              mnuSvnRev;

    private JPanel              pnlIcon;
    private JLabel              lblIconOK;

    private JLabel              lblIconError;
    private JLabel              lblIconWarning;

    private JLabel              lblIconMissing;
    private JLabel              lblIconDefault;

    private boolean             svnBusy;
    private Long                latestRevision;

    private String              svnUser;
    private String              svnPass;
    private boolean             svnLoginOK;

    public TranslatorGui(TranslatorExtension plg) {
        super(plg);

        this.panel = new SwitchPanel(new MigLayout("ins 0,wrap 1", "[grow,fill]", "[grow,fill][]")) {

            @Override
            protected void onShow() {
                IOEQ.add(new Runnable() {

                    public void run() {
                        if (!svnLoginOK) requestSvnLogin();
                    }

                });

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

        this.svnUser = "sascha";
        this.svnPass = "91RCpyddH43zY";

        mnuSvnLogin = new JMenuItem(new LoginSvnAction(this));
        mnuSvnTest = new JMenuItem(new TestSvnAction(this));
        mnuSvnUpdate = new JMenuItem(new UpdateSVNAction(this));

        mnuSvnRev = new JLabel("Revision: n/a", null, JLabel.RIGHT) {
            {
                this.setEnabled(false);
            }

            @Override
            public String getText() {
                return "Revision: " + getLatestRevision().toString();
            }
        };

        mnuSvn = new JMenu("SVN");
        mnuSvn.add(mnuSvnLogin);
        mnuSvn.add(mnuSvnTest);
        mnuSvn.addSeparator();
        mnuSvn.add(mnuSvnUpdate);
        mnuSvn.add(mnuSvnRev);

        // Menu-Bar zusammensetzen
        menubar.add(this.mnuFile);
        menubar.add(this.mnuView);
        menubar.add(this.mnuSvn);

        // tableModel.setMarkDefaults(mnuViewMarkDef.getState());
        // tableModel.setMarkOK(mnuViewMarkOK.getState());

        svnLoginOK = false;
        setSvnBusy(true);
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

    public boolean isSvnBusy() {
        return svnBusy;
    }

    public void setSvnBusy(boolean svnBusy) {
        this.svnBusy = svnBusy;
        this.mnuFile.setEnabled(!this.svnBusy);
        this.mnuView.setEnabled(!this.svnBusy);
        this.mnuSvn.setEnabled(!this.svnBusy);
    }

    public Long getLatestRevision() {
        if (latestRevision != null)
            return latestRevision;
        else {

            try {
                Subversion svn = new Subversion("svn://svn.jdownloader.org/jdownloader/trunk/translations");
                latestRevision = new Long(svn.latestRevision());
                return latestRevision;
            } catch (SVNException e) {
                return new Long(-1);
            }

        }
    }

    public String getSvnUser() {
        return svnUser;
    }

    public String getSvnPass() {
        return svnPass;
    }

    public void setSvnLogin(String svnUser, String svnPass) {
        this.svnUser = svnUser;
        this.svnPass = svnPass;
    }

    public boolean isSvnLoginOK() {
        return svnLoginOK;
    }

    public void setSvnLoginOK(boolean svnLoginOK) {
        this.svnLoginOK = svnLoginOK;
    }

    public boolean validateSvnLogin(String svnUser, String svnPass) {
        if (svnUser.length() > 3 && svnPass.length() > 3) {
            String url = "svn://svn.jdownloader.org/jdownloader";

            Subversion s = null;
            try {
                s = new Subversion(url, svnUser, svnPass);

                return true;
            } catch (SVNException e) {
                Dialog.getInstance().showMessageDialog("SVN Test Error", "Login failed. Username and/or password are not correct!\r\n\r\nServer: " + url);
            } finally {
                try {
                    s.dispose();
                } catch (final Throwable e) {
                }
            }
        } else {
            Dialog.getInstance().showMessageDialog("SVN Test Error", "Username and/or password seem malformed. Test failed.");
        }
        return false;

    }

    public void requestSvnLogin() {
        this.setSvnBusy(true);
        while (true) {
            this.svnLoginOK = false;
            final LoginDialog d = new LoginDialog(0, "SVN Login", "<html>A JDownloader SVN login is required to use the Translator Extension.<br>Please insert a valid SVN username and password.", null);
            d.setUsernameDefault(svnUser);
            d.setPasswordDefault(svnPass);
            d.setRememberDefault(true);

            LoginData response;
            try {
                response = Dialog.getInstance().showDialog(d);
            } catch (DialogClosedException e) {
                // if (!this.svnLoginOK) validateSvnLogin();
                return;
            } catch (DialogCanceledException e) {
                // if (!this.svnLoginOK) validateSvnLogin();
                return;
            }
            if (validateSvnLogin(response.getUsername(), response.getPassword())) {
                this.setSvnLogin(response.getUsername(), response.getPassword());
                this.svnLoginOK = true;
                this.setSvnBusy(false);
                return;
            }
        }

    }
}
