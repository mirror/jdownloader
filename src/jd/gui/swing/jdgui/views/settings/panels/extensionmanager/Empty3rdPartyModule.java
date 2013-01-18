package jd.gui.swing.jdgui.views.settings.panels.extensionmanager;

import org.jdownloader.gui.translate._GUI;

public class Empty3rdPartyModule extends ThirdPartyModule {

    public Empty3rdPartyModule() {

    }

    @Override
    protected String getTitle() {
        return _GUI._.Empty3rdPartyModule_getTitle_();
    }

    @Override
    protected String getIconKey() {
        return "question";
    }

    @Override
    protected String getDescription() {
        return _GUI._.Empty3rdPartyModule_getDescription_();
    }

}
