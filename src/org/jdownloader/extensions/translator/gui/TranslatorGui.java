package org.jdownloader.extensions.translator.gui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;

import javax.swing.Icon;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
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
import org.jdownloader.extensions.translator.gui.actions.NewTranslationAction;

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
    private JMenu               mnuLoad;
    private JMenu               mnuFile;
    private JMenuItem           mnuSave;
    private SwitchPanel         panel;
    private EntryInfoPanel      ip;

    public TranslatorGui(TranslatorExtension plg) {
        super(plg);
        this.panel = new SwitchPanel(new MigLayout("ins 0,wrap 1", "[grow,fill][]", "[grow,fill]")) {

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

        panel.add(new JScrollPane(table));
        panel.add(ip);
    }

    private void initComponents() {
        tableModel = new TranslateTableModel();
        table = new TranslateTable(tableModel);
        table.getSelectionModel().addListSelectionListener(this);
        ip = new EntryInfoPanel();
    }

    protected void initMenu(JMenuBar menubar) {
        // Load Menu
        this.mnuLoad = new JMenu("Load");
        for (TLocale t : getExtension().getTranslations()) {
            mnuLoad.add(new LoadTranslationAction(this, t));
        }
        if (getExtension().getTranslations().size() > 0) mnuLoad.add(new JSeparator());
        mnuLoad.add(new NewTranslationAction(this));
        mnuFile = new JMenu("File");
        this.mnuFile.add(this.mnuLoad);
        this.mnuFile.addSeparator();
        this.mnuFile.add(this.mnuSave = new JMenuItem("Save"));
        mnuSave.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                save();
            }
        });
        this.mnuSave.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_DOWN_MASK));

        // Menu-Bar zusammensetzen
        menubar.add(this.mnuFile);

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
        ip.setEntries(tableModel.getSelectedObjects());
    }

}
