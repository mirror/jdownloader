package jd.gui.skins.jdgui.views;

import javax.swing.Icon;

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
//        ConfigTaskPane cfgTskPane;
//        this.setSideBar(cfgTskPane = new ConfigTaskPane(JDL.L("gui.taskpanes.configuration", "Configuration"), JDTheme.II("gui.images.taskpanes.configuration", 16, 16)));

//        Object[] configConstructorObjects = new Object[] { JDUtilities.getConfiguration() };
//
//        cfgTskPane.addPanelAt(ConfigTaskPane.ACTION_ADDONS, new SingletonPanel(ConfigPanelAddons.class, configConstructorObjects));
//        cfgTskPane.addPanelAt(ConfigTaskPane.ACTION_CAPTCHA, new SingletonPanel(ConfigPanelCaptcha.class, configConstructorObjects));
//        cfgTskPane.addPanelAt(ConfigTaskPane.ACTION_DOWNLOAD, new SingletonPanel(ConfigPanelDownload.class, configConstructorObjects));
//        cfgTskPane.addPanelAt(ConfigTaskPane.ACTION_EVENTMANAGER, new SingletonPanel(ConfigPanelEventmanager.class, configConstructorObjects));
//        cfgTskPane.addPanelAt(ConfigTaskPane.ACTION_GENERAL, new SingletonPanel(ConfigPanelGeneral.class, configConstructorObjects));
//        cfgTskPane.addPanelAt(ConfigTaskPane.ACTION_GUI, new SingletonPanel(ConfigPanelGUI.class, configConstructorObjects));
//        cfgTskPane.addPanelAt(ConfigTaskPane.ACTION_HOST, new SingletonPanel(ConfigPanelPluginForHost.class, configConstructorObjects));
//        cfgTskPane.addPanelAt(ConfigTaskPane.ACTION_RECONNECT, new SingletonPanel(ConfigPanelReconnect.class, configConstructorObjects));
//
//        cfgTskPane.addActionListener(new ActionListener() {
//
//            public void actionPerformed(final ActionEvent e) {
//
//                switch (e.getID()) {
//                case DownloadTaskPane.ACTION_CLICK:
//
//                    setContent(((TaskPanel) e.getSource()).getPanel(SimpleGuiConstants.GUI_CONFIG.getIntegerProperty("LAST_CONFIG_PANEL", ConfigTaskPane.ACTION_GENERAL)));
//
//                    break;
//                case ConfigTaskPane.ACTION_SAVE:
//                    boolean restart = false;
//
//                    for (SingletonPanel panel : ((ConfigTaskPane) e.getSource()).getPanels()) {
//
//                        if (panel != null && panel.getPanel() != null && panel.getPanel() instanceof ConfigPanel) {
//                            if (((ConfigPanel) panel.getPanel()).hasChanges() == PropertyType.NEEDS_RESTART) restart = true;
//                            ((ConfigPanel) panel.getPanel()).save();
//                        }
//                    }
//
//                    if (restart) {
//                        if (JDUtilities.getGUI().showConfirmDialog(JDL.L("gui.config.save.restart", "Your changes need a restart of JDownloader to take effect.\r\nRestart now?"), JDL.L("gui.config.save.restart.title", "JDownloader restart requested"))) {
//                            JDUtilities.restartJD();
//                        }
//                    }
//                    break;
//
//                case ConfigTaskPane.ACTION_ADDONS:
//                case ConfigTaskPane.ACTION_CAPTCHA:
//                case ConfigTaskPane.ACTION_DOWNLOAD:
//                case ConfigTaskPane.ACTION_EVENTMANAGER:
//                case ConfigTaskPane.ACTION_GENERAL:
//                case ConfigTaskPane.ACTION_GUI:
//                case ConfigTaskPane.ACTION_HOST:
//                case ConfigTaskPane.ACTION_RECONNECT:
//                    SimpleGuiConstants.GUI_CONFIG.setProperty("LAST_CONFIG_PANEL", e.getID());
//
//                    setContent(((ConfigTaskPane) e.getSource()).getPanel(e.getID()));
//                    SimpleGuiConstants.GUI_CONFIG.save();
//                    break;
//                }
//
//            }
//        });
//        setContent(cfgTskPane.getPanel(SimpleGuiConstants.GUI_CONFIG.getIntegerProperty("LAST_CONFIG_PANEL", ConfigTaskPane.ACTION_GENERAL)));

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
