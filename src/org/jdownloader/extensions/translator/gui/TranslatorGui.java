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

import jd.gui.swing.jdgui.interfaces.SwitchPanel;
import jd.plugins.AddonPanel;
import net.miginfocom.swing.MigLayout;

import org.jdownloader.extensions.translator.TLocale;
import org.jdownloader.extensions.translator.TranslatorExtension;

public class TranslatorGui extends AddonPanel<TranslatorExtension> {

    private static final String ID = "TRANSLATORGUI";
    private TranslateTableModel tableModel;
    private TranslateTable      table;
    private JMenu               mnuLoad;
    private JMenu               mnuFile;
    private JMenuItem           mnuSave;
    private SwitchPanel         panel;

    public TranslatorGui(TranslatorExtension plg) {
        super(plg);
        this.panel = new SwitchPanel(new MigLayout("ins 0", "[grow,fill]", "[grow,fill]")) {

            @Override
            protected void onShow() {
            }

            @Override
            protected void onHide() {
            }
        };
        this.setContent(panel);
        initComponents();

        layoutPanel();
    }

    private void layoutPanel() {

        panel.add(new JScrollPane(table));
    }

    private void initComponents() {
        tableModel = new TranslateTableModel();
        table = new TranslateTable(tableModel);
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

    @Override
    protected void onDeactivated() {
    }

    @Override
    protected void onActivated() {

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

    @Override
    protected void onShow() {
    }

    @Override
    protected void onHide() {
    }

    public TLocale getLoaded() {
        return getExtension().getLoadedLocale();
    }

    public void load(TLocale locale) {

        getExtension().load(locale);
    }

}
