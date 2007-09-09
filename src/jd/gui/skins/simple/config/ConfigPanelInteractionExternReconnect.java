package jd.gui.skins.simple.config;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JOptionPane;

import jd.Configuration;
import jd.controlling.interaction.ExternReconnect;
import jd.gui.UIInterface;
/**
 * Diese Klasse ist der Konfig Dialog für Extern reconnect. basiert auf ConfigPanel und der ConfigEntry Klasse
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
        ConfigEntry ce;

        ce = new ConfigEntry(ConfigEntry.TYPE_TEXTFIELD, configuration, ExternReconnect.PROPERTY_IP_REGEX, "RegEx zum auslesen der IP");
        ce.setDefaultText("\\Q<td><b>\\E([0-9.]*)\\Q</b></td>\\E");
        addConfigEntry(ce);

        add(panel, BorderLayout.CENTER);

        ce = new ConfigEntry(ConfigEntry.TYPE_TEXTFIELD, configuration, ExternReconnect.PROPERTY_IP_CHECK_SITE, "IP-Check-Seite");
        ce.setDefaultText("http://meineip.de/");
        addConfigEntry(ce);

        ce = new ConfigEntry(ConfigEntry.TYPE_TEXTFIELD, configuration, ExternReconnect.PROPERTY_IP_OFFLINE, "Offlinestring");

        addConfigEntry(ce);

        ce = new ConfigEntry(ConfigEntry.TYPE_TEXTFIELD, configuration, ExternReconnect.PROPERTY_IP_RETRIES, "Anzahl der Reconnectversuche bis zum Abbruch");
        ce.setDefaultText(0);
        addConfigEntry(ce);

        ce = new ConfigEntry(ConfigEntry.TYPE_TEXTFIELD, configuration, ExternReconnect.PROPERTY_IP_WAITCHECK, "Wartezeit bis zum IPCheck");
        ce.setDefaultText(0);
        addConfigEntry(ce);

        ce = new ConfigEntry(ConfigEntry.TYPE_TEXTFIELD, configuration, ExternReconnect.PROPERTY_RECONNECT_COMMAND, "Reconnectbefehl (Pfadangaben absolut)");

        addConfigEntry(ce);

        ce = new ConfigEntry(ConfigEntry.TYPE_BUTTON, this, "Reconnect Testen");

        addConfigEntry(ce);

    }

    @Override
    public String getName() {

        return "ExternRC";
    }
}
