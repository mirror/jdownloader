package jd.gui.skins.simple.config;

import java.awt.BorderLayout;

import jd.config.Configuration;
import jd.controlling.interaction.InfoFileWriter;
import jd.gui.UIInterface;
import jd.utils.JDLocale;

public class ConfigPanelInfoFileWriter extends ConfigPanel {
    /**
     * serialVersionUID
     */
    private static final long serialVersionUID = 3383448498625377495L;
   // private JLabel            lblHomeDir;
   // private BrowseFile        brsHomeDir;
   // private Configuration     configuration;
    private InfoFileWriter fileWriter;
    public ConfigPanelInfoFileWriter(Configuration configuration, UIInterface uiinterface) {
        super(uiinterface);
       // this.configuration = configuration;
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
        return JDLocale.L("gui.config.infoFileWriter.name","Info Datei");
    }
}
