package jd.gui.skins.simple.config;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JOptionPane;

import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.config.Configuration;
import jd.controlling.interaction.ExternReconnect;
import jd.controlling.interaction.Interaction;
import jd.gui.UIInterface;
/**
 * Diese Klasse ist der Konfig Dialog für Extern reconnect. basiert auf ConfigPanel und der GUIConfigEntry Klasse
 * @author coalado
 *
 */
public class ConfigPanelInteractionExternReconnect extends ConfigPanelInteraction implements ActionListener {

    /**
     * 
     */
    private static final long serialVersionUID = 4630549888740242517L;


    ConfigPanelInteractionExternReconnect(Configuration configuration, UIInterface uiinterface,Interaction interaction) {
        super( uiinterface,interaction);
 
      
      
    }


    public void actionPerformed(ActionEvent e) {

        int button = JOptionPane.showConfirmDialog(this, "Sollen die Daten gespeichert und der Reconnect durchgeführt werden?", "Reconnect-Test", JOptionPane.YES_NO_OPTION);
        if (button == JOptionPane.YES_OPTION) {
            save();
           
            boolean success = interaction.interact(this);
            if (success)
                JOptionPane.showMessageDialog(this, "Reconnect erfolgreich");
            else
                JOptionPane.showMessageDialog(this, "Reconnect fehlgeschlagen");
        }

    }

    @Override
    public void initPanel() {
  super.initPanel();
        GUIConfigEntry ce = new GUIConfigEntry(new ConfigEntry(ConfigContainer.TYPE_BUTTON, this, "Reconnect Testen"));

        addGUIConfigEntry(ce);

    }

    @Override
    public String getName() {

        return "ExternRC";
    }
}
