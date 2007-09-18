package jd.gui.skins.simple.config;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JOptionPane;

import jd.Configuration;
import jd.controlling.interaction.ExternExecute;
import jd.controlling.interaction.ExternReconnect;
import jd.gui.UIInterface;
import jd.plugins.PluginConfig;
/**
 * Konfigurationspanel für die INteraction ExternExecute
 * @author coalado
 *
 */

public class ConfigPanelInteractionExternCommand extends ConfigPanel implements ActionListener {



/**
     * 
     */
    private static final long serialVersionUID = -1543456288909278519L;
    /**
     * Instanz zum speichern der parameter
     */
private ExternExecute externExecute;
    ConfigPanelInteractionExternCommand(Configuration configuration, UIInterface uiinterface, ExternExecute externExecute) {
        super(configuration, uiinterface);
        this.externExecute=externExecute;
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

        int button = JOptionPane.showConfirmDialog(this, "Sollen die Daten gespeichert und der Befehl ausgeführt werden?", "Befehl-Test", JOptionPane.YES_NO_OPTION);
        if (button == JOptionPane.YES_OPTION) {
            save();
           
            boolean success = externExecute.interact(this);
            if (success)
                JOptionPane.showMessageDialog(this, "erfolgreich");
            else
                JOptionPane.showMessageDialog(this, "fehlgeschlagen");
        }

    }

    @Override
    public void initPanel() {
        ConfigEntry ce;        

        ce = new ConfigEntry(PluginConfig.TYPE_BROWSEFILE, externExecute, ExternExecute.PROPERTY_COMMAND, "Befehl (%LASTFILE,%CAPTCHAIMAGE)");

        ce.setDefaultText("");
        addConfigEntry(ce);       

        ce = new ConfigEntry(PluginConfig.TYPE_CHECKBOX, externExecute, ExternExecute.PROPERTY_WAIT_TERMINATION, "Warten bis der Befehl beendet wurde?");
        ce.setDefaultText(true);
        addConfigEntry(ce); 
   
   

        ce = new ConfigEntry(PluginConfig.TYPE_BUTTON, this, "Testen");

        addConfigEntry(ce);
        add(panel, BorderLayout.CENTER);

    }

    @Override
    public String getName() {

        return "ExternExecute";
    }
}
