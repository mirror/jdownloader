package jd.gui.skins.simple.config;

import java.awt.ComponentOrientation;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.lang.reflect.Constructor;
import java.util.Vector;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTabbedPane;
import javax.swing.SwingConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import jd.config.Configuration;
import jd.config.SubConfiguration;
import jd.gui.UIInterface;
import jd.gui.skins.simple.LocationListener;
import jd.gui.skins.simple.SimpleGUI;
import jd.utils.JDLocale;
import jd.utils.JDTheme;
import jd.utils.JDUtilities;

/**
 * Diese Klasse ist das Hauptfemster der Konfiguration. Sie verwaltet die
 * Tabpane.
 * 
 * @author coalado/astaldo
 * 
 */
public class ConfigurationDialog extends JDialog implements ActionListener, ChangeListener {
    /**
     * serialVersionUID
     */
    private static final long         serialVersionUID = 4046836223202290819L;

    private Configuration             configuration;

    private JTabbedPane               tabbedPane;

    private JButton                   btnSave;

    private JButton                   btnCancel;

    private boolean                   configChanged    = false;

    @SuppressWarnings("unused")
    private UIInterface               uiinterface;

    private Vector<ConfigPanel>       configPanels     = new Vector<ConfigPanel>();

    public static ConfigurationDialog DIALOG;

    public static Frame               PARENTFRAME      = null;

    private JCheckBox                 chbExpert;

    private Vector<Class>             configClasses    = new Vector<Class>();

    private Vector<JPanel>            containerPanels  = new Vector<JPanel>();

    private SubConfiguration          guiConfig;

    private JButton btnRestart;

