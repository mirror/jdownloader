package jd.gui.skins.simple.config;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Vector;
import java.util.logging.Logger;

import jd.Configuration;
import jd.gui.UIInterface;
import jd.plugins.Plugin;
import jd.plugins.PluginConfig;
import jd.plugins.PluginConfigEntry;

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

        Vector<PluginConfigEntry> entries = plugin.getConfig().getEntries();
        PluginConfigEntry entry;
        for (int i = 0; i < entries.size(); i++) {
            entry = entries.elementAt(i);

            ConfigEntry ce = null;

            switch (entry.getType()) {
                case PluginConfig.TYPE_BUTTON:
                    ce = new ConfigEntry(entry.getType(), entry.getActionListener(), entry.getLabel());
                    break;
                case PluginConfig.TYPE_RADIOFIELD:
                case PluginConfig.TYPE_COMBOBOX:
                    ce = new ConfigEntry(entry.getType(), entry.getPropertyInstance(), entry.getPropertyName(), entry.getList(), entry.getLabel());
                    ce.setDefaultText(entry.getDefaultValue());

                    break;
                    
                case PluginConfig.TYPE_SPINNER:
                    ce = new ConfigEntry(entry.getType(), entry.getPropertyInstance(), entry.getPropertyName(), entry.getLabel(),entry.getStart(), entry.getEnd());
                    ce.setSteps(entry.getStep());
                    ce.setDefaultText(entry.getDefaultValue());

                    break; 
                case PluginConfig.TYPE_LABEL:
                    ce = new ConfigEntry(entry.getType(), entry.getLabel());
                    break;
                case PluginConfig.TYPE_SEPERATOR:
                    ce = new ConfigEntry(entry.getType());
                    break;
                case PluginConfig.TYPE_CHECKBOX:
                case PluginConfig.TYPE_TEXTFIELD:

                    ce = new ConfigEntry(entry.getType(), entry.getPropertyInstance(), entry.getPropertyName(), entry.getLabel());
                    ce.setDefaultText(entry.getDefaultValue());
                    break;

            }

            if (ce != null) addConfigEntry(ce);

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
