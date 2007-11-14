package jd.gui.skins.simple.config;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

import javax.swing.JOptionPane;

import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.config.Configuration;
import jd.controlling.interaction.Unrar;
import jd.gui.UIInterface;
import jd.unrar.jdUnrar;
import jd.utils.JDUtilities;
/**
 * Konfigurationspanel für Unrar
 * @author DwD
 *
 */

public class ConfigPanelInteractionUnrar extends ConfigPanel implements ActionListener{



/**
     * 
     */
    private static final long serialVersionUID = -1543456288909278519L;
    /**
     * Instanz zum speichern der parameter
     */
private Unrar unrar;
    ConfigPanelInteractionUnrar(Configuration configuration, UIInterface uiinterface, Unrar unrar) {
        super(uiinterface);
        this.unrar=unrar;
        initPanel();

        load();
    }

    public void save() {

        this.saveConfigEntries();
    }

    public void load() {

        this.loadConfigEntries();
    }


    @Override
    public void initPanel() {
        GUIConfigEntry ce;        
        ce = new GUIConfigEntry(new ConfigEntry(ConfigContainer.TYPE_COMBOBOX, unrar, Unrar.PROPERTY_MODE, new String[] {"Erst die Dateien vom Ordner des letzten Packetes entpacken und dann die im Downloadordner", "Alle Dateien im Downloadordner entpacken", "Die Dateien im Ordner des letzten Packets entpacken (mit PacketPasswort)"}, "Entpackmodus: ").setDefaultValue("Erst die Dateien vom Ordner des letzten Packetes entpacken und dann die im Downloadordner"));
        addGUIConfigEntry(ce); 
        ce = new GUIConfigEntry(new ConfigEntry(ConfigContainer.TYPE_BROWSEFILE, unrar, Unrar.PROPERTY_UNRARCOMMAND, "Unrar Befehl: ").setDefaultValue(new jdUnrar(JDUtilities.getConfiguration().getStringProperty(Configuration.PARAM_DOWNLOAD_DIRECTORY)).getUnrarCommand()));
        addGUIConfigEntry(ce);       
        ce = new GUIConfigEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, unrar, Unrar.PROPERTY_AUTODELETE, "Bei erfolgreichem Entpacken automatisch löschen: ").setDefaultValue(true));
        addGUIConfigEntry(ce); 
        ce = new GUIConfigEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, unrar, Unrar.PROPERTY_OVERWRITE_FILES, "Dateien automatisch überschreiben: ").setDefaultValue(false));
        addGUIConfigEntry(ce); 
        ce = new GUIConfigEntry(new ConfigEntry(ConfigContainer.TYPE_SPINNER, unrar, Unrar.PROPERTY_MAX_FILESIZE, "Maximale Dateigröße für die Passwortsuche in MB: ",0,500).setDefaultValue(2));
        addGUIConfigEntry(ce); 
        ce = new GUIConfigEntry(new ConfigEntry(ConfigContainer.TYPE_BUTTON, this, "Passwort Hinzufügen"));
        addGUIConfigEntry(ce);
        add(panel, BorderLayout.CENTER);

    }

    @Override
    public String getName() {

        return "Unrar";
    }

    public void actionPerformed(ActionEvent e) {
        System.out.println(e);
        String selected = (String) JOptionPane.showInputDialog(this, "geben sie ein Passwort oder den Pfad zur Passwortliste (txt) ein"+System.getProperty("line.separator")+"mit {\"Password1\",\"Password2\",\"Password3\"} lassen sich mehrere Passwörter hinzufügen", "Passwort Hinzufügen", JOptionPane.INFORMATION_MESSAGE, null, null, "");
        if(selected!=null)
        {
           jdUnrar unrar = new jdUnrar();
           File file = new File(selected);
           if(file.isFile())
               unrar.addToPasswordlist(file);
           else
               unrar.addToPasswordlist(selected);
        }
    }
}
