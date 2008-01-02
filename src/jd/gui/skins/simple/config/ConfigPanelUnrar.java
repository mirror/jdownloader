package jd.gui.skins.simple.config;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.MalformedURLException;
import java.net.URL;

import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.config.Configuration;
import jd.controlling.interaction.Unrar;
import jd.gui.UIInterface;
import jd.gui.skins.simple.SimpleGUI;
import jd.gui.skins.simple.Link.JLinkButton;
import jd.unrar.JUnrar;
import jd.utils.JDLocale;
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
    public   ConfigPanelUnrar(Configuration configuration, UIInterface uiinterface) {
        super(uiinterface);
        this.unrar = Unrar.getInstance();
        initPanel();
        this.configuration = configuration;
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
        configuration = JDUtilities.getConfiguration();
        String unrarcmd = JDUtilities.getConfiguration().getStringProperty("GUNRARCOMMAND");
        if (unrarcmd == null) {
            unrarcmd = new JUnrar(false).getUnrarCommand();
            if (unrarcmd == null)
                configuration.setProperty("GUNRARCOMMAND", "NOT FOUND");
            else
                configuration.setProperty("GUNRARCOMMAND", unrarcmd);
            JDUtilities.saveConfig();
        } else if (unrarcmd.matches("NOT FOUND"))
            unrarcmd = null;

        // ce = new GUIConfigEntry(new
        // ConfigEntry(ConfigContainer.TYPE_COMBOBOX, configuration,
        // Unrar.PROPERTY_ENABLED_TYPE,new String[] {
        // Unrar.ENABLED_TYPE_ALWAYS,Unrar.ENABLED_TYPE_LINKGRABBER,Unrar.ENABLED_TYPE_NEVER
        // },"Unrar
        // aktivieren:").setDefaultValue(Unrar.ENABLED_TYPE_LINKGRABBER));
        ce = new GUIConfigEntry(new ConfigEntry(ConfigContainer.TYPE_COMBOBOX, configuration, Unrar.PROPERTY_ENABLED_TYPE, new String[]{Unrar.ENABLED_TYPE_ALWAYS, Unrar.ENABLED_TYPE_LINKGRABBER, Unrar.ENABLED_TYPE_NEVER}, JDLocale.L("gui.config.unrar.activate")).setDefaultValue(Unrar.ENABLED_TYPE_LINKGRABBER));
        addGUIConfigEntry(ce);
        if(unrarcmd==null)
        {

            try {
                JLinkButton bb = new JLinkButton(JDLocale.L("gui.config.unrar.download", "Bitte laden sie Unrar herunter"), new URL("http://www.rarlab.com/rar_add.htm"));
                JDUtilities.addToGridBag(panel, bb, GridBagConstraints.RELATIVE, GridBagConstraints.RELATIVE, GridBagConstraints.REMAINDER, 1, 0, 0, null, GridBagConstraints.NONE, GridBagConstraints.NORTHWEST);
            } catch (MalformedURLException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

        }
        ce = new GUIConfigEntry(new ConfigEntry(ConfigContainer.TYPE_BROWSEFILE, configuration, Unrar.PROPERTY_UNRARCOMMAND, JDLocale.L("gui.config.unrar.cmd")).setDefaultValue(unrarcmd));
        addGUIConfigEntry(ce);
        ce = new GUIConfigEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, configuration, Unrar.PROPERTY_AUTODELETE, JDLocale.L("gui.config.unrar.delete")).setDefaultValue(true));
        addGUIConfigEntry(ce);
        ce = new GUIConfigEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, configuration, Unrar.PROPERTY_OVERWRITE_FILES, JDLocale.L("gui.config.unrar.overwrite")).setDefaultValue(false));
        addGUIConfigEntry(ce);
        ce = new GUIConfigEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, configuration, Unrar.PROPERTY_WAIT_FOR_TERMINATION, JDLocale.L("gui.config.unrar.thread", "Erst nach dem Entpacken mit dem Download fortfahren")).setDefaultValue(false).setExpertEntry(true));
        addGUIConfigEntry(ce);
        ce = new GUIConfigEntry(new ConfigEntry(ConfigContainer.TYPE_BROWSEFOLDER, configuration, Unrar.PROPERTY_EXTRACTFOLDER, JDLocale.L("gui.config.unrar.extractfolder", "Zielordner: ")).setDefaultValue(null).setExpertEntry(true));
        addGUIConfigEntry(ce);
        ce = new GUIConfigEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, configuration, Unrar.PROPERTY_ENABLE_EXTRACTFOLDER, JDLocale.L("gui.config.unrar.extractfolderenabled", "Im Zielordner entpacken (Wenn die Dateien nicht im Downloadordner entpackt werden sollen)")).setDefaultValue(false).setExpertEntry(true));
        addGUIConfigEntry(ce);
        ce = new GUIConfigEntry(new ConfigEntry(ConfigContainer.TYPE_BUTTON, this, JDLocale.L("gui.menu.action.passwordlist.name")));
        addGUIConfigEntry(ce);

        add(panel, BorderLayout.NORTH);

    }

    @Override
    public String getName() {

        return JDLocale.L("modules.unrar.name");
    }

    public void actionPerformed(ActionEvent e) {
        new jdUnrarPasswordListDialog(((SimpleGUI) this.uiinterface).getFrame()).setVisible(true);
    }
}
