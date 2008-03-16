package jd.gui.skins.simple.config;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.File;

import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.event.TableModelEvent;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableColumn;

import jd.captcha.JAntiCaptcha;
import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.config.Configuration;
import jd.gui.UIInterface;
import jd.utils.JDLocale;
import jd.utils.JDUtilities;

public class ConfigPanelCaptcha extends ConfigPanel implements MouseListener, ActionListener {

    /**
     * 
     */
    private static final long serialVersionUID = 1592765387324291781L;

    /**
     * 
     */

    private JTable            table;

    private Configuration     configuration;

    private File[]            methods;

    public ConfigPanelCaptcha(Configuration configuration, UIInterface uiinterface) {
        super(uiinterface);
        this.configuration = configuration;
        methods = JAntiCaptcha.getMethods("jd/captcha/methods/");
        logger.info(methods.length + "");

        initPanel();

        load();

    }

    /**
     * Lädt alle Informationen
     */
    public void load() {
        this.loadConfigEntries();
    }

    /**
     * Speichert alle Änderungen auf der Maske
     */
    public void save() {
        // Interaction[] tmp= new Interaction[interactions.size()];
        // PluginForSearch plg;
        // for (int i = 0; i < pluginsForSearch.size(); i++) {
        // plg = pluginsForSearch.elementAt(i);
        // if (plg.getProperties() != null)
        // configuration.setProperty("PluginConfig_" + plg.getPluginName(),
        // plg.getProperties());
        // }
        this.saveConfigEntries();

    }

    public void mouseClicked(MouseEvent e) {

        configuration.setProperty(Configuration.PARAM_JAC_METHODS + "_" + methods[table.getSelectedRow()].getName(), !configuration.getBooleanProperty(Configuration.PARAM_JAC_METHODS + "_" + methods[table.getSelectedRow()].getName(), true));
        table.tableChanged(new TableModelEvent(table.getModel()));
    }

    @Override
    public void initPanel() {
        setLayout(new BorderLayout());

        GUIConfigEntry gce;
        ConfigEntry ce;
        // ce= new GUIConfigEntry( new
        // ConfigEntry(ConfigContainer.TYPE_CHECKBOX,
        // JDUtilities.getSubConfig("JAC"),
        // Configuration.USE_CAPTCHA_EXCHANGE_SERVER,
        // JDLocale.L("gui.config.captcha.autotrain","Autotrain
        // aktivieren")).setDefaultValue(false));
        // addGUIConfigEntry(ce);
        ce = new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, JDUtilities.getSubConfig("JAC"), "SHOW_EXTENDED_CAPTCHA", JDLocale.L("gui.config.captcha.extendedCaptcha", "Captchaverarbeitung anzeigen"));
        ce.setDefaultValue(true);
        //ce.setInstantHelp(JDLocale.L("gui.config.captcha.extendedCaptcha.instanthelp", "http://ns2.km32221.keymachine.de/jdownloader/web/page.php?id=3"));
        gce = new GUIConfigEntry(ce);
        addGUIConfigEntry(gce);
        ce=new ConfigEntry(ConfigContainer.TYPE_SPINNER, JDUtilities.getSubConfig("JAC"), Configuration.AUTOTRAIN_ERROR_LEVEL, JDLocale.L("gui.config.captcha.train.level", "Anzeigeschwelle"), 0, 100).setDefaultValue(80).setExpertEntry(true);
        ce.setInstantHelp(JDLocale.L("gui.config.captcha.train.level.instanthelp", "http://ns2.km32221.keymachine.de/jdownloader/web/page.php?id=3"));
       
        gce = new GUIConfigEntry(ce);
        
        
        addGUIConfigEntry(gce);
        gce = new GUIConfigEntry(new ConfigEntry(ConfigContainer.TYPE_SPINNER, JDUtilities.getSubConfig("JAC"), Configuration.AUTOTRAIN_SHOW_TIMEOUT, JDLocale.L("gui.config.captcha.train.show_timeout", "Countdown für das Eingabefenster"), 0, 600).setDefaultValue(20).setExpertEntry(true));
        addGUIConfigEntry(gce);

        // ce= new GUIConfigEntry( new ConfigEntry(ConfigContainer.TYPE_SPINNER,
        // configuration, Configuration.PARAM_CAPTCHA_INPUT_SHOWTIME,
        // JDLocale.L("gui.config.captcha.show_input_dialog","Zeige den
        // Eingabedialog"),0,180).setDefaultValue(0));
        // addGUIConfigEntry(ce);

