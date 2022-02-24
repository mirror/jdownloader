package org.jdownloader.extensions.eventscripter;

import java.util.ArrayList;

import org.appwork.remoteapi.annotations.AllowNonStorableObjects;
import org.appwork.storage.config.annotations.AboutConfig;

import jd.plugins.ExtensionConfigInterface;

public interface EventScripterConfig extends ExtensionConfigInterface {
    @AboutConfig
    @AllowNonStorableObjects
    ArrayList<ScriptEntry> getScripts();

    @AllowNonStorableObjects
    void setScripts(ArrayList<ScriptEntry> entries);

    @AboutConfig
    boolean isAPIPanelVisible();

    void setAPIPanelVisible(boolean b);
}