    private ConfigurationDialog(JFrame parent, UIInterface uiinterface) {
        super(parent);
        DIALOG = this;
        this.guiConfig = JDUtilities.getSubConfig(SimpleGUI.GUICONFIGNAME);
        PARENTFRAME = parent;
        this.uiinterface = uiinterface;
        setTitle(JDLocale.L("gui.config.title", "Konfiguration"));
        setModal(true);
        setLayout(new GridBagLayout());
        this.setName("CONFIGDIALOG");
      



        configuration = JDUtilities.getConfiguration();
        tabbedPane = new JTabbedPane();
        tabbedPane.setComponentOrientation(ComponentOrientation.LEFT_TO_RIGHT);
        tabbedPane.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
        tabbedPane.setTabPlacement(JTabbedPane.LEFT);
        tabbedPane.addChangeListener(this);
        tabbedPane.setPreferredSize(new Dimension(800, 500));
        this.addConfigPanel(ConfigPanelGeneral.class, JDTheme.I("gui.images.config.home"), JDLocale.L("gui.config.tabLables.general", "General settings"));

        this.addConfigPanel(ConfigPanelDownload.class, JDTheme.I("gui.images.config.network_local"), JDLocale.L("gui.config.tabLables.download", "Download/Network settings"));
        this.addConfigPanel(ConfigPanelGUI.class, JDTheme.I("gui.images.config.home"), JDLocale.L("gui.config.tabLables.gui", "Benutzeroberfläche"));

        this.addConfigPanel(ConfigPanelReconnect.class, JDTheme.I("gui.images.config.reboot"), JDLocale.L("gui.config.tabLables.reconnect", "Reconnect settings"));
        if (guiConfig.getBooleanProperty(SimpleGUI.PARAM_USE_EXPERT_VIEW, false)) this.addConfigPanel(ConfigPanelCaptcha.class, JDTheme.I("gui.images.config.ocr", "ocr"), JDLocale.L("gui.config.tabLables.jac", "OCR Captcha settings"));
        this.addConfigPanel(ConfigPanelUnrar.class, JDTheme.I("gui.images.config.package"), JDLocale.L("gui.config.tabLables.unrar", "Archiv extract settings"));
        if (guiConfig.getBooleanProperty(SimpleGUI.PARAM_USE_EXPERT_VIEW, false)) this.addConfigPanel(ConfigPanelInfoFileWriter.class, JDTheme.I("gui.images.config.load", "load"), JDLocale.L("gui.config.tabLables.infoFileWriter", "'Info File Writer' settings"));
        if (guiConfig.getBooleanProperty(SimpleGUI.PARAM_USE_EXPERT_VIEW, false)) this.addConfigPanel(ConfigPanelEventmanager.class, JDTheme.I("gui.images.config.switch", "switch"), JDLocale.L("gui.config.tabLables.eventManager", "Eventmanager"));

        this.addConfigPanel(ConfigPanelPluginForHost.class, JDTheme.I("gui.images.config.star"), JDLocale.L("gui.config.tabLables.hostPlugin", "Host Plugin settings"));
        this.addConfigPanel(ConfigPanelPluginForDecrypt.class, JDTheme.I("gui.images.config.tip"), JDLocale.L("gui.config.tabLables.decryptPlugin", "Decrypter Plugin settings"));
//        if (guiConfig.getBooleanProperty(SimpleGUI.PARAM_USE_EXPERT_VIEW, false)) this.addConfigPanel(ConfigPanelPluginForSearch.class, JDTheme.I("gui.images.config.find"), JDLocale.L("gui.config.tabLables.searchPlugin", "Search Plugin settings"));
        if (guiConfig.getBooleanProperty(SimpleGUI.PARAM_USE_EXPERT_VIEW, false)) this.addConfigPanel(ConfigPanelPluginsOptional.class, JDTheme.I("gui.images.config.edit_redo"), JDLocale.L("gui.config.tabLables.optionalPlugin", "Optional Plugin settings"));
        if (guiConfig.getBooleanProperty(SimpleGUI.PARAM_USE_EXPERT_VIEW, false)) this.addConfigPanel(ConfigPanelPluginForContainer.class, JDTheme.I("gui.images.config.database"), JDLocale.L("gui.config.tabLables.containerPlugin", "Link-Container settings"));
     this.addConfigPanel(ConfigPanelLinks.class, JDTheme.I("gui.images.config.tip"), JDLocale.L("gui.config.tabLables.links", "Wichtige Links"));

        btnSave = new JButton(JDLocale.L("gui.config.btn_save", "Speichern"));
        btnSave.addActionListener(this);
        btnCancel = new JButton(JDLocale.L("gui.config.btn_cancel", "Abbrechen"));
        btnCancel.addActionListener(this);
        btnRestart = new JButton(JDLocale.L("gui.config.btn_restart", "Speichern und neu starten"));
        btnRestart.addActionListener(this);
        chbExpert = new JCheckBox(JDLocale.L("gui.config.cbo_expert", "Experten Ansicht"));
        chbExpert.setSelected(guiConfig.getBooleanProperty(SimpleGUI.PARAM_USE_EXPERT_VIEW, false));
        chbExpert.addActionListener(this);
        Insets insets = new Insets(5, 5, 5, 5);

        JDUtilities.addToGridBag(this, tabbedPane, 0, 0, 3, 3, 1, 1, null, GridBagConstraints.BOTH, GridBagConstraints.NORTHWEST);
        JDUtilities.addToGridBag(this, chbExpert, 0, 4, 1, 1, 1, -1, insets, GridBagConstraints.HORIZONTAL, GridBagConstraints.SOUTHEAST);
        JDUtilities.addToGridBag(this, btnRestart, 1, 4, 1, 1, 0, -1, insets, GridBagConstraints.NONE, GridBagConstraints.SOUTHEAST);
        
        JDUtilities.addToGridBag(this, btnSave, 2, 4, 1, 1, 0, -1, insets, GridBagConstraints.NONE, GridBagConstraints.SOUTHEAST);
        JDUtilities.addToGridBag(this, btnCancel, 3, 4, 1, 1, 0, -1, insets, GridBagConstraints.NONE, GridBagConstraints.SOUTHEAST);

        paintPanel(0);
        pack();
        
          JDUtilities.getLogger().info("GETLOCATION");
      this.setLocation(SimpleGUI.getLastLocation(parent, null, this));
      LocationListener list = new LocationListener();
     if(SimpleGUI.getLastDimension(this, null)!=null) this.setSize(SimpleGUI.getLastDimension(this, null));
      this.addComponentListener(list);
        this.setVisible(true);
    }

