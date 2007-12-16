package jd.gui.skins.simple.config;

import java.awt.ComponentOrientation;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Vector;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.JTabbedPane;
import javax.swing.SwingConstants;

import jd.config.Configuration;
import jd.gui.UIInterface;
import jd.utils.JDLocale;
import jd.utils.JDUtilities;
/**
 * Diese Klasse ist das Hauptfemster der Konfiguration. Sie verwaltet die Tabpane.
 * @author coalado/astaldo
 *
 */
public class ConfigurationDialog extends JDialog implements ActionListener {
    /**
     * serialVersionUID
     */
    private static final long                     serialVersionUID = 4046836223202290819L;

    private Configuration                         configuration;

    private JTabbedPane                           tabbedPane;

    private JButton                               btnSave;

    private JButton                               btnCancel;

    private boolean                               configChanged    = false;

    @SuppressWarnings("unused")
    private UIInterface                           uiinterface;

    private Vector<ConfigPanel>                   configPanels     = new Vector<ConfigPanel>();

    private JCheckBox chbExpert;

    private ConfigurationDialog(JFrame parent, UIInterface uiinterface) {
        super(parent);
        this.uiinterface = uiinterface;
        setTitle(JDLocale.L("gui.config.title","Konfiguration"));
        setModal(true);
        setLayout(new GridBagLayout());
        configuration = JDUtilities.getConfiguration();
        tabbedPane = new JTabbedPane();
        tabbedPane.setComponentOrientation(ComponentOrientation.LEFT_TO_RIGHT);
        tabbedPane.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
        tabbedPane.setTabPlacement(JTabbedPane.LEFT);
        this.addConfigPanel(new ConfigPanelGeneral(configuration, uiinterface),JDLocale.L("gui.images.config.home","home"),JDLocale.L("gui.config.tabLables.general","General settings"));
        this.addConfigPanel(new ConfigPanelDownload(configuration, uiinterface),JDLocale.L("gui.images.config.network_local","network_local"),JDLocale.L("gui.config.tabLables.download","Download/Network settings"));
        this.addConfigPanel(new ConfigPanelReconnect(configuration, uiinterface),JDLocale.L("gui.images.config.reboot","reboot"),JDLocale.L("gui.config.tabLables.reconnect","Reconnect settings"));
        this.addConfigPanel(new ConfigPanelCaptcha(configuration, uiinterface),JDLocale.L("gui.images.config.ocr","ocr"),JDLocale.L("gui.config.tabLables.jac","OCR Captcha settings"));
        this.addConfigPanel(new ConfigPanelUnrar(configuration, uiinterface),JDLocale.L("gui.images.config.package","package"),JDLocale.L("gui.config.tabLables.unrar","Archiv extract settings"));  
        this.addConfigPanel(new ConfigPanelInfoFileWriter(configuration, uiinterface),JDLocale.L("gui.images.config.load","load"),JDLocale.L("gui.config.tabLables.infoFileWriter","'Info File Writer' settings"));
        this.addConfigPanel(new ConfigPanelEventmanager(configuration, uiinterface),JDLocale.L("gui.images.config.switch","switch"),JDLocale.L("gui.config.tabLables.eventManager","Eventmanager"));
       
        this.addConfigPanel(new ConfigPanelPluginForHost(configuration, uiinterface),JDLocale.L("gui.images.config.star","star"),JDLocale.L("gui.config.tabLables.hostPlugin","Host Plugin settings"));
        this.addConfigPanel(new ConfigPanelPluginForDecrypt(configuration, uiinterface),JDLocale.L("gui.images.config.tip","tip"),JDLocale.L("gui.config.tabLables.decryptPlugin","Decrypter Plugin settings"));
        this.addConfigPanel(new ConfigPanelPluginForSearch(configuration, uiinterface),JDLocale.L("gui.images.config.find","find"),JDLocale.L("gui.config.tabLables.searchPlugin","Search Plugin settings"));
        this.addConfigPanel(new ConfigPanelPluginsOptional(configuration, uiinterface),JDLocale.L("gui.images.config.edit_redo","edit_redo"),JDLocale.L("gui.config.tabLables.optionalPlugin","Optional Plugin settings"));
        this.addConfigPanel(new ConfigPanelPluginForContainer(configuration, uiinterface),JDLocale.L("gui.images.config.database","database"),JDLocale.L("gui.config.tabLables.containerPlugin","Link-Container settings"));
        
        btnSave = new JButton(JDLocale.L("gui.config.btn_save","Speichern"));
        btnSave.addActionListener(this);
        btnCancel = new JButton(JDLocale.L("gui.config.btn_cancel","Abbrechen"));
        btnCancel.addActionListener(this);
        chbExpert= new JCheckBox(JDLocale.L("gui.config.cbo_expert","Experten Ansicht"));
        chbExpert.setSelected(configuration.getBooleanProperty(Configuration.PARAM_USE_EXPERT_VIEW, false));
        chbExpert.addActionListener(this);
        Insets insets = new Insets(5, 5, 5, 5);

        JDUtilities.addToGridBag(this, tabbedPane,  0, 0, 3, 3, 1, 1, null, GridBagConstraints.BOTH, GridBagConstraints.NORTHWEST);
        JDUtilities.addToGridBag(this, chbExpert,  0, 4, 1, 1, 1, -1, insets, GridBagConstraints.HORIZONTAL, GridBagConstraints.SOUTHEAST);
        
        JDUtilities.addToGridBag(this, btnSave,  1, 4, 1, 1, 0, -1, insets, GridBagConstraints.NONE, GridBagConstraints.SOUTHEAST);
        JDUtilities.addToGridBag(this, btnCancel, 2,4,1, 1, 0, -1, insets, GridBagConstraints.NONE, GridBagConstraints.SOUTHEAST);

        pack();
    }

