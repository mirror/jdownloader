package jd.gui.swing.jdgui.views;

import javax.swing.Icon;

import jd.config.ConfigEntry.PropertyType;
import jd.gui.swing.jdgui.MainTabbedPane;
import jd.gui.swing.jdgui.interfaces.SwitchPanel;
import jd.gui.swing.jdgui.interfaces.View;
import jd.gui.swing.jdgui.settings.ConfigPanel;
import jd.gui.swing.jdgui.views.sidebars.configuration.ConfigSidebar;
import jd.utils.JDTheme;
import jd.utils.locale.JDL;

public class ConfigurationView extends View {

    private static final long serialVersionUID = -5607304856678049342L;

    /**
     * DO NOT MOVE THIS CONSTANT. IT's important to have it in this file for the
     * LFE to parse JDL Keys correct
     */
    private static final String IDENT_PREFIX = "jd.gui.swing.jdgui.views.configurationview.";

    private Thread changethread;

    public ConfigurationView() {
        super();
        sidebar.setBorder(null);
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
        MainTabbedPane.getInstance().setChanged(false, -1);
        if (changethread != null) changethread.interrupt();
    }

    @Override
    protected void onShow() {
        if (changethread != null) changethread.interrupt();
        changethread = new Thread() {
            public void run() {
                while (true) {
                    SwitchPanel panel = getContent();
                    if (panel instanceof ConfigPanel) {
                        PropertyType changes = ((ConfigPanel) panel).hasChanges();
                        if (changes != PropertyType.NONE) {
                            MainTabbedPane.getInstance().setChanged(true, -1);
                        } else {
                            MainTabbedPane.getInstance().setChanged(false, -1);
                        }
                    }

                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                        break;
                    }
                }

            }
        };
        changethread.start();

    }

}
