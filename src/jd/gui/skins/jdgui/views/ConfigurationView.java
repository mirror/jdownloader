package jd.gui.skins.jdgui.views;

import javax.swing.Icon;

import jd.gui.skins.jdgui.interfaces.View;
import jd.gui.skins.jdgui.views.sidebars.configuration.ConfigSidebar;
import jd.utils.JDTheme;
import jd.utils.locale.JDL;

public class ConfigurationView extends View {

    private static final long serialVersionUID = -5607304856678049342L;

    /**
     * DO NOT MOVE THIS CONSTANT. IT's important to have it in this file for the
     * LFE to parse JDL Keys correct
     */
    private static final String IDENT_PREFIX = "jd.gui.skins.jdgui.views.configurationview.";

    public ConfigurationView() {
        super();

        this.setSideBar(new ConfigSidebar(this));

     
    }

    @Override
    public Icon getIcon() {
        return JDTheme.II("gui.images.taskpanes.configuration", ICON_SIZE, ICON_SIZE);
    }

    @Override
    public String getTitle() {
        return JDL.L(IDENT_PREFIX + "tab.title", "Settings");
    }

    @Override
    public String getTooltip() {
        return JDL.L(IDENT_PREFIX + "tab.tooltip", "All options and settings for JDownloader");
    }

    @Override
    protected void onHide() {
        // TODO Auto-generated method stub
        
    }

    @Override
    protected void onShow() {
        // TODO Auto-generated method stub
        
    }

}
