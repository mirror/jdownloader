package org.jdownloader.extensions.scriptlauncher.translate;
import org.appwork.txtresource.*;
@Defaults(lngs = { "en"})
public interface ScriptlauncherTranslation extends TranslateInterface {

@Default(lngs = { "en" }, values = { "No scripts were found." })
String plugins_optional_JDScriptLauncher_noscripts();
@Default(lngs = { "en" }, values = { "Script Launcher" })
String jd_plugins_optional_scriptlauncher_jdscriptlauncher();
@Default(lngs = { "en" }, values = { "Allows you to execute shell/bash scripts or other executable files out of the menu bar.\r\nAll scripts or symbolic links must be placed in the \"scripts\"-folder in JD's root." })
String jd_plugins_optional_scriptlauncher_jdscriptlauncher_description();
}