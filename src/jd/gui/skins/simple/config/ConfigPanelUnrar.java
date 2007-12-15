package jd.gui.skins.simple.config;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.config.Configuration;
import jd.controlling.interaction.Unrar;
import jd.gui.UIInterface;
import jd.gui.skins.simple.SimpleGUI;
import jd.unrar.JUnrar;
import jd.utils.JDUtilities;
/**
 * Konfigurationspanel für Unrar
 * 
 * @author DwD
 * 
 */

public class ConfigPanelUnrar extends ConfigPanel implements ActionListener {

    /**
     * 
     */
    private static final long serialVersionUID = -1543456288909278519L;
    /**
     * Instanz zum speichern der parameter
     */
    private Unrar unrar;
    private Configuration configuration;
    ConfigPanelUnrar(Configuration configuration, UIInterface uiinterface) {
        super(uiinterface);
        this.unrar = Unrar.getInstance();
        initPanel();
this.configuration=configuration;
        load();
    }

    public void save() {

    
        this.saveConfigEntries();
      
            configuration.setProperty(Configuration.PARAM_UNRAR_INSTANCE, unrar);
     
    }

    public void load() {
        this.loadConfigEntries();
    }

    @Override
    public void initPanel() {
        GUIConfigEntry ce;
        configuration=JDUtilities.getConfiguration();
        String unrarcmd=JDUtilities.getConfiguration().getStringProperty("GUNRARCOMMAND");
        if(unrarcmd==null)
        {
        unrarcmd=new JUnrar(false).getUnrarCommand();
        if(unrarcmd==null)
            configuration.setProperty("GUNRARCOMMAND", "NOT FOUND");
        else
            configuration.setProperty("GUNRARCOMMAND", unrarcmd);
        }
        else if(unrarcmd.matches("NOT FOUND"))
            unrarcmd=null;
        
//        ce = new GUIConfigEntry(new ConfigEntry(ConfigContainer.TYPE_COMBOBOX, configuration, Unrar.PROPERTY_ENABLED_TYPE,new String[] { Unrar.ENABLED_TYPE_ALWAYS,Unrar.ENABLED_TYPE_LINKGRABBER,Unrar.ENABLED_TYPE_NEVER },"Unrar aktivieren:").setDefaultValue(Unrar.ENABLED_TYPE_LINKGRABBER));
        ce = new GUIConfigEntry(new ConfigEntry(ConfigContainer.TYPE_COMBOBOX, configuration, Unrar.PROPERTY_ENABLED_TYPE, new String[] { Unrar.ENABLED_TYPE_ALWAYS,Unrar.ENABLED_TYPE_LINKGRABBER,Unrar.ENABLED_TYPE_NEVER }, JDUtilities.getResourceString("unrar.config.activate")).setDefaultValue(Unrar.ENABLED_TYPE_LINKGRABBER));
        addGUIConfigEntry(ce);
        ce = new GUIConfigEntry(new ConfigEntry(ConfigContainer.TYPE_BROWSEFILE, configuration, Unrar.PROPERTY_UNRARCOMMAND, JDUtilities.getResourceString("unrar.config.cmd")).setDefaultValue(unrarcmd));
        addGUIConfigEntry(ce);
        ce = new GUIConfigEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, configuration, Unrar.PROPERTY_AUTODELETE, JDUtilities.getResourceString("unrar.config.delete")).setDefaultValue(true));
        addGUIConfigEntry(ce);
        ce = new GUIConfigEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, configuration, Unrar.PROPERTY_OVERWRITE_FILES, JDUtilities.getResourceString("unrar.config.overwrite")).setDefaultValue(false));
addGUIConfigEntry(ce);        
ce = new GUIConfigEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, configuration,  Unrar.PROPERTY_WAIT_FOR_TERMINATION, "Erst nach dem Entpacken mit dem Download fortfahren").setDefaultValue(false));
        
        addGUIConfigEntry(ce);
        ce = new GUIConfigEntry(new ConfigEntry(ConfigContainer.TYPE_BUTTON, this, JDUtilities.getResourceString("action.passwordlist.name")));
        addGUIConfigEntry(ce);
        add(panel, BorderLayout.NORTH);

    }

    @Override
    public String getName() {

        return JDUtilities.getResourceString("unrar.name");
    }

    public void actionPerformed(ActionEvent e) {
       new jdUnrarPasswordListDialog(((SimpleGUI) this.uiinterface).getFrame()).setVisible(true);
    }
}