        gce = new GUIConfigEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, configuration, Configuration.PARAM_CAPTCHA_JAC_DISABLE, JDLocale.L("gui.config.captcha.jac_disable", "Automatische Bilderkennung abschalten")).setDefaultValue(false));
        addGUIConfigEntry(gce);

        
        
        gce = new GUIConfigEntry(new ConfigEntry(ConfigContainer.TYPE_BUTTON,  this, JDLocale.L("gui.config.captcha.btn_train", "Captcha Training starten")));
        addGUIConfigEntry(gce);
        gce = new GUIConfigEntry(new ConfigEntry(ConfigContainer.TYPE_BUTTON,  this, JDLocale.L("gui.config.captcha.btn_show", "Testbild auswerten")));
        addGUIConfigEntry(gce);
        // ce= new GUIConfigEntry( new
        // ConfigEntry(ConfigContainer.TYPE_CHECKBOX, configuration,
        // Configuration.USE_CAPTCHA_COLLECTOR,
        // JDLocale.L("gui.config.captcha.use_collector","nicht erkannte
        // Captchas Online sammeln
        // lassen")).setDefaultValue(true).setExpertEntry(true));
        // addGUIConfigEntry(ce);

        gce = new GUIConfigEntry(new ConfigEntry(ConfigContainer.TYPE_LABEL, JDLocale.L("gui.config.captcha.jac_methods", "Automatische Bilderkennung verwenden für:")));
        addGUIConfigEntry(gce);
        table = new JTable();
        table.getTableHeader().setPreferredSize(new Dimension(-1, 25));
        InternalTableModel internalTableModel = new InternalTableModel();
        table.setModel(internalTableModel);
        table.setEditingRow(0);
        table.addMouseListener(this);
        this.setPreferredSize(new Dimension(700, 350));

        TableColumn column = null;
        for (int c = 0; c < internalTableModel.getColumnCount(); c++) {
            column = table.getColumnModel().getColumn(c);
            switch (c) {

                case 0:
                    column.setPreferredWidth(50);
                    break;
                case 1:
                    column.setPreferredWidth(600);
                    break;

            }
        }

        // add(scrollPane);
        // list = new JList();

        JScrollPane scrollpane = new JScrollPane(table);
        scrollpane.setPreferredSize(new Dimension(400, 200));

        JDUtilities.addToGridBag(panel, scrollpane, GridBagConstraints.RELATIVE, GridBagConstraints.RELATIVE, GridBagConstraints.REMAINDER, 1, 1, 1, insets, GridBagConstraints.BOTH, GridBagConstraints.CENTER);

        // JDUtilities.addToGridBag(this, panel,0, 0, 1, 1, 1, 1, insets,
        // GridBagConstraints.BOTH, GridBagConstraints.WEST);
        add(panel, BorderLayout.CENTER);

    }

    /*
     * private int getSelectedIndex() { return table.getSelectedRow(); }
     */
    @Override
    public String getName() {

        return JDLocale.L("gui.config.jac.name", "jAntiCaptcha");
    }

    /*
     * private File getSelectedMethod() { int index = getSelectedIndex(); if
     * (index < 0) return null; return this.methods[index]; }
     */

    private class InternalTableModel extends AbstractTableModel {

        /**
         * 
         */
        private static final long serialVersionUID = 1155282457354673850L;

        public Class<?> getColumnClass(int columnIndex) {
            switch (columnIndex) {
                case 0:
                    return Boolean.class;
                case 1:
                    return String.class;

            }
            return String.class;
        }

        public int getColumnCount() {
            return 2;
        }

        public int getRowCount() {
            return methods.length;
        }

        public Object getValueAt(int rowIndex, int columnIndex) {

            switch (columnIndex) {
                case 0:
                    return configuration.getBooleanProperty(Configuration.PARAM_JAC_METHODS + "_" + methods[rowIndex].getName(), true);
                case 1:
                    return methods[rowIndex].getName() + " : " + (configuration.getBooleanProperty(Configuration.PARAM_JAC_METHODS + "_" + methods[rowIndex].getName(), true) ? JDLocale.L("gui.config.jac.status.auto", "Automatische Erkennung") : JDLocale.L("gui.config.jac.status.noauto", "Manuelle Eingabe"));

            }
            return null;
        }

        public String getColumnName(int column) {
            switch (column) {
                case 0:
                    return JDLocale.L("gui.config.jac.column.use", "Verwenden");
                case 1:
                    return JDLocale.L("gui.config.jac.column.method", "Methode");

            }
            return super.getColumnName(column);
        }
    }

    public void mouseEntered(MouseEvent e) {
    // TODO Auto-generated method stub

    }

    public void mouseExited(MouseEvent e) {
    // TODO Auto-generated method stub

    }

    public void mousePressed(MouseEvent e) {
    // TODO Auto-generated method stub

    }

    public void mouseReleased(MouseEvent e) {
    // TODO Auto-generated method stub

    }

    public void actionPerformed(ActionEvent e) {
        logger.info(e.getActionCommand());
        if(e.getActionCommand().equalsIgnoreCase(JDLocale.L("gui.config.captcha.btn_train", "Captcha Training starten"))){
          JDUtilities.runCommand("java", new String[] { "-jar", "-Xmx512m","JDownloader.jar","-t" }, JDUtilities.getResourceFile(".").getAbsolutePath(), 0);
            
            
        }else{
            
            JDUtilities.runCommand("java", new String[] { "-jar", "-Xmx512m","JDownloader.jar","-s" }, JDUtilities.getResourceFile(".").getAbsolutePath(), 0);
                
        }
        
    }

}
