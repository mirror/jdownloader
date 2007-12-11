package jd.gui.skins.simple.config;

import java.awt.BorderLayout;
import java.util.Locale;
import java.util.logging.Level;

import javax.swing.JLabel;

import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.config.Configuration;
import jd.controlling.interaction.InfoFileWriter;
import jd.event.UIEvent;
import jd.gui.UIInterface;
import jd.gui.skins.simple.components.BrowseFile;
import jd.utils.JDUtilities;

public class ConfigPanelInfoFileWriter extends ConfigPanel {
    /**
     * serialVersionUID
     */
    private static final long serialVersionUID = 3383448498625377495L;
    private JLabel            lblHomeDir;
    private BrowseFile        brsHomeDir;
    private Configuration     configuration;
    private InfoFileWriter fileWriter;
    ConfigPanelInfoFileWriter(Configuration configuration, UIInterface uiinterface) {
        super(uiinterface);
        this.configuration = configuration;
        fileWriter= InfoFileWriter.getInstance();
        initPanel();
        load();
    }
    public void save() {
        this.saveConfigEntries();
        
    }
    @Override
    public void initPanel() {
        GUIConfigEntry ce;
        for(int i=0; i<fileWriter.getConfig().getEntries().size();i++){
            ce = new GUIConfigEntry(fileWriter.getConfig().getEntries().get(i));
            addGUIConfigEntry(ce);
        }
       
        add(panel, BorderLayout.NORTH);
    }
    @Override
    public void load() {
        this.loadConfigEntries();
    }
    @Override
    public String getName() {
        return "Info Datei(inactive)";
    }
}
