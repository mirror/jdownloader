package jd.gui.skins.simple.config;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JOptionPane;

import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.config.Configuration;
import jd.controlling.interaction.ExternReconnect;
import jd.gui.UIInterface;
/**
 * Diese Klasse ist der Konfig Dialog für Extern reconnect. basiert auf ConfigPanel und der GUIConfigEntry Klasse
 * @author coalado
 *
 */
public class ConfigPanelInteractionExternReconnect extends ConfigPanel implements ActionListener {

    /**
     * 
     */
    private static final long serialVersionUID = 4630549888740242517L;

    ConfigPanelInteractionExternReconnect(Configuration configuration, UIInterface uiinterface) {
        super(configuration, uiinterface);
        initPanel();

        load();
    }

    public void save() {

        this.saveConfigEntries();
    }

    public void load() {

        this.loadConfigEntries();
    }

    public void actionPerformed(ActionEvent e) {

        int button = JOptionPane.showConfirmDialog(this, "Sollen die Daten gespeichert und der Reconnect durchgeführt werden?", "Reconnect-Test", JOptionPane.YES_NO_OPTION);
        if (button == JOptionPane.YES_OPTION) {
            save();
            ExternReconnect er = new ExternReconnect();
            boolean success = er.interact(this);
            if (success)
                JOptionPane.showMessageDialog(this, "Reconnect erfolgreich");
            else
                JOptionPane.showMessageDialog(this, "Reconnect fehlgeschlagen");
        }

    }

    @Override
    public void initPanel() {
        GUIConfigEntry ce;

        ce = new GUIConfigEntry(new ConfigEntry(ConfigContainer.TYPE_TEXTFIELD, configuration, ExternReconnect.PROPERTY_IP_REGEX, "RegEx zum auslesen der IP").setDefaultValue("\\Q<td><b>\\E([0-9.]*)\\Q</b></td>\\E"));
   
        addGUIConfigEntry(ce);

        add(panel, BorderLayout.CENTER);

        ce = new GUIConfigEntry(new ConfigEntry(ConfigContainer.TYPE_TEXTFIELD, configuration, ExternReconnect.PROPERTY_IP_CHECK_SITE, "IP-Check-Seite").setDefaultValue("http://meineip.de/"));
      
        addGUIConfigEntry(ce);

        ce = new GUIConfigEntry(new ConfigEntry(ConfigContainer.TYPE_TEXTFIELD, configuration, ExternReconnect.PROPERTY_IP_OFFLINE, "Offlinestring"));

        addGUIConfigEntry(ce);

        ce = new GUIConfigEntry(new ConfigEntry(ConfigContainer.TYPE_TEXTFIELD, configuration, ExternReconnect.PROPERTY_IP_RETRIES, "Anzahl der Reconnectversuche bis zum Abbruch").setDefaultValue(0));
   
        addGUIConfigEntry(ce);
        
        ce = new GUIConfigEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, configuration, ExternReconnect.PROPERTY_IP_WAIT_FOR_RETURN, "Warten bis der Befehl beendet wurde").setDefaultValue(true));
        
        addGUIConfigEntry(ce);
        
        
        ce = new GUIConfigEntry(new ConfigEntry(ConfigContainer.TYPE_TEXTFIELD, configuration, ExternReconnect.PROPERTY_IP_WAITCHECK, "Wartezeit bis zum IPCheck").setDefaultValue(0));
      
        addGUIConfigEntry(ce);

        ce = new GUIConfigEntry(new ConfigEntry(ConfigContainer.TYPE_BROWSEFILE, configuration, ExternReconnect.PROPERTY_RECONNECT_COMMAND, "Reconnectbefehl (Pfadangaben absolut)"));

        addGUIConfigEntry(ce);

        ce = new GUIConfigEntry(new ConfigEntry(ConfigContainer.TYPE_BUTTON, this, "Reconnect Testen"));

        addGUIConfigEntry(ce);

    }

    @Override
    public String getName() {

        return "ExternRC";
    }
}
