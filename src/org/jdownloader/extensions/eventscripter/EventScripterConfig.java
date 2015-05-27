package org.jdownloader.extensions.eventscripter;

import java.util.ArrayList;

import jd.plugins.ExtensionConfigInterface;

import org.appwork.storage.config.annotations.AboutConfig;
import org.appwork.storage.config.annotations.AllowStorage;

public interface EventScripterConfig extends ExtensionConfigInterface {

    @AboutConfig
    @AllowStorage(Object.class)
    ArrayList<ScriptEntry> getScripts();

    @AllowStorage(Object.class)
    void setScripts(ArrayList<ScriptEntry> entries);

    @AboutConfig
    boolean isAPIPanelVisible();

    void setAPIPanelVisible(boolean b);

}
