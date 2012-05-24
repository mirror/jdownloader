package org.jdownloader.extensions.translator.gui;

import java.awt.event.ActionEvent;

import javax.swing.Icon;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
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
import org.jdownloader.actions.AppAction;
import org.jdownloader.extensions.translator.TLocale;
import org.jdownloader.extensions.translator.TranslatorExtension;
import org.jdownloader.extensions.translator.gui.actions.LoadTranslationAction;
import org.jdownloader.extensions.translator.gui.actions.NewTranslationAction;
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

    private JMenu               mnuView;

    private boolean             svnBusy;

    private String              svnUser;
    private String              svnPass;
    private boolean             svnLoginOK;

    public TranslatorGui(TranslatorExtension plg) {
        super(plg);

        this.panel = new SwitchPanel(new MigLayout("ins 0,wrap 1", "[grow,fill]", "[grow,fill]")) {

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

    }

    private void initComponents() {
        tableModel = new TranslateTableModel();
        table = new TranslateTable(tableModel);
        table.getSelectionModel().addListSelectionListener(this);

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

        this.mnuFile.add(this.mnuFileLoad);

        this.mnuFile.add(new AppAction() {
            {
                setName("Save");
                setAccelerator("CTRL+S");
            }

            @Override
            public void actionPerformed(ActionEvent e) {
                save();
            }
        });
        this.mnuFile.addSeparator();

        mnuView = new JMenu("View");

        this.svnUser = null;
        this.svnPass = null;

        // Menu-Bar zusammensetzen
        menubar.add(this.mnuFile);
        menubar.add(this.mnuView);

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
        svnLoginOK = false;
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
        Log.L.finer("Shown " + getClass().getSimpleName());
    }

    /**
     * gets called of the extensiongui is not visible any more. for example because it has been closed or user switched to a different
     * tab/view
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

    public boolean isSvnBusy() {
        return svnBusy;
    }

    public void setSvnBusy(boolean svnBusy) {
        this.svnBusy = svnBusy;
        this.mnuFile.setEnabled(!this.svnBusy);
        this.mnuView.setEnabled(!this.svnBusy);

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
            final LoginDialog d = new LoginDialog(0, "Translation Server Login", "To modify existing translations, or to create a new one, you need a JDownloader Translator Account.", null);
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