    private void paintPanel(int i) {

        if (i < configPanels.size() && configPanels.get(i) != null) {
            return;
        }
        Class class1 = configClasses.get(i);
        ConfigPanel panel = initSubPanel(class1);
        configPanels.remove(i);
        configPanels.add(i, panel);
        JPanel container = containerPanels.get(i);
        JDUtilities.addToGridBag(container, new JScrollPane(panel), GridBagConstraints.RELATIVE, GridBagConstraints.RELATIVE, GridBagConstraints.REMAINDER, 1, 1, 1, null, GridBagConstraints.BOTH, GridBagConstraints.NORTH);

    }

    @SuppressWarnings("unchecked")
	public ConfigPanel initSubPanel(Class class1) {
        try {

            Class[] classes = new Class[] { Configuration.class, UIInterface.class };
            Constructor con = class1.getConstructor(classes);
            return (ConfigPanel) con.newInstance(new Object[] { configuration, uiinterface });
        }

        catch (Exception e) {

            e.printStackTrace();
        }
        return null;
    }

    /**
     * @author coalado Fügt einen neuen ConfigTab hinzu
     * @param configPanel
     */
    private void addConfigPanel(Class configPanelClass, String img, String title) {

        this.configClasses.add(configPanelClass);

        JPanel p = new JPanel(new GridBagLayout());
        this.containerPanels.add(p);
        configPanels.add(null);
        JDUtilities.addToGridBag(p, new JLabel(title, new ImageIcon(JDUtilities.getImage(img)), SwingConstants.LEFT), GridBagConstraints.RELATIVE, GridBagConstraints.RELATIVE, GridBagConstraints.REMAINDER, 1, 1, 0, null, GridBagConstraints.HORIZONTAL, GridBagConstraints.FIRST_LINE_START);
        JDUtilities.addToGridBag(p, new JSeparator(), GridBagConstraints.RELATIVE, GridBagConstraints.RELATIVE, GridBagConstraints.REMAINDER, 1, 1, 0, null, GridBagConstraints.HORIZONTAL, GridBagConstraints.NORTH);

        tabbedPane.addTab(title, p);

    }

    /**
     * Zeigt die Konfiguration an
     * 
     * @param frame
     * @param uiinterface
     * @return
     */
    public static boolean showConfig(JFrame frame, UIInterface uiinterface) {
        ConfigurationDialog c = new ConfigurationDialog(frame, uiinterface);
        JDUtilities.getLogger().info("END");
        c.setVisible(false);
        c.dispose();
        return c.configChanged;
    }

    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == btnRestart) {
            for (int i = 0; i < configPanels.size(); i++) {
                if (configPanels.elementAt(i) != null) configPanels.elementAt(i).save();
            }
            configChanged = true;
            JDUtilities.setConfiguration(configuration);
            JDUtilities.saveConfig();
            JDUtilities.restartJD();
    

        }
        
        if (e.getSource() == btnSave) {
            for (int i = 0; i < configPanels.size(); i++) {
                if (configPanels.elementAt(i) != null) configPanels.elementAt(i).save();
            }
            configChanged = true;
            JDUtilities.setConfiguration(configuration);

            // Entgültig gespeichert wird über ein fireUIEvent(new UIEvent(this,
            // UIEvent.UI_SAVE_CONFIG)); event in simpleGui

        }
        if (e.getSource() == this.chbExpert) {
            guiConfig.setProperty(SimpleGUI.PARAM_USE_EXPERT_VIEW, chbExpert.isSelected());
            JDUtilities.saveConfig();
            this.setVisible(false);
            this.dispose();

            return;
        }
        this.dispose();

        setVisible(false);
    }

    public void stateChanged(ChangeEvent e) {

        int index = tabbedPane.getSelectedIndex();
        paintPanel(index);
        validate();
        pack();

    }

}
