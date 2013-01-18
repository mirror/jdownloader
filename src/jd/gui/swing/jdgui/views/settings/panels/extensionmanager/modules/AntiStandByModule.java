package jd.gui.swing.jdgui.views.settings.panels.extensionmanager.modules;

public class AntiStandByModule extends ExtensionModule {
    public AntiStandByModule() {
        super("antistandby");
    }

    @Override
    protected String getTitle() {
        return "Anti Standby - No standby while downloading";
    }

    @Override
    protected String getIconKey() {
        return "cancel";
    }

    @Override
    protected String getDescription() {
        return "This extension helps you to avoid the System to shut down or go into idle/standby mode while downloading.";
    }
}
