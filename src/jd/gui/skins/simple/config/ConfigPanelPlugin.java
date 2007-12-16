package jd.gui.skins.simple.config;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Vector;
import java.util.logging.Logger;

import jd.config.ConfigEntry;
import jd.config.Configuration;
import jd.gui.UIInterface;
import jd.plugins.Plugin;
import jd.utils.JDLocale;
import jd.utils.JDUtilities;

public class ConfigPanelPlugin extends ConfigPanel implements ActionListener {
    /**
     * 
     */
    private static final long serialVersionUID = -7983057329558110899L;

    /**
     * serialVersionUID
     */
    @SuppressWarnings("unused")
    private Logger            logger           = JDUtilities.getLogger();

    private Plugin            plugin;

    ConfigPanelPlugin(Configuration configuration, UIInterface uiinterface, Plugin plugin) {
        super(uiinterface);
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
            ce = new GUIConfigEntry(entry);
            if (ce != null) addGUIConfigEntry(ce);

           
        }
        add(panel, BorderLayout.CENTER);
    }

    @Override
    public String getName() {

        return JDLocale.L("gui.config.plugin.defaultName","Plugin Konfiguration");
    }

    @Override
    public void load() {
        loadConfigEntries();

    }

}
