package jd.gui.skins.simple.config;

import javax.swing.JComponent;

import jd.Configuration;

public class ConfigPanelAutomatic extends JComponent{

    /**
     * serialVersionUID
     */
    private static final long serialVersionUID = 7055395315659682282L;

    private Configuration configuration;
    
    public ConfigPanelAutomatic(Configuration configuration){
        this.configuration = configuration;
    }
}
