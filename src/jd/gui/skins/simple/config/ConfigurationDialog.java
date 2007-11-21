package jd.gui.skins.simple.config;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Vector;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JTabbedPane;

import jd.config.Configuration;
import jd.gui.UIInterface;
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

    private ConfigurationDialog(JFrame parent, UIInterface uiinterface) {
        super(parent);
        this.uiinterface = uiinterface;
        setTitle(JDUtilities.getResourceString("title.config"));
        setModal(true);
        setLayout(new GridBagLayout());
        configuration = JDUtilities.getConfiguration();
        tabbedPane = new JTabbedPane();
        this.addConfigPanel(new ConfigPanelGeneral(configuration, uiinterface));
        this.addConfigPanel(new ConfigPanelDownload(configuration, uiinterface));
        this.addConfigPanel(new ConfigPanelEventmanager(configuration, uiinterface));
       
        this.addConfigPanel(new ConfigPanelPluginForHost(configuration, uiinterface));
        this.addConfigPanel(new ConfigPanelPluginForDecrypt(configuration, uiinterface));
        this.addConfigPanel(new ConfigPanelPluginForSearch(configuration, uiinterface));
        this.addConfigPanel(new ConfigPanelPluginsOptional(configuration, uiinterface));
        this.addConfigPanel(new ConfigPanelPluginForContainer(configuration, uiinterface));
        
        btnSave = new JButton("Speichern");
        btnSave.addActionListener(this);
        btnCancel = new JButton("Abbrechen");
        btnCancel.addActionListener(this);

        Insets insets = new Insets(5, 5, 5, 5);

        JDUtilities.addToGridBag(this, tabbedPane, 0, 0, 2, 1, 1, 1, null, GridBagConstraints.BOTH, GridBagConstraints.CENTER);
        JDUtilities.addToGridBag(this, btnSave, 0, 1, 1, 1, 1, 0, insets, GridBagConstraints.NONE, GridBagConstraints.CENTER);
        JDUtilities.addToGridBag(this, btnCancel, 1, 1, 1, 1, 1, 0, insets, GridBagConstraints.NONE, GridBagConstraints.CENTER);

        pack();
    }

    /**
     * @author coalado 
     * Fügt einen neuen ConfigTab hinzu
     * @param configPanel
     */
    private void addConfigPanel(ConfigPanel configPanel) {
        this.configPanels.add(configPanel);
        tabbedPane.addTab(configPanel.getName(), configPanel);

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
        setVisible(false);
    }

}
