package org.jdownloader.extensions.eventscripter;

import java.util.ArrayList;

import jd.plugins.ExtensionConfigInterface;

import org.appwork.storage.config.annotations.AboutConfig;

public interface EventScripterConfig extends ExtensionConfigInterface {

    @AboutConfig
    ArrayList<ScriptEntry> getScripts();

    void setScripts(ArrayList<ScriptEntry> entries);

    @AboutConfig
    boolean isAPIPanelVisible();

    void setAPIPanelVisible(boolean b);

}