    /**
     * @author coalado 
     * Fügt einen neuen ConfigTab hinzu
     * @param configPanel
     */
    private void addConfigPanel(ConfigPanel configPanel,String img,String title ) {
        this.configPanels.add(configPanel);
        JPanel p = new JPanel(new GridBagLayout());
        
        JDUtilities.addToGridBag(p, new JLabel(title,new ImageIcon(JDUtilities.getImage(img)),SwingConstants.LEFT), GridBagConstraints.RELATIVE, GridBagConstraints.RELATIVE, GridBagConstraints.REMAINDER, 1, 1, 0, null, GridBagConstraints.HORIZONTAL, GridBagConstraints.FIRST_LINE_START);
       JDUtilities.addToGridBag(p, new JSeparator(), GridBagConstraints.RELATIVE, GridBagConstraints.RELATIVE, GridBagConstraints.REMAINDER, 1, 1, 0, null, GridBagConstraints.HORIZONTAL, GridBagConstraints.NORTH);
        JDUtilities.addToGridBag(p, configPanel, GridBagConstraints.RELATIVE, GridBagConstraints.RELATIVE, GridBagConstraints.REMAINDER, 1, 1, 1, null, GridBagConstraints.BOTH, GridBagConstraints.NORTH);
   
        tabbedPane.addTab(configPanel.getName(), p);

    }
/**
 * Zeigt die Konfiguration an
 * @param frame
 * @param uiinterface
 * @return
 */
    public static boolean showConfig(JFrame frame, UIInterface uiinterface) {
        ConfigurationDialog c = new ConfigurationDialog(frame, uiinterface);
        c.setLocation(JDUtilities.getCenterOfComponent(frame, c));
        c.setVisible(true);
        return c.configChanged;
    }

    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == btnSave) {
            for (int i = 0; i < configPanels.size(); i++) {
                configPanels.elementAt(i).save();
            }
            configChanged = true;
            JDUtilities.setConfiguration(configuration);

//Entgültig gespeichert wird über ein fireUIEvent(new UIEvent(this, UIEvent.UI_SAVE_CONFIG)); event in simpleGui
           
        }
     if(e.getSource()==this.chbExpert){
         configuration.setProperty(Configuration.PARAM_USE_EXPERT_VIEW, chbExpert.isSelected());
         JDUtilities.saveConfig();
         if(JDUtilities.getController().getUiInterface().showConfirmDialog(JDLocale.L("sys.confirm.doRestart","Diese Einstellung benötigt einen JD-Neustart. Neustart jetzt durchführen?"))){
             JDUtilities.restartJD();
         }
         return;
     }
         
         
         setVisible(false);
    }

}
