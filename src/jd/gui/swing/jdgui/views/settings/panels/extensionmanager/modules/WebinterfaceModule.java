package jd.gui.swing.jdgui.views.settings.panels.extensionmanager.modules;

public class WebinterfaceModule extends ExtensionModule {
    public WebinterfaceModule() {
        super("webinterface");
    }

    @Override
    protected String getTitle() {
        return "Webinterface - A Remote Control";
    }

    @Override
    protected String getIconKey() {
        return "bandwidth";
    }

    @Override
    protected String getDescription() {
        return "A Webinterface to controll JDownloader remotely using your browser";
    }
}
