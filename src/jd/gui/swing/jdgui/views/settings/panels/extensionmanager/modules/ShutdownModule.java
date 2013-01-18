package jd.gui.swing.jdgui.views.settings.panels.extensionmanager.modules;

public class ShutdownModule extends ExtensionModule {
    public ShutdownModule() {
        super("shutdown");
    }

    @Override
    protected String getTitle() {
        return "Shutdown - Shutdown your PC after Download";
    }

    @Override
    protected String getIconKey() {
        return "logout";
    }

    @Override
    protected String getDescription() {
        return "This Extension helps you to schedule a System Shutdown after your downloads finished";
    }
}
