package jd.gui.skins.simple.config;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Vector;
import java.util.logging.Logger;

import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.config.Configuration;
import jd.gui.UIInterface;
import jd.plugins.Plugin;

public class ConfigPanelPlugin extends ConfigPanel implements ActionListener {
    /**
     * 
     */
    private static final long serialVersionUID = -7983057329558110899L;

    /**
     * serialVersionUID
     */
    @SuppressWarnings("unused")
    private Logger            logger           = Plugin.getLogger();

    private Plugin            plugin;

    ConfigPanelPlugin(Configuration configuration, UIInterface uiinterface, Plugin plugin) {
        super(configuration, uiinterface);
        this.plugin = plugin;
        initPanel();

        load();
    }

    public void save() {
        this.saveConfigEntries();
    }

    public void actionPerformed(ActionEvent e) {

    }

    @Override
    public void initPanel() {

        Vector<ConfigEntry> entries = plugin.getConfig().getEntries();
        ConfigEntry entry;
        for (int i = 0; i < entries.size(); i++) {
            entry = entries.elementAt(i);

            GUIConfigEntry ce = null;

            switch (entry.getType()) {
                case ConfigContainer.TYPE_BUTTON:
                    ce = new GUIConfigEntry(new ConfigEntry(entry.getType(), entry.getActionListener(), entry.getLabel()));
                    break;
                case ConfigContainer.TYPE_RADIOFIELD:
                case ConfigContainer.TYPE_COMBOBOX:
                    ce = new GUIConfigEntry(new ConfigEntry(entry.getType(), entry.getPropertyInstance(), entry.getPropertyName(), entry.getList(), entry.getLabel()).setDefaultValue(entry.getDefaultValue()));

                    break;

                case ConfigContainer.TYPE_SPINNER:
                    ce = new GUIConfigEntry(new ConfigEntry(entry.getType(), entry.getPropertyInstance(), entry.getPropertyName(), entry.getLabel(), entry.getStart(), entry.getEnd()).setStep(entry.getStep()).setDefaultValue(entry.getDefaultValue()));

                    break;
                case ConfigContainer.TYPE_LABEL:
                    ce = new GUIConfigEntry(new ConfigEntry(entry.getType(), entry.getLabel()));
                    break;
                case ConfigContainer.TYPE_SEPERATOR:
                    ce = new GUIConfigEntry(new ConfigEntry(entry.getType()));
                    break;
                case ConfigContainer.TYPE_CHECKBOX:
                case ConfigContainer.TYPE_TEXTFIELD:

                    ce = new GUIConfigEntry(new ConfigEntry(entry.getType(), entry.getPropertyInstance(), entry.getPropertyName(), entry.getLabel()).setDefaultValue(entry.getDefaultValue()));

                    break;

            }

            if (ce != null) addGUIConfigEntry(ce);

            add(panel, BorderLayout.CENTER);
        }

    }

    @Override
    public String getName() {

        return "Dummy Konfiguration";
    }

    @Override
    public void load() {
        loadConfigEntries();

    }

}
