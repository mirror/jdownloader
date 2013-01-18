package jd.gui.swing.jdgui.views.settings.panels.extensionmanager.modules;

public class GenericExtensionModule extends ExtensionModule {

    public GenericExtensionModule(String s) {
        super(s);

    }

    @Override
    protected String getTitle() {
        return getID() + "-Extension";
    }

    @Override
    protected String getIconKey() {
        return "settings";
    }

    @Override
    protected String getDescription() {
        return getID() + " Extension -  No further description available!";
    }

}
