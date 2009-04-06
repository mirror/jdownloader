package jd.config;

import javax.swing.ImageIcon;

public class ConfigGroup {

    private ImageIcon icon;
    private String name;
    public ConfigGroup(String name, ImageIcon icon) {
        this.name=name;
        this.icon=icon;
     }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public ImageIcon getIcon() {
        return icon;
    }

    public void setIcon(ImageIcon icon) {
        this.icon = icon;
    }

   
    

}
